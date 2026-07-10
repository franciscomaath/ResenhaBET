# Plano de Implementação: Suporte a Torneios BRACKET (Mata-Mata)

> **Status:** Documento de referência. Será implementado quando o sistema estiver consolidado no formato LEAGUE.

---

## 1. Visão Geral

Este documento descreve todas as mudanças necessárias para suportar torneios no formato **BRACKET** (eliminação direta / mata-mata) além do formato atual **LEAGUE** (pontos corridos / round-robin).

O formato **BRACKET** é fundamental para torneios de copa, onde jogadores são eliminados a cada rodada e apenas o vencedor de cada confronto avança. Isso implica em mudanças profundas na geração de rodadas, criação de eventos, cálculo de ranking e comportamento de eventos.

---

## 2. Estado Atual: Premissas LEAGUE Hardcodadas

O código atual foi construído exclusivamente para o formato LEAGUE. As seguintes premissas estão espalhadas pelo sistema:

### 2.1 Criação do torneio (`TournamentServiceImpl.create()`)

```java
Tournament.builder()
    .format(TournamentFormat.LEAGUE)   // hardcodado
    .type(TournamentType.FIFA_MATCH)   // hardcodado
```

O `TournamentRequestDTO` não aceita `format` nem `type` do cliente. Não é possível criar um torneio BRACKET pela API.

### 2.2 Geração de rodadas (`TournamentServiceImpl.startTournament()`)

```java
if (tournament.getFormat() == TournamentFormat.LEAGUE) {
    for (int i = 1; i <= (n - 1) * 2; i++) {
        // gera (n-1)*2 rodadas com multiplicador flat = 1.0
    }
}
// TODO: BRACKET já veio com rounds do POST /tournaments
```

Para BRACKET, existe apenas um comentário `TODO`. Não há mecanismo para gerar rounds de eliminação nem para criar eventos da primeira rodada.

### 2.3 Scoreboard (`TournamentServiceImpl.getScoreboard()`)

O scoreboard atual calcula **pontos** (3×W + 1×D) e ordena por pontos > saldo > gols > Elo. Esse modelo **não se aplica** a BRACKET, onde empates não existem e o que importa é a **colocação** (em qual rodada o jogador foi eliminado).

### 2.4 Criação de eventos (`EventServiceImpl.create()`)

Eventos são criados manualmente via `POST /api/v1/events`. Em BRACKET, os eventos da próxima rodada **dependem do resultado** da rodada anterior. Não é possível criar todos upfront.

### 2.5 Mercado de apostas

Sempre cria 3 outcomes: `Vitória Casa`, `Empate`, `Vitória Fora`. Em BRACKET, não deveria haver mercado de empate.

### 2.6 `Event.isKnockout`

O campo é preenchido (`true` para BRACKET), mas **nenhuma lógica no sistema consulta esse campo**. Ele deveria influenciar validações (ex: proibir empates) e comportamento (ex: avançar vencedor).

---

## 3. Diferenças Fundamentais: LEAGUE vs BRACKET

| Aspecto | LEAGUE | BRACKET |
|---------|--------|---------|
| **Confrontos** | Todos contra todos (round-robin) | Eliminação direta (tree) |
| **Nº de partidas** | (n-1) × 2 para n jogadores | n-1 para n jogadores |
| **Empates** | Comuns e permitidos | **Proibidos** — precisa de vencedor |
| **Eliminação** | Não existe | Metade eliminado por rodada |
| **Rodadas** | `Rodada 1`, `Rodada 2`... | `Quartas`, `Semifinal`, `Final`... |
| **Byes** | Evento raro (W.O. de 1 jogador) | **Mecanismo automático** para não-potências de 2 |
| **Ranking** | Tabela de pontos (3×W + 1×D) | Bracket tree / Colocação final |
| **Scheduling** | Todos os eventos podem ser criados upfront | Próximos eventos dependem de resultados |
| **Mercado** | Casa, Empate, Fora | Casa, Fora (sem empate) |
| **Elo** | Aplica delta a cada evento | Aplica delta a cada evento |

