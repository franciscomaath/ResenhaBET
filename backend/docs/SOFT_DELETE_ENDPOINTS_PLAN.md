# ResenhaBET — Soft Delete e Endpoints Faltantes para Boa UX

## Objetivo

Implementar uma estratégia segura de **soft delete** usando apenas `deleted_at` nas entidades que precisam ser removíveis pela interface, evitando hard delete em dados com histórico esportivo, financeiro, apostas, wallets, eventos e entidades compartilhadas entre grupos.

A decisão principal é: **não apagar fisicamente dados de negócio importantes**. Em vez disso, marcar como deletado/cancelado e esconder das listagens normais.

Isso é especialmente importante no ResenhaBET porque:

- `Tournament`, `Event`, `Market` e `Outcome` podem estar ligados a apostas, resultados, transações e estatísticas.
- `REAL_FOOTBALL` pode ser compartilhado por múltiplos grupos através de `GroupTournament`.
- `TournamentWallet`, `BetSlip`, `Transaction` e histórico de resultados precisam continuar auditáveis.
- Um hard delete pode quebrar FK, histórico financeiro, settlement de apostas e isolamento multi-group.

---

## Decisão de Produto

### Regra geral

Toda entidade removível pela UX deve receber:

```sql
deleted_at TIMESTAMP NULL
```

Quando `deleted_at IS NULL`, a entidade está ativa.

Quando `deleted_at IS NOT NULL`, a entidade foi removida logicamente e deve ser escondida das listagens comuns.

### Diferença entre `deleted_at`, `active` e `status`

| Campo | Uso |
| --- | --- |
| `deleted_at` | Remoção lógica: o item não deve mais aparecer como opção normal na UI. |
| `active` | Disponibilidade operacional: ativo/inativo, mas ainda visível em alguns contextos. |
| `status` | Estado de fluxo de negócio: CREATED, IN_PROGRESS, COMPLETED, CANCELLED etc. |

Exemplo:

- Um `Player` criado errado pode receber `deleted_at`.
- Um `Tournament` em andamento cancelado deve ter `status = CANCELLED`, e talvez não `deleted_at`.
- Um `Competition` antiga pode ficar `active = false`, não necessariamente deletada.

---

## Entidades Recomendadas para `deleted_at`

### P0 — necessário para boa UX

Adicionar `deleted_at` em:

```text
Tournament
Event
Player
Team
Group
GroupMember
Competition
```

### P1 — avaliar depois

Adicionar `deleted_at` em:

```text
TournamentPlayer
GroupTournament
TournamentRound
```

Normalmente essas entidades são melhor tratadas por remoção controlada de relacionamento ou cancelamento do agregado principal, não por delete direto.

### Não recomendado

Não usar soft delete simples em:

```text
BetSlip
BetSlipItem
TournamentWallet
Transaction
Market
Outcome
```

Motivo: essas entidades são histórico financeiro/apostas/settlement. Para elas, usar `status = CANCELLED`, `BET_REFUND`, `MarketStatus.CANCELLED`, etc.

---

## Estratégia por Domínio

# 1. Tournaments

## Problema atual

Hoje existem endpoints para criar, listar, iniciar, adicionar players, rounds, scoreboard, sync e avançar bracket, mas falta:

- Ver detalhe de um torneio específico.
- Editar torneio antes de iniciar.
- Remover torneio criado errado.
- Cancelar torneio em andamento.

## Endpoints recomendados

```http
GET    /api/v1/tournaments/{id}
PATCH  /api/v1/tournaments/{id}
DELETE /api/v1/tournaments/{id}
POST   /api/v1/tournaments/{id}/cancel
```

## `GET /api/v1/tournaments/{id}`

Retorna o detalhe completo do torneio dentro do grupo ativo.

Deve validar:

- Usuário autenticado.
- Grupo ativo selecionado.
- `GroupTournament` existe para `currentGroup` + `tournamentId`.
- `Tournament.deletedAt IS NULL`, exceto se houver query/admin para incluir deletados.

Uso no frontend:

- Página de detalhes do torneio.
- Tela de edição.
- Tela de confirmação de cancelamento.
- Evitar depender somente de listagem + rounds/scoreboard.

