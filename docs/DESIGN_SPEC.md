# DESIGN_SPEC

> **Version 2** — Updated to reflect mobile-first refactor decisions.
> The app is used 99% on mobile. Every layout, interaction pattern, and
> component decision in this document defaults to mobile. Desktop is a
> secondary concern and is handled via responsive scaling only.

---

## Current Design Inventory

- **Pages:** authenticated app shell in `src/app/app.html`; `/groups` page in
  `src/app/pages/groups/groups-page.html`; the compact active-group switcher
  mounted in the shell via
  `src/app/components/active-group-switcher/active-group-switcher.html`.
- **Layouts:** global sticky header plus a mobile bottom navigation bar and
  content canvas in the app shell; the `/groups` page uses a two-zone workspace
  with an overview/list column and an action/detail column, followed by a
  conditional member-management area; the switcher uses a compact card layout
  with a select and primary action.
- **Components:** `GroupsPage` presents group listing, switching, creation, and
  member management; `ActiveGroupSwitcherComponent` presents the current group
  and a switch control; `GroupAdminOnlyDirective` gates admin-only UI fragments.
- **Shared UI patterns:** dark surfaces, rounded cards, cyan primary actions,
  uppercase eyebrow labels, role chips, responsive stack-to-grid behavior,
  empty states, loading states, error banners, and consistent min-height touch
  targets.

---

## Confirmed Refactor Decisions

These are locked decisions resulting from the design planning session.
They must be treated as requirements, not suggestions.

| # | Decision | Detail |
|---|---|---|
| 1 | Mobile-first, always | All layouts designed for mobile viewport first. Desktop is a secondary scaling concern. |
| 2 | Bottom tab bar navigation | 6 tabs: Dashboard, Tournaments, Groups, Bets, Players, Profile. No 7th Rankings tab. |
| 3 | No standalone Rankings tab | Player rankings live in the Players tab; tournament bet rankings live in the Bets tab. |
| 4 | Active group in the top header | Compact chip/label showing the current group name. Tapping it navigates to the Groups tab. |
| 5 | No active group = full-screen gate | A full-screen modal/overlay blocks ALL navigation until the user selects or joins a group via invite code. |
| 6 | `/groups` page uses in-page tabs | Three tabs: **Overview**, **Members**, **Settings**. No more stacked single-scroll layout. |
| 7 | FAB for primary creation actions | Floating Action Button (bottom-right, above the tab bar) triggers the primary creation action for the current page. |
| 8 | Dark theme only | No light mode. No system preference toggle. Dark surfaces with cyan accent remain the single visual identity. |

---

## Design Boundaries

- **Global shell ownership:** sticky top header (logo + active-group chip),
  bottom tab bar, full-screen group-gate overlay, and cross-page chrome. The
  shell does not own any page content.
- **Page ownership:** each tab page owns its internal layout, section
  hierarchy, in-page tabs (where applicable), and its own FAB label/action.
  The shell provides the FAB _container_ (position, z-index); the active page
  provides the FAB _intent_ (icon, label, action).
- **Component ownership:** `ActiveGroupSwitcherComponent` owns only the compact
  header chip and the tap-to-navigate behavior. Group management UI (listing,
  joining, creation, member actions) lives entirely in the Groups tab page.
  `GroupAdminOnlyDirective` owns visibility of protected fragments, not their
  styling.
- **No-group gate ownership:** the full-screen overlay is owned by the app
  shell, rendered above all navigation, and is non-dismissible until a valid
  group context is established. It is not a page or a route — it is a blocking
  layer on the authenticated shell.
- **Rankings ownership:** the Players tab owns the player leaderboard view.
  The Bets tab owns the bet/tournament ranking view. Neither is a standalone
  route or a shared Rankings module.
- **Styling ownership:** shell-level styles define chrome and navigation
  density; page-level styles define section spacing and card hierarchy;
  component-level styles define local control density and state affordances.
  A shared design-token layer (see Design System Opportunities below) is a
  refactoring target, not a prerequisite.

---

## Design Problems (Updated Assessment)

