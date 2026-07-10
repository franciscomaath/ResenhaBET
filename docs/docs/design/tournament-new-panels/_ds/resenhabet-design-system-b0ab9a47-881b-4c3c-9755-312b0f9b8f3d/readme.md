# ResenhaBET Design System

Design system for **ResenhaBET** — a FIFA tournament management and social sports-betting
platform for closed groups of friends. Players compete in FIFA tournaments (league, bracket,
or league+bracket); group members bet on match outcomes using Elo-based probabilistic odds.
There is **no house margin and no commercial intent** — it's a game economy for the resenha
(the crew). The product is moving to a single multi-tenant deployment serving many independent
friend groups, each with its own group-scoped wallet, players, and betting economy.

**Platform:** dark theme only, mobile-first (used ~99% on mobile). Angular 21 standalone +
signals + Tailwind v4 on the front end; Spring Boot / PostgreSQL on the back end.

---

## Sources

Everything here was reverse-engineered from materials provided by the team. Store for reference;
do not assume the reader has access.

- **Frontend codebase** — Angular app, mounted read-only at `frontend/` (paths like
  `frontend/src/app/...`). Ground truth for tokens (`frontend/src/styles.css`), components
  (`frontend/src/app/components/ui/`), and screens (`frontend/src/app/pages/`).
- **Backend codebase** — Spring Boot, mounted read-only at `backend/` (not required for visuals).
- **`uploads/DESIGN_SPEC.md`** — the v2 mobile-first refactor spec (shell architecture, tab bar,
  group gate, in-page tabs, FAB contract, named reusable components, and the `:root` token plan).

### ⚠️ Palette reconciliation (read this)
The company brand direction and the token file (`frontend/src/styles.css`) agree on a
**blue / green / gold** system:
- **green** → primary actions + money (stakes, balances, winnings)
- **blue** → navigation + accent (active tab, links, focus)
- **gold** → rankings + trophies **only**

…but many **live templates still use legacy `cyan`** (the DESIGN_SPEC v2 even calls cyan the
"single visual identity"). The app is mid-migration. **This design system encodes the canonical
blue/green/gold intent** and treats cyan as deprecated (kept as `--color-cyan-*` for reference,
shown on the "Legacy cyan" card). New work should not use cyan. **If the team actually wants to
keep cyan as the accent, tell me and I'll flip the tokens back.**

---

## Content fundamentals

- **Language:** Brazilian Portuguese, always. UI copy, labels, and errors are pt-BR.
- **Register:** casual and warm, the tone of a friend group — not a corporate sportsbook.
  Login literally asks *"Quem é você na resenha?"* ("Who are you in the crew?"). Dashboard
  greets with *"Bem vindo ao ResenhaBET."*
- **Person:** speaks **to** the user informally (você), possessives like *"Minhas Apostas"*,
  *"Sua carteira"*. No formal *"o senhor"*.
- **Casing:** Title Case or sentence case for headings; **UPPERCASE** reserved for tiny eyebrow
  labels and status shouts (*"APOSTAS ABERTAS!"*). Never all-caps body text.
- **Money:** always `R$` with `1.234,56` pt-BR formatting, and always in green.
- **Numbers/odds:** decimal odds shown as multipliers with an `x` suffix (`1.85x`).
- **Emoji:** not used in the product UI. Don't add them.
- **Terminology:** *grupo* (group), *torneio* (tournament), *aposta* / *bilhete* (bet / slip),
  *carteira* (wallet), *jogador* (player), *odd combinada* (combined odds), *retorno potencial*
  (potential return), roles *Dono / Admin / Membro* (Owner / Admin / Member).
- **Empty states are encouraging, not dry:** e.g. *"Nenhuma aposta ainda — vá até uma partida
  com mercado aberto e faça sua primeira aposta!"*

---

## Visual foundations

