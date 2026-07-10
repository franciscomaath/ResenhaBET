# ResenhaBET v2 — Real Football Support (Copa do Mundo)
## Feature Group Design Document

---

## 1. Visão Geral

Este documento descreve o design completo para suporte a **torneios de futebol real**
(Copa do Mundo 2026) no ResenhaBET v2. É um documento de referência para implementação —
leia-o integralmente antes de começar qualquer tarefa.

### O que muda

| Área | Situação atual | Com Real Football |
|---|---|---|
| `TournamentType` | `FIFA_MATCH` | + `REAL_FOOTBALL` |
| `Event` | `playerHomeId` / `playerAwayId` obrigatórios | + `teamHomeId` / `teamAwayId` para torneios reais |
| Mercados por evento | 1 (implícito) | N (múltiplos, com `MarketType`) |
| Criação de eventos | Admin manual | Automático via APIfootball fixture sync |
| Placar | Admin manual | Automático via live polling |
| Odds | `OddsCalculator` interno (Elo) | GameForecastAPI (importadas) |
| Tipos de mercado | Somente resultado (1X2) | Resultado, Over/Under, BTTS, Placar Exato |

### O que NÃO muda

- `BetSlip` / `BetSlipItem` — estrutura idêntica, apenas `outcomeId` aponta para mercados diferentes
- `Wallet` / `Transaction` — sem alteração
- `Market` status flow: `OPEN → SUSPENDED → CLOSED`
- Lógica de suspensão por `suspendAt` (MarketScheduler existente)
- WebSocket broadcast de scores e mercados

---

## 2. APIs Externas

### 2.1 APIfootball

- **Base URL:** `https://apiv3.apifootball.com/`
- **Auth:** query parameter `&APIkey={key}` em todas as requisições
- **Limite:** conforme plano contratado (free: 100 req/dia)

Todos os endpoints seguem o padrão `GET https://apiv3.apifootball.com/?action={nome}&APIkey={key}&...`

**Endpoints utilizados:**

| Endpoint (`action=`) | Uso | Frequência |
|---|---|---|
| `get_leagues` | Descobrir `league_id` da Copa do Mundo | 1x setup |
| `get_events` + `league_id` + `from`/`to` | Sync inicial de todos os jogos da Copa | 1x ao criar torneio |
| `get_events` + `match_live=1` + `league_id` | Poll de placares ao vivo | A cada 60s se houver jogo em andamento |


**Exemplo de request (buscar eventos da Copa):**
```
GET https://apiv3.apifootball.com/?action=get_events
    &league_id={COPA_LEAGUE_ID}
    &from=2026-06-01
    &to=2026-07-20
    &APIkey=xxxxxxxxxxxxxx
```

> **Nota:** O `league_id` exato da Copa do Mundo 2026 deve ser descoberto via
> `action=get_leagues` antes da implementação. Não hardcodar sem verificar.

**Campos relevantes do response de `get_events`:**

```json
{
  "match_id": "112282",
  "league_id": "...",
  "league_name": "...",
  "match_date": "2026-06-15",
  "match_time": "21:00",
  "match_status": "Finished",
  "match_hometeam_id": "3",
  "match_hometeam_name": "Brazil",
  "match_hometeam_score": "2",
  "match_awayteam_id": "16",
  "match_awayteam_name": "Argentina",
  "match_awayteam_score": "1",
  "match_hometeam_halftime_score": "1",
  "match_awayteam_halftime_score": "0",
  "match_hometeam_extra_score": "",
  "match_awayteam_extra_score": "",
  "match_hometeam_penalty_score": "",
  "match_awayteam_penalty_score": "",
  "match_live": "0",
  "match_round": "Group A",
  "fk_stage_key": "...",
  "stage_name": "Group Stage"
}
```

**Status de partida mapeados → ações do ResenhaBET:**