- **Duplicated patterns:** group switching appears both as a compact shell
  widget and as a larger page action. Under the new architecture the shell
  chip is the only switching entry point; the groups page tab structure
  eliminates the duplicated page-level switcher.
- **Density imbalance on mobile:** the `/groups` page collapses overview,
  creation, switching, and member administration into one scroll — this is
  the most acute problem on mobile. The new in-page tab structure (Overview /
  Members / Settings) directly resolves this by putting one concern on screen
  at a time.
- **Navigation lacks a primary mobile pattern:** the current sticky header +
  miscellaneous mobile nav is not a standard mobile pattern. The bottom tab
  bar replaces it with a universally understood paradigm and keeps primary
  destinations reachable by thumb without scrolling.
- **No group context = broken app with no clear recovery path:** currently
  there is no first-class "no group" state. The new full-screen group gate
  makes the required action unambiguous and non-skippable.
- **Inconsistent card chrome, button emphasis, and role chip styling:** cards,
  buttons, and labels repeat with small unintentional variations across the
  shell, switcher, and groups page. This is a visual consistency problem that
  makes the app read as several adjacent panels rather than one system.
- **Coupling between UI and state-driven branches:** presentation is heavily
  shaped by inline conditional rendering for active-group, loading, empty,
  and permission states. These branches need to be extracted into named,
  reusable state components (see Reusable Component Structure below).

---

## Refactoring Targets (Prioritized)

Priorities ranked: **Visual consistency → Mobile layout & touch targets →
Reducing page density / cognitive load → Component reusability / design system.**

### Priority 1 — Visual consistency (quick wins first)

- Standardize card chrome: one base card variant (surface + border-radius +
  padding) and one elevated variant (stronger background). No other card
  shapes.
- Standardize role chips: one size, one border treatment, color only varies
  by role value (OWNER / ADMIN / MEMBER). Same chip used everywhere.
- Standardize primary / secondary / destructive button treatment across all
  pages. One each. No bespoke per-page button styles.
- Standardize section eyebrow labels: uppercase, subdued color, consistent
  `font-size` and `letter-spacing` token. Same element used everywhere.
- Standardize empty-state panels and error banners into named components
  rather than inline conditional blocks.

### Priority 2 — Mobile layout & touch targets

- All tappable elements must meet a minimum 44×44 px touch target. Audit and
  fix any control that falls below this (role chips used as actions, small
  icon buttons, list-row cards with low padding).
- Bottom tab bar: icons + labels, full-width, safe-area-inset-bottom aware.
  Active tab uses cyan accent. Inactive tabs use subdued icon + label.
- FAB: 56×56 px minimum, cyan background, white icon, `position: fixed`,
  `bottom: calc(tab-bar-height + 16px)`, `right: 16px`. Always rendered above
  the tab bar, never overlapping it.
- Top header: fixed height (56 px), logo left, active-group chip center or
  left-of-center, no right-side clutter. The group chip is a tappable element
  (44 px minimum tap target height).
- In-page tabs (Groups page): full-width tab strip below the page header.
  Each tab label is touch-friendly. Tab content replaces the full content area
  below the strip — no side-by-side columns on mobile.

### Priority 3 — Reducing page density / cognitive load

- `/groups` Overview tab: show the active group summary card + a flat list of
  other groups the user belongs to. No creation form here — creation is behind
  the FAB.
- `/groups` Members tab: full-width member list with role chips and contextual
  actions (promote, remove) available via a per-row action menu (⋮ button or
  swipe-to-reveal). Member invite/add is behind the FAB.
- `/groups` Settings tab: group metadata, danger zone (leave group, delete
  group). Admin-only blocks gated by `GroupAdminOnlyDirective`.
- Dashboard: "Live Now" events, recent results, wallet summary for the active
  group. Single-column card stack. No sidebar. No grid on mobile.
- Bets tab: list of my bets for the active group-tournament, with a per-
  tournament ranking accessible via an in-page segment control or a secondary
  tab. Creation (new bet slip) is behind the FAB.
- Players tab: player list with current Elo, with a leaderboard view accessible
  via an in-page segment control. No separate Rankings tab.

### Priority 4 — Component reusability / design system opportunities

