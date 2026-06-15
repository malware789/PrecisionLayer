# Current Project Status

## ✅ Core Features Implemented
- **Workspace/Auth**: User authentication, workspace switching, and profile management basics.
- **Module & App Version Flow**: Fully functional APK extraction, edge function validation, and signed R2 upload.
- **Bug Reporting Pipeline**:
  - Multi-bug draft system with SavedStateHandle persistence.
  - Local caching and image compression.
  - Parallel batch uploading of media via signed R2 URLs.
  - `BugGroupedListFragment` FAB creates a new testing session and opens `ReportBugFormFragment`.
  - `BugListFragment` FAB passes an existing `sessionId` to `ReportBugFormFragment`, adding new bugs to the existing session without creating duplicates.
- **Secure Bug Media Viewing**:
  - App securely fetches Bug screenshots using the `view-bug-screenshot` Edge Function, which validates permissions and returns a short-lived R2 signed GET URL.
  - Integrated into `ReportedBugDetailsFragment` via `ReportedBugDetailsViewModel`.
- **Workspace Management (Phase 1 & 2)**:
  - Navigation and UI implemented for "Invite Team Members" and "Roles & Permissions".
  - `workspace_invitations` table deployed with full RLS (admin/manager create, self-view for invitees, self-decline for pending invitations).
  - `InviteTeamMembersFragment` wired to real backend — sends invitations to `workspace_invitations` via `AuthRepository.createInvitation()`.
  - `JoinWorkspaceFragment` now loads the real authenticated user session from `SharedPreferences` (no hardcoded values). Fetches pending invitations by actual authenticated email. Logcat-verified session loading and invitation count.
  - Invitation declining (PATCH) works correctly due to a targeted RLS UPDATE policy allowing transitions from `pending` -> `revoked`.
  - `accept-invitation` Edge Function deployed and fully idempotent. Handles missing `profiles` gracefully by mirroring rows before updating invitations.
  - Post-acceptance Android navigation flow is complete: the app now fetches updated workspace memberships and immediately redirects the user from the `JoinWorkspaceFragment` to the `MainActivity` home dashboard upon successful acceptance.
## 🌩️ Supabase Edge Functions (Deployed)
All functions are deployed with the `--no-verify-jwt` flag to allow custom token validation logic inside the functions.
1. `validate-apk`
2. `confirm-upload`
3. `prepare-bug-screenshot-upload`
4. `prepare-bug-screenshot-upload-batch`
5. `view-bug-screenshot`
6. `accept-invitation`

## 🔒 Security
- Row Level Security (RLS) is enabled and enforced.
- Access strictly relies on workspace membership.
- No public Cloudflare R2 buckets. All interactions are via presigned URLs.
