# ResenhaBET — Goal Markets Design (FIFA_MATCH)
**Status:** Designed, pending implementation  
**Scope:** Novos markets para torneios `FIFA_MATCH`: Over 2.5, Over 3.5, BTTS,
Exact Score e Para se Classificar (knockout only).

---

## 1. Contexto e Decisões Arquiteturais

### Decisões confirmadas

| Decisão | Resolução |
|---|---|
| Markets obrigatórios ou opcionais por torneio? | **Opcionais** — via `group_tournament_market_type` (tabela e fluxo já implementados) |
| Geração em eventos eliminatórios? | **Sim** — todos os markets de gols + novo market `QUALIFY` |
| Escopo do Exact Score | **Top 8 placares + "Outro Placar"** |
| Knockout `MATCH_RESULT` | **3 outcomes com Empate** — placar do tempo normal; `QUALIFY` assume o avanço |
| Constraint `BetSlipItem` | **Dropar `UNIQUE(bet_slip_id, event_id)`, enforçar nova regra no `BetServiceImpl`** — sem nova coluna |
| Probabilidades para `QUALIFY` | **Adicionar `pHome`/`pAway` ao `OddsResult`** |

### Estado atual confirmado pelo agente

| Item | Estado real |
|---|---|
| `market.market_type` | ✅ Já existe (`V30__ADD_MARKET_TYPE.sql`) |
| `UNIQUE(event_id, market_type)` em `market` | ✅ Já existe |
| `OVER_UNDER_25`, `OVER_UNDER_35`, `BTTS`, `EXACT_SCORE` em `MarketType` | ✅ Já existem |
| `QUALIFY` em `MarketType` | ❌ Faltando — único valor novo a adicionar |
| `CUSTOM_STAT` em `MarketType` | ❌ Não existe no código — fora do escopo deste design |
| `spring.flyway.target` | `41` — qualquer nova migration deve atualizar para `42+` |
| Tabela de market types do torneio | `group_tournament_market_type` (não `tournament_market_type`) |

### Relação com o sistema existente

- `OddsCalculator` permanece **inalterado** na sua lógica, com adição de `pHome`/`pAway` no `OddsResult`.
- `group_tournament_market_type` já define quais `MarketType`s estão ativos por torneio.
  A criação dos novos markets lê essa tabela exatamente como hoje.
- A constraint `UNIQUE(bet_slip_id, event_id)` em `BetSlipItem` é **dropada** e substituída
  por um check em `BetServiceImpl.placeBet()`. Isso permite apostar em MATCH_RESULT + BTTS
  do mesmo evento no mesmo slip, mantendo a regra de "um bet por market por slip".
- Os dois serviços que criam markets — `EventServiceImpl` e `TournamentServiceImpl` —
  devem manter comportamento idêntico. Ver seção 7.

---

## 2. MarketType — única adição necessária

```java
public enum MarketType {
    MATCH_RESULT,       // existente
    OVER_UNDER_25,      // existente
    OVER_UNDER_35,      // existente
    BTTS,               // existente
    EXACT_SCORE,        // existente
    QUALIFY             // NOVO — Para se Classificar (knockout only)
}
```

### Outcomes por MarketType

| MarketType | Outcomes | Gerado em |
|---|---|---|
| `MATCH_RESULT` | Vitória Casa / Empate / Vitória Fora | Todos os eventos (exceto bye) |
| `OVER_UNDER_25` | Acima de 2.5 / Abaixo de 2.5 | Todos os eventos (exceto bye) |
| `OVER_UNDER_35` | Acima de 3.5 / Abaixo de 3.5 | Todos os eventos (exceto bye) |
| `BTTS` | Ambas Marcam - Sim / Ambas Marcam - Não | Todos os eventos (exceto bye) |
| `EXACT_SCORE` | Top 8 placares + Outro Placar | Todos os eventos (exceto bye) |
| `QUALIFY` | `{nomeHome} avança` / `{nomeAway} avança` | **Apenas knockout** (exceto bye) |