---

## 4. Mudanças Detalhadas por Componente

### 4.1 DTOs

#### `TournamentRequestDTO`
**Adicionar campos:**
```java
@NotNull(message = "O formato do torneio é obrigatorio.")
private TournamentFormat format;  // LEAGUE ou BRACKET

@NotNull(message = "O tipo do torneio é obrigatorio.")
private TournamentType type;       // REAL_MATCH ou FIFA_MATCH
```

**Remover hardcoding** de `TournamentServiceImpl.create()`.

#### `TournamentResponseDTO`
Já retorna `format` e `type` como strings. Não precisa de mudança.

---

### 4.2 Entidades

#### `Tournament` — sem mudanças necessárias
Já possui `format` (LEAGUE/BRACKET) e `type` (REAL_MATCH/FIFA_MATCH). O banco também tem a coluna.

#### `TournamentRound` — sem mudanças estruturais
O modelo atual (`name`, `multiplier`, `roundOrder`) funciona para BRACKET. A semântica muda:
- LEAGUE: `"Rodada 1"`, `"Rodada 2"`... (round-robin)
- BRACKET: `"Oitavas"`, `"Quartas"`, `"Semifinal"`, `"Final"` (eliminação)

Poderia adicionar um campo `bracketLevel` para facilitar ordenação lógica, mas `roundOrder` já serve.

#### `Event` — sem mudanças estruturais
O campo `isKnockout` já existe. A mudança é **comportamental**: usar esse campo para validações e lógica de avanço.

---

### 4.3 Serviços

#### `TournamentServiceImpl.create()`
**Mudança:** Aceitar `format` e `type` do DTO.

```java
Tournament tournament = Tournament.builder()
    .name(dto.getName())
    .uuid(UUID.randomUUID())
    .type(dto.getType())
    .format(dto.getFormat())
    .status(TournamentStatus.CREATED)
    .startDate(dto.getStartDate())
    .endDate(dto.getEndDate())
    .build();
```

#### `TournamentServiceImpl.startTournament()`
**LEAGUE:** Lógica atual permanece.

**BRACKET:** Nova lógica completa:

1. **Validar número de participantes:**
   - Mínimo: 2 jogadores
   - Se não for potência de 2: jogadores de maior Elo recebem bye automaticamente
   - Ordenar por Elo decrescente para seeding

2. **Gerar rodadas de eliminação:**
   - Para n jogadores, calcular `log2(n arredondado para cima)` = número de rounds
   - Ex: 8 jogadores = 3 rounds (Quartas, Semifinal, Final)
   - Ex: 5 jogadores = 3 rounds (1 bye + Quartas, Semifinal, Final)
   - Round names: `Round of 16`, `Quarter-Finals`, `Semi-Finals`, `Final`, `3rd Place` (opcional)
   - Multipliers progressivamente maiores para rounds mais avançados (ex: 1.0, 1.5, 2.0, 3.0)

3. **Criar eventos da primeira rodada:**
   - Parear jogadores por seeding (1º vs último, 2º vs penúltimo, etc.)
   - Criar bye events (isBye=true) para jogadores que avançam automaticamente
   - Criar eventos vazios (sem playerHome/playerAway) para rounds subsequentes
   - Os eventos vazios serão preenchidos conforme os vencedores avançam

4. **Seeding:**
   - Ordenar por `currentElo` decrescente
   - 1º vs nº, 2º vs (n-1)º, etc. (pareamento standard de bracket)

**Código esboço:**
```java
if (tournament.getFormat() == TournamentFormat.BRACKET) {
    List<TournamentPlayer> players = tournamentPlayerRepository.findByTournamentId(tournamentId);
    players.sort((a, b) -> b.getPlayer().getCurrentElo().compareTo(a.getPlayer().getCurrentElo()));

    int n = players.size();
    int nextPowerOfTwo = Integer.highestOneBit(n - 1) << 1;  // ex: 5 -> 8
    int byes = nextPowerOfTwo - n;

    // Criar rounds
    int rounds = (int) (Math.log(nextPowerOfTwo) / Math.log(2));
    for (int i = 1; i <= rounds; i++) {
        // criar round com multiplier progressivo
    }

    // Criar eventos da primeira rodada + byes
    // ...
}
```

