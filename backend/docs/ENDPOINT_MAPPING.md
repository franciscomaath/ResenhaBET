# Endpoint Mapping

Mapeamento gerado a partir de `src/main/java/com/franciscomaath/resenhaapi/controller/`.

## Criterios

- **Admin-only**: endpoint com `Admin only` indicado no controller ou validacao explicita de admin.
- **Group-admin only**: endpoint com validacao explicita de admin do torneio/grupo no controller.
- **Geral**: endpoint sem restricao indicada no controller.

## Auth

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | Geral |
| GET | `/api/v1/auth/me` | Geral |
| POST | `/api/v1/auth/logout` | Geral |
| PATCH | `/api/v1/auth/pin` | Geral |

## Bets

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/bets` | Geral |
| GET | `/api/v1/bets/me` | Geral |
| GET | `/api/v1/bets?eventId={eventId}` | Admin-only |

## Competitions

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/competitions` | Admin-only |
| GET | `/api/v1/competitions?active={active}` | Geral |
| GET | `/api/v1/competitions/{id}` | Geral |
| PATCH | `/api/v1/competitions/{id}` | Admin-only |

## Events

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/events` | Geral |
| GET | `/api/v1/events?tournamentId={tournamentId}&status={status}` | Geral |
| GET | `/api/v1/events/{id}` | Geral |
| POST | `/api/v1/events/{id}/score` | Geral |
| POST | `/api/v1/events/{id}/start` | Geral |
| POST | `/api/v1/events/{id}/end` | Geral |
| PATCH | `/api/v1/events/{id}/players` | Admin-only |
| PATCH | `/api/v1/events/{id}/penalties` | Geral |
| PATCH | `/api/v1/events/{id}` | Group-admin/System-admin |
| PATCH | `/api/v1/events/{id}/datetime` | Group-admin/System-admin |
| POST | `/api/v1/events/{id}/cancel` | Group-admin/System-admin |
| DELETE | `/api/v1/events/{id}` | Group-admin/System-admin |

## Groups

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/groups` | Geral |
| GET | `/api/v1/groups` | Geral |
| GET | `/api/v1/groups/{id}/members` | Geral |
| POST | `/api/v1/groups/{id}/members` | Geral |
| POST | `/api/v1/groups/{id}/claim-player` | Geral |
| GET | `/api/v1/groups/{id}/players/available` | Geral |
| GET | `/api/v1/groups/{id}/ranking` | Geral |
| POST | `/api/v1/groups/{id}/switch` | Geral |
| POST | `/api/v1/groups/{id}/recalculate-elo` | Admin-only |
| PATCH | `/api/v1/groups/{id}` | Group-owner ou admin |
| DELETE | `/api/v1/groups/{id}` | Group-owner |
| DELETE | `/api/v1/groups/{id}/members/{userId}` | Group-owner ou admin |
| PATCH | `/api/v1/groups/{id}/members/{userId}/role` | Group-owner |

## Markets

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| GET | `/api/v1/markets/{eventId}` | Geral |
| POST | `/api/v1/markets/{eventId}/status` | Admin-only para `REAL_FOOTBALL`; Group-admin only para os demais tipos |

## Players

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/players` | Geral |
| GET | `/api/v1/players` | Geral |
| GET | `/api/v1/players/{id}` | Geral |
| PUT | `/api/v1/players/{id}` | Geral |
| PATCH | `/api/v1/players/{id}/link-user` | Admin-only |
| GET | `/api/v1/players/{id}/stats?tournamentId={tournamentId}` | Geral |
| PATCH | `/api/v1/players/{id}/active` | Group-admin |
| DELETE | `/api/v1/players/{id}` | Group-admin |

## Teams

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/teams` | Geral |
| GET | `/api/v1/teams` | Geral |
| GET | `/api/v1/teams/{id}` | Geral |
| PATCH | `/api/v1/teams/{id}/game-forecast-id` | Geral |

## Tournaments

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| POST | `/api/v1/tournaments` | Geral |
| GET | `/api/v1/tournaments` | Geral |
| GET | `/api/v1/tournaments/{id}/players` | Geral |
| POST | `/api/v1/tournaments/{id}/players/{playerId}/team` | Geral |
| GET | `/api/v1/tournaments/tournament-group-config?playerCount={playerCount}` | Geral |
| GET | `/api/v1/tournaments/{id}/rounds` | Geral |
| GET | `/api/v1/tournaments/{id}/scoreboard` | Geral |
| GET | `/api/v1/tournaments/{id}/ranking` | Geral |
| POST | `/api/v1/tournaments/{id}/players` | Geral |
| POST | `/api/v1/tournaments/{id}/start` | Geral |
| POST | `/api/v1/tournaments/{id}/advance-to-bracket` | Admin-only |
| POST | `/api/v1/tournaments/{id}/force-advance-to-bracket` | Admin-only |
| POST | `/api/v1/tournaments/{id}/sync-fixtures` | Admin-only |
| POST | `/api/v1/tournaments/{id}/sync-odds` | Admin-only |
| PATCH | `/api/v1/tournaments/{id}` | Group-admin |
| POST | `/api/v1/tournaments/{id}/cancel` | Group-admin |
| DELETE | `/api/v1/tournaments/{id}` | Group-admin |

## Users

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| GET | `/api/v1/users` | Geral |
| POST | `/api/v1/users` | Geral |
| PATCH | `/api/v1/users/{id}/reset-pin` | Admin-only |
| GET | `/api/v1/users/{id}` | Geral |
| PATCH | `/api/v1/users/{id}` | Geral |

## Wallet

| Metodo | Endpoint | Acesso |
| --- | --- | --- |
| GET | `/api/v1/wallet?userId={userId}&tournamentId={tournamentId}` | Geral |
| POST | `/api/v1/wallet/deposit` | Admin-only |
| POST | `/api/v1/wallet/deposit-all?tournamentId={tournamentId}&amount={amount}` | Admin-only |
