# AI Agent Changelog

*This file tracks significant architectural changes, completed features, and system modifications made by AI agents. Add a new entry to the top after every feature completion.*

## [Current] - Team Management Phase 2 (Invitation System Client Integration)
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