#### `EventServiceImpl.create()`
**Mudança para BRACKET:**
- Se `tournament.getFormat() == BRACKET`, não criar outcome de "Empate"
- Validar que `roundId` pertence ao torneio
- Para eventos de rounds posteriores, validar que os jogadores são vencedores de eventos anteriores

#### `EventServiceImpl.finishEvent()`
**Mudança para BRACKET:**
- Se `event.isKnockout() == true`, verificar que `homeScore != awayScore` (empate proibido)
- Se `event.isKnockout() == true` e o evento é da rodada N (exceto final), **avaliar o vencedor e alocá-lo no evento da rodada N+1**

**Novo mecanismo: avanço do vencedor**

```java
if (event.getIsKnockout()) {
    // 1. Identificar vencedor
    Player winner = homeScore > awayScore ? homePlayer : awayPlayer;

    // 2. Buscar o evento da próxima rodada que espera este vencedor
    //    Ex: o evento da semi-final que espera o vencedor das quartas
    Event nextRoundEvent = findNextRoundEvent(event);

    // 3. Alocar o vencedor
    if (nextRoundEvent.getPlayerHome() == null) {
        nextRoundEvent.setPlayerHome(winner);
    } else {
        nextRoundEvent.setPlayerAway(winner);
    }

    // 4. Se nextRoundEvent agora tem os dois jogadores, mudar status para CREATED
    if (nextRoundEvent.getPlayerHome() != null && nextRoundEvent.getPlayerAway() != null) {
        // (opcional) gerar mercado de apostas para o próximo evento
    }

    // 5. Se for a final, marcar torneio como COMPLETED
    if (isFinalRound(event)) {
        tournament.setStatus(TournamentStatus.COMPLETED);
        tournament.setEndDate(LocalDateTime.now());
    }
}
```

**Como mapear "qual evento da próxima rodada recebe o vencedor?"**

Opção A: **NextRoundEventId** — adicionar `nextRoundEventId` ao `Event` entity.

Opção B: **Slot-based** — cada evento sabe em qual "slot" (posição) ele está na bracket. O evento da próxima rodada é `slot / 2` (integer division). Ex:
- Quartas: slots 0, 1, 2, 3
- Semi: slots 0, 1 (vencedor do slot 0 das quartas vai para slot 0 das semi, slot 1 -> slot 0, slot 2 -> slot 1, slot 3 -> slot 1)

Opção A é mais simples de implementar.

#### `EventServiceImpl.updateScore()`
**Mudança para BRACKET:**
- Se `event.getIsKnockout()`, validar que `homeScore != awayScore`

#### `TournamentServiceImpl.getScoreboard()`
**LEAGUE:** Permanece igual (tabela de pontos).

**BRACKET:** Novo comportamento. Não retornar uma lista ordenada por pontos. Retornar:

**Opção A — Bracket Tree:**
```json
{
  "tournamentId": 1,
  "format": "BRACKET",
  "bracket": {
    "rounds": [
      {
        "name": "Quartas de Final",
        "events": [
          { "playerHome": "João", "playerAway": "Maria", "winner": "João" },
          { "playerHome": "Carlos", "playerAway": "Ana", "winner": "Ana" }
        ]
      },
      {
        "name": "Semifinal",
        "events": [
          { "playerHome": "João", "playerAway": "Ana", "winner": "Ana" }
        ]
      }
    ]
  }
}
```

