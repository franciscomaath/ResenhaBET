# Competition Model — Design & Refactoring Plan

## Contexto

Hoje a sincronização REAL_FOOTBALL depende de `league_id`/`country_id`
hardcoded em `application.properties`, desacoplados do `Tournament.name`
(texto livre). Isso impede rodar mais de um campeonato real simultaneamente
e exige redeploy para adicionar um novo campeonato.

Este documento propõe a entidade `Competition` como catálogo de competições
externas, e o plano de refatoração dos serviços existentes para usá-la.

---

## 1. Modelo de Dados

### Entidade `Competition`

| Coluna | Tipo | Constraints | Notas |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `uuid` | UUID | UNIQUE, NOT NULL | padrão já usado em Tournament |
| `name` | VARCHAR(255) | NOT NULL | ex: "Copa do Mundo 2026" |
| `season` | VARCHAR(20) | NOT NULL | ex: "2026" |
| `api_football_league_id` | VARCHAR(20) | NOT NULL | ID da liga na APIfootball |
| `api_football_country_id` | VARCHAR(20) | NOT NULL | ID do país na APIfootball |
| `game_forecast_league_id` | VARCHAR(20) | NOT NULL | ID da liga na GameForecastAPI (independente do anterior) |
| `active` | BOOLEAN | NOT NULL, default `true` | permite desativar sem deletar (preserva histórico) |
| `created_at` | TIMESTAMP | NOT NULL | |

**Unique constraint:** `UNIQUE(api_football_league_id, api_football_country_id, season)`
— evita cadastrar a mesma competição/temporada duas vezes.

### Alteração em `Tournament`

| Coluna | Tipo | Constraints | Notas |
|---|---|---|---|
| `competition_id` | BIGINT | FK → competition, **NULLABLE** | `NULL` para `FIFA_MATCH`; obrigatório (validado em service) para `REAL_FOOTBALL` |

### Relacionamento

```
competition 1──N tournament
```

Uma `Competition` pode ter múltiplos `Tournament`s ao longo do tempo
(ex: re-fazer o torneio com o mesmo grupo de amigos numa segunda rodada),
mas cada `Tournament` REAL_FOOTBALL aponta para exatamente uma `Competition`.

### Migration

```sql
-- V33__CREATE_COMPETITION_TABLE.sql
CREATE TABLE resenha.competition (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE,
    name                        VARCHAR(255) NOT NULL,
    season                      VARCHAR(20) NOT NULL,
    api_football_league_id     VARCHAR(20) NOT NULL,
    api_football_country_id    VARCHAR(20) NOT NULL,
    game_forecast_league_id    VARCHAR(20) NOT NULL,
    active                      BOOLEAN NOT NULL DEFAULT true,
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_competition_external
        UNIQUE (api_football_league_id, api_football_country_id, season)
);

-- V34__ADD_COMPETITION_TO_TOURNAMENT.sql
ALTER TABLE resenha.tournament
    ADD COLUMN competition_id BIGINT REFERENCES resenha.competition(id);

-- Seed inicial (opcional, facilita o primeiro uso)
INSERT INTO resenha.competition
    (uuid, name, season, api_football_league_id, api_football_country_id, game_forecast_league_id)
VALUES
    (gen_random_uuid(), 'Copa do Mundo 2026', '2026', '28', '8', '149');
```

---

## 2. Como se alinha ao plano REAL_FOOTBALL

| Item do briefing/overview atual | Como muda com `Competition` |
|---|---|
| `ApiFootballProperties.copaLeagueId/copaCountryId` (hardcoded, global) | Removido do properties. Lido de `tournament.getCompetition()` em runtime. |
| `GameForecastProperties.copaLeagueId` (hardcoded, global) | Idem — vem de `competition.getGameForecastLeagueId()`. |
| `TournamentServiceImpl.create()` hardcoda `FIFA_MATCH` (bug já identificado) | Corrigido **junto** desta refatoração: `type` passa a ser lido do DTO, e se `REAL_FOOTBALL`, `competitionId` se torna obrigatório. |
| `TournamentRequestDTO` sem campo `type`/`marketTypes` (gap já identificado) | Ganha `type`, `marketTypes` **e** `competitionId` no mesmo PR — os três gaps fazem parte da mesma correção de criação de torneio. |
| `FixtureSyncServiceImpl.sync()` lê IDs do properties | Passa a receber `tournament.getCompetition()` e ler os IDs de lá. |
| `OddsImportServiceImpl.importForTournament()` lê `GameForecastProperties.copaLeagueId` | Mesma troca — lê de `competition.getGameForecastLeagueId()`. |
| Só é possível 1 campeonato real por instância | Múltiplas `Competition`s cadastradas, múltiplos `Tournament`s REAL_FOOTBALL coexistindo, cada um sincronizando sua própria liga. |
| Cadastro de novo campeonato exige editar properties + redeploy | Vira um `POST /api/v1/competitions` — sem deploy. |