### Mudança de comportamento: knockout `MATCH_RESULT`

**Estado anterior:** knockout criava só 2 outcomes (nomes dos jogadores), pênaltis
decidiam o vencedor no settlement.

**Novo comportamento:** `MATCH_RESULT` em knockout passa a ter **3 outcomes**
(Vitória Casa / Empate / Vitória Fora), exatamente como eventos de liga.
O settlement de `MATCH_RESULT` usa apenas o placar do **tempo normal**.
`QUALIFY` é o market que determina quem avança, desempatando via pênaltis quando necessário.

> Impacto direto em `EventServiceImpl` e `TournamentServiceImpl` na criação de outcomes,
> e em `BetServiceImpl` no settlement. Ver seções 7 e 8.

---

## 3. Constraint de BetSlipItem — solução sem nova coluna

### O problema

`UNIQUE(bet_slip_id, event_id)` impede que o usuário aposte em dois markets
diferentes do mesmo evento no mesmo slip (ex: MATCH_RESULT + BTTS).

### Solução adotada: application-level enforcement

1. **Migration:** dropar a constraint antiga.
2. **`BetServiceImpl.placeBet()`:** antes de criar cada `BetSlipItem`, verificar se
   já existe um item no slip com o mesmo `market` daquele outcome.

```java
// Em BetServiceImpl.placeBet(), antes de criar BetSlipItem
Outcome outcome = outcomeRepository.findById(request.outcomeId())
    .orElseThrow(...);

boolean marketAlreadyInSlip = slip.getItems().stream()
    .anyMatch(item -> item.getOutcome().getMarket().getId()
                         .equals(outcome.getMarket().getId()));

if (marketAlreadyInSlip) {
    throw new BusinessException(
        "Já existe uma aposta no market " + outcome.getMarket().getType()
        + " para este evento neste slip.");
}
```

**Por que sem nova coluna:** o `outcome_id` já existente em `BetSlipItem` provê
acesso transitivo a `market_id` via `outcome → market`. Para um app de grupo
fechado, a validação em código é suficiente e evita alterar a entidade e fazer
backfill de dados existentes.

---

## 4. `OddsResult` — adição de probabilidades brutas

`QUALIFY` precisa de `pHome` e `pAway` brutos para renormalizar sem o empate.
Adicionar os campos ao record/classe existente:

```java
// OddsResult — adicionar os dois campos
public record OddsResult(
    double homeOdd,
    double drawOdd,
    double awayOdd,
    double pHome,   // NOVO — probabilidade bruta de vitória home (pré-conversão a odd)
    double pAway    // NOVO — probabilidade bruta de vitória away (pré-conversão a odd)
) {}
```

`OddsCalculator` já calcula `pHome` e `pAway` internamente antes de converter para
odds — basta expô-los no retorno. **Nenhuma lógica de cálculo muda.**

Uso em `QUALIFY`:
```java
OddsResult oddsResult = oddsCalculator.calculate(home, away, h2hMatches);

double pHomeAdv = oddsResult.pHome() / (oddsResult.pHome() + oddsResult.pAway());
double pAwayAdv = oddsResult.pAway() / (oddsResult.pHome() + oddsResult.pAway());
```

---

## 5. Modelo de Cálculo — λ Híbrido (Elo + Histórico)

### Princípio

Replica o padrão do `OddsCalculator` (Elo base + blend H2H) mas para expected goals (λ).

```
λ_final = (1 − w) × λ_elo  +  w × λ_hist

w cresce linearmente de 0 até maxHistLambdaWeight
conforme o número de partidas completadas aumenta até histLambdaThreshold
```

Quando não há histórico suficiente, `w = 0` → sistema roda 100% em Elo.

---

### 5.1 λ derivado do Elo (baseline, sempre disponível)