## `PATCH /api/v1/tournaments/{id}`

Edita dados do torneio antes de iniciar.

Campos possíveis:

```json
{
  "name": "Copa Resenha 2026",
  "format": "LEAGUE_BRACKET",
  "generationMode": "AUTO",
  "hasThirdPlaceMatch": true,
  "numberOfGroups": 2,
  "playersAdvancingPerGroup": 2,
  "marketTypes": ["MATCH_RESULT", "OVER_UNDER_25", "BTTS"]
}
```

Regras:

- Permitido apenas se `status = CREATED`.
- Bloquear se `IN_PROGRESS`, `COMPLETED` ou `CANCELLED`.
- Para `FIFA_MATCH`, group admin pode editar.
- Para `REAL_FOOTBALL`, cuidado: `Tournament` pode ser compartilhado. Não permitir group admin editar infraestrutura compartilhada.
- `marketTypes` deve ser editado no `GroupTournament`, não diretamente no `Tournament`.

Recomendação:

- Separar edição de dados globais do torneio e settings do grupo.
- Para `REAL_FOOTBALL`, group admin só deveria editar configurações do próprio `GroupTournament`.

## `DELETE /api/v1/tournaments/{id}`

Soft delete para torneio criado errado.

Implementação:

```java
tournament.setDeletedAt(LocalDateTime.now());
```

Regras obrigatórias:

- Permitir somente se `status = CREATED`.
- Bloquear se houver apostas reais associadas.
- Bloquear se houver `TournamentWallet` com transações além de depósito inicial, ou definir regra explícita.
- Para `FIFA_MATCH`, pode marcar o `Tournament` como deletado.
- Para `REAL_FOOTBALL`, se compartilhado por vários grupos, **não deletar o Tournament global** por ação de um grupo.

Para `REAL_FOOTBALL`, o comportamento correto é:

- Se o objetivo é remover somente do grupo atual, soft delete/desanexar o `GroupTournament`.
- Se o objetivo é remover a infraestrutura global compartilhada, exigir `userType == ADMIN` e garantir que nenhum grupo esteja usando.

### Regra recomendada

```text
DELETE /tournaments/{id}
```

Para `FIFA_MATCH`:

- `Tournament.deleted_at = now()`.
- Ocultar eventos, rounds e tournament players relacionados por consequência.

Para `REAL_FOOTBALL`:

- Se existe mais de um `GroupTournament`, marcar somente o `GroupTournament` do grupo atual como deletado.
- Se existe apenas um `GroupTournament` e o usuário é system admin, pode marcar o `Tournament` como deletado.
- Não apagar `Event`, `Market` ou `Outcome` compartilhados automaticamente.

## `POST /api/v1/tournaments/{id}/cancel`

Cancela torneio que já começou.

Diferença para delete:

- Delete = criado errado, remove da UX.
- Cancel = competição existiu, mas foi encerrada antes da conclusão.

Implementação:

```java
tournament.setStatus(TournamentStatus.CANCELLED);
```

Regras:

- Permitido em `CREATED` ou `IN_PROGRESS`.
- Bloquear se `COMPLETED`.
- Cancelar ou suspender eventos pendentes.
- Cancelar mercados abertos.
- Reembolsar apostas pendentes.
- Criar `Transaction` do tipo `BET_REFUND`.
- Não apagar histórico.

Para `REAL_FOOTBALL`:

- Cancelar o `Tournament` global afeta todos os grupos.
- Deve exigir `userType == ADMIN`.
- Se o grupo quer apenas sair do torneio, usar endpoint de remoção/soft delete do `GroupTournament`.

---

# 2. Events

## Problema atual

Eventos podem ser criados, listados, iniciados, ter score atualizado e finalizados, mas falta UX segura para:

- Cancelar partida criada errada.
- Remover partida manual ainda sem uso.
- Reagendar horário.
- Corrigir dados gerais.
- Reabrir/estornar resultado quando houve erro humano.

## Endpoints recomendados

