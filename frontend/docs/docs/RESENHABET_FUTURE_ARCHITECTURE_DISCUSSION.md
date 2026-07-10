# ResenhaBET v2 — Architecture Discussion Notes

**Date:** 2026-06-03  
**Topic:** Multi-group hosting vs. single-instance-per-group architecture

---

## 1. Context

ResenhaBET is a private, self-hosted FIFA tournament management and betting platform for groups of friends. The current project brief defines a **single-instance, single-group-per-deployment** approach: each group runs its own instance and uses its own database.

During the discussion, a second idea was explored:

- host **one shared application instance**
- allow **multiple groups** inside it
- let users belong to **more than one group**
- keep wallets, tournaments, bets, and standings isolated per group

The goal of this note is to document the tradeoffs and the decision taken for the project roadmap.

---

## 2. The two approaches

### Approach A — One instance per group
Each group of friends gets its own deployment and its own database.

#### Advantages
- simpler data model
- easier to reason about
- strong isolation by default
- less risk of cross-group data leakage
- easier to implement in the short term
- matches the current project brief

#### Disadvantages
- self-hosting is harder
- each new group needs infrastructure setup
- users must create or configure a new database for every group
- onboarding new people is less friendly
- scaling the project to more groups becomes operationally expensive

---

### Approach B — Single hosted instance with multiple groups
One application serves many groups, and each user can join multiple groups.

#### Advantages
- much easier onboarding
- no need for each group to configure its own instance
- users can participate in multiple groups
- better product experience for broader adoption
- closer to a real hosted platform / SaaS-style model
- easier for casual users who do not want to self-host

#### Disadvantages
- major architectural refactor
- requires multi-tenant thinking across most of the domain
- more complex authorization and context handling
- more complex schema design and migration
- risk of delaying the actual betting features
- more testing required to avoid data leakage between groups

---

## 3. Why this is not a small change

Moving to a true multi-group model is not just “adding a group table”.

It affects the entire domain model:

- users
- group membership
- wallets
- players
- tournaments
- tournament players
- events
- bets
- transactions
- permissions
- authentication/session context

It also changes the way the backend understands the “current scope” of the request, because every action must know **which group is active**.

---

## 4. The key modeling decision

Because a user can belong to multiple groups, the project should use a **true multi-tenant approach**.

That means:

- **User** remains global
- **Group** becomes a first-class entity
- **GroupMember** links users and groups
- **Wallet** becomes group-scoped
- **Tournament** belongs to a group
- **Player** should be group-scoped
- **CurrentContext** must know both `userId` and `groupId`

This model is the most appropriate if ResenhaBET evolves into a shared platform rather than a separate deployment for each group.

---

## 5. The main design implications

### Group membership
A user does not belong to only one group.

Instead, the relationship becomes:

- one User
- many Group memberships
- one Group can have many Users

That naturally calls for a join entity such as:

- `GroupMember`
- `role` inside the membership
- join date
- possibly invite status later

### Wallet isolation
Wallets should not be global anymore.

They should belong to a user **inside a specific group**, so the same user can have different balances in different groups.

### Player isolation
Players should also be scoped by group.

That avoids:
- Elo contamination between groups
- standings mixing
- player histories leaking across communities

### Context awareness
The backend needs a notion of:
- who is the user?
- which group is active?

This should be handled through a centralized context service.

---

## 6. Benefits of changing now

There are real benefits if the architecture changes early:

- avoids locking the project into a structure that will be painful to undo later
- makes the product more usable for other groups
- removes the self-hosting friction for casual users
- supports the vision of a broader platform
- allows the codebase to evolve with the right abstractions from the start

If the project’s long-term goal is a shared platform, then preparing for multi-group support early is strategically valuable.

---

## 7. Disadvantages of changing now

There are also important costs:

- the betting system is not finished yet
- the data model would need a broad refactor
- the backend services would need to be rewritten in many places
- frontend flows would need group selection and group switching
- the implementation would slow down core feature delivery
- the risk of introducing bugs would increase

In short: the multi-group refactor is valuable, but it is also a large change that could derail the current phase if done too soon.

---

## 8. Decision reached

The decision was:

### Do not implement the full multi-group refactor immediately.
Instead:

- finish the betting system first
- keep the current Phase 2 roadmap intact
- prepare the codebase for multi-group support where it is cheap to do so
- create a dedicated future phase for the true multi-tenant refactor

This gives the project the best balance between:
- shipping the current core product
- preserving future flexibility
- avoiding premature complexity

---

## 9. Recommended roadmap shape

### Phase 2 — Betting Platform
Continue and finish the current betting-related work:

- markets and odds
- betting endpoints
- bet resolution
- player stats
- WebSocket updates
- frontend betting UI
- deployment/infrastructure work

### Future phase — Multi-Group Platform
Create a dedicated future phase for the refactor:

- Group entity
- GroupMember entity
- group-scoped wallet model
- group-scoped player model
- group-aware tournaments and bets
- group selector in the frontend
- invite and membership flow
- migration to group-based context handling

---

## 10. What should be prepared now

Even though the full refactor is postponed, it is worth preparing some foundations now:

- introduce a `CurrentContext` service
- avoid hardcoding global admin assumptions in new code
- keep services as isolated as possible
- avoid spreading user-only global assumptions throughout the backend
- document future tenancy boundaries clearly in the project brief

These are small changes now that will reduce future migration pain.

---

## 11. Final conclusion

The best path is:

- **finish the betting system now**
- **do not interrupt Phase 2 for the multi-group migration**
- **treat multi-group support as a real future phase**
- **prepare the codebase lightly for it during current development**

This keeps the project moving while preserving the option to evolve into a proper shared platform later.

---

## 12. Open question for later

When the multi-group phase begins, one important product decision will need to be finalized:

- should users be able to **create groups freely**
- or should groups be **invite-only**

That choice will affect onboarding, moderation, permissions, and platform growth.