```java
// eloRatio > 1 quando self é mais forte
double eloRatio = Math.pow(10.0, (selfElo - opponentElo) / (2.0 * eloScale));

double λSelf_elo     = avgGoalsPerSide * Math.pow(eloRatio,  eloLambdaAlpha);
double λOpponent_elo = avgGoalsPerSide * Math.pow(eloRatio, -eloLambdaAlpha);
```

**Propriedade invariante:** `λSelf × λOpponent = avgGoalsPerSide²`
(média geométrica preservada; quando Elo igual → ambos recebem `avgGoalsPerSide`).

**Exemplo:** `avgGoalsPerSide=2.0`, `alpha=0.40`, diferença de 200 Elo →
`eloRatio ≈ 1.78` → `λHome ≈ 2.51`, `λAway ≈ 1.59`.

---

### 5.2 λ derivado do histórico

```
λSelf_hist = attackStrength(self) × defenseStrength(opponent) × leagueAvg

attackStrength(player)  = avgGoalsScored(player)   / leagueAvg
defenseStrength(player) = avgGoalsConceded(player)  / leagueAvg
```

Quando não há dados para um jogador: strength retorna `1.0` (exatamente na média).

**Queries em `EventRepository`** — todas excluem byes obrigatoriamente:

```java
@Query("""
    SELECT AVG(CASE
        WHEN e.playerHome.id = :playerId THEN e.homeScore
        ELSE e.awayScore
    END)
    FROM Event e
    WHERE (e.playerHome.id = :playerId OR e.playerAway.id = :playerId)
      AND e.status = 'COMPLETED' AND e.isBye = false
""")
Double findAvgGoalsScoredByPlayer(@Param("playerId") Long playerId);

@Query("""
    SELECT AVG(CASE
        WHEN e.playerHome.id = :playerId THEN e.awayScore
        ELSE e.homeScore
    END)
    FROM Event e
    WHERE (e.playerHome.id = :playerId OR e.playerAway.id = :playerId)
      AND e.status = 'COMPLETED' AND e.isBye = false
""")
Double findAvgGoalsConcededByPlayer(@Param("playerId") Long playerId);

@Query("""
    SELECT COUNT(e) FROM Event e
    WHERE (e.playerHome.id = :playerId OR e.playerAway.id = :playerId)
      AND e.status = 'COMPLETED' AND e.isBye = false
""")
Long countCompletedMatchesByPlayer(@Param("playerId") Long playerId);

@Query("""
    SELECT AVG((e.homeScore + e.awayScore) / 2.0)
    FROM Event e WHERE e.status = 'COMPLETED' AND e.isBye = false
""")
Double findGlobalAvgGoalsPerSide();
```

---

### 5.3 O blend

```java
long sampleSize = Math.min(
    eventRepository.countCompletedMatchesByPlayer(homeId),
    eventRepository.countCompletedMatchesByPlayer(awayId)
);

double weight = Math.min((double) sampleSize / histLambdaThreshold, maxHistLambdaWeight);

double λHome = (1 - weight) * λHome_elo + weight * λHome_hist;
double λAway = (1 - weight) * λAway_elo + weight * λAway_hist;
```

---

## 6. `PoissonCalculator` — utilitário puro

Sem dependências de Spring. Totalmente testável via JUnit puro.

