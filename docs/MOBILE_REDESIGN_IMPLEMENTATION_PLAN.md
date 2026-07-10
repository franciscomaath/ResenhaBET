# Mobile Redesign Implementation Plan

## 1. Final Design Source Of Truth

The final visual source of truth is:

- `docs/docs/design/resenhabet-mobile-redesign.html`

Secondary theme reference:

- `docs/docs/design/resenhabet-dark-theme-kit.html`

The Angular implementation must match `resenhabet-mobile-redesign.html` as closely as possible for:

- Mobile app shell
- Bottom navigation
- Sticky headers
- Dashboard structure
- Tournament header and tabs
- Compact match rows
- Odds chips
- Market accordions
- Match details layout
- Bet slip bar and bottom sheet
- Wallet-style balance and activity layout
- Ranking rows
- Spacing, hierarchy, typography, colors, borders, shadows, and safe-area behavior

The HTML is a visual reference only.

Do not copy:

- React components
- React state patterns
- Mock data
- Mock routes
- Mock wallet transactions
- Mock rankings
- Mock matches
- Any frontend-only fake behavior

The real implementation must remain Angular + Tailwind and use existing Angular data, DTOs, services, guards, routes, state, and APIs.

## 2. Non-Negotiable Preservation Rules

### Product Behavior

Do not remove or simplify:

- Login
- Logout
- Auth/session behavior
- Active group switching
- Group join flow
- Group create flow
- Claim player flow
- Route guards
- Existing routes
- Existing API calls
- Existing DTO usage
- Existing services
- WebSocket behavior
- Betting cart behavior
- Wallet balance refresh behavior
- Tournament management behavior
- Event management behavior
- Admin-only and group-admin-only visibility rules
- Existing loading/error/empty states

### Feature Preservation

If a current frontend feature is not visible in the HTML mockup, keep it and adapt it into the new UI using one of:

- Overflow action menu
- Collapsed admin section
- Compact secondary button
- Icon button
- Accordion
- Bottom sheet
- Contextual action row
- Desktop-only expanded action area when appropriate

Never delete functionality because the mockup does not show it.

### Backend Preservation

Do not:

- Add fake fields
- Invent DTO properties
- Mock missing dashboard, wallet, ranking, or transaction data
- Add frontend-only fake endpoints
- Change API semantics
- Implement backend code in this redesign task

If ideal UI behavior needs backend support that does not exist, keep current frontend behavior working and document the backend need in `Backend Requirements Discovered During Redesign`.

## 3. Component Architecture

### Layout Components

Create or refactor:

- `AppShell`
- `MobileHeader`
- `BottomNav`

Recommended files:

- `src/app/components/layout/app-shell.ts`
- `src/app/components/layout/app-shell.html`
- `src/app/components/layout/mobile-header.ts`
- `src/app/components/layout/mobile-header.html`
- `src/app/components/layout/bottom-nav.ts`
- `src/app/components/layout/bottom-nav.html`

Responsibilities:

- `AppShell` owns only visual layout for authenticated, active-group pages.
- `App` keeps auth gates, group gates, claim-player route exception, logout, group join/create logic, FAB state, router outlet ownership, and bet slip host.
- `MobileHeader` matches the HTML sticky header direction but preserves active group switcher and logout.
- `BottomNav` matches the HTML bottom nav structure while using existing Angular routes.

Existing route mapping:

- Home: `/`
- Tournaments: `/tournaments`
- Groups: `/groups`
- Bets: `/my-bets`

Do not replace Angular routing with local tab state like the HTML mockup.

### Base UI Components

Create/refactor reusable base components before page refactors:

- `AppCard`
- `AppButton`
- `StatusBadge`
- `AmountBadge`
- `TabBar`
- `SectionHeader`
- `ListRow`
- `EmptyState`
- `OverflowActionMenu`

Recommended files:

- `src/app/components/ui/app-card.ts`
- `src/app/components/ui/app-button.ts`
- `src/app/components/ui/status-badge.ts`
- `src/app/components/ui/amount-badge.ts`
- `src/app/components/ui/tab-bar.ts`
- `src/app/components/ui/list-row.ts`
- `src/app/components/ui/overflow-action-menu.ts`
- Update `src/app/components/ui/app-section-header.ts`
- Update `src/app/components/ui/app-empty-state.ts`
- Update `src/app/components/ui/app-state-banner.ts`

Rules:

- Components must be standalone Angular components.
- Prefer inputs/outputs over service injection.
- Keep components presentational unless there is already a clear app-wide pattern.
- Use Tailwind classes based on theme tokens.
- Avoid duplicated long Tailwind class strings across pages.

### Betting UI Components

Create/refactor:

