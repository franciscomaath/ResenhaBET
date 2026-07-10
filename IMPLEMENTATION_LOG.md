# Tournament Redesign Implementation Log

## Step 1 - Normalize Visual Tokens/Colors

- Files modified: `src/app/pages/tournament/tournament-page.html`, `src/app/pages/tournament/tournament-page.css`.
- What changed: replaced tournament-page cyan/indigo styling with the plan palette: blue for navigation/secondary UI, green for primary actions/money, and gold/amber only for ranking/placement markers.
- Why it changed: the prototype/design-system palette reserves green for primary actions/money, blue for navigation/accent, and gold for ranking/trophy-related UI only.
- Important decisions: kept existing Tailwind utility approach and did not alter data flow, routing, permissions, or API calls.
- Issues discovered during review: pênaltis text briefly used amber, which could violate the gold-only convention; changed it to blue because it is event metadata, not ranking/trophy.
- Issues fixed during review: updated `.tab-button.active` in page CSS from cyan to blue and removed tournament-page cyan/indigo usages.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 2 - Restyle Header, Tabs, Buttons, Empty States, Ranking

- Files modified: `src/app/pages/tournament/tournament-page.html`, `src/app/pages/tournament/tournament-page.css`.
- What changed: restyled the tournament header card, loading/error shells, tab strip, primary/secondary/destructive buttons, empty states, and the betting ranking list to match the mobile dark prototype more closely.
- Why it changed: these are cosmetic-only areas from the plan and can be ported without changing backend contracts or screen behavior.
- Important decisions: kept all existing actions, permissions, signals, and API calls; used Tailwind utilities and existing CSS rather than adding new styling systems.
- Issues discovered during review: the ranking table was visually heavier than the prototype; converted it to a compact row card while keeping all DTO-backed values unchanged.
- Issues fixed during review: updated remaining main action buttons and form controls from small `rounded-md` styling to the softer `rounded-xl` visual language where it did not affect match-card work reserved for Step 3.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 3 - Restyle Match Cards

- Files modified: `src/app/pages/tournament/tournament-page.html`.
- What changed: redesigned the Partidas panel round headers, match cards, team/player rows, scores, status footer, and attached odds row using the existing helpers and signals.
- Why it changed: match/event cards are the main visual target of the prototype and can be restyled without changing data contracts.
- Important decisions: kept `eventsByRound()`, `eventPlayerLabel()`, `playerTeam()`, `translatedTeamName()`, `matchResultMarket()`, `isOutcomeSelected()`, and `addOutcomeToCart()` intact; no WebSocket/live subscription was added.
- Issues discovered during review: status was duplicated on desktop and unavailable markets initially kept green styling.
- Issues fixed during review: removed the duplicate desktop status label and made non-open market indicators neutral gray while preserving green for open markets.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 4 - Presentational Component Extraction Review

- Files modified: none.
- What changed: no component extraction was made.
- Why it changed: the plan explicitly says to extract small standalone presentational components only where useful. Extracting at this point would require many inputs/callbacks and duplicate or move helpers that still belong to the page-level data orchestration.
- Important decisions: kept the existing standalone `TournamentPage` architecture and deferred extraction until a component boundary is clearly reusable and simpler than the current template.
- Issues discovered during review: no dead code or new duplicated helper code introduced in prior steps.
- Issues fixed during review: none required.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 5 - Conditional Tab Visibility And Active Fallback

- Files modified: `src/app/pages/tournament/tournament-page.ts`, `src/app/pages/tournament/tournament-page.html`.
- What changed: added `showStandingsTab`, `visibleTabs`, guarded `setTab()`, and an effect that resets `activeTab` if the selected tab becomes invalid after tournament/round data loads.
- Why it changed: `Classificação` only applies to `LEAGUE` and `LEAGUE_BRACKET`, while `Mata-mata` remains conditional on bracket eligibility/existence.
- Important decisions: kept the existing `showBracketTab` semantics for `LEAGUE_BRACKET` after bracket rounds are generated; did not change admin permissions or route behavior.
- Issues discovered during review: pure `BRACKET` tournaments previously still rendered the `Classificação` tab and standings panel.
- Issues fixed during review: hid the standings tab/panel behind `showStandingsTab()` and prevented programmatic selection of hidden tabs.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 6 - Gate Manual Match Creation