```http
PATCH  /api/v1/events/{id}
PATCH  /api/v1/events/{id}/datetime
POST   /api/v1/events/{id}/cancel
DELETE /api/v1/events/{id}
POST   /api/v1/events/{id}/reopen
POST   /api/v1/events/{id}/reset-result
```

## `PATCH /api/v1/events/{id}`

Edição geral controlada.

Campos possíveis:

```json
{
  "roundId": 10,
  "playerHomeId": 1,
  "playerAwayId": 2,
  "teamHomeId": null,
  "teamAwayId": null,
  "gameDatetime": "2026-07-01T20:00:00",
  "isKnockout": false,
  "isThirdPlaceMatch": false
}
```

Regras:

- Permitido somente se `status = CREATED`.
- Bloquear se evento já começou, terminou ou tem apostas em mercados fechados.
- Para `REAL_FOOTBALL`, alterações de time/data devem ser restritas a `userType == ADMIN`.
- Para `FIFA_MATCH`, group admin pode editar.

## `PATCH /api/v1/events/{id}/datetime`

Endpoint específico para reagendamento.

Campos:

```json
{
  "gameDatetime": "2026-07-01T21:30:00"
}
```

Regras:

- Permitido se `CREATED`.
- Se mercado já estava `SUSPENDED` por proximidade do jogo, avaliar reabrir manualmente ou manter suspenso.
- Para `REAL_FOOTBALL`, system admin apenas.

## `POST /api/v1/events/{id}/cancel`

Cancela uma partida.

Regras:

- Pode ser usado quando partida não vai acontecer, foi anulada ou cadastrada errada mas já possui histórico/apostas.
- Setar `event.status = CANCELLED`.
- Setar `market.status = CANCELLED` para mercados relacionados.
- Cancelar `BetSlipItem` pendentes ligados ao evento.
- Recalcular status dos `BetSlip` afetados.
- Reembolsar stake quando necessário.
- Criar transação `BET_REFUND`.

Observação importante:

Se uma múltipla contém um item cancelado, definir regra de produto:

1. Cancelar a aposta inteira e reembolsar stake total; ou
2. Remover o item cancelado e recalcular odd combinada; ou
3. Tratar odd do item cancelado como `1.00`.

Recomendação para v1:

```text
Cancelar aposta inteira e reembolsar stake total se qualquer item PENDING for cancelado.
```

É mais simples, transparente e reduz bugs.

## `DELETE /api/v1/events/{id}`

Soft delete para evento criado errado.

Implementação:

```java
event.setDeletedAt(LocalDateTime.now());
```

Regras:

- Permitir somente se `status = CREATED`.
- Permitir somente se não há apostas associadas.
- Se houver mercados/outcomes auto-criados, eles podem permanecer escondidos pelo `event.deleted_at`, ou também receber `deleted_at` se o campo existir.
- Para eventos de bracket com `nextRoundEvent`, `homeSourceEvent` ou `awaySourceEvent`, bloquear delete se a remoção quebrar a árvore.

## `POST /api/v1/events/{id}/reopen`

Reabre evento finalizado por engano.

Regras:

- Alto risco.
- Exigir admin.
- Para `FIFA_MATCH`, group admin.
- Para `REAL_FOOTBALL`, system admin.
- Só permitir se for possível desfazer settlement.

Recomendação:

Implementar depois, não no primeiro lote.

## `POST /api/v1/events/{id}/reset-result`

Desfaz resultado e consequências.

Precisa reverter:

- `homeScore`
- `awayScore`
- `penaltiesHome`
- `penaltiesAway`
- `status`
- Elo dos players
- avanço de bracket
- BetSlipItem WON/LOST
- BetSlip WON/LOST
- créditos de `BET_WON`
- transações de settlement

Recomendação:

Não implementar no P0. Criar apenas quando tiver auditoria clara.

---

# 3. Players

## Problema atual

Player é group-scoped e pode ser editado por `PUT`, mas falta remoção lógica clara.

## Endpoints recomendados

```http
PATCH  /api/v1/players/{id}/active
DELETE /api/v1/players/{id}
POST   /api/v1/players/{id}/invite
```

## `DELETE /api/v1/players/{id}`

Soft delete:

```java
player.setDeletedAt(LocalDateTime.now());
```

Regras:

- Se player nunca participou de torneio/evento, pode soft deletar sem impacto.
- Se já participou, soft delete apenas remove de listagens e seleção futura.
- Não apagar histórico esportivo.
- Não permitir se o player está em torneio `IN_PROGRESS`, exceto se for apenas inativação futura.

## `PATCH /api/v1/players/{id}/active`

Alternativa mais simples para uso operacional.

Campos:

```json
{
  "active": false
}
```

Diferença:

- `active=false`: jogador ainda existe e pode aparecer em histórico.
- `deleted_at`: jogador foi criado errado ou removido da UI principal.

## `POST /api/v1/players/{id}/invite`

Gera token/link para um usuário reivindicar/vincular o player.

Motivo:

- Completa o fluxo multi-group de onboarding.
- Evita admin precisar linkar manualmente user/player.

Regras:

- Group admin/owner.
- Player deve pertencer ao grupo ativo.
- Player não pode já estar vinculado a outro user, exceto fluxo de troca controlado.

---

# 4. Groups e Members

## Problema atual

Já existe criar/listar/switch/members/claim-player, mas falta gestão real do grupo.

## Endpoints recomendados

```http
PATCH  /api/v1/groups/{id}
DELETE /api/v1/groups/{id}
DELETE /api/v1/groups/{id}/members/{userId}
PATCH  /api/v1/groups/{id}/members/{userId}/role
```

## `PATCH /api/v1/groups/{id}`

Edita nome/status do grupo.

Campos:

```json
{
  "name": "Resenha da Faculdade"
}
```

Regras:

- OWNER ou ADMIN do grupo.
- Nome único.
- Não permitir editar grupo deletado.

## `DELETE /api/v1/groups/{id}`

Soft delete do grupo.

Implementação:

```java
group.setDeletedAt(LocalDateTime.now());
```

Regras:

- Somente OWNER.
- Bloquear se há torneios em andamento.
- Pode permitir se todos os torneios estão `CREATED`, `COMPLETED` ou `CANCELLED`.
- Não apagar membros, players, wallets ou bets.
- Após deletado, grupo não aparece na troca de contexto.

## `DELETE /api/v1/groups/{id}/members/{userId}`

Remove membro do grupo.

Melhor implementação:

```java
groupMember.setDeletedAt(LocalDateTime.now());
```

Regras:

- OWNER ou ADMIN pode remover MEMBER.
- OWNER pode remover ADMIN.
- OWNER só pode remover outro OWNER se ainda sobrar pelo menos um OWNER ativo.
- Usuário não pode remover a si mesmo se for o único OWNER.
- Se o usuário tiver apostas pendentes, decidir regra de produto.

Recomendação para v1:

- Preservar `TournamentWallet`, `BetSlip` e `Transaction` como histórico read-only.
- Bloquear novas apostas porque o usuário não é mais membro ativo.
- Não apagar histórico.

## `PATCH /api/v1/groups/{id}/members/{userId}/role`

Promove/rebaixa membro.

Campos:

```json
{
  "role": "ADMIN"
}
```

Regras:

- OWNER pode promover/rebaixar ADMIN/MEMBER.
- Apenas OWNER pode conceder OWNER.
- Não permitir remover o último OWNER.
- Não permitir alterar role de membro deletado.

---

# 5. Users

## Problema atual

Existe criação, listagem e reset de PIN, mas falta detalhe e edição básica.

## Endpoints recomendados

```http
GET   /api/v1/users/{id}
PATCH /api/v1/users/{id}
```

## `GET /api/v1/users/{id}`

Retorna detalhe do usuário.

Regras:

- Admin pode ver qualquer usuário.
- Usuário pode ver a si mesmo.
- Cuidado com exposição de `pinHash` — nunca retornar.

## `PATCH /api/v1/users/{id}`

Edita nome ou dados básicos.

Campos:

```json
{
  "name": "Francisco Matheus"
}
```

Regras:

- Admin pode editar usuários.
- Usuário pode editar a si mesmo, se desejado.
- Não permitir alteração direta de `userType` por endpoint genérico.

---