- `OddsChip`
- `MarketAccordion`
- `BetSlipBar`
- `MatchRow`
- `MatchListSection`

Recommended files:

- `src/app/components/betting/odds-chip.ts`
- `src/app/components/betting/market-accordion.ts`
- `src/app/components/betting/market-accordion.html`
- `src/app/components/betting/bet-slip-bar.ts`
- `src/app/components/betting/match-row.ts`
- `src/app/components/betting/match-row.html`
- `src/app/components/betting/match-list-section.ts`
- `src/app/components/betting/match-list-section.html`

`MatchRow` is the highest-priority visual component.

It must follow the HTML structure:

- `flex items-stretch`
- Surface background
- Border-bottom separator
- Left fixed-width time/status column, approximately `w-14`
- Two-line team/player area
- Score aligned to the right of each team line when available
- Odds chips on the far right
- Compact row height
- No giant mobile match cards
- Entire row navigates to match details
- Odds chip click stops row navigation

`MatchRow` must not fetch data. It receives prepared labels, scores, status labels, and outcomes from the parent page.

### Tournament Components

Create:

- `CompactTournamentHeader`
- `CompactRankingRow`

Recommended files:

- `src/app/components/tournament/compact-tournament-header.ts`
- `src/app/components/tournament/compact-tournament-header.html`
- `src/app/components/tournament/compact-ranking-row.ts`

Responsibilities:

- `CompactTournamentHeader` mirrors HTML sticky tournament header.
- `CompactRankingRow` mirrors HTML ranking rows for mobile.
- Parent page keeps API calls and all admin actions.

### Wallet/Bets Components

Create only where real existing data supports it:

- `WalletTransactionRow`

Recommended file:

- `src/app/components/wallet/wallet-transaction-row.ts`

Rules:

- Use for real bet history rows or real wallet transaction rows only.
- Do not invent wallet transactions.
- If no transaction endpoint exists, document missing backend support.

## 4. Phase-By-Phase Implementation Plan

### Phase 0: Design Tokens And Theme

Files touched:

- `src/styles.css`
- Potentially `src/index.html` if fonts are loaded globally
- No page logic files

Implement:

- Add exact brand tokens from `resenhabet-mobile-redesign.html`:
- `--brand-bg: #06101f`
- `--brand-surface: #121822`
- `--brand-border: #232c3a`
- `--brand-blue: #3d8bfd`
- `--brand-green: #10b981`
- `--brand-red: #ef4444`
- `--brand-gold: #e0b243`
- `--brand-text: #ffffff`
- `--brand-muted: #64748b`
- Add typography:
- Body: Inter
- Display/headlines: Inter Tight
- Add semantic aliases:
- `--surface-1`
- `--surface-2`
- `--surface-3`
- `--accent`
- `--success`
- `--danger`
- `--navigate`
- `--gold`
- `--text-primary`
- `--text-secondary`
- `--text-muted`
- Add mobile utilities:
- Safe-area bottom padding
- Hidden scrollbar utility
- App viewport height utility using `100dvh`
- Bottom nav spacing variable
- Bet slip spacing variable
- Add shared shadows:
- Bottom nav top shadow
- Bottom sheet shadow
- Card shadow
- Preserve current Tailwind import and desktop compatibility.

Acceptance criteria:

- App background uses the redesign dark blue.
- Text and borders match the HTML reference.
- No business logic changes.
- Existing pages remain readable before page refactors.

### Phase 1: AppShell

Files touched:

- `src/app/app.html`
- `src/app/app.ts`
- `src/app/app.css`
- `src/app/components/layout/app-shell.ts`
- `src/app/components/layout/app-shell.html`
- `src/app/components/layout/mobile-header.ts`
- `src/app/components/layout/mobile-header.html`
- `src/app/components/layout/bottom-nav.ts`
- `src/app/components/layout/bottom-nav.html`

Implement:

- Move only the authenticated active-group visual shell into layout components.
- Keep auth and group gate conditions in `App`.
- Keep `router-outlet` exactly as the active page outlet.
- Keep `app-login`.
- Keep claim-player route behavior outside the active-group shell.
- Keep no-active-group join/create screen.
- Keep active group switcher.
- Keep logout button/action.
- Keep existing FAB service behavior.
- Keep existing `app-bet-slip`.
- Keep `app-toast-host`.

Shell visual target:

- Root authenticated layout:
- `h-[100dvh]`
- `bg-brand-bg`
- `text-brand-text`
- Relative/overflow-hidden shell like HTML
- Header:
- Sticky/fixed top visual behavior where appropriate
- Brand text `ResenhaBET` with green highlight
- Active group switcher
- Logout in compact overflow/icon action if space is limited
- Main:
- Scrollable content
- Bottom padding for bottom nav and bet slip
- Desktop max-width behavior must not break existing full-width admin tables
- Bottom nav:
- Fixed bottom
- Height approximately `64px`
- `bg-brand-surface`
- `border-t border-brand-border`
- Icons and labels
- Active route uses `brand-blue`
- Safe-area padding