- **Theme:** dark only. No light mode, no system toggle. `color-scheme: dark`.
- **Backgrounds:** flat, near-black navy. Three surface levels, never more:
  `--surface-1` `#06101f` (page) → `--surface-2` `#121822` (card) → `--surface-3` `#030712`
  (inputs / elevated). No gradients, no images, no textures, no patterns behind content.
- **Color vibe:** cool, dark, high-contrast. Accents are saturated but used sparingly against the
  dark field — blue for wayfinding, green for money, gold only for rank/trophies, red for loss.
- **Type:** **Inter** for body/UI, **Inter Tight** for display (headings, big numbers, wordmark).
  Headings run heavy — `font-black` (900) for page titles and stat numbers. Money, odds, and
  scores use tabular figures.
- **Spacing:** 4-based scale (`4 / 8 / 16 / 24 / 32 / 48`). Mobile card padding 16px → 24px on
  larger widths.
- **Corner radii:** inputs 8px (`--radius-sm`), buttons/inset rows 12px (`--radius-md`), cards
  16px (`--radius-lg`, the app's `rounded-2xl`), chips fully rounded (pill). Cards are generously
  rounded; this is a soft, friendly shape language.
- **Cards:** one base chrome (hairline `#232c3a` border + `--surface-2` + 16px radius) and one
  `elevated` variant (stronger `gray-800` background + soft shadow). No other card shapes.
- **Borders:** 1px hairlines (`--border-hairline` `#232c3a`); stronger `gray-700` for inputs and
  elevated edges. Dashed borders signal empty/zero-data panels.
- **Shadows:** soft, dark, downward — `--shadow-card` `0 10px 30px rgba(0,0,0,.24)`. The bottom
  nav casts an **upward** shadow; bottom sheets cast a deeper one. No colored glows except the
  FAB and primary-slip pill, which carry a faint tinted shadow of their own color.
- **Elevation logic:** depth comes from surface level + shadow, not from many stacked z-planes.
- **Motion:** restrained. `--dur-fast` 120ms / `--dur-base` 180ms on the standard ease
  `cubic-bezier(.4,0,.2,1)`. Live indicators use a gentle opacity pulse; loading uses a
  left-to-right shimmer. No bounces, no long or decorative animations.
- **Hover:** darken/lighten the fill a step (desktop scaling only — this is a touch app).
- **Press / active:** color shift; the app is thumb-first, so **44px minimum touch targets**
  everywhere, 56px FAB.
- **Transparency & blur:** used only for scrims — the group-gate overlay and bottom-sheet backdrop
  darken the layer behind. No glassmorphism over content.
- **Layout:** fixed 56px top header (wordmark + active-group chip), scrollable content canvas,
  fixed bottom tab bar (safe-area aware), FAB fixed bottom-right above the bar. One concern per
  screen on mobile; in-page tab strips (not side-by-side columns) split dense pages.

---

## Iconography

- **System:** **Google Material Icons** (the classic filled ligature font), loaded from Google
  Fonts — exactly as the app does (`frontend/src/index.html`). This is the **only** icon system.
- **Usage:** ligature names via the `Icon` component or a `.material-icons` span. Core glyphs:
  `home` (Dashboard), `emoji_events` (Torneios), `group` (Grupos), `receipt_long` (Apostas),
  `person` (Jogadores), `account_circle` (Perfil), `add` (FAB), `more_vert` (row actions),
  `account_balance_wallet`, `shopping_cart`, `arrow_back`, `expand_more`.
- **SVGs:** the live bet-slip uses a couple of inline stroke SVGs (cart, chevron, close) as a
  legacy exception; prefer the Material equivalents (`shopping_cart`, `expand_more`, `close`).
- **Emoji / unicode as icons:** not used. Don't introduce them. (`×` is used as a score separator,
  which is typographic, not iconographic.)
- **No custom icon set, no sprite sheet.** Do not hand-draw SVG icons — use Material Icons.