**Opção B — Placar de Colocações (mais simples):**
```json
{
  "tournamentId": 1,
  "format": "BRACKET",
  "placements": [
    { "playerId": 3, "playerName": "Ana", "position": 1, "eliminationRound": "Champion" },
    { "playerId": 1, "playerName": "João", "position": 2, "eliminationRound": "Final" },
    { "playerId": 2, "playerName": "Maria", "position": 3, "eliminationRound": "Quarter-Finals" },
    { "playerId": 4, "playerName": "Carlos", "position": 4, "eliminationRound": "Quarter-Finals" }
  ]
}
```

Opção B é mais fácil de consumir para rankings externos e mais simples de implementar.

**Implementação de Opção B:**
- Para cada evento COMPLETED, saber em qual round ele ocorreu
- Jogador que perdeu = eliminado naquele round
- Jogador que venceu = avançou (ou campeão se for final)
- Ordenar: campeão > vice > semifinalistas > quartas...

---

### 4.4 Repositories

#### `EventRepository`
- `findCompletedByTournamentId` — já existe, funciona para ambos os formatos
- `findCompletedByPlayerId` — já existe, funciona para ambos
- Não precisa de novas queries, exceto se adicionar `nextRoundEventId` (query simples)

#### `TournamentPlayerRepository`
- Sem mudanças

#### `TournamentRepository`
- Sem mudanças

---

## 5. Mudanças no Banco de Dados

### 5.1 Não precisa de novas tabelas

Todas as entidades existentes (`tournament`, `tournament_round`, `event`, `tournament_player`) já suportam BRACKET. O que muda é a **semântica** dos dados.

### 5.2 Possível alteração: `Event.next_round_event_id` (opcional)

Se optar pela **Opção A** (cada evento sabe qual é o próximo evento), adicionar uma FK:

```sql
ALTER TABLE event
ADD COLUMN next_round_event_id BIGINT,
ADD CONSTRAINT fk_event_next_round FOREIGN KEY (next_round_event_id) REFERENCES event(id);
```

**Vantagem:** avanço do vencedor é trivial (`eventRepository.findById(event.getNextRoundEventId())`).

**Alternativa sem schema change:** calcular o próximo evento dinamicamente via `roundId` + posição (mais complexo).

**Recomendação:** Adicionar `nextRoundEventId` para simplificar a lógica de avanço.

### 5.3 Possível alteração: `TournamentRound.bracket_level` (opcional)

```sql
ALTER TABLE tournament_round
ADD COLUMN bracket_level INT;
```

**Exemplo:**
- `bracket_level = 1` = Final
- `bracket_level = 2` = Semifinal
- `bracket_level = 3` = Quartas

**Vantagem:** facilita identificar "qual é a final?" e "qual é a próxima rodada?" sem depender de nomes de strings.

**Recomendação:** Opcional. `roundOrder` já pode ser usado (maior = mais avançado), mas a semântica fica menos clara.

---

## 6. Mudanças na API

### 6.1 `POST /api/v1/tournaments`

**Request body (atual):**
```json
{ "name": "Copa do Brasil", "startDate": "2026-07-01", "endDate": "2026-08-01" }
```

**Request body (novo):**
```json
{ 
  "name": "Copa do Brasil", 
  "format": "BRACKET",
  "type": "FIFA_MATCH",
  "startDate": "2026-07-01", 
  "endDate": "2026-08-01" 
}
```

### 6.2 `POST /api/v1/tournaments/{id}/start`

**Comportamento LEAGUE:** Igual ao atual.

**Comportamento BRACKET:**
- Valida nº de jogadores (>= 2)
- Gera rounds de eliminação
- Cria seeding por Elo
- Cria eventos da primeira rodada + byes
- Cria eventos vazios para rounds subsequentes
- Retorna torneio com status `IN_PROGRESS`

### 6.3 `POST /api/v1/events`

**Comportamento LEAGUE:** Igual ao atual.

**Comportamento BRACKET:**
- Valida que os jogadores são vencedores dos eventos anteriores
- Não cria mercado de "Empate"
- `isKnockout` = true

**Nota:** Em BRACKET, a maioria dos eventos será criada automaticamente pelo `startTournament()`, não manualmente. A API de criação manual pode ser usada apenas para correções ou casos especiais.