| `match_status` (APIfootball) | Ação |
|---|---|
| `""` / `"Not Started"` | Nenhuma — evento permanece `CREATED` |
| `"13'"` (minuto em jogo), `"Half Time"` | Iniciar evento se `CREATED`, atualizar score |
| `"Finished"` | Encerrar evento automaticamente (tempo normal) |
| `"After ET"` | Encerrar evento (após prorrogação) |
| `"After Pen."` | Encerrar evento (após pênaltis) |
| `"Postponed"` | Cancelar evento (bets devolvidas) |
| `"Cancelled"` | Cancelar evento (bets devolvidas) |

> Para detectar "jogo em andamento": verificar se `match_status` contém `"'"` (minuto),
> ou é igual a `"Half Time"`. Não é um campo booleano — é uma string variável.

### 2.2 GameForecastAPI

- **Base URL:** `https://game-forecast-api.p.rapidapi.com`
- **Auth:** headers RapidAPI (`X-RapidAPI-Key` e `X-RapidAPI-Host: game-forecast-api.p.rapidapi.com`)
- **Planos:**

| Plano | Preço | Limite | Rate limit |
|---|---|---|---|
| Basic (free) | $0 | **10 req/dia** | 10 req/hora |
| Pro | $19/mês | 5.000 req/mês | 120 req/min |
| Ultra | $74/mês | 50.000 req/mês | 300 req/min |

> **Impacto prático no free tier:** 10 req/dia é extremamente restrito. Para a Copa do Mundo
> (64 partidas), uma única sync com `page_size=50` consome 2 requests do dia inteiro.
> Re-syncs periódicas de odds exigem o plano **Pro**. Planejar o orçamento antes da
> implementação.

**Endpoint utilizado:**

```
GET https://game-forecast-api.p.rapidapi.com/events
    ?league_id={id}
    &page_size=50
    &include_all_history=false
```

Filtragem por `league_id`, `team_id`, janela de tempo ou status. Não há endpoint por
`fixture_id` específico — o filtro é por liga/competição.

**Exemplo de request (JavaScript):**
```javascript
const url = new URL("https://game-forecast-api.p.rapidapi.com/events");
url.searchParams.set("league_id", "{COPA_LEAGUE_ID}");
url.searchParams.set("page_size", "50");
url.searchParams.set("include_all_history", "false");

const response = await fetch(url, {
  headers: {
    "X-RapidAPI-Key": process.env.RAPIDAPI_KEY,
    "X-RapidAPI-Host": "game-forecast-api.p.rapidapi.com",
  },
});
const { data } = await response.json();
```

**Campos consumidos do response (dentro de cada evento em `data[]`):**

```json
{
  "id": "...",
  "start_at": "2026-06-15T21:00:00Z",
  "team_home": { "name": "Brazil", "id": "..." },
  "team_away": { "name": "Argentina", "id": "..." },
  "predictions": [
    {
      "match_result":      { "home": 72, "draw": 18, "away": 10 },
      "total_goals":       { "over_2_5": 52, "under_2_5": 48, "over_3_5": 28, "under_3_5": 72 },
      "both_teams_score":  { "yes": 38, "no": 62 },
      "exact_score":       { "2_0": 16, "1_0": 14, "3_0": 11, "other": 7, ... },
      "first_half_winner": { "home": 52, "draw": 38, "away": 10 },
      "recommended_bets":  { "1": "matchResult.homeWinProbability", ... }
    }
  ]
}
```

> **Nota sobre `exact_score`:** O bucket `"other"` **não é criado como Outcome apostável**.
> Placares com probabilidade < 2% também são descartados para manter a lista gerenciável.
> O campo `predictions` é um array — usar `predictions[0]`.

### 2.3 Notas sobre `league_id` nas duas APIs

APIfootball e GameForecastAPI usam IDs de liga **independentes** — o mesmo campeonato
terá IDs diferentes nas duas APIs. Ambos precisam ser descobertos e configurados antes
do sync. Armazenar nos dois campos de configuração correspondentes.

---