### Logo / brand mark
**No logo file exists** in the provided codebase (only `assets/favicon.ico`, copied in). The brand
is rendered as a **wordmark in Inter Tight 900** — usually all-blue "ResenhaBET", with an accepted
alternate lockup that greens the "BET". See the "Wordmark" brand card. **A real logo was not
created** — if the team has one, send it and I'll wire it in.

---

## Tokens

Consumers link the single root **`styles.css`**, which `@import`s:
- `tokens/fonts.css` — Inter, Inter Tight, Material Icons (Google Fonts).
- `tokens/colors.css` — gray scale, brand base palette, semantic aliases, legacy cyan.
- `tokens/typography.css` — font families, weights, type-role sizes, tabular figures.
- `tokens/spacing.css` — spacing, radii, elevation, shell geometry, motion.
- `tokens/base.css` — element resets + `.material-icons`, `.tnum`, shadow utilities.

Key semantic aliases: `--surface-1/2/3`, `--accent` (blue), `--primary`/`--money` (green),
`--rank-gold`, `--danger` (red), `--text-primary/secondary/muted`, `--radius-sm/md/lg`,
`--space-*`, `--shadow-card`, `--header-height`, `--tab-bar-height`, `--fab-size`, `--touch-min`.

---

## Components

React primitives (namespace resolved at build — import via `window.<Namespace>` in card HTML;
run `check_design_system` for the exact name). Grouped by concern:

**core/** — `Button` (primary/accent/destructive/secondary/ghost), `Icon` (Material Icons wrapper),
`Fab`, `Input`, `Card` (base + elevated).

**data-display/** — `RoleChip` (Dono/Admin/Membro), `StatusBadge` (live/ended/won/lost/pending/…),
`SectionHeader` (eyebrow + action), `MoneyValue` (green BRL, tabular), `StatCell` (inset stat tile),
`OddButton` (Elo odd, blue/gold/selected), `RankRow` (leaderboard row, gold top-3).

**feedback/** — `StateBanner` (error/warn/info), `EmptyState` (dashed zero-data), `LoadingSkeleton`
(card shimmer).

### Intentional additions
- **`Icon`** — a thin Material Icons wrapper (the source uses raw `.material-icons` spans; a
  component gives new work a typed, consistent surface).
- **`MoneyValue`, `StatCell`, `OddButton`, `RankRow`** — extracted from patterns repeated across
  the ranking, players, my-bets, and match-panel views; they aren't named `<app-*>` components in
  the source but recur enough to standardize. Everything else maps directly to a spec-named
  primitive (`app-card`, `app-role-chip`, `app-section-header`, `app-empty-state`,
  `app-state-banner`, `app-loading-skeleton`) or a "standardize" target (button, input, status pill).

---

## UI kits

- **`ui_kits/mobile-app/`** — interactive recreation of the ResenhaBET mobile app in a phone frame:
  login (name → PIN) → no-group **full-screen gate** (invite code `482913`) → tabbed shell
  (Dashboard, Torneios, Grupos, Apostas, Jogadores) with the FAB, a live match with tappable
  Elo odds, a bottom-sheet **bet slip**, bettor + Elo **rankings**, and group in-page tabs
  (Overview / Membros / Config). Files: `index.html` (orchestrator), `shell.jsx`, `auth.jsx`,
  `screens.jsx`, `data.js`.

---

## File index

- `styles.css` — root entry (imports only).
- `tokens/` — `fonts.css`, `colors.css`, `typography.css`, `spacing.css`, `base.css`.
- `components/core/`, `components/data-display/`, `components/feedback/` — primitives
  (`*.jsx` + `*.d.ts` + `*.prompt.md` + one `*.card.html` per group).
- `guidelines/` — foundation specimen cards (Colors, Type, Spacing, Brand).
- `ui_kits/mobile-app/` — the app recreation.
- `assets/favicon.ico` — the only brand asset that exists.
- `SKILL.md` — Agent-Skill entry point.
- `readme.md` — this file.

Generated automatically (do not edit): `_ds_bundle.js`, `_ds_manifest.json`, `_adherence.oxlintrc.json`.