# 6. Teams

## Problema atual

Time pode ser criado/listado/buscado, mas falta edição de dados visuais e correção.

## Endpoints recomendados

```http
PUT    /api/v1/teams/{id}
PATCH  /api/v1/teams/{id}
DELETE /api/v1/teams/{id}
```

## `PUT/PATCH /api/v1/teams/{id}`

Campos:

```json
{
  "name": "Brazil",
  "abbreviation": "BRA",
  "badgeUrl": "https://...",
  "country": "Brazil",
  "league": "National Teams",
  "apiFootballTeamId": 123,
  "gameForecastTeamId": "abc-123"
}
```

Regras:

- Como `Team` é global, idealmente exigir system admin.
- Badge URL deve ser corrigível após criação.
- `gameForecastTeamId` já tem endpoint específico, mas pode entrar no PATCH geral se fizer sentido.

## `DELETE /api/v1/teams/{id}`

Soft delete:

```java
team.setDeletedAt(LocalDateTime.now());
```

Regras:

- Bloquear se team está vinculado a `TournamentPlayer` ou `Event` ativo.
- Se já usado historicamente, apenas soft delete.
- Não apagar eventos antigos.

---

# 7. Competitions

## Problema atual

Competition pode ser criada/listada/buscada/editada, mas não removida.

## Endpoint recomendado

```http
DELETE /api/v1/competitions/{id}
```

## Regra

Soft delete:

```java
competition.setDeletedAt(LocalDateTime.now());
```

Ou, alternativamente:

```java
competition.setActive(false);
```

Recomendação:

- Usar `active=false` para competição antiga.
- Usar `deleted_at` para cadastro errado.

Bloqueios:

- Se houver `Tournament` vinculado, não deletar de verdade.
- Se for soft delete, esconder de novas criações.
- Existing tournaments continuam funcionando.

Acesso:

- Apenas `userType == ADMIN`, porque `Competition` é global.

---

# 8. Bets

## Problema atual

Usuário vê suas apostas e admin vê por evento, mas falta detalhe e cancelamento manual.

## Endpoints recomendados

```http
GET  /api/v1/bets/{id}
GET  /api/v1/bets?userId={id}
POST /api/v1/bets/{id}/cancel
```

## `GET /api/v1/bets/{id}`

Retorna detalhe da aposta:

- Slip
- Itens
- Evento
- Mercado
- Outcome
- Odd snapshot
- Stake
- Potential return
- Status
- Transações relacionadas

Regras:

- Usuário pode ver a própria aposta.
- Admin/group admin pode ver apostas do torneio/grupo, conforme regra de autorização.

## `GET /api/v1/bets?userId={id}`

Filtro administrativo.

Regras:

- Group admin pode ver apostas de usuários dentro do grupo ativo.
- System admin pode ter visão mais ampla, se necessário.

## `POST /api/v1/bets/{id}/cancel`

Cancela aposta PENDING.

Regras:

- Permitido somente se `BetSlip.status = PENDING`.
- Todos os eventos relacionados ainda precisam estar antes do início.
- Reembolsar stake.
- Criar `Transaction` tipo `BET_REFUND`.
- Marcar `BetSlip.status = CANCELLED`.
- Marcar `BetSlipItem.status = CANCELLED`.

Quem pode cancelar:

- Usuário dono da aposta antes do evento iniciar, se esse for o produto desejado.
- Group admin para correção operacional.

---

# 9. Wallet e Transactions

## Problema atual

Existe consulta de wallet e depósito, mas falta histórico financeiro visível.

## Endpoint recomendado

```http
GET /api/v1/wallet/transactions?groupTournamentId={id}
```

Filtros opcionais:

```http
GET /api/v1/wallet/transactions?groupTournamentId={id}&userId={id}&type=BET_REFUND
```

Resposta esperada:

```json
[
  {
    "id": 1,
    "type": "DEPOSIT",
    "amount": 1000,
    "balanceAfter": 1000,
    "betSlipId": null,
    "createdAt": "2026-06-27T15:00:00"
  },
  {
    "id": 2,
    "type": "BET_PLACED",
    "amount": -50,
    "balanceAfter": 950,
    "betSlipId": 20,
    "createdAt": "2026-06-27T16:00:00"
  }
]
```

