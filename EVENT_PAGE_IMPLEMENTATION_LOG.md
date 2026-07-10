# Event Page Implementation Log

## Step 1 - Cosmetic refactor

- Files modified:
  - `src/app/pages/event/event-page.html`
  - `src/app/pages/event/event-page.ts`
- Changes made:
  - Restyled the Event page shell to match the dark prototype more closely.
  - Updated top navigation, event card, market card, admin panel, wallet card, and bet history surfaces to use the current brand palette and rounded card language.
  - Removed legacy cyan-heavy visual treatment from the page and replaced it with blue/green/red semantic tokens.
  - Added `CANCELLED` display text for market status and `QUALIFY` label support in `marketTypeLabel()`.
- Rationale:
  - The first pass needed to be purely cosmetic while preserving existing event, market, wallet, and bet-history behavior.
  - The page now visually matches the exported prototype more closely without changing backend contracts.
- Implementation decisions:
  - Kept the current single-active-market behavior for now; data-driven multi-market rendering is the next step.
  - Reused existing signals and handlers instead of introducing new abstractions.
- Issues found during review:
  - Tailwind/Angular class bindings initially used invalid bracketed `class` bindings.
  - The sticky header background used an invalid arbitrary opacity suffix on a CSS variable.
- Fixes applied:
  - Replaced invalid class bindings with `ngClass`.
  - Switched the sticky header background to a valid `rgba(...)` arbitrary value.
  - Verified the refactor with `npm run build`.
- Remaining gaps:
  - Markets are still rendered as a single active market in the UI.
  - Market mutation remains constrained by the current backend contract.
  - Admin authorization still needs to be split by `FIFA_MATCH` vs `REAL_FOOTBALL` scope.

## Step 2 - Data-driven market rendering

- Files modified:
  - `src/app/pages/event/event-page.html`
  - `src/app/pages/event/event-page.ts`
  - `src/app/components/betting/market-accordion.html`
  - `src/app/services/api/api.models.ts`
- Changes made:
  - Replaced the single active-market rendering flow with a data-driven market list.
  - Added market-type chips driven by the loaded market data, including an explicit `Todos` filter.
  - Rendered each market through `app-market-accordion` with its own status badge.
  - Added per-market open/close state in signals and a dedicated exact-score expand/collapse signal.
  - Filtered `QUALIFY` out when the event is not knockout.
  - Expanded frontend types to include `QUALIFY` and `MarketResponseDto.status = CANCELLED`.
- Rationale:
  - The prototype shows a list of market cards, not a single global market card.
  - The screen must reflect independent market statuses and avoid a global status badge.
- Implementation decisions:
  - Kept the current bet-cart flow unchanged.
  - Used the existing standalone `app-market-accordion` component to reduce template complexity.
  - Left market mutation logic untouched for now because the backend contract does not expose a market identifier.
- Issues found during review:
  - The first version still exposed a global market status badge through `activeMarket()`.
  - `QUALIFY` was still visible in the chip list even when the event was not knockout.
- Fixes applied:
  - Replaced the global badge with a neutral count chip.
  - Filtered `availableMarketTypes()` with the same knockout rule used for `visibleMarkets()`.
  - Verified the refactor with `npm run build`.
- Remaining gaps:
  - Per-market mutation still needs backend support if each market must be mutated independently.
  - Market admin actions still need to be split by tournament scope.

## Step 3 - Authorization-sensitive admin panel

- Files modified:
  - `src/app/pages/event/event-page.html`
  - `src/app/pages/event/event-page.ts`
- Changes made:
  - Added an explicit admin-scope hint so the panel states whether the current user is acting on a FIFA_MATCH group-scoped event or a shared REAL_FOOTBALL event.
  - Kept lifecycle actions gated by existing computed permission signals, with the REAL_FOOTBALL path requiring system admin access.
  - Kept wallet top-up under group-admin scope, matching the current tournament-wallet flow.
  - Updated the market control section copy to make the shared-market scope explicit.
- Rationale:
  - The plan required mixed authorization scopes and a clear distinction between group-scoped FIFA actions and system-wide REAL_FOOTBALL actions.
  - The UI now communicates those scopes instead of implying a single generic admin role.
- Implementation decisions:
  - Reused the existing auth computed signals instead of introducing new permission services.
  - Preserved existing lifecycle and wallet behaviors while making the scope visible in the panel.
- Issues found during review:
  - None beyond the pre-existing backend contract gap for independently mutating a specific market.
- Fixes applied:
  - Verified the step with `npm run build` after the panel updates.
- Remaining gaps:
  - Per-market mutation still requires backend support for an unambiguous market target.
  - Wallet top-up still depends on the backend confirming tournament-wallet scoping.

## Step 4 - Final review and QA handoff

- Files modified:
  - `EVENT_PAGE_BACKEND_GAPS.md`
  - `EVENT_PAGE_MANUAL_QA.md`
- Changes made:
  - Documented the knockout `QUALIFY` backend dependency in the gaps file.
  - Added a manual QA checklist covering the refactor, market filtering, exact-score expansion, auth scope messaging, wallet top-up, and WebSocket sync.
- Rationale:
  - The frontend changes are complete enough to hand off, but the remaining contract gaps should be explicit for backend follow-up and manual verification.
- Review outcome:
  - No additional event-page code issues were found in the final repo-wide pass.
  - The build remained green from the previous verification step.
- Remaining gaps:
  - Per-market mutation still requires backend support for an unambiguous market target.
  - Wallet top-up still depends on the backend confirming tournament-wallet scoping.
  - `QUALIFY` market data must be emitted by the backend for knockout events.
