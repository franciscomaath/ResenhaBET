# Multi-Group Test Creation And Fixing Plan

## Purpose

This document is a handoff plan for a follow-up AI agent to repair and expand the backend test suite after the multi-group implementation.

Do not treat this as an architecture document. The implementation source of truth is `docs/MULTI_GROUP_BRIEFING.md` plus the current Java code.

The immediate goal for the next agent is:

1. Restore test compilation.
2. Update stale tests affected by the multi-group refactor.
3. Add focused coverage for group tenancy, `GroupTournament`, and `TournamentWallet`.
4. Run the full test suite and fix regressions.

## Current Verification Status

Production code compiles:

```powershell
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" compile
```

Known local tooling issue:

- `.\mvnw.cmd test` fails before Maven starts because the wrapper script hits a Windows PowerShell wrapper error.
- Use the cached Maven binary above unless the wrapper is repaired.

Known test-suite issue:

- `test-compile` currently fails broadly.
- Maven debug output shows `target\classes` is present on the test classpath, and `javap` can read main classes from it.
- Many existing tests are stale against constructor/signature changes from the multi-group refactor.

Do not start by adding many new tests. First restore test compilation with the smallest set of mechanical updates.

## Step 1: Repair Test Compilation

Run this command and focus only on compilation errors:

```powershell
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" "-DskipTests" "test-compile"
```

Expected stale areas to update:

- `CurrentUserContext.set(...)` now requires `(User user, Group group, UUID token)`.
- `UserServiceImpl` no longer receives or uses `WalletRepository`.
- `AuthServiceImpl` now depends on `GroupMemberRepository`.
- `PlayerServiceImpl` now depends on `GroupAuthorizationService`.
- `BetServiceImpl` now uses `GroupTournamentRepository` and `TournamentWalletRepository`, not `WalletRepository`.
- `WalletServiceImpl` now uses `GroupTournamentRepository`, `TournamentWalletRepository`, `GroupAuthorizationService`, `UserRepository`, and `GroupMemberRepository`.
- `BetSlip` no longer has `setTournament(...)`; use `setGroupTournament(...)`.
- `Transaction` uses `setTournamentWallet(...)` for new behavior.
- `WalletMapper.toResponse(...)` now accepts `TournamentWallet`.

Do not rewrite test intent yet. Keep each existing test as close as possible to its previous assertion, only adapting object construction/mocks.

## Step 2: Create Shared Test Fixtures

Add a small fixture helper for multi-group tests, preferably under:

```text
src/test/java/com/franciscomaath/resenhaapi/testsupport/
```

Suggested helper methods:

- `user(Long id, String name, UserType type)`
- `group(Long id, String name)`
- `groupMember(Group group, User user, GroupRole role)`
- `tournament(Long id, TournamentType type)`
- `groupTournament(Long id, Group group, Tournament tournament)`
- `tournamentWallet(Long id, GroupTournament gt, User user, BigDecimal balance)`
- `currentUserContextWith(User user, Group group)`

Keep helpers plain Java. Avoid Spring context bootstrapping for service unit tests unless an existing test already uses it.

## Step 3: Update Existing Service Tests

Update these first because they are closest to the changed domain model:

- `AuthServiceImplTest`
  - Mock `GroupMemberRepository`.
  - Verify login sets `currentGroupId/currentGroupName` when the user has a membership.
  - Verify login still works when the user has no group membership.

- `UserServiceImplTest`
  - Remove `WalletRepository` mock and wallet assertions.
  - Keep assertions that user creation creates a regular user without PIN.
  - Update `CurrentUserContext.set(...)` calls to include a group where admin context is needed.

- `PlayerServiceImplTest` and `PlayerLinkUserServiceImplTest`
  - Add `GroupAuthorizationService` mock.
  - Use `findByIdAndGroupId(...)`, `findByGroupIdOrderByNameAsc(...)`, and `existsByGroupIdAndUserId(...)`.
  - Verify create/update/link operations require active group admin.
  - Verify linking the same user is blocked only inside the same group.

- `TournamentServiceImplTest`
  - Add `GroupTournamentRepository`, `GroupAuthorizationService`, and `TournamentWalletProvisioningService` mocks.
  - Verify `findAll(...)` uses active group id.
  - Verify FIFA tournament creation saves a tournament, creates `GroupTournament`, and calls wallet provisioning.
  - Verify REAL_FOOTBALL creation reuses an existing tournament by competition and attaches the active group.
  - Verify FIFA tournament cannot attach when another group already owns it.