Regras:

- Usuário normal vê apenas as próprias transações.
- Group admin pode ver transações do grupo/tournament.
- Sempre filtrar por `GroupTournament`, não apenas `Tournament`.

---

# 10. Markets

## Problema atual

Mercados são consultados por evento, mas falta visão de todos os mercados de um torneio.

## Endpoint recomendado

```http
GET /api/v1/markets?groupTournamentId={id}
```

Alternativa:

```http
GET /api/v1/tournaments/{id}/markets
```

Retornar:

- Event
- Market
- Outcomes
- Status
- Game datetime
- Teams/players
- Odds

Uso no frontend:

- Dashboard de apostas.
- Tela de mercados disponíveis.
- Evitar abrir evento por evento.

Regra importante:

- Mesmo que `Market` seja compartilhado em `REAL_FOOTBALL`, a visibilidade deve passar pelo `GroupTournament` do grupo ativo.

---

# 11. GroupTournament Settings

## Problema atual

O modelo multi-group define `GroupTournament` como limite de tenancy e lugar natural para settings por grupo, mas ainda falta endpoint explícito.

## Endpoints recomendados

```http
GET   /api/v1/tournaments/{id}/settings
PATCH /api/v1/tournaments/{id}/settings
```

Campos possíveis agora:

```json
{
  "marketTypes": ["MATCH_RESULT", "OVER_UNDER_25", "BTTS"]
}
```

Campos possíveis no futuro:

```json
{
  "initialBankroll": 1000,
  "minBet": 5,
  "maxBet": 100,
  "allowBetCancel": true,
  "marketTypes": ["MATCH_RESULT", "OVER_UNDER_25", "BTTS"]
}
```

Regras:

- Sempre buscar o `GroupTournament` pelo `tournamentId` + `currentGroupId`.
- Group admin/owner pode editar settings do seu próprio grupo.
- Não alterar settings de outros grupos.
- Não editar `Tournament` global para configurações que são do grupo.

---

# 12. Bracket View

## Problema atual

O frontend provavelmente precisa montar bracket combinando rounds + events.

## Endpoint recomendado

```http
GET /api/v1/tournaments/{id}/bracket
```

Retornar estrutura pronta:

```json
{
  "tournamentId": 1,
  "rounds": [
    {
      "roundId": 10,
      "name": "Semifinal",
      "order": 1,
      "events": [
        {
          "eventId": 100,
          "homeLabel": "Francisco",
          "awayLabel": "Jonas",
          "nextRoundEventId": 120,
          "homeSourceEventId": null,
          "awaySourceEventId": null,
          "status": "COMPLETED"
        }
      ]
    }
  ]
}
```

Regras:

- Backend pode resolver labels como "Vencedor de Semifinal 1".
- Evita duplicar lógica complexa no Angular.

---

## Migração de Banco

Criar migration Flyway:

```sql
ALTER TABLE resenha.tournament ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE resenha.event ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE resenha.player ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE resenha.team ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE resenha."group" ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE resenha.group_member ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE resenha.competition ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_tournament_deleted_at ON resenha.tournament(deleted_at);
CREATE INDEX idx_event_deleted_at ON resenha.event(deleted_at);
CREATE INDEX idx_player_deleted_at ON resenha.player(deleted_at);
CREATE INDEX idx_team_deleted_at ON resenha.team(deleted_at);
CREATE INDEX idx_group_deleted_at ON resenha."group"(deleted_at);
CREATE INDEX idx_group_member_deleted_at ON resenha.group_member(deleted_at);
CREATE INDEX idx_competition_deleted_at ON resenha.competition(deleted_at);
```

Se usar `GroupTournament.deleted_at`:

```sql
ALTER TABLE resenha.group_tournament ADD COLUMN deleted_at TIMESTAMP NULL;
CREATE INDEX idx_group_tournament_deleted_at ON resenha.group_tournament(deleted_at);
```

---

## Ajustes em Entidades Java

Criar interface opcional:

