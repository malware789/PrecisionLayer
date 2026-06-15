# AI Agent Changelog

*This file tracks significant architectural changes, completed features, and system modifications made by AI agents. Add a new entry to the top after every feature completion.*

## [Current] - Post-Acceptance Navigation Flow
- **Feature**: Re-directed the user to MainActivity upon successful workspace invitation acceptance.
- **Details**:
  - `PrefsManager.kt`: Added `saveUserRole` and `getUserRole` to persist the user role.
  - `AuthViewModel.kt`: Refactored `_invitationActionState` to emit `Result<String>` ("ACCEPT" or "REJECT"). Added logic to `handleWorkspaceResolution` and `switchWorkspace` to save the user role to SharedPreferences.
  - `JoinWorkspaceFragment.kt`: Modified to fetch detailed workspaces only when receiving an "ACCEPT" success from the Edge Function, while always fetching pending invitations regardless.
  - Added observer for `detailedWorkspaces` to `JoinWorkspaceFragment`. If the workspace list is non-empty, it hides the "Create Workspace" button, displays the "Redirecting to workspace..." message, and transitions immediately to `MainActivity`.
  - `fragment_join_workspace.xml`: Added `tvRedirecting` to gracefully handle the visual transition during API calls.

## [Current] - Accept Invitation Edge Function Fixes (Idempotency & Profile Upsert)
- **Feature**: Fixed `accept-invitation` Edge Function to handle idempotency and foreign key violations gracefully.
- **Details**:
  - Identified that the `workspace_invitations.accepted_by` foreign key correctly expects an `id` from `public.profiles`.
  - Discovered that users signing up via the Android app sometimes lack a `public.profiles` row if the initial `createProfile()` API call drops/fails, triggering a foreign key violation when `accept-invitation` tries to update the invitation.
  - Added a reliable Profile mirroring step (an `upsert` on `profiles`) inside the Edge Function immediately before updating the invitation.
  - Implemented proper idempotency in the Edge Function:
    - If `status === 'accepted'`, the function immediately returns success.
    - If `status === 'revoked'` or `expired`, it returns an explicit error.
    - If the user is already in `workspace_members`, it skips insertion but still successfully updates the invitation state to `accepted`.
  - Version 2 of `accept-invitation` is actively deployed.

## [Current] - Invitation Decline RLS Policy Fix
- **Feature**: Fixed an issue where declining an invitation (PATCH) returned a 204 success but silently failed to update the row.
- **Details**:
  - Investigated existing RLS policies on `public.workspace_invitations`.
  - Found that while users could *read* their own invitations (`Users can view their own invitations`), and Admins/Managers could update them (`Admins and Managers can update workspace invitations`), there was no policy permitting the invitee themselves to update their invitation status.
  - Applied new Supabase migration: `CREATE POLICY "Users can decline their own pending invitations" ON public.workspace_invitations FOR UPDATE TO authenticated USING (email = auth.jwt()->>'email' AND status = 'pending') WITH CHECK (status = 'revoked');`
  - This precisely restricts the operation to only allow setting a pending invitation to `revoked`, preventing invalid state transitions (like revoked -> pending).

---

## [Previous] - JoinWorkspaceFragment Real Session Integration
- **Feature**: Replaced hardcoded `user@example.com` / dummy UUID in `JoinWorkspaceFragment` with the real authenticated Supabase user.
- **Details**:
  - Added `saveUserEmail` / `getUserEmail` to `PrefsManager` — email is now persisted alongside `userId` after login and signup.
  - `AuthRepository.login()` and `signUp()` now call `prefsManager.saveUserEmail(body.user.email)` immediately after a successful auth response.
  - Added `AuthRepository.getCurrentSession()` — returns `Pair<userId, email>` from SharedPreferences as a single call.
  - Added `AuthViewModel.loadCurrentSession()` — posts the session pair to `_currentSession` LiveData.
  - `JoinWorkspaceFragment` now observes `currentSession`: sets `currentUserId`/`currentUserEmail` from prefs and triggers `fetchPendingInvitations(email)` with the real email.
  - Added `android.util.Log` statements:
    - `D/JoinWorkspaceFragment: Authenticated session loaded — email: <email>, userId: <id>`
    - `D/JoinWorkspaceFragment: Invitations fetched — email: <email>, count: <n>`
    - `W/JoinWorkspaceFragment: Session email is null` (null guard)
    - `E/JoinWorkspaceFragment: Failed to fetch invitations: <msg>`
  - No changes to invitation schema, RLS policies, or the `accept-invitation` Edge Function.

---

## [Previous] - Team Management Phase 2 (Invitation System Client Integration)
- **Feature**: Android App integration with the new backend invitation architecture.
- **Details**:
  - Migrated `AuthApiService` from legacy `/invitations` table to the robust `/workspace_invitations` endpoint.
  - Implemented client call for the atomic `accept-invitation` Edge Function.
  - Aligned `Invitation` model to the new backend schema structure (`accepted_by`, `accepted_at`, `expires_at`).
  - Added `InviteTeamMembersViewModel` to handle sending invitations securely using `AuthRepository`.
  - Wired `InviteTeamMembersFragment` to observe ViewModel state and transition away from mock Phase 1 implementation.

---

## [Previous] - Team Management Phase 1 (Navigation + UI Skeleton)
- **Feature**: Initial implementation of "Invite Team Members" and "Roles & Permissions" screens.
- **Details**:
  - Registered `InviteTeamMembersFragment` and `RolesPermissionsFragment` in `main_nav_graph.xml`.
  - Added new options to `nav_drawer_menu.xml` under a "Workspace Management" group.
  - Configured `MainActivity.kt`'s `ToolbarConfig` and `isTopLevelDestination` to handle drawer toggling seamlessly without the bottom nav.
  - Implemented `fragment_invite_team_members.xml` and `fragment_roles_permissions.xml` as standard UI skeletons using modern Android Material components.
  - Verified UI build with `./gradlew assembleDebug`.

---

## [Previous] - Existing Session Add-Bug Flow & Secure Screenshots
- **Feature**: Existing session add-bug flow completed.
- **Details**: `BugListFragment` FAB now opens `ReportBugFormFragment` and explicitly passes the `sessionId`. Bugs drafted here are added directly to the existing testing session instead of spawning a new one. `BugGroupedListFragment` behavior remains unchanged (creates new session).
- **Database & Security Audit**: 
  - Identified and fixed a flaw where `bug_reports` could be inserted into arbitrary workspaces. 
  - Added a composite unique constraint on `testing_sessions(id, workspace_id)`.
  - Added a composite foreign key on `bug_reports(session_id, workspace_id)` referencing `testing_sessions(id, workspace_id)`.
  - Replaced the overly permissive global RLS policy on `bug_reports` with strict, workspace-scoped SELECT, INSERT, UPDATE, and DELETE policies.
  - **Note**: The old `bug_reports_session_id_fkey` FK was left intact alongside the new composite FK. This caused a `PGRST201` ambiguity error when querying `bug_reports(count)` from `testing_sessions`. Fixed by explicitly referencing the new relationship in `BugApiService.kt`: `bug_reports!bug_reports_session_workspace_fkey(count)`.
- **Security (Screenshots)**: Transitioned bug screenshot viewing from insecure path-loading to secure server-side presigned URLs. Created and deployed `view-bug-screenshot` edge function. Refactored `ReportedBugDetailsFragment` and added `ReportedBugDetailsViewModel` to manage secure URL fetching. Fixed Retrofit ES256 bug related to API Keys in headers.