## 3. Mudanças no Domain Model

### 3.1 TournamentType (enum)

```java
// Antes
FIFA_MATCH

// Depois
FIFA_MATCH,
REAL_FOOTBALL
```

### 3.2 Event (entity)

Dois FKs opcionais adicionados. Obrigatoriedade validada no `EventService` por `TournamentType`.
O campo `externalMatchId` corresponde ao `match_id` do APIfootball:

```sql
ALTER TABLE resenha.event
    ADD COLUMN team_home_id BIGINT REFERENCES resenha.team(id),
    ADD COLUMN team_away_id BIGINT REFERENCES resenha.team(id),
    ADD COLUMN external_match_id VARCHAR(20),   -- match_id do APIfootball
    ALTER COLUMN player_home_id DROP NOT NULL,
    ALTER COLUMN player_away_id DROP NOT NULL;
```

**Invariante de integridade (validado no service, não via constraint):**
- `FIFA_MATCH`: `playerHomeId` e `playerAwayId` obrigatórios; `teamHomeId`/`teamAwayId` nulos
- `REAL_FOOTBALL`: `teamHomeId` e `teamAwayId` obrigatórios; `playerHomeId`/`playerAwayId` nulos

### 3.3 Market (entity)

**Adicionado `marketType`.** A constraint de unicidade muda de `UNIQUE(event_id)` para
`UNIQUE(event_id, market_type)`:

```sql
ALTER TABLE resenha.market
    ADD COLUMN market_type VARCHAR(30) NOT NULL DEFAULT 'MATCH_RESULT',
    DROP CONSTRAINT IF EXISTS market_event_id_key,
    ADD CONSTRAINT market_event_market_type_key UNIQUE (event_id, market_type);
```

**Enum `MarketType`:**

```java
MATCH_RESULT,       // 1X2 — padrão para todos os eventos
OVER_UNDER_25,      // Total de gols Over/Under 2.5
OVER_UNDER_35,      // Total de gols Over/Under 3.5
BTTS,               // Ambas as equipes marcam
EXACT_SCORE         // Placar exato
```

> Para torneios `FIFA_MATCH`, o mercado `MATCH_RESULT` continua sendo criado automaticamente
> como hoje. Os demais tipos ficam disponíveis mas opcionais (implementação futura).

### 3.4 Outcome (entity)

Sem mudança na estrutura. A diferença é na criação:

- `MATCH_RESULT`: 3 outcomes fixos (`"Vitória Casa"`, `"Empate"`, `"Vitória Fora"`)
- `OVER_UNDER_*`: 2 outcomes (`"Over X.5"`, `"Under X.5"`)
- `BTTS`: 2 outcomes (`"Sim"`, `"Não"`)
- `EXACT_SCORE`: N outcomes dinâmicos, um por placar retornado pela API (ex: `"2-0"`, `"1-0"`)

A odd de cada outcome é calculada a partir da probabilidade retornada pela API:
```
odd = 1 / (probabilidade / 100)
```
Com o guard de odd mínima existente (`min-odd=1.05`).

### 3.5 Team (entity)

Sem mudança de schema. O campo `externalApiId` (já existente, nullable) passa a ser
populado com o `match_hometeam_id`/`match_awayteam_id` do APIfootball durante o fixture sync.

---

## 4. Novos Serviços

### 4.1 `ApiFootballClient`

Wrapper sobre `RestClient` para o APIfootball. Todos os calls usam query parameters.

```java
public interface ApiFootballClient {
    List<MatchDto> fetchEventsByLeague(String leagueId, LocalDate from, LocalDate to);
    List<MatchDto> fetchLiveEvents(String leagueId);
}
```

O `MatchDto` mapeia os campos do response de `get_events`:
`matchId`, `matchStatus`, `matchDate`, `matchTime`,
`homeTeamId`, `homeTeamName`, `homeScore`,
`awayTeamId`, `awayTeamName`, `awayScore`,
`halftimeHomeScore`, `halftimeAwayScore`,
`extraTimeHomeScore`, `extraTimeAwayScore`,
`penaltyHomeScore`, `penaltyAwayScore`,
`matchRound`, `stageName`.