```java
public final class PoissonCalculator {

    private static final int MAX_GOALS = 9; // matriz 10×10, suficiente para FIFA

    private PoissonCalculator() {}

    public static double probability(int k, double lambda) {
        if (k < 0 || lambda <= 0) return 0.0;
        return Math.pow(lambda, k) * Math.exp(-lambda) / factorial(k);
    }

    public static double[][] scoreMatrix(double lambdaHome, double lambdaAway) {
        double[][] m = new double[MAX_GOALS + 1][MAX_GOALS + 1];
        for (int i = 0; i <= MAX_GOALS; i++)
            for (int j = 0; j <= MAX_GOALS; j++)
                m[i][j] = probability(i, lambdaHome) * probability(j, lambdaAway);
        return m;
    }

    // threshold=3 → Over 2.5 | threshold=4 → Over 3.5
    public static double over(double[][] matrix, int threshold) {
        double p = 0.0;
        for (int i = 0; i <= MAX_GOALS; i++)
            for (int j = 0; j <= MAX_GOALS; j++)
                if (i + j >= threshold) p += matrix[i][j];
        return p;
    }

    // Forma fechada O(1): 1 − P(home=0) − P(away=0) + P(home=0,away=0)
    public static double bttsYes(double lHome, double lAway, double[][] matrix) {
        return 1.0 - probability(0, lHome) - probability(0, lAway) + matrix[0][0];
    }

    // Retorna topN + 1 entradas (última é "Outro Placar" com probabilidade residual)
    public static List<ScoreEntry> topScores(double[][] matrix, int topN) {
        List<ScoreEntry> all = new ArrayList<>();
        for (int i = 0; i <= MAX_GOALS; i++)
            for (int j = 0; j <= MAX_GOALS; j++)
                all.add(new ScoreEntry(i + "-" + j, matrix[i][j]));

        all.sort(Comparator.comparingDouble(ScoreEntry::probability).reversed());

        List<ScoreEntry> top = new ArrayList<>(all.subList(0, Math.min(topN, all.size())));
        double residual = Math.max(0.0,
            1.0 - top.stream().mapToDouble(ScoreEntry::probability).sum());
        top.add(new ScoreEntry("Outro Placar", residual));
        return top;
    }

    private static double factorial(int n) {
        double r = 1.0;
        for (int i = 2; i <= n; i++) r *= i;
        return r;
    }

    public record ScoreEntry(String label, double probability) {}
}
```

---

## 7. Criação de markets e outcomes na criação do evento

### Pré-condição: chamar calculadores uma única vez por evento

```java
// Ambos calculados uma vez — reutilizados no loop de MarketType
OddsResult oddsResult = oddsCalculator.calculate(home, away, h2hMatches);
GoalMarketsOdds goalOdds = (!event.isBye())
    ? goalMarketsCalculator.calculate(home, away)
    : null;
```

### Loop pelos market types ativos do torneio

```java
for (MarketType type : groupTournament.getActiveMarketTypes()) {
    if (event.isBye() && type != MATCH_RESULT) continue; // byes: só MATCH_RESULT, se tanto

    switch (type) {

        case MATCH_RESULT -> {
            // MATCH_RESULT agora é SEMPRE 3 outcomes — inclusive em knockout
            Market m = createMarket(event, MATCH_RESULT);
            createOutcome(m, "Vitória Casa", oddsResult.homeOdd());
            createOutcome(m, "Empate",       oddsResult.drawOdd());
            createOutcome(m, "Vitória Fora", oddsResult.awayOdd());
        }

        case OVER_UNDER_25 -> {
            Market m = createMarket(event, OVER_UNDER_25);
            createOutcome(m, "Acima de 2.5",  goalOdds.over25());
            createOutcome(m, "Abaixo de 2.5", goalOdds.under25());
        }

        case OVER_UNDER_35 -> {
            Market m = createMarket(event, OVER_UNDER_35);
            createOutcome(m, "Acima de 3.5",  goalOdds.over35());
            createOutcome(m, "Abaixo de 3.5", goalOdds.under35());
        }

        case BTTS -> {
            Market m = createMarket(event, BTTS);
            createOutcome(m, "Ambas Marcam - Sim", goalOdds.bttsYes());
            createOutcome(m, "Ambas Marcam - Não", goalOdds.bttsNo());
        }

        case EXACT_SCORE -> {
            Market m = createMarket(event, EXACT_SCORE);
            goalOdds.exactScoreOdds().forEach((label, odd) ->
                createOutcome(m, label, odd));
        }

        case QUALIFY -> {
            // Gerado APENAS em knockout; ignorado silenciosamente em liga
            if (event.isKnockout()) {
                double pHomeAdv = oddsResult.pHome() / (oddsResult.pHome() + oddsResult.pAway());
                double pAwayAdv = oddsResult.pAway() / (oddsResult.pHome() + oddsResult.pAway());
                Market m = createMarket(event, QUALIFY);
                createOutcome(m, home.getName() + " avança", toOdd(pHomeAdv));
                createOutcome(m, away.getName() + " avança", toOdd(pAwayAdv));
            }
        }
    }
}
```