Acceptance criteria:

- Existing routes still load.
- Bottom nav uses router links, not local state.
- FAB appears when existing service says it should.
- Bet slip overlay still appears.
- Desktop remains usable.

### Phase 2: Base UI Components

Files touched:

- `src/app/components/ui/app-card.ts`
- `src/app/components/ui/app-section-header.ts`
- `src/app/components/ui/app-empty-state.ts`
- `src/app/components/ui/app-state-banner.ts`
- New base UI component files listed in Component Architecture

Implement:

- `AppCard` variants:
- `surface`
- `subtle`
- `borderless`
- `interactive`
- `AppButton` variants:
- `primary` green
- `secondary` blue/border
- `ghost`
- `danger`
- `admin-inline`
- `StatusBadge` variants:
- live
- open
- suspended
- closed
- pending
- won
- lost
- created
- in-progress
- completed
- danger
- `AmountBadge`:
- positive green
- negative white/red depending context
- neutral muted
- `TabBar`:
- Horizontal scroll
- HTML-like active underline
- Compact `text-[13px]`
- `ListRow`:
- Compact row primitive for wallet/bets/groups/players
- `OverflowActionMenu`:
- Keep simple Angular state
- No business logic
- Parent passes actions
- Use for admin actions that do not fit the mockup

Acceptance criteria:

- Components compile.
- Existing pages can adopt them incrementally.
- No API/service behavior changes.

### Phase 3: Betting UI Components

Files touched:

- New betting component files listed in Component Architecture
- Existing `src/app/components/bet-slip/*` only if needed for visual wrapper integration

Implement `OddsChip`:

- Mobile compact dimensions from HTML:
- Match row chip approximately `44px x 44px`
- Small label `text-[10px]`
- Odd value `text-[13px] font-bold`
- Market chip variant:
- Full-width row/grid chip
- Label left, odd right for market detail
- States:
- Default: brand bg/surface, border
- Selected: brand green, dark text
- Disabled: opacity and not-allowed
- Hover/focus: brand blue border
- Output click event.
- Must support stopping propagation in parent or via host click handler.

Implement `MarketAccordion`:

- Header:
- `bg-brand-surface/70`
- `border-b border-brand-border`
- Title `text-[13px] font-bold`
- Chevron rotates
- Body:
- Collapsible content
- `p-3`
- Brand background
- Parent controls content and market outcomes.

Implement `MatchRow`:

- Inputs:
- `eventId`
- `timeLabel`
- `statusLabel`
- `isLive`
- `homeLabel`
- `awayLabel`
- `homeScore`
- `awayScore`
- `showScore`
- `homeMuted`
- `awayMuted`
- `outcomes`
- `marketStatus`
- `selectedOutcomeId`
- `disabled`
- Outputs:
- `openMatch`
- `selectOutcome`
- Behavior:
- Row click opens match.
- Odds click selects outcome and must not open match.
- If no market exists, row still navigates and reserves minimal/no odds area.
- If market is not open, odds chips render disabled.

Implement `MatchListSection`:

- Section label like HTML:
- `px-4 py-2`
- `text-[10px]`
- uppercase
- brand muted
- sticky if inside scroll container
- Contains multiple `MatchRow` components.
- Used for round/group/date sections.

Acceptance criteria:

- Tournament page can be refactored to compact rows without losing betting behavior.
- Event page can reuse odds/accordion primitives.

### Phase 4: Tournament Page

Files touched:

- `src/app/pages/tournament/tournament-page.html`
- `src/app/pages/tournament/tournament-page.ts`
- `src/app/pages/tournament/tournament-page.css`
- `src/app/components/tournament/compact-tournament-header.ts`
- `src/app/components/tournament/compact-tournament-header.html`
- `src/app/components/tournament/compact-ranking-row.ts`
- Betting components from Phase 3
- Base UI components from Phase 2

Preserve all current behavior in `TournamentPage`, including:

- `loadTournamentPage`
- `loadMarkets`
- `loadScoreboard`
- `loadBettingRanking`
- `startTournament`
- `syncFixtures`
- `syncOdds`
- `openStartConfigModal`
- `startTournamentWithConfig`
- `openAdvanceModal`
- `advanceToBracket`
- `openCreateMatchModal`
- `createMatch`
- `updateTournament`
- `cancelTournament`
- `deleteTournament`
- Player add/team assignment flows
- All current tabs:
- matches
- standings
- bracket when available
- players
- ranking
- admin