Configuração em `application.properties`:
```properties
resenhabet.apifootball.key=
resenhabet.apifootball.base-url=https://apiv3.apifootball.com
resenhabet.apifootball.copa-league-id=     # descobrir via get_leagues antes de preencher
```

### 4.2 `GameForecastClient`

```java
public interface GameForecastClient {
    List<ForecastEventDto> fetchPredictions(String leagueId, int pageSize);
}
```

Internamente pode paginar se `pageSize < total` de partidas, mas para a Copa
(64 partidas, `page_size=50`) serão no máximo 2 requisições por sync.

Configuração:
```properties
resenhabet.gameforecast.key=
resenhabet.gameforecast.base-url=https://game-forecast-api.p.rapidapi.com
resenhabet.gameforecast.copa-league-id=    # ID independente do APIfootball — descobrir separado
resenhabet.gameforecast.min-exact-score-probability=2
```

### 4.3 `FixtureSyncService`

Responsável pelo sync inicial ao criar um torneio `REAL_FOOTBALL`.

Fluxo:
1. Chama `ApiFootballClient.fetchEventsByLeague(leagueId, from, to)`
2. Para cada partida: cria `Event` com `teamHomeId`/`teamAwayId` e `externalMatchId`
3. Cria um `TournamentRound` por fase usando o campo `stage_name` do response
4. Popula `Team.externalApiId` se ainda não preenchido (usando `match_hometeam_id`)
5. Chama `OddsImportService.importForTournament()` em seguida
6. Retorna resumo: `{ eventsCreated, teamsLinked, roundsCreated, marketsCreated }`

### 4.4 `OddsImportService`

Responsável por buscar previsões do GameForecastAPI e criar Markets/Outcomes.

Fluxo:
1. Chama `GameForecastClient.fetchPredictions(copaLeagueId, 50)`
2. Para cada evento do GameForecast, faz match com `Event` local pelo nome dos times
   (o GameForecastAPI usa seus próprios IDs — o link é pelo nome ou via mapeamento manual)
3. Para cada `MarketType` ativo no torneio:
   - Cria `Market` com status `OPEN` e `suspendAt = event.scheduledAt`
   - Cria `Outcome`s com odds calculadas da probabilidade
4. Se mercado já existir (re-sync): atualiza odds dos Outcomes, não recria

> **Atenção no match de IDs:** GameForecastAPI e APIfootball não compartilham IDs de
> fixture. O link entre os dois sistemas é feito pelo nome dos times + data da partida,
> ou por mapeamento manual de `team_id`. Implementar com tolerância a falha: se nenhuma
> partida local bater, logar o aviso e continuar.

### 4.5 `RealFootballScheduler`

```java
@Component
public class RealFootballScheduler {

    // Roda a cada 60 segundos
    @Scheduled(fixedDelay = 60_000)
    public void tick() {
        autoStartEvents();   // Inicia eventos cujo scheduledAt passou
        pollLiveScores();    // Atualiza scores se houver IN_PROGRESS
    }
}
```

**`autoStartEvents()`**
- Busca Events com status `CREATED`, `tournament.type = REAL_FOOTBALL`,
  `scheduledAt <= now()`
- Chama `EventService.startEvent()` para cada um (fecha mercados)

**`pollLiveScores()`**
- Verifica se existe algum Event `IN_PROGRESS` com `tournament.type = REAL_FOOTBALL`
- Se não: retorna sem fazer requisição (zero custo de API)
- Se sim: chama `ApiFootballClient.fetchLiveEvents(copaLeagueId)`
  (usa `?action=get_events&match_live=1&league_id={id}`)