### 6.4 `POST /api/v1/events/{id}/end`

**Comportamento LEAGUE:** Igual ao atual.

**Comportamento BRACKET (novo):**
- Valida que não houve empate (`homeScore != awayScore`)
- Avança o vencedor para o próximo evento
- Se próximo evento agora tem 2 jogadores, gera mercado de apostas
- Se for final, marca torneio como `COMPLETED`

### 6.5 `GET /api/v1/tournaments/{id}/scoreboard`

**Response LEAGUE:** Igual ao atual (tabela de pontos).

**Response BRACKET (novo):**
```json
{
  "tournamentId": 1,
  "tournamentName": "Copa do Brasil",
  "format": "BRACKET",
  "placements": [
    { "playerId": 3, "playerName": "Ana", "position": 1, "eliminationRound": "Champion" },
    { "playerId": 1, "playerName": "João", "position": 2, "eliminationRound": "Final" },
    { "playerId": 2, "playerName": "Maria", "position": 3, "eliminationRound": "Quarter-Finals" },
    { "playerId": 4, "playerName": "Carlos", "position": 4, "eliminationRound": "Quarter-Finals" }
  ]
}
```

### 6.6 `GET /api/v1/players/{id}/stats?tournamentId={id}`

**Comportamento:** Funciona para ambos os formatos. Inclui stats de partidas de BRACKET (com bye events). `points` e `goalDifference` ainda fazem sentido como métricas de performance, mas não definem ranking em BRACKET.

---

## 7. Novos Comportamentos / Fluxos

### 7.1 Fluxo de Criação de Torneio BRACKET

```
POST /api/v1/tournaments
  { "name": "Copa", "format": "BRACKET", "type": "FIFA_MATCH" }
    ↓
POST /api/v1/tournaments/{id}/players  (adicionar N jogadores)
    ↓
POST /api/v1/tournaments/{id}/start
  - Gera rounds: Oitavas, Quartas, Semi, Final
  - Cria seeding por Elo
  - Cria eventos da 1a rodada
  - Cria byes para não-potências de 2
  - Cria eventos vazios para rounds subsequentes
    ↓
[Eventos jogados sequencialmente]
    ↓
POST /api/v1/events/{id}/start
POST /api/v1/events/{id}/score
POST /api/v1/events/{id}/end
  - Vencedor avança automaticamente para o próximo evento
  - Se final → torneio COMPLETED
```

### 7.2 Fluxo de Avanço do Vencedor

```
Evento das Quartas (slot 0): João vs Maria → João vence
    ↓
Evento da Semi (slot 0): João vs [vazio] → João entra como home
    ↓
Evento das Quartas (slot 1): Carlos vs Ana → Ana vence
    ↓
Evento da Semi (slot 0): João vs Ana → agora tem 2 jogadores
    ↓
    (auto-gerar mercado de apostas para a semi)
```

### 7.3 Seeding

```
Jogadores ordenados por Elo decrescente:
  1. Ana (1200)
  2. João (1150)
  3. Maria (1100)
  4. Carlos (1050)

Pareamento das quartas:
  Quartas 0: Ana (1) vs Carlos (4)
  Quartas 1: João (2) vs Maria (3)
```

### 7.4 Byes em BRACKET

```
5 jogadores → próxima potência de 2 = 8 → 3 byes
Jogadores ordenados por Elo:
  1. Ana
  2. João
  3. Maria
  4. Carlos
  5. Pedro

3 byes → os 3 de maior Elo avançam automaticamente:
  - Ana, João, Maria recebem bye events

Quartas restantes:
  - Carlos vs Pedro

Semifinais:
  - Ana vs [vencedor Carlos/Pedro]
  - João vs Maria
```

---

## 8. Fases de Implementação

### Fase 1: Fundação (infraestrutura)
- [ ] Aceitar `format` e `type` no `TournamentRequestDTO`
- [ ] Remover hardcoding de LEAGUE/FIFA_MATCH em `create()`
- [ ] Adicionar `next_round_event_id` ao `Event` (migration)
- [ ] Adicionar `bracket_level` ao `TournamentRound` (migration, opcional)