Visual changes:

- Replace giant hero card with sticky compact tournament header like HTML.
- Header includes:
- Tournament name
- Format/date/status metadata
- Status badge
- Overflow action menu for admin/secondary actions
- Actions to keep in overflow/collapsed admin area:
- Sync fixtures
- Import odds
- Start tournament
- Advance to bracket
- Create match
- Rename tournament
- Cancel tournament
- Delete tournament
- Add player
- Any other existing admin action
- Tabs:
- Match HTML tab style
- `text-[13px]`
- Active underline blue
- Horizontal scroll
- No large cyan block active tab on mobile
- Matches:
- Replace current large match cards with `MatchListSection` and `MatchRow`
- Preserve grouping by round/group from `eventsByRound`
- Preserve match-result markets from `matchResultMarket`
- Preserve selected state from `isOutcomeSelected`
- Preserve betting cart action from `addOutcomeToCart`
- Ranking:
- Use compact rows on mobile
- Keep desktop table when data is dense
- Do not invent form badges from mockup unless backend provides form data
- Standings:
- Mobile compact row view
- Desktop table retained
- Bracket:
- Restyle but do not remove existing bracket logic
- Admin:
- Move into collapsed section or overflow-triggered panel
- Keep all forms and destructive actions available

Acceptance criteria:

- No tournament action disappears.
- Matches are compact rows on mobile.
- Odds click does not navigate.
- Match row click navigates to `/events/:id`.
- Admin actions remain permission-gated.
- Real football and FIFA tournaments still work.

### Phase 5: Event / Match Details Page

Files touched:

- `src/app/pages/event/event-page.html`
- `src/app/pages/event/event-page.ts`
- Betting components from Phase 3
- Base UI components from Phase 2

Preserve all current behavior in `EventPage`, including:

- Initial event loading
- Context loading
- Market loading
- User bet loading
- Wallet loading
- WebSocket connect/disconnect
- Event topic subscription
- Market topic subscription
- Wallet topic subscription
- Start match
- Assign players
- Update score
- Finish match
- Penalties flow
- Datetime editing
- Deposit
- Deposit all
- Event bets loading
- Market refresh
- Bet resolution check
- Cart selection

Visual changes:

- Top bar:
- Match HTML back bar
- Back button to tournament when event has tournament id
- Compact centered title
- Overflow action button for secondary/admin controls
- Scoreboard:
- Compact surface block
- Status/time label above teams
- Home and away labels aligned like HTML
- Score in center on desktop and compact mobile equivalent
- Penalties/result info retained
- Market tabs:
- Horizontal compact tabs for available market types
- Do not show fake tabs for unavailable markets
- Markets:
- Use `MarketAccordion`
- Use real `MarketResponseDto` outcomes only
- Use `OddsChip` market variant
- Preserve selected state and disabled state
- User bet:
- Compact existing bet summary below market chips
- Admin controls:
- Collapse below main betting content
- Keep all controls available:
- Assign players
- Start
- Score update
- End
- Penalties
- Datetime edit
- Deposit
- Deposit all
- Event bets
- Do not remove management panel.

Acceptance criteria:

- WebSocket score and market updates still reflect in UI.
- Betting selection still updates existing cart.
- Admin flows still work.
- Match details visually match HTML layout closely.

### Phase 6: Bet Slip

Files touched:

- `src/app/components/bet-slip/bet-slip.html`
- `src/app/components/bet-slip/bet-slip.ts`
- `src/app/components/bet-slip/bet-slip.css`
- Optional: `src/app/components/betting/bet-slip-bar.ts`

Preserve:

- `BetCartService`
- `cart.entries`
- `cart.itemCount`
- `cart.combinedOdd`
- `cart.pendingWarning`
- `cart.crossTournamentWarning`
- `cart.walletBalance`
- `cart.stakeAmount`
- `cart.potentialReturn`
- `cart.betSlipError`
- `cart.canPlaceBet`
- `cart.placeBet`
- `cart.clearCart`
- `cart.removeEntry`
- `cart.setMaxStake`
- `cart.toggleExpanded`

Visual changes:

- Collapsed mobile state becomes HTML-like bottom bar:
- Fixed bottom above nav or integrated above safe area
- Brand surface
- Border top
- Shadow
- Selection count
- Total odds
- Warning indicator
- Tap to expand
- Expanded mobile state:
- Bottom sheet
- Safe-area padding
- Internal scroll
- Compact selected odds list
- Stake input
- Potential return
- Validation messages
- Submit button green
- Desktop:
- Keep floating card behavior
- Do not block main admin content

Acceptance criteria:

