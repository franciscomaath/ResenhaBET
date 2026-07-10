# Event Page Manual QA

## Scope

- Event page visual refresh
- Data-driven market rendering
- Exact score expand/collapse
- Knockout-only `QUALIFY` visibility
- Admin scope messaging and actions
- Wallet top-up flow
- WebSocket updates for event, market, and wallet data

## Pre-conditions

- Have one `FIFA_MATCH` event and one `REAL_FOOTBALL` event available.
- Have at least one knockout event with a `QUALIFY` market.
- Have at least one event with multiple markets, including `EXACT_SCORE`.
- Have one user with group admin permissions and one with system admin permissions.

## Smoke Check

1. Open the Event page for a populated event.
2. Confirm the page loads without console errors.
3. Confirm the dark card layout, header, and action areas match the new visual treatment.

Expected:

- Event header, market section, admin panel, wallet panel, and bet history render normally.
- No missing data placeholders or broken buttons on initial load.

## Markets

1. Verify the market chips render from the loaded market list.
2. Switch between `Todos` and individual market type chips.
3. Confirm the visible market count changes with the filter.
4. Confirm each visible market renders inside `app-market-accordion`.
5. Confirm each market shows its own status badge.

Expected:

- Filtering is data-driven.
- The page no longer shows a single global market card as the primary market UI.

## Exact Score

1. Open an `EXACT_SCORE` market with more than 6 outcomes.
2. Confirm the compact view shows only the first 6 outcomes.
3. Click `Ver todos os placares`.
4. Confirm the full outcome list appears.
5. Click `Ver menos`.

Expected:

- The market expands and collapses only for that market.
- Other markets are unaffected.

## Knockout `QUALIFY`

1. Open a non-knockout event.
2. Confirm `QUALIFY` does not appear in the market chips or market list.
3. Open a knockout event that has `QUALIFY` data.
4. Confirm `QUALIFY` is visible and renderable.

Expected:

- `QUALIFY` is only shown for knockout events.

## Admin Scope

1. Open a `FIFA_MATCH` event as a group admin.
2. Confirm the admin hint describes group-scoped actions.
3. Open a `REAL_FOOTBALL` event as a non-system admin.
4. Confirm the admin hint warns that shared match actions require system admin access.
5. Open the same `REAL_FOOTBALL` event as a system admin.
6. Confirm match and market controls are available.

Expected:

- The UI makes the active authorization scope explicit.
- `REAL_FOOTBALL` does not imply group-only mutation rights.

## Market Controls

1. Change the selected market type or update market data so a different market becomes the active one.
2. Trigger the market status action.
3. Confirm the request updates the currently active market contract path.

Expected:

- The UI remains limited by the current backend contract for per-market mutation.

## Wallet Top-up

1. Open the wallet top-up section as a group admin.
2. Select a user and enter a positive amount.
3. Submit the deposit.
4. Confirm the success message and updated wallet state.

Expected:

- Deposit actions target the tournament wallet flow, not the legacy global wallet.

## WebSocket Updates

1. Update the event status from another session or backend fixture.
2. Confirm the page updates without reload.
3. Update a market from another session.
4. Confirm the relevant market card updates in place.
5. Update wallet balance data for the logged-in user.
6. Confirm the wallet section updates in place.

Expected:

- Event, market, and wallet subscriptions stay in sync with the page.

## Known Backend Gap

- Per-market mutation still depends on backend support for an explicit market target.
- If the backend does not emit `QUALIFY` for knockout events, that market section cannot populate.