- Para cada partida no response:
  - Detecta "em andamento": `match_status` contém `"'"` (ex: `"67'"`) ou é `"Half Time"`
  - Detecta "encerrada": `match_status` é `"Finished"`, `"After ET"`, ou `"After Pen."`
  - Detecta "cancelada": `match_status` é `"Postponed"` ou `"Cancelled"`
  - Atualiza `homeScore` / `awayScore` se mudou
  - Atualiza `halftimeScore` quando disponível
  - Se encerrada: chama `EventService.finishEvent()`
  - Se cancelada: cancela evento e devolve apostas
  - WebSocket broadcast via `/topic/events/{id}` após cada atualização

---

## 5. Endpoints Novos / Modificados

### Novos

```
POST /api/v1/tournaments/{id}/sync-fixtures
```
Admin dispara o fixture sync + odds import. Também chamado internamente na criação do torneio.
Response: `{ eventsCreated, teamsLinked, roundsCreated, marketsCreated, oddsImported }`

```
POST /api/v1/tournaments/{id}/sync-odds
```
Admin re-importa odds para todos os eventos futuros do torneio.
Útil para re-sync nas 48h pré-jogo nas fases finais.

### Modificados

```
GET /api/v1/markets/{eventId}
```
Hoje retorna um único `MarketResponseDTO`.
Passa a retornar `List<MarketResponseDTO>`.
**Breaking change** — frontend precisa ser atualizado junto.

---

## 6. Ciclo de Vida Completo — Torneio Real Football

```
Admin cria torneio REAL_FOOTBALL
  └── POST /api/v1/tournaments
      └── FixtureSyncService.sync()
          ├── Chama APIfootball get_events (1 req)
          ├── Cria Events (CREATED) com externalMatchId
          ├── Cria TournamentRounds por stage_name
          └── OddsImportService.importForTournament()
              ├── Chama GameForecastAPI /events (1-2 req)
              └── Cria Markets (OPEN) + Outcomes por evento

Até scheduledAt - ~1min
  └── MarketScheduler suspende Markets (suspendAt = scheduledAt)

scheduledAt passa
  └── RealFootballScheduler.autoStartEvents()
      └── EventService.startEvent() → Event IN_PROGRESS, Markets CLOSED

A cada 60s durante a Copa (somente se houver evento IN_PROGRESS)
  └── RealFootballScheduler.pollLiveScores()
      ├── APIfootball get_events?match_live=1 (1 req)
      ├── Atualiza scores
      └── WebSocket broadcast /topic/events/{id}

APIfootball retorna status "Finished" / "After ET" / "After Pen."
  └── RealFootballScheduler → EventService.finishEvent()
      ├── Resolve BetSlipItems
      ├── Credita vencedores
      └── WebSocket broadcast /topic/wallet/{userId}
```

---

## 7. Orçamento de Requisições — Plano Free

| Operação | APIfootball | GameForecastAPI |
|---|---|---|
| Sync inicial (64 partidas) | 1 req | 2 req (paginado em 50) |
| Poll ao vivo durante jogo (90 min) | ~90 req | 0 |
| Re-sync de odds (fase final, 4 jogos) | 0 | 1 req |
| **Total por dia de jogo** | **~91 req** | **~1-2 req** |
| **Limite free** | **100/dia** | **10/dia** |

> **Conclusão:** APIfootball free está no limite em dias com muitos jogos (fase de grupos
> com 4 partidas simultâneas pode exigir upgrade). GameForecastAPI free é suficiente
> para o sync inicial + re-sync ocasional da Copa, mas **sem margem para re-syncs
> frequentes**. Para uso regular, plano Pro do GameForecastAPI ($19/mês) é recomendado.

---

## 8. Configuração

Todas as propriedades novas em `application.properties` (e portanto em `.env.example`):