- Empty cart hides slip.
- Collapsed bar appears after odds selection.
- Expanded sheet can place bets.
- Desktop remains usable.
- No service behavior changes.

### Phase 7: Dashboard / Home

Files touched:

- `src/app/pages/home/home-page.html`
- `src/app/pages/home/home-page.ts`
- Base UI components
- Betting/list components where useful

Preserve:

- `loadLiveEvents`
- `loadTeams`
- `loadTournaments`
- Active tournaments
- Player counts
- Team badge resolution
- Team translations
- Loading/error/empty states

Visual changes:

- Match HTML dashboard structure:
- Sticky top area already handled by shell
- Compact tournament carousel/cards
- Active bets section if data exists
- Live events section using compact match/list rows where possible
- Tournament cards:
- `min-w-[220px]`
- Surface background
- Border
- Rounded `8px`
- Small metadata
- Strong tournament title
- Existing status/type/date/player count fields only
- Do not display fake balance/rank per tournament unless backend provides it.
- If active bets are not available on dashboard through existing APIs, document backend requirement.

Acceptance criteria:

- Home remains data-driven.
- No mock dashboard data.
- Mobile layout resembles HTML dashboard.
- Desktop grid still works.

### Phase 8: Wallet / My Bets

Files touched:

- `src/app/pages/my-bets/my-bets-page.html`
- `src/app/pages/my-bets/my-bets-page.ts`
- `src/app/components/wallet/wallet-transaction-row.ts` if useful
- Existing bet slip components as needed

Preserve:

- `betsApi.getMyBets`
- Tournament name loading
- Bet slip cards
- Item status labels
- Outcome labels
- Team translations
- Loading/error/empty states

Visual changes:

- Use wallet/activity style from HTML:
- Compact rows
- Surface background
- Border-bottom separators
- Amount badges
- Small muted date text
- Strong primary title
- My Bets should not use huge cards on mobile.
- Keep all slip details:
- Created date
- Stake
- Tournament
- Slip status
- Potential return
- Combined odd
- Item count
- Each item outcome and odd snapshot
- Each item status

Backend limitation:

- There is wallet balance API, deposit APIs, and bet history.
- If true wallet transaction history is desired, document backend requirement.
- Do not fake deposits/prizes in UI.

Acceptance criteria:

- My Bets remains complete and readable.
- No wallet mock transactions.
- Won/lost/pending states remain visible.

### Phase 9: Groups / Ranking / Players

Files to inspect and touch as needed:

- `src/app/pages/groups/groups-page.html`
- `src/app/pages/groups/groups-page.ts`
- `src/app/pages/players/players-page.html`
- `src/app/pages/players/players-page.ts`
- `src/app/pages/teams/teams-page.html`
- `src/app/pages/teams/teams-page.ts`
- `src/app/pages/tournaments/tournaments-page.html`
- `src/app/pages/tournaments/tournaments-page.ts`
- `src/app/pages/claim-player/claim-player.component.html`
- `src/app/pages/claim-player/claim-player-card.component.html`
- `src/app/components/ranking/ranking.html`
- `src/app/components/ranking/ranking.ts`

Preserve:

- Group switching
- Group creation
- Group membership
- Invite/join flows
- Claim player flows
- Admin role controls
- Player management
- Team display
- Tournament list behavior
- Existing route guards

Visual changes:

- Use compact list rows inspired by HTML ranking rows.
- Use `StatusBadge`, `AmountBadge`, `ListRow`, and `OverflowActionMenu`.
- Keep dense tables on desktop where useful.
- Use collapsed admin sections on mobile.
- Do not remove management actions.

Acceptance criteria:

- Every existing action remains available.
- Mobile pages become compact and consistent with redesign.
- Desktop pages remain readable.

### Phase 10: QA, Cleanup, Backend Requirements

Files touched:

- Any duplicated styles discovered during cleanup
- `docs/MOBILE_REDESIGN_IMPLEMENTATION_PLAN.md` only if backend requirements are updated after implementation

Work:

- Remove duplicated one-off Tailwind patterns where base components exist.
- Verify no mock data was introduced.
- Verify no backend behavior changed.
- Verify all buttons/features still exist.
- Verify mobile and desktop layouts.
- Compile/build/test.

Recommended commands:

- `npm run build`
- `npm test` if configured
- `npm run lint` if configured

Acceptance criteria:

- App builds.
- Core routes render.
- Manual QA checklist passes.
- Backend requirements section is complete and honest.

## 5. File-By-File Impact List

### Global

- `src/styles.css`
- Add brand tokens, fonts, utilities, safe-area helpers.
- `src/index.html`
- Add/preconnect Inter and Inter Tight if not handled through CSS import.

### App Shell