### Dois serviços, comportamento idêntico

`EventServiceImpl` (eventos criados manualmente via `POST /api/v1/events`) e
`TournamentServiceImpl` (eventos gerados automaticamente em AUTO mode) devem
executar exatamente o mesmo bloco de criação de markets acima.
Recomendado: extrair para um método privado em um `MarketCreationHelper` ou
serviço compartilhado para evitar divergência futura.

---

## 8. Settlement em `finishEvent`

Estender o método de resolução de winners em `BetServiceImpl`:

```java
private String determineWinningOutcomeLabel(Market market, Event event) {
    int home  = event.getHomeScore();
    int away  = event.getAwayScore();
    int total = home + away;

    return switch (market.getType()) {

        case MATCH_RESULT -> {
            // Sempre placar do tempo normal — inclusive em knockout
            if (home > away)      yield "Vitória Casa";
            else if (away > home) yield "Vitória Fora";
            else                  yield "Empate";
        }

        case OVER_UNDER_25 -> total >= 3 ? "Acima de 2.5" : "Abaixo de 2.5";

        case OVER_UNDER_35 -> total >= 4 ? "Acima de 3.5" : "Abaixo de 3.5";

        case BTTS -> (home > 0 && away > 0)
            ? "Ambas Marcam - Sim"
            : "Ambas Marcam - Não";

        case EXACT_SCORE -> {
            String label = home + "-" + away;
            boolean hasSpecificOutcome = market.getOutcomes().stream()
                .anyMatch(o -> o.getLabel().equals(label));
            yield hasSpecificOutcome ? label : "Outro Placar";
        }

        case QUALIFY -> {
            // Desempata via pênaltis quando necessário
            Player winner;
            if (home > away) {
                winner = event.getPlayerHome();
            } else if (away > home) {
                winner = event.getPlayerAway();
            } else {
                winner = event.getPenaltiesHome() > event.getPenaltiesAway()
                    ? event.getPlayerHome()
                    : event.getPlayerAway();
            }
            yield winner.getName() + " avança";
        }
    };
}
```

> **Consistência de labels:** os labels de outcome definidos na criação (seção 7)
> e os labels resolvidos no settlement (acima) devem ser idênticos ao caractere.
> Qualquer divergência resulta em bets nunca resolvidas. Centralizar as strings
> em constantes ou enum é fortemente recomendado.

---

## 9. `GoalMarketsOddsCalculator` — Spring component