**Importante:** essa mudança não quebra nada do que já existe — `FIFA_MATCH`
continua com `competition_id = NULL` e segue seu fluxo normal.

---

## 3. Plano de Refatoração

### Fase A — Mínimo viável

**Backend — novos artefatos:**
1. `Competition` entity + `CompetitionRepository`
2. `CompetitionRequestDTO` / `CompetitionResponseDTO` + `CompetitionMapper`
3. `CompetitionController` + `CompetitionServiceImpl`:
   - `POST /api/v1/competitions` (admin only)
   - `GET /api/v1/competitions` (lista, filtra por `active=true` por padrão)
   - `GET /api/v1/competitions/{id}`
   - `PATCH /api/v1/competitions/{id}` (ex: desativar)

**Backend — correção do fluxo de criação de torneio (já era débito conhecido):**
4. `TournamentRequestDTO` ganha `type`, `marketTypes`, `competitionId`
5. `TournamentServiceImpl.create()`:
   - Lê `type` do DTO em vez de hardcodar `FIFA_MATCH`
   - Se `type == REAL_FOOTBALL`: valida `competitionId` não nulo, busca a `Competition`, associa ao `Tournament`
   - Se `type == FIFA_MATCH`: `competitionId` deve ser nulo (validar e rejeitar se vier preenchido)
   - Persiste `marketTypes` selecionados (hoje ignorados — precisa de uma tabela `tournament_market_type` ou coluna JSON/array, a decidir)

**Backend — desacoplamento dos clients externos:**
6. `FixtureSyncServiceImpl.sync()`:
   - Remove leitura de `ApiFootballProperties.copaLeagueId/copaCountryId`
   - Passa a usar `tournament.getCompetition().getApiFootballLeagueId()` / `getApiFootballCountryId()`
7. `OddsImportServiceImpl.importForTournament()`:
   - Remove leitura de `GameForecastProperties.copaLeagueId`
   - Usa `tournament.getCompetition().getGameForecastLeagueId()`
8. `ApiFootballProperties` / `GameForecastProperties` mantêm apenas `key`, `base-url` e config técnica (sem IDs de liga)

**Frontend:**
9. `CompetitionsApi` service (novo) — `findAll()`, `create()`
10. `HomePage` — modal de criação de torneio REAL_FOOTBALL ganha um **dropdown de Competition** (substituindo qualquer ideia de digitar IDs manualmente). Lista vem de `GET /competitions?active=true`.
11. Tela simples de cadastro de Competition (pode ser uma página `/admin/competitions` ou até um modal dentro da HomePage para o admin avançado — a decidir conforme prioridade)

**Migrations:** V33, V34 conforme seção 1.

### Fase B — Descoberta automática (não bloqueante)

12. `ApiFootballClient.fetchLeagues()` — novo método chamando `get_leagues`
13. Endpoint `GET /api/v1/competitions/discover?country=...` — busca ligas disponíveis na APIfootball para o admin escolher visualmente, em vez de digitar `league_id`/`country_id` manualmente
14. Tela de "importar competição" — busca por nome, mostra resultados, admin confirma e o sistema já cria a `Competition` com os IDs corretos
15. **Limitação conhecida:** `game_forecast_league_id` precisa ser mapeado manualmente mesmo na Fase B, pois é um ID de um provedor diferente e não há correlação automática entre as duas APIs (mesmo problema já documentado do matching por nome de time)

---

## 4. Itens que ficam resolvidos de tabela com essa refatoração

Esta mudança aproveita o mesmo PR para fechar três débitos já catalogados
no `REAL_FOOTBALL_TECHNICAL_OVERVIEW.md`:

- ✅ `TournamentRequestDTO` sem campo `type` (bloqueador crítico)
- ✅ `TournamentServiceImpl.create()` hardcoda `FIFA_MATCH`
- ✅ `marketTypes` do frontend sendo ignorado

E adiciona a capacidade nova de múltiplos campeonatos sem redeploy, que
não estava no escopo original do briefing.

---

## 5. O que esta refatoração **não** resolve (continua como débito separado)

- `RealFootballScheduler` não implementado — continua pendente, não relacionado a este modelo
- Idempotência do `sync-fixtures` (duplicação de eventos em re-sync)
- Matching de nomes de times entre APIfootball e GameForecastAPI
- API keys em texto plano no properties
- `Team.abbreviation` com anotação `unique=true` divergente do schema real

Esses seguem como itens independentes do plano de Group 12 / cleanup já
documentado no handoff.