- `src/app/app.html`
- Replace inline authenticated shell with `AppShell`, `MobileHeader`, `BottomNav`.
- Keep auth/group gates and router outlet.
- `src/app/app.ts`
- Import new layout components.
- Keep all auth/group methods.
- `src/app/app.css`
- Remove or reduce shell-specific styles after extraction.

### Layout Components

- `src/app/components/layout/app-shell.ts`
- `src/app/components/layout/app-shell.html`
- `src/app/components/layout/mobile-header.ts`
- `src/app/components/layout/mobile-header.html`
- `src/app/components/layout/bottom-nav.ts`
- `src/app/components/layout/bottom-nav.html`

### Base UI

- `src/app/components/ui/app-card.ts`
- `src/app/components/ui/app-button.ts`
- `src/app/components/ui/status-badge.ts`
- `src/app/components/ui/amount-badge.ts`
- `src/app/components/ui/tab-bar.ts`
- `src/app/components/ui/list-row.ts`
- `src/app/components/ui/overflow-action-menu.ts`
- `src/app/components/ui/app-section-header.ts`
- `src/app/components/ui/app-empty-state.ts`
- `src/app/components/ui/app-state-banner.ts`

### Betting UI

- `src/app/components/betting/odds-chip.ts`
- `src/app/components/betting/market-accordion.ts`
- `src/app/components/betting/market-accordion.html`
- `src/app/components/betting/bet-slip-bar.ts`
- `src/app/components/betting/match-row.ts`
- `src/app/components/betting/match-row.html`
- `src/app/components/betting/match-list-section.ts`
- `src/app/components/betting/match-list-section.html`

### Tournament

- `src/app/pages/tournament/tournament-page.html`
- Main visual refactor.
- `src/app/pages/tournament/tournament-page.ts`
- Minimal changes for presentational inputs/actions only.
- `src/app/pages/tournament/tournament-page.css`
- Replace old tab styles.
- `src/app/components/tournament/compact-tournament-header.ts`
- `src/app/components/tournament/compact-tournament-header.html`
- `src/app/components/tournament/compact-ranking-row.ts`

### Event

- `src/app/pages/event/event-page.html`
- Main visual refactor.
- `src/app/pages/event/event-page.ts`
- Minimal changes for market accordion state or display helpers only.

### Bet Slip

- `src/app/components/bet-slip/bet-slip.html`
- `src/app/components/bet-slip/bet-slip.ts`
- `src/app/components/bet-slip/bet-slip.css`

### Home

- `src/app/pages/home/home-page.html`
- `src/app/pages/home/home-page.ts`

### Bets / Wallet-Like UI

- `src/app/pages/my-bets/my-bets-page.html`
- `src/app/pages/my-bets/my-bets-page.ts`
- `src/app/components/wallet/wallet-transaction-row.ts`

### Secondary Pages

- `src/app/pages/groups/groups-page.html`
- `src/app/pages/groups/groups-page.ts`
- `src/app/pages/players/players-page.html`
- `src/app/pages/players/players-page.ts`
- `src/app/pages/teams/teams-page.html`
- `src/app/pages/teams/teams-page.ts`
- `src/app/pages/tournaments/tournaments-page.html`
- `src/app/pages/tournaments/tournaments-page.ts`
- `src/app/pages/claim-player/claim-player.component.html`
- `src/app/pages/claim-player/claim-player-card.component.html`

## 6. Existing Functionality Preservation Checklist

Before completing the redesign, verify every item remains present and functional:

- Login works.
- Logout works.
- Current user state works.
- Auth guard still protects routes.
- Active group guard still protects routes.
- Claim player guard still works.
- Active group switcher still works.
- User can join group by invite code.
- User can create group.
- User without active group sees group gate.
- Claim-player route still bypasses active shell where required.
- Home loads active tournaments.
- Home loads live events.
- Home loads teams for badges.
- Tournaments list route still works.
- Tournament detail loads tournament data.
- Tournament detail loads players.
- Tournament detail loads rounds.
- Tournament detail loads events.
- Tournament detail loads markets.
- Tournament detail loads scoreboard.
- Tournament detail loads betting ranking.
- Tournament tabs still switch.
- Matches tab still shows all grouped events.
- Match row navigation opens event detail.
- Odds selection adds to cart.
- Selected odds state remains accurate.
- Real football fixture sync remains available to system admin.
- Real football odds import remains available to system admin.
- Tournament start remains available to group admin.
- League-bracket start config modal remains available.
- Advance to bracket remains available.
- Create match remains available.
- Add player remains available.
- Team assignment remains available.
- Rename tournament remains available.
- Cancel tournament remains available.
- Delete tournament remains available.
- Event detail loads event context.
- Event detail loads markets.
- Event detail loads user bet.
- Event detail refreshes wallet.
- Event WebSocket updates score.
- Market WebSocket updates markets.
- Wallet WebSocket updates balance.
- Assign players works.
- Start match works.
- Update score works.
- Finish match works.
- Penalties flow works.
- Datetime edit works.
- Deposit works where available.
- Deposit all works where available.
- Event bets section remains available.
- Bet slip warnings remain visible.
- Stake input works.
- Max stake works.
- Clear cart works.
- Remove cart item works.
- Place bet works.
- My Bets loads bet history.
- My Bets shows every bet item.
- Groups page preserves membership and management actions.
- Players page preserves management actions.
- Teams page preserves display/actions.
- Claim-player flow remains intact.
- Toasts still render.
- FAB still appears and executes configured action.