```java
@Component
@RequiredArgsConstructor
public class GoalMarketsOddsCalculator {

    private final EventRepository eventRepository;

    @Value("${resenhabet.odds.avg-goals-per-side:2.0}")
    private double avgGoalsPerSide;

    @Value("${resenhabet.odds.elo-scale:400}")
    private double eloScale;

    @Value("${resenhabet.odds.elo-lambda-alpha:0.40}")
    private double eloLambdaAlpha;

    @Value("${resenhabet.odds.hist-lambda-threshold:10}")
    private int histLambdaThreshold;

    @Value("${resenhabet.odds.max-hist-lambda-weight:0.40}")
    private double maxHistLambdaWeight;

    @Value("${resenhabet.odds.min-odd:1.05}")
    private double minOdd;

    @Value("${resenhabet.odds.exact-score-top-n:8}")
    private int exactScoreTopN;

    public GoalMarketsOdds calculate(Player home, Player away) {
        double λHome = computeBlendedLambda(home, away);
        double λAway = computeBlendedLambda(away, home);

        double[][] matrix = PoissonCalculator.scoreMatrix(λHome, λAway);

        double pOver25 = PoissonCalculator.over(matrix, 3);
        double pOver35 = PoissonCalculator.over(matrix, 4);
        double pBtts   = PoissonCalculator.bttsYes(λHome, λAway, matrix);

        List<PoissonCalculator.ScoreEntry> exactScores =
            PoissonCalculator.topScores(matrix, exactScoreTopN);

        return new GoalMarketsOdds(
            toOdd(pOver25),  toOdd(1 - pOver25),
            toOdd(pOver35),  toOdd(1 - pOver35),
            toOdd(pBtts),    toOdd(1 - pBtts),
            exactScores.stream().collect(Collectors.toMap(
                PoissonCalculator.ScoreEntry::label,
                e -> toOdd(e.probability())))
        );
    }

    private double computeBlendedLambda(Player self, Player opponent) {
        double λElo = avgGoalsPerSide * Math.pow(
            Math.pow(10.0, (self.getCurrentElo() - opponent.getCurrentElo()) / (2.0 * eloScale)),
            eloLambdaAlpha
        );

        long sampleSize = Math.min(
            eventRepository.countCompletedMatchesByPlayer(self.getId()),
            eventRepository.countCompletedMatchesByPlayer(opponent.getId())
        );

        double weight = Math.min((double) sampleSize / histLambdaThreshold, maxHistLambdaWeight);
        if (weight <= 0.0) return λElo;

        double league  = Optional.ofNullable(eventRepository.findGlobalAvgGoalsPerSide())
                                 .orElse(avgGoalsPerSide);
        double attack  = Optional.ofNullable(eventRepository.findAvgGoalsScoredByPlayer(self.getId()))
                                 .map(v -> v / league).orElse(1.0);
        double defense = Optional.ofNullable(eventRepository.findAvgGoalsConcededByPlayer(opponent.getId()))
                                 .map(v -> v / league).orElse(1.0);
        double λHist   = attack * defense * league;

        return (1 - weight) * λElo + weight * λHist;
    }

    private double toOdd(double probability) {
        if (probability <= 0.0) return minOdd;
        return Math.max(1.0 / probability, minOdd);
    }

    public record GoalMarketsOdds(
        double over25,  double under25,
        double over35,  double under35,
        double bttsYes, double bttsNo,
        Map<String, Double> exactScoreOdds   // "2-1" → 6.50, "Outro Placar" → 1.30
    ) {}
}
```

---

## 10. Migrations necessárias

### Única migration nova: dropar constraint de BetSlipItem

```sql
-- V42__FIX_BET_SLIP_ITEM_UNIQUE_CONSTRAINT.sql
-- Verificar nome exato da constraint no schema antes de aplicar
ALTER TABLE resenha.bet_slip_item
    DROP CONSTRAINT IF EXISTS uq_bet_slip_item_event;
-- Nota: a regra "um bet por market por slip" passa a ser enforçada
-- exclusivamente em BetServiceImpl.placeBet() — sem nova constraint no banco.
```

> **`spring.flyway.target`:** atualizar de `41` para `42` em `application.properties`
> para que esta migration rode.

### Migrations que NÃO são necessárias

| Migration | Motivo |
|---|---|
| `market_type` column em `market` | Já existe via `V30__ADD_MARKET_TYPE.sql` |
| `UNIQUE(event_id, market_type)` em `market` | Já existe |
| `market_id` em `bet_slip_item` | Não necessário — enforcement em código |

---

## 11. Configuração