- Extract a `<app-card>` base component wrapping the standard surface + border.
- Extract `<app-empty-state>` (icon + title + subtitle + optional CTA).
- Extract `<app-role-chip>` with input `role: GroupMemberRole`.
- Extract `<app-section-header>` (eyebrow label + optional action link).
- Extract `<app-state-banner>` for error/warning/info banners.
- Extract `<app-loading-skeleton>` for card-shaped loading placeholders.
- Define CSS custom properties for the spacing scale, surface levels, and
  typography tokens (see Design System Opportunities below).
- These extractions happen incrementally during page refactors, not as a
  prerequisite blocking refactor work.

---

## Proposed Design Architecture

### Global Shell Structure

```
┌─────────────────────────────────────┐  ← fixed, 56px
│  [Logo]   [Active Group Chip]       │  ← top header
└─────────────────────────────────────┘
│                                     │
│         Page Content Area           │  ← scrollable
│         (active tab page)           │
│                                     │
│                          [FAB ●]    │  ← fixed, above tab bar
└─────────────────────────────────────┘
│  Dashboard │ Tourn. │ Groups │ ... │  ← fixed bottom tab bar
└─────────────────────────────────────┘
                                         ← safe-area-inset-bottom
```

### Bottom Tab Bar

| Tab | Icon suggestion | Route | Notes |
|---|---|---|---|
| Dashboard | `home` | `/dashboard` | Default landing after login + group selection |
| Tournaments | `emoji_events` | `/tournaments` | FAB = Create Tournament (admin only) |
| Groups | `group` | `/groups` | FAB = Join Group (code input) or Create Group |
| Bets | `receipt_long` | `/bets` | FAB = New Bet Slip |
| Players | `person` | `/players` | FAB = hidden or Add Player (admin only) |
| Profile | `account_circle` | `/profile` | No FAB |

### No Active Group — Full-Screen Gate

Triggered when: authenticated user has no `session.currentGroup` set.

```
┌─────────────────────────────────────┐
│                                     │
│           [App Logo]                │
│                                     │
│   Você ainda não está em um grupo.  │
│   Insira o código de convite para   │
│   entrar em um grupo existente.     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  Código do grupo            │   │
│   └─────────────────────────────┘   │
│   [ Entrar no Grupo ]               │
│                                     │
│   — ou —                            │
│                                     │
│   [ Criar novo grupo ]              │  ← admin/owner only
│                                     │
└─────────────────────────────────────┘
```

- Non-dismissible. Bottom tab bar and all page content are hidden behind
  this overlay.
- The backend flow for invite-code validation is outside this spec's scope
  and will be planned separately.
- After successful group join or creation, the overlay dismisses and the
  session is updated with the new active group.

### `/groups` Page — In-Page Tab Structure

```
┌─────────────────────────────────────┐
│  Groups                             │  ← page header
├──────────┬──────────┬───────────────┤
│ Overview │ Members  │  Settings     │  ← in-page tab strip
├──────────┴──────────┴───────────────┤
│                                     │
│  [Tab content — one concern only]   │
│                                     │
└─────────────────────────────────────┘
                           [FAB ●]
```

**Overview tab:** active group summary card (name, member count, your role) +
flat list of other groups you belong to (tap to switch). Switching updates
`session.currentGroup` via `POST /api/v1/groups/{id}/switch`.

**Members tab:** full-width list of group members. Each row: avatar initial +
name + role chip + ⋮ action menu (Promote / Demote / Remove — admin only,
gated by `GroupAdminOnlyDirective`). FAB = "Convidar membro" (admin only).

**Settings tab:** group name (editable by admin), created date, danger zone
(Leave Group always visible; Delete Group owner-only). Gating via
`GroupAdminOnlyDirective`.

### Active Group Chip (Header)

- Displays: current group name, truncated to ~16 characters with ellipsis.
- Tap navigates to the Groups tab (Overview).
- Visual: small rounded pill, subdued background, group icon left, chevron-
  right or down-arrow right. Not a dropdown — it navigates.
- When no group is active, this chip triggers the full-screen gate instead.

### FAB — Behavior Contract

- The shell renders the FAB container (fixed position, z-index, safe area
  offset).
