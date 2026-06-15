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
- **Workspace Management (Phase 1)**:
  - Base Navigation and UI Skeletons implemented for "Invite Team Members" and "Roles & Permissions".

## 🌩️ Supabase Edge Functions (Deployed)
All functions are deployed with the `--no-verify-jwt` flag to allow custom token validation logic inside the functions.
1. `validate-apk`
2. `confirm-upload`
3. `prepare-bug-screenshot-upload`
4. `prepare-bug-screenshot-upload-batch`
5. `view-bug-screenshot`

## 🔒 Security
- Row Level Security (RLS) is enabled and enforced.
- Access strictly relies on workspace membership.
- No public Cloudflare R2 buckets. All interactions are via presigned URLs.