```properties
# APIfootball
resenhabet.apifootball.key=
resenhabet.apifootball.base-url=https://apiv3.apifootball.com
resenhabet.apifootball.copa-league-id=

# GameForecastAPI (via RapidAPI)
resenhabet.gameforecast.rapidapi-key=
resenhabet.gameforecast.base-url=https://game-forecast-api.p.rapidapi.com
resenhabet.gameforecast.copa-league-id=
resenhabet.gameforecast.min-exact-score-probability=2

# Scheduler
resenhabet.scheduler.live-poll-enabled=true
```

A propriedade `live-poll-enabled` permite desligar o scheduler em ambientes de desenvolvimento
sem precisar comentar código.

---

## 9. Lista de Tarefas

### Grupo RF-1 — Domain e Migrations

- **RF-01** Adicionar `REAL_FOOTBALL` ao enum `TournamentType`
- **RF-02** Flyway: adicionar `team_home_id`, `team_away_id`, `external_match_id` em `event`;
  tornar `player_home_id` e `player_away_id` nullable
- **RF-03** Flyway: adicionar `market_type` em `market`; trocar constraint unique de
  `event_id` para `(event_id, market_type)`; backfill `market_type = 'MATCH_RESULT'`
- **RF-04** Criar enum `MarketType` e atualizar entidade `Market`
- **RF-05** Atualizar `EventService`: validar invariante FIFA_MATCH vs REAL_FOOTBALL
  nos campos de player/team; atualizar MapStruct mappers
- **RF-06** Atualizar `MarketService`: remover pressuposição de mercado único por evento;
  adaptar `GET /markets/{eventId}` para retornar lista

### Grupo RF-2 — Integrações Externas

- **RF-07** Implementar `ApiFootballClient` (RestClient com query params, deserialização
  de `MatchDto` incluindo todos os campos de score e status)
- **RF-08** Implementar `GameForecastClient` (headers RapidAPI, paginação automática,
  filtro de placares abaixo do `min-exact-score-probability` configurado)
- **RF-09** Implementar `FixtureSyncService` (criação de Events, TournamentRounds por
  `stage_name`, link de Teams via `externalApiId`)
- **RF-10** Implementar `OddsImportService` (match de eventos por nome de time + data,
  criação de Markets por tipo, cálculo de odd a partir de probabilidade, guard de odd mínima)
- **RF-11** Adicionar endpoints `POST /sync-fixtures` e `POST /sync-odds`

### Grupo RF-3 — Scheduler

- **RF-12** Implementar `RealFootballScheduler` com `autoStartEvents()` e `pollLiveScores()`
- **RF-13** Mapear status texto do APIfootball para ações do sistema (start/finish/cancel);
  implementar detecção de "em jogo" via presença de `"'"` no status string
- **RF-14** Lógica de cancelamento de evento: devolver stakes, mudar BetSlipItems para `CANCELLED`
- **RF-15** Propriedade `live-poll-enabled` para desligar scheduler em dev

### Grupo RF-4 — Frontend

- **RF-17** Atualizar consumo de `GET /markets/{eventId}` para lista em todos os componentes
- **RF-18** Componente de seleção de mercado no Event page (tabs ou accordion por `MarketType`)
- **RF-19** Label de equipe no lugar de jogador para eventos `REAL_FOOTBALL`
  (badge + nome da seleção)
- **RF-20** Tela de criação de torneio: exibir checklist de `MarketType` ao selecionar
  `REAL_FOOTBALL`

---

## 10. Decisões

| Decisão | Decisão Consolidada |
|---|---|
| Cancelamento de evento | Devolver stake integral |
| Re-sync de odds | Manual via admin |
| Bracket da Copa | Configuração manual pelo admin |
| Mercados ativos | Configuráveis via checklist na criação do torneio |
| Live score | Polling via APIfootball |
| Match de fixtures (APIfootball ↔ GameForecast) | Nome + data com fallback para log de aviso |

---

## 11. Dependências com Tarefas Existentes

| Tarefa existente | Impacto |
|---|---|
| **Cleanup** (componentes legados frontend) | Boa prática antes de adicionar RF-17 a RF-20. |