```properties
# --- Configurações existentes (não alterar) ---
resenhabet.odds.elo-scale=400
resenhabet.odds.draw-factor=0.12
resenhabet.odds.max-h2h-weight=0.20
resenhabet.odds.min-odd=1.05
resenhabet.odds.h2h-match-limit=10

# --- Novos parâmetros para goal markets ---
resenhabet.odds.avg-goals-per-side=2.0
# Média de gols por lado quando não há histórico suficiente.
# Calibrar após primeiras partidas. Se jogos terminam tipicamente 3-2 ou 4-1, 2.0–2.5 é razoável.

resenhabet.odds.elo-lambda-alpha=0.40
# Sensibilidade do Elo sobre o λ. 0 = Elo não afeta gols. 0.5 = efeito proporcional à raiz da razão.

resenhabet.odds.hist-lambda-threshold=10
# Partidas para atingir o peso máximo do histórico (análogo a h2h-match-limit).

resenhabet.odds.max-hist-lambda-weight=0.40
# Peso máximo do histórico no blend (40%). Análogo ao max-h2h-weight do MATCH_RESULT.

resenhabet.odds.exact-score-top-n=8
# Placares específicos no Exact Score. Os demais são agregados em "Outro Placar".

# Atualizar target para incluir V42
spring.flyway.target=42
```

---

## 12. Arquivos a criar ou modificar

| Arquivo | Ação | Detalhe |
|---|---|---|
| `MarketType.java` | **Modificar** | Adicionar apenas `QUALIFY` |
| `OddsResult.java` | **Modificar** | Adicionar `pHome` e `pAway` ao record |
| `OddsCalculator.java` | **Modificar** | Expor `pHome`/`pAway` no `OddsResult` retornado |
| `PoissonCalculator.java` | **Criar** | Utilitário puro em `domain/utils/` |
| `GoalMarketsOddsCalculator.java` | **Criar** | Spring component em `service/` |
| `EventRepository.java` | **Modificar** | 4 novas queries JPQL |
| `EventServiceImpl.java` | **Modificar** | Criação de markets (MATCH_RESULT vira 3 outcomes em knockout; novos markets) |
| `TournamentServiceImpl.java` | **Modificar** | Idem — comportamento idêntico ao `EventServiceImpl` |
| `BetServiceImpl.java` | **Modificar** | (1) check "um bet por market" em `placeBet()`; (2) settlement com novos labels e `QUALIFY` |
| `V42__FIX_BET_SLIP_ITEM_UNIQUE_CONSTRAINT.sql` | **Criar** | Dropar constraint antiga |
| `application.properties` | **Modificar** | 5 novos parâmetros + `flyway.target=42` |

---

## 13. Ordem de implementação recomendada

1. **`V42` migration + `flyway.target=42`** — dropar constraint; sem isso `placeBet` pode rejeitar cenários novos enquanto o resto é desenvolvido
2. **`MarketType.QUALIFY`** — enum change; dependência de tudo que vem depois
3. **`OddsResult` + `OddsCalculator`** — expor `pHome`/`pAway`; dependência do `QUALIFY` market creation
4. **`PoissonCalculator`** — escrever testes unitários antes do código de produção
5. **Queries em `EventRepository`** — testar individualmente com dados reais
6. **`GoalMarketsOddsCalculator`** — integra calculator + queries + blend
7. **`EventServiceImpl` + `TournamentServiceImpl`** — criação de markets (MATCH_RESULT knockout agora 3 outcomes + novos markets)
8. **`BetServiceImpl`** — check de market duplicado em `placeBet()` + settlement completo
9. **`application.properties`** — 5 novos parâmetros
10. **Calibrar `avg-goals-per-side`** após primeiras partidas com dados reais

---

## 14. Decisões em aberto (pós-implementação)

| Tópico | Situação |
|---|---|
| Threshold Over/Under ideal | Calibrar após dados reais — se Over 2.5 passa de ~85% hit rate, o market perde interesse; considerar Over 3.5 e Over 4.5 como par mais relevante para a resenha |
| Labels como constantes | Centralizar strings de outcome em constantes ou enum de labels para eliminar risco de divergência entre criação e settlement |
| Cache de aggregates | Se a criação de eventos degradar em performance, cachear `avgGoalsScored`/`avgGoalsConceded` por jogador e invalidar via `@TransactionalEventListener` após `finishEvent` |
| `QUALIFY` + multi-group | Confirmar que `"{nome} avança"` não colide em torneios compartilhados quando multi-group for implementado |