- Each page registers its FAB action via a shared `FabService` or Angular
  signal input on the shell: `{ icon, label, action, visible, adminOnly }`.
- Pages that have no primary creation action set `visible: false` (e.g.
  Profile).
- Admin-only FABs are hidden entirely (not disabled) for non-admin members.

### Reusable Component Structure

| Component | Inputs | Responsibility |
|---|---|---|
| `<app-card>` | `elevated?: boolean` | Base surface, border-radius, padding |
| `<app-empty-state>` | `icon, title, subtitle, ctaLabel?, ctaAction?` | Empty list / zero-data states |
| `<app-role-chip>` | `role: GroupMemberRole` | Colored label chip for OWNER / ADMIN / MEMBER |
| `<app-section-header>` | `label, actionLabel?, actionRoute?` | Eyebrow + optional right-aligned action |
| `<app-state-banner>` | `type: error\|warn\|info, message` | Inline feedback banners |
| `<app-loading-skeleton>` | `rows?: number` | Card-shaped shimmer placeholders |
| `FabService` | Angular service | Registers current page's FAB intent |

### Design System Opportunities (Incremental)

These are targets to define during the refactor, not prerequisites:

**CSS custom properties (`:root`):**
```css
--surface-1: /* darkest background */
--surface-2: /* card background */
--surface-3: /* elevated card / input background */
--accent:    /* cyan primary action */
--accent-muted: /* subdued cyan for chips, secondary actions */
--text-primary:
--text-secondary:
--text-muted:
--radius-sm: 8px
--radius-md: 12px
--radius-lg: 16px
--space-xs: 4px
--space-sm: 8px
--space-md: 16px
--space-lg: 24px
--space-xl: 32px
```

**Typography tokens:**
```css
--type-eyebrow:  /* uppercase, 10–11px, letter-spacing 0.08em, muted */
--type-title:    /* 18–20px, semibold */
--type-body:     /* 14–15px, regular */
--type-meta:     /* 12px, muted */
```

**Surface levels:** `--surface-1` for the page background, `--surface-2` for
standard cards, `--surface-3` for inputs and elevated secondary cards. Never
more than three levels to avoid depth ambiguity.

---

## Out of Scope for This Refactor

- ~~Invite-code backend flow (join group by code) — product + backend decision
  pending.~~ **✅ Implemented — see Backend Implementation Status below.**
- Bet slip creation UI (FAB tap on Bets tab leads to this — spec separately).
- Competition discovery UI for REAL_FOOTBALL tournaments.
- Deploy and environment variable hardening.
- Light mode or system-preference theming.
- Desktop-specific layout work beyond responsive scaling of the mobile layout.

---

## Backend Implementation Status

Tracks backend work completed in support of this design spec.

| Feature | Endpoint | Status | Notes |
|---|---|---|---|
| Join group by invite code | `POST /api/v1/groups/join` | ✅ Implemented | V44 migration; `groupCode VARCHAR(6)` on `Group`; `SecureRandom` generation on create; `GroupJoinRequestDTO` with `@Pattern(regexp="^\d{6}$")`; `GroupMember(role=MEMBER, playerClaimed=false)` created on join; `TournamentWallet` provisioned for all existing `GroupTournament`s via `tournamentWalletProvisioningService.provisionForMember()`; `groupCode` exposed in `GroupResponseDTO` |

### Frontend contract for `POST /api/v1/groups/join`

The no-active-group full-screen gate (see Proposed Design Architecture) must
call this endpoint when the user submits an invite code. After a `200`
response:

1. Call `POST /api/v1/groups/{id}/switch` using the `id` from the response
   body to set the returned group as the active session group.
2. Dismiss the full-screen gate.
3. Check `playerClaimCompleted` on the user's `GroupMember` — if `false`,
   trigger the claim-player flow before allowing normal navigation.

Error states the frontend must handle:

| HTTP | Condition | UI response |
|---|---|---|
| `404` | Code not found | Inline error: "Código inválido. Verifique e tente novamente." |
| `409` | Already a member | Inline error: "Você já faz parte deste grupo." |
| `400` | Malformed code (not 6 digits) | Validation error on the input field. |