## 7. Mobile UX Checklist

Test at:

- 360 x 800
- 390 x 844
- 430 x 932

Checklist:

- App uses `100dvh` and does not jump behind browser UI.
- Bottom nav respects safe-area.
- Bet slip respects safe-area.
- FAB does not overlap bottom nav or bet slip.
- Header is compact and does not consume excessive vertical space.
- No page has unintended horizontal scroll.
- Tournament header is sticky/compact.
- Tournament tabs are horizontally scrollable.
- Match rows are compact and readable.
- Match rows do not become giant cards.
- Odds chips are reachable with thumb.
- Odds click does not navigate to event detail.
- Disabled odds are visually disabled.
- Event detail back button is easy to tap.
- Event scoreboard fits narrow screens.
- Market tabs scroll horizontally.
- Market accordions open/close cleanly.
- Bet slip bottom sheet scrolls internally.
- Admin controls are findable but not dominant.
- Overflow menus work on touch.
- Destructive actions remain visually distinct.
- Loading states fit compact layout.
- Error states remain readable.
- Empty states are compact and clear.

## 8. Desktop Regression Checklist

Test at:

- 1024 x 768
- 1280 x 900
- 1440 x 1024

Checklist:

- Layout does not look broken or overly constrained.
- Main content uses available width where appropriate.
- Tables remain readable on desktop.
- Admin panels remain usable.
- Modals center correctly on desktop.
- Bet slip remains a floating card/sheet, not an awkward full-width bar.
- Bottom nav does not block desktop content.
- Header and active group switcher remain accessible.
- Tournament page still supports dense admin workflows.
- Event management controls remain efficient on desktop.
- No content is hidden behind fixed elements.
- Keyboard focus states remain visible.

## 9. Risk Analysis

### Risk: Shell extraction breaks auth/group flows

Impact: High

Mitigation:

- Keep business decisions in `App`.
- Extract only visual shell.
- Do not move `resolveCurrentGroup`, `submitJoinGroup`, `submitCreateGroup`, or `logout`.

### Risk: MatchRow loses betting behavior

Impact: High

Mitigation:

- Parent owns markets and cart logic.
- `MatchRow` emits outcome selection only.
- Add explicit event propagation stop for odds clicks.

### Risk: Admin actions disappear during visual cleanup

Impact: High

Mitigation:

- Use preservation checklist.
- Put hidden-by-mockup actions into overflow/collapsed sections.
- Verify with admin/group-admin permissions.

### Risk: Mockup has data the backend does not provide

Impact: Medium

Mitigation:

- Use only existing DTO fields.
- Document missing backend requirements.
- Avoid fake frontend state.

### Risk: Desktop regresses due mobile-first layout

Impact: Medium

Mitigation:

- Keep responsive desktop variants.
- Preserve desktop tables where appropriate.
- Test admin-heavy screens on desktop.

### Risk: Fixed nav, FAB, and bet slip overlap

Impact: Medium

Mitigation:

- Centralize spacing variables.
- Test empty cart, collapsed cart, expanded cart, and visible FAB states.

### Risk: Theme token changes reduce contrast

Impact: Medium

Mitigation:

- Use exact HTML palette.
- Verify selected, disabled, error, warning, and success states.

## 10. Rollback Strategy

Implement phase-by-phase and keep each phase independently revertible.

Rollback units:

- Phase 0: revert `src/styles.css` and font changes.
- Phase 1: restore inline shell in `app.html`; leave unused layout components if needed.
- Phase 2: stop importing base components; restore inline markup.
- Phase 3: replace betting components with previous inline odds/match markup.
- Phase 4: revert tournament page files only.
- Phase 5: revert event page files only.
- Phase 6: revert `components/bet-slip/*` only; do not touch `BetCartService`.
- Phase 7: revert home page only.
- Phase 8: revert my-bets/wallet visual components only.
- Phase 9: revert affected secondary pages only.

Do not use destructive git commands.

Recommended implementation discipline:

- Commit after each successful phase.
- Run build after each major page phase.
- Never combine service/API changes with visual refactors.
- Do not modify DTOs unless separately required and approved.

# Backend Requirements Discovered During Redesign

## Dashboard Active Bets Summary

Desired frontend behavior:

- Home dashboard shows an `Active Bets` section like the HTML reference with compact active bet cards.

Missing endpoint or field:

- Current dashboard can load tournaments and live events, but there is no dedicated active-bets summary endpoint confirmed for dashboard use beyond `getMyBets`.

Suggested HTTP method/path:

- `GET /api/v1/bets/my/active`

Suggested response DTO:

```ts
export interface ActiveBetSummaryDto {
  betSlipId: number;
  tournamentId: number;
  tournamentName: string;
  eventId: number;
  eventLabel: string;
  outcomeName: string;
  oddSnapshot: number;
  stake: number;
  potentialReturn: number;
  createdAt: string;
}
```

Priority:

- Medium

Can frontend work without it temporarily:

- Yes. Keep dashboard active bets hidden or derive cautiously from existing `getMyBets` only if already loaded through the existing bets API without changing behavior.

## Wallet Transaction History

Desired frontend behavior:

- Wallet screen/activity list like the HTML reference, showing deposits, bets, and prizes as transaction rows.

Missing endpoint or field:

- Current frontend has wallet balance and deposit APIs, but no confirmed transaction history page/API usage.

Suggested HTTP method/path:

- `GET /api/v1/wallet/transactions?userId={userId}&tournamentId={tournamentId}`

Suggested response DTO:

```ts
export interface WalletTransactionResponseDto {
  id: number;
  userId: number;
  tournamentId?: number | null;
  betId?: number | null;
  type: 'DEPOSIT' | 'BET' | 'PRIZE' | 'ADJUSTMENT';
  amount: number;
  balanceAfter?: number | null;
  description: string;
  createdAt: string;
}
```

Priority:

- Medium

Can frontend work without it temporarily:

- Yes. Use My Bets for betting history and existing wallet balance display only. Do not fake transaction rows.

## Tournament Card Balance And Rank

Desired frontend behavior:

- Dashboard tournament carousel cards show balance and rank like the HTML mockup.

Missing endpoint or field:

- Existing tournament DTO and player count summary do not provide per-tournament bettor balance/rank for dashboard cards.

Suggested HTTP method/path:

- `GET /api/v1/tournaments/{id}/me/summary`

Suggested response DTO:

```ts
export interface TournamentUserSummaryDto {
  tournamentId: number;
  userId: number;
  balance: number;
  bettingRank?: number | null;
  pendingBetsCount: number;
  wonBetsCount: number;
  lostBetsCount: number;
}
```

Priority:

- Low to Medium

Can frontend work without it temporarily:

- Yes. Show existing tournament metadata only.

## Ranking Form Badges

Desired frontend behavior:

- Ranking rows show recent form badges like `W`, `L`, `D` from the HTML ranking mockup.

Missing endpoint or field:

- Existing betting ranking appears to provide user name and balance only.
- Tournament standings provide aggregate stats, not recent form sequence.

Suggested HTTP method/path:

- `GET /api/v1/tournaments/{id}/ranking/form`

Suggested response DTO:

```ts
export interface RankingFormResponseDto {
  subjectId: number;
  subjectType: 'USER' | 'PLAYER' | 'TEAM';
  recentResults: Array<'W' | 'L' | 'D'>;
}
```

Priority:

- Low

Can frontend work without it temporarily:

- Yes. Do not show form badges until real data exists.

## Upcoming Matches For Dashboard

Desired frontend behavior:

- Dashboard can show live/upcoming matches in compact `MatchRow` style.

Missing endpoint or field:

- Current home page loads live events only through existing live events API and tournament list.
- It does not have a confirmed “upcoming matches across active group/tournaments” endpoint.

Suggested HTTP method/path:

- `GET /api/v1/events/upcoming?groupId={groupId}&limit={limit}`

Suggested response DTO:

```ts
export interface UpcomingEventResponseDto {
  id: number;
  tournamentId: number;
  tournamentName: string;
  roundId?: number | null;
  roundName?: string | null;
  gameDatetime: string;
  status: string;
  playerHomeId?: number | null;
  playerHomeName?: string | null;
  playerAwayId?: number | null;
  playerAwayName?: string | null;
  teamHomeId?: number | null;
  teamHomeName?: string | null;
  teamAwayId?: number | null;
  teamAwayName?: string | null;
  homeScore?: number | null;
  awayScore?: number | null;
}
```

Priority:

- Medium

Can frontend work without it temporarily:

- Yes. Keep current live events and tournament cards.