- Files modified: `src/app/pages/tournament/tournament-page.ts`, `src/app/pages/tournament/tournament-page.html`.
- What changed: added `canCreateManualMatch` computed signal and used it to disable/demote the `Criar Partida` button for non-manual tournaments. Added method guards in `openCreateMatchModal()` and `createMatch()`.
- Why it changed: `Criar Partida` should only be prominent/enabled when `TournamentResponseDto.generationMode === 'MANUAL'`; `AUTO` tournaments should not present manual match creation as the expected flow.
- Important decisions: preserved the existing create-match modal and API path for manual tournaments; did not remove the button entirely, so existing functionality remains discoverable but disabled when inappropriate.
- Issues discovered during review: the click handler previously had no generation-mode guard, so code could open the modal even if the UI later changed.
- Issues fixed during review: added both UI-level disabling and method-level guards.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 7 - Tighten Odds Rendering Conditions

- Files modified: `src/app/pages/tournament/tournament-page.ts`, `src/app/pages/tournament/tournament-page.html`.
- What changed: added `shouldShowOdds()`, `oddsSourceLabel()`, and `isPreKickoff()`; odds rows now render only for `CREATED` events with an `OPEN` market. `REAL_FOOTBALL` also requires a future `gameDatetime`; `FIFA_MATCH` uses the same visual row but labels the source as Elo odds.
- Why it changed: real football imported odds and FIFA Elo/internal odds must not be conflated, and odds should not render after kickoff or for closed/suspended markets.
- Important decisions: used only existing `EventResponseDto`, `TournamentResponseDto`, and `MarketResponseDto` fields; did not add new endpoints or DTO fields.
- Issues discovered during review: previous UI rendered disabled odds rows for non-open markets, which still visually suggested a market existed on the card.
- Issues fixed during review: non-open/ineligible odds rows are now absent instead of disabled.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 8 - Rework Standings Grouping And Highlighting

- Files modified: `src/app/pages/tournament/tournament-page.ts`, `src/app/pages/tournament/tournament-page.html`.
- What changed: added `groupDisplayLabel()` and `isQualifyingPosition()`; restyled league and group-stage standings tables; group headers now use `groupName` when present and fall back to `groupNumber`; qualifying rows are highlighted only for `LEAGUE_BRACKET` using `playersAdvancingPerGroup`.
- Why it changed: standings grouping/highlighting must be backed by real scoreboard DTO fields and must not assume every tournament has groups.
- Important decisions: kept table data sourced from `TournamentScoreboardResponseDto.entries`, `groups[].standings`, and `groups[].teamStandings`; did not invent group or qualification fields.
- Issues discovered during review: a `BRACKET` placements block inside the standings panel became dead code after Step 5 hid `Classificação` for pure bracket tournaments.
- Issues fixed during review: removed the unreachable pure-bracket placements block from the standings panel.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 9 - Rework Bracket Layout

- Files modified: `src/app/pages/tournament/tournament-page.ts`, `src/app/pages/tournament/tournament-page.html`.
- What changed: replaced the selected-round bracket view with a horizontally scrollable column layout by knockout round; added `bracketColumns`, `bracketSideLabel()`, and `bracketScoreLabel()` using existing rounds/events/source-slot logic.
- Why it changed: the prototype presents `Mata-mata` as columns that can be horizontally scrolled rather than a segmented single-round view.
- Important decisions: preserved links to individual Event pages and existing winner/source resolution logic; did not add WebSocket updates or new backend requirements.
- Issues discovered during review: the old `activeBracketRound` state became unnecessary after switching to columns.
- Issues fixed during review: removed the old active-round signal/effect/method and verified no references remained.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.

## Step 10 - Align Status Enums With Backend

- Files modified: `src/app/services/api/api.models.ts`, `src/app/pages/tournament/tournament-page.ts`, `src/app/pages/tournament/tournament-page.html`, `src/app/pages/home/home-page.ts`, `src/app/pages/home/home-page.html`, `src/app/pages/tournaments/tournaments-page.ts`, `src/app/pages/tournaments/tournaments-page.html`, `src/app/pages/event/event-page.ts`, `src/app/pages/event/event-page.html`.
- What changed: replaced obsolete `ENDED` checks/labels with backend-backed `COMPLETED` and added explicit `CANCELLED` tournament handling where status badges are rendered.
- Why it changed: backend enums are `TournamentStatus.CREATED/IN_PROGRESS/COMPLETED/CANCELLED` and `EventStatus.CREATED/IN_PROGRESS/PENALTIES/COMPLETED/CANCELLED`; `ENDED` is not a backend status.
- Important decisions: updated shared/list pages touched by the same incorrect status assumptions to prevent inconsistent UI after the DTO type was corrected.
- Issues discovered during review: `ENDED` appeared not only in tournament detail but also home, tournament list, and event page status checks.
- Issues fixed during review: removed obsolete `ENDED` mappings and comparisons; corrected `TournamentResponseDto.status` typing.
- Verification: `npm run build` passed. Existing warning remains: initial bundle exceeds the configured 500 kB budget.