```java
public interface SoftDeletable {
    LocalDateTime getDeletedAt();
    void setDeletedAt(LocalDateTime deletedAt);

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }
}
```

Adicionar nas entidades:

```java
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

Método helper:

```java
public void softDelete() {
    this.deletedAt = LocalDateTime.now();
}
```

---

## Ajustes em Repositories

Evitar `findAll()` sem filtro em entidades com soft delete.

Exemplo:

```java
List<Tournament> findByDeletedAtIsNull();

Optional<Tournament> findByIdAndDeletedAtIsNull(Long id);
```

Para entidades group-scoped:

```java
List<Player> findByGroupIdAndDeletedAtIsNull(Long groupId);

Optional<Player> findByIdAndGroupIdAndDeletedAtIsNull(Long id, Long groupId);
```

Para `GroupTournament`:

```java
Optional<GroupTournament> findByTournamentIdAndGroupIdAndDeletedAtIsNull(Long tournamentId, Long groupId);

boolean existsByTournamentIdAndGroupIdAndDeletedAtIsNull(Long tournamentId, Long groupId);
```

---

## Ajustes em Queries de Visibilidade

Toda validação de tenancy deve ignorar vínculos deletados:

```java
groupTournamentRepository.existsByTournamentIdAndGroupIdAndDeletedAtIsNull(
    tournamentId,
    currentGroupId
);
```

Toda listagem normal deve esconder deletados:

```sql
WHERE deleted_at IS NULL
```

Criar query param opcional apenas para admin:

```http
GET /api/v1/tournaments?includeDeleted=true
```

---

## Autorização Recomendada

| Ação | FIFA_MATCH | REAL_FOOTBALL |
| --- | --- | --- |
| Editar torneio local | OWNER/ADMIN do grupo | Não editar infraestrutura global |
| Editar settings do GroupTournament | OWNER/ADMIN do grupo | OWNER/ADMIN do grupo |
| Cancelar torneio global | OWNER/ADMIN do grupo | System ADMIN |
| Remover torneio da visão do grupo | OWNER/ADMIN do grupo | OWNER/ADMIN do grupo via GroupTournament |
| Editar evento | OWNER/ADMIN do grupo | System ADMIN |
| Cancelar evento | OWNER/ADMIN do grupo | System ADMIN |
| Editar competição | System ADMIN | System ADMIN |
| Editar time global | System ADMIN | System ADMIN |

---

## Ordem de Implementação Recomendada

## Fase 1 — Base do soft delete

1. Criar migration com `deleted_at`.
2. Adicionar campo nas entidades.
3. Ajustar repositories para filtrar `deletedAt IS NULL`.
4. Ajustar services principais para não retornar deletados.
5. Ajustar validação de `GroupTournament` para ignorar deletados.

## Fase 2 — Tournaments

1. `GET /api/v1/tournaments/{id}`.
2. `PATCH /api/v1/tournaments/{id}`.
3. `DELETE /api/v1/tournaments/{id}` como soft delete restrito.
4. `POST /api/v1/tournaments/{id}/cancel` com cancelamento seguro.
5. Tests de FIFA_MATCH e REAL_FOOTBALL compartilhado.

## Fase 3 — Events

1. `PATCH /api/v1/events/{id}`.
2. `PATCH /api/v1/events/{id}/datetime`.
3. `POST /api/v1/events/{id}/cancel`.
4. `DELETE /api/v1/events/{id}` soft delete restrito.
5. Tests com apostas pendentes e sem apostas.

## Fase 4 — Groups e Members

1. `PATCH /api/v1/groups/{id}`.
2. `DELETE /api/v1/groups/{id}/members/{userId}`.
3. `PATCH /api/v1/groups/{id}/members/{userId}/role`.
4. `DELETE /api/v1/groups/{id}`.
5. Tests para último OWNER.

## Fase 5 — UX financeira

1. `GET /api/v1/wallet/transactions`.
2. `GET /api/v1/bets/{id}`.
3. `POST /api/v1/bets/{id}/cancel`.
4. `GET /api/v1/markets?groupTournamentId={id}`.

## Fase 6 — Catálogo e QoL

1. `PUT/PATCH /api/v1/teams/{id}`.
2. `DELETE /api/v1/teams/{id}`.
3. `DELETE /api/v1/competitions/{id}`.
4. `GET /api/v1/tournaments/{id}/bracket`.

---

## Testes Obrigatórios

### Tournament soft delete

- Não listar torneio deletado.
- Não permitir acessar detalhe deletado.
- Não permitir iniciar torneio deletado.
- Não permitir apostar em evento de torneio deletado.
- FIFA_MATCH deletado não aparece no grupo.
- REAL_FOOTBALL compartilhado não é deletado globalmente por admin de grupo.

### Event soft delete/cancel

- Evento deletado não aparece na listagem.
- Evento deletado não permite start/end/score.
- Evento com bet não pode ser deletado, apenas cancelado.
- Cancelamento reembolsa aposta pendente.
- Cancelamento não duplica refund em chamada repetida.

### Group/member soft delete

- Membro removido não consegue trocar para o grupo.
- Membro removido não consegue apostar no grupo.
- Histórico financeiro do membro removido permanece consultável para admin.
- Não é possível remover o último OWNER.

### Tenancy

- Grupo A não vê torneio deletado/desanexado.
- Grupo B continua vendo REAL_FOOTBALL compartilhado se apenas Grupo A saiu.
- Queries usam `GroupTournament.deleted_at IS NULL`, se esse campo existir.

---

## Riscos e Cuidados

## 1. `@Where` / Hibernate filter

Pode parecer tentador usar:

```java
@Where(clause = "deleted_at IS NULL")
```

Mas isso pode atrapalhar telas/admin que precisam consultar deletados ou históricos.

Recomendação:

- Usar queries explícitas nos repositories inicialmente.
- Só usar `@Where` em entidades onde nunca será necessário buscar deletados normalmente.

## 2. Soft delete não substitui status

Não usar `deleted_at` para tudo.

Exemplo errado:

```text
Evento cancelado => deleted_at = now()
```

Melhor:

```text
Evento cancelado => status = CANCELLED
Evento criado errado e sem uso => deleted_at = now()
```

## 3. Dados financeiros nunca devem sumir

Nunca deletar:

- Transaction
- TournamentWallet
- BetSlip
- BetSlipItem

A regra é cancelar/refundar, não apagar.

## 4. REAL_FOOTBALL compartilhado

Nunca permitir que uma ação de grupo apague/cancele infraestrutura global compartilhada sem system admin.

Para o grupo sair de um torneio real:

```text
Soft delete em GroupTournament, não em Tournament.
```

---

## Resumo Executivo

Implementar soft delete com `deleted_at` é a melhor escolha para o ResenhaBET.

A abordagem recomendada é:

1. Usar `deleted_at` para esconder entidades criadas errado.
2. Usar `status = CANCELLED` para fluxos de negócio que realmente foram cancelados.
3. Nunca apagar histórico financeiro/apostas.
4. Em `REAL_FOOTBALL`, preferir remover/desativar `GroupTournament` do grupo atual em vez de deletar o `Tournament` compartilhado.
5. Começar por `Tournament`, `Event`, `GroupMember`, `Player`, `Team` e `Competition`.
6. Implementar endpoints de detalhe/edição/cancelamento antes dos hard cases como reset de resultado.

Prioridade real para frontend/admin:

```http
GET    /api/v1/tournaments/{id}
PATCH  /api/v1/tournaments/{id}
DELETE /api/v1/tournaments/{id}
POST   /api/v1/tournaments/{id}/cancel

PATCH  /api/v1/events/{id}
POST   /api/v1/events/{id}/cancel
DELETE /api/v1/events/{id}

PATCH  /api/v1/groups/{id}
DELETE /api/v1/groups/{id}/members/{userId}
PATCH  /api/v1/groups/{id}/members/{userId}/role

GET    /api/v1/wallet/transactions?groupTournamentId={id}
GET    /api/v1/bets/{id}
GET    /api/v1/markets?groupTournamentId={id}
```

Essa base resolve a maior parte dos problemas de UX sem colocar em risco histórico, apostas, wallets, multi-group e dados compartilhados.