### Fase 2: Geração de Bracket
- [ ] Implementar lógica BRACKET em `startTournament()`
  - Cálculo de potência de 2
  - Seeding por Elo
  - Geração de rounds de eliminação
  - Criação de eventos da primeira rodada
  - Criação de byes automáticos
  - Criação de eventos vazios para rounds subsequentes
- [ ] Adicionar `nextRoundEventId` aos eventos gerados

### Fase 3: Comportamento de Evento
- [ ] Validar empate proibido em eventos `isKnockout`
- [ ] Implementar avanço do vencedor em `finishEvent()`
- [ ] Auto-gerar mercado para próximo evento quando ambos os jogadores são conhecidos
- [ ] Auto-finalizar torneio ao completar a final
- [ ] Em BRACKET, não criar mercado de empate em `create()`

### Fase 4: Scoreboard
- [ ] Criar `TournamentScoreboardResponseDTO` com formato de colocações
- [ ] Implementar `getScoreboard()` para BRACKET
- [ ] Adicionar `eliminationRound` a cada entrada
- [ ] Ordenar por colocação (campeão > vice > ...)

### Fase 5: Testes e Validação
- [ ] Testar torneio BRACKET com 2 jogadores (final direto)
- [ ] Testar torneio BRACKET com 4 jogadores (sem byes)
- [ ] Testar torneio BRACKET com 5 jogadores (com byes)
- [ ] Testar fluxo completo: start → jogar → avançar → final → complete
- [ ] Testar que LEAGUE continua funcionando normalmente

---

## 9. Decisões em Aberto

### 9.1 3º Lugar (Disputa do Bronze)

Em muitos torneios BRACKET, há uma disputa entre os perdedores das semifinais pelo 3º lugar. Devemos suportar isso?

**Opção A:** Sim, adicionar round extra `"3rd Place"`.
**Opção B:** Não, apenas campeão e vice.

### 9.2 Empate por Penalties

Em BRACKET, se o jogo terminar empatado, como registrar? Atualmente o sistema não tem conceito de "penalties".

**Opção A:** Simplificar — proibir empates e forçar um score diferente (mesmo que seja 1x0 fictício).
**Opção B:** Adicionar `penaltiesHome`/`penaltiesAway` ao `Event` e alterar `finishEvent` para usar isso como desempate.

**Recomendação:** Opção A para MVP. BRACKET em FIFA real geralmente tem prorrogação/penalties, mas no sistema atual basta registrar o resultado final (mesmo que contabilmente 1x0).

### 9.3 Double Elimination (Eliminação Dupla)

O formato BRACKET atual discutido é **Single Elimination** (eliminação simples). Alguns jogos usam Double Elimination (você precisa perder 2 vezes para ser eliminado). Isso é para outro documento futuro.

---

## 10. Resumo de Impacto

| Componente | Mudança | Tamanho |
|------------|---------|---------|
| `TournamentRequestDTO` | +2 campos | Pequeno |
| `TournamentServiceImpl.create()` | Usar DTO | Pequeno |
| `TournamentServiceImpl.startTournament()` | +BRACKET logic | **Grande** |
| `EventServiceImpl.create()` | Sem empate em knockout | Pequeno |
| `EventServiceImpl.finishEvent()` | +Advance winner | **Médio** |
| `EventServiceImpl.updateScore()` | Validação empate | Pequeno |
| `TournamentServiceImpl.getScoreboard()` | +BRACKET response | **Médio** |
| `Event` entity | +`nextRoundEventId` | Pequeno |
| Migrations | 1-2 alterações | Pequeno |
| API | Novos responses | Pequeno |
| Testes | Novos cenários BRACKET | **Médio** |

---

> **Nota final:** Este documento é um roadmap. A implementação deve ser feita em branches separadas e testada exaustivamente com torneios BRACKET de 2, 4, 5, 8 e 16 jogadores antes de merge na main.
