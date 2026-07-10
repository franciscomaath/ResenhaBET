# Event Page Backend Gaps

## 1. Per-market mutation target is missing

- Endpoint: `POST /api/v1/markets/{eventId}/status`
- DTO changes: request needs a market selector (`marketId` or `marketType`) so the frontend can mutate one market at a time.
- Request: current frontend only knows `{ status: 'OPEN' | 'CLOSED' }`.
- Response: should return the updated `MarketResponseDto` or the full `MarketResponseDto[]`, but the selected market must be unambiguous.
- Business rules: market status changes must remain independent per market.
- Validation: reject updates when the target market is missing or not part of the event.
- Priority: high.
- Reason: the Event screen now renders multiple market sections, each with its own status. The current contract does not specify which market is being changed.

## 2. Wallet top-up source of truth must be the tournament wallet

- Endpoint: `POST /api/v1/wallet/deposit` and `POST /api/v1/wallet/deposit-all?tournamentId={id}&amount={value}`
- DTO changes: confirm that the request is bound to `TournamentWallet` / `groupTournamentId + userId` rather than the legacy global `Wallet` entity.
- Request: current frontend sends `userId`, `tournamentId`, and `amount`.
- Response: should return the updated tournament wallet balance for the target user or the affected users.
- Business rules: deposits must affect the current tournament wallet scope only.
- Validation: reject deposits that would target the legacy global wallet path.
- Priority: high.
- Reason: the Event admin panel includes wallet actions, and a naive implementation could regress to the legacy global wallet.

## 3. Market status lifecycle contract is still narrow

- Endpoint: `POST /api/v1/markets/{eventId}/status`
- DTO changes: if the UI ever exposes suspend/cancel controls, the request must support `SUSPENDED` and `CANCELLED` in addition to `OPEN` and `CLOSED`.
- Request: current frontend request is limited to `OPEN | CLOSED`.
- Response: should preserve each market's independent status.
- Business rules: `SUSPENDED` and `CANCELLED` are valid market states in the domain model.
- Validation: enforce allowed transitions server-side.
- Priority: medium.
- Reason: the Event page can now display these statuses, but the mutation contract does not currently expose them.

## 4. Knockout `QUALIFY` market exposure must be confirmed

- Endpoint: `GET /api/v1/markets/{eventId}` and `/topic/markets/{eventId}`
- DTO changes: `MarketResponseDto.marketType` must be allowed to return `QUALIFY`.
- Request: no new request shape required.
- Response: knockout events should surface a `QUALIFY` market when the tournament configuration includes it.
- Business rules: `QUALIFY` must only be shown when `event.isKnockout === true`.
- Validation: backend should not expose `QUALIFY` for non-knockout events.
- Priority: medium.
- Reason: the Event screen now renders `QUALIFY` when present, and the tournament creation flow allows the type. Without backend support the section cannot populate.