- `BetServiceImplTest`
  - Replace all `Wallet` setup with `TournamentWallet`.
  - Replace `BetSlip.setTournament(...)` with `BetSlip.setGroupTournament(...)`.
  - Mock `GroupTournamentRepository.findByTournamentIdAndGroupId(...)`.
  - Mock `TournamentWalletRepository.findByGroupTournamentIdAndUserId(...)`.
  - Verify duplicate pending bet checks include `groupTournamentId`.
  - Verify settlement credits the wallet for `betSlip.groupTournament`, not only `userId`.

- `WalletServiceImplTest`
  - Replace `WalletRepository` with tournament wallet repositories.
  - Add `GroupAuthorizationService`, `UserRepository`, and `GroupMemberRepository` mocks.
  - Verify `deposit(...)` rejects users outside the active group.
  - Verify `deposit(...)` creates a missing tournament wallet only for an active group member.
  - Verify `depositAll(...)` updates only wallets for the current `GroupTournament`.

## Step 4: Add New Focused Tests

After compilation is restored, add these new tests.

### Group Service

Create `GroupServiceImplTest`.

Scenarios:

- `create_shouldCreateGroupOwnerMembershipAndSwitchSession`
- `addMember_asAdmin_shouldCreateMembershipAndProvisionWallets`
- `addMember_ownerRoleRequiresOwner`
- `switchGroup_requiresMembership`

Assertions:

- Created group owner role is `OWNER`.
- `Session.currentGroup` is updated on create/switch.
- `TournamentWalletProvisioningService.provisionForMember(...)` is called after adding a member.

### Group Authorization Service

Create `GroupAuthorizationServiceImplTest`.

Scenarios:

- `requireCurrentGroupAdmin_allowsOwnerAndAdmin`
- `requireCurrentGroupAdmin_rejectsMember`
- `requireTournamentAccess_requiresGroupTournament`
- `requireTournamentAdmin_requiresBothAccessAndAdminRole`

### Tournament Wallet Provisioning

Create `TournamentWalletProvisioningServiceImplTest`.

Scenarios:

- `provisionForGroupTournament_createsWalletForEveryGroupMember`
- `provisionForGroupTournament_isIdempotent`
- `provisionForMember_createsWalletForEveryGroupTournament`
- `provisionForMember_isIdempotent`

Use repository mocks. Do not require database integration for this service.

### Migration Smoke Test

If the project has or can support a lightweight Flyway integration test, add one migration smoke test later. This is lower priority than service tests.

Goal:

- Apply migrations to a clean PostgreSQL-compatible test database.
- Assert `groups`, `group_member`, `group_tournament`, `tournament_wallet`, `player.group_id`, `bet_slip.group_tournament_id`, and `transaction.tournament_wallet_id` exist.

Do not block unit test repair on this.

## Step 5: Controller Test Updates

Controller tests that instantiate controllers directly need constructor updates.

Known changed controller:

- `MarketController`
  - Now requires `EventRepository`, `GroupAuthorizationService`, and `CurrentUserContext`.

Add `GroupControllerTest` with:

- `POST /api/v1/groups`
- `GET /api/v1/groups`
- `POST /api/v1/groups/{id}/members`
- `POST /api/v1/groups/{id}/switch`

Keep controller tests thin. Authorization behavior belongs mostly in service tests.

## Step 6: Run Tests Incrementally

Use this order:

```powershell
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" "-DskipTests" "test-compile"
```

Then targeted tests:

```powershell
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" "-Dtest=GroupServiceImplTest,GroupAuthorizationServiceImplTest,TournamentWalletProvisioningServiceImplTest" "test"
```

Then broader affected tests:

```powershell
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" "-Dtest=AuthServiceImplTest,UserServiceImplTest,PlayerServiceImplTest,TournamentServiceImplTest,WalletServiceImplTest,BetServiceImplTest" "test"
```

Finally:

```powershell
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd" "test"
```

Do not use `.\mvnw.cmd` until the wrapper script is fixed.

## Acceptance Criteria

The follow-up test work is complete when:

- `mvn test` passes using the cached Maven binary or a repaired wrapper.
- Existing tests no longer refer to global wallet behavior except legacy migration/entity tests.
- New tests cover:
  - group creation/switching;
  - membership roles;
  - tournament visibility through `GroupTournament`;
  - wallet provisioning;
  - betting debit/credit by `groupTournamentId`;
  - REAL_FOOTBALL reuse/attach behavior;
  - FIFA_MATCH one-group rule.
- Production compile remains green.

## Things Not To Change During Test Repair

- Do not reintroduce `tournament.group_id`.
- Do not make `Competition`, `Team`, `Round`, `Event`, `Market`, or `Outcome` group-scoped.
- Do not change the public bet request from `tournamentId` to `groupTournamentId`; the service resolves `GroupTournament` from active group context.
- Do not remove the legacy `wallet` table/entity until a separate cleanup migration is explicitly planned.
- Do not add DB partial indexes for the FIFA_MATCH one-group rule in this pass; v1 enforcement is service-level.
