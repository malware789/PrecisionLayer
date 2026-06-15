# Architecture & Development Rules

## 📱 App UI Architecture
- **Single Activity**: `MainActivity` is the sole activity.
- **Global UI Ownership**: `MainActivity` owns the global Toolbar, Bottom Navigation, and Navigation Drawer. Do not spread Toolbar/BottomNav logic into individual Fragments.
- **Fragment Rules**: Fragments must contain only screen-specific content. 
- **Navigation States**:
  - Top-level screens (Dashboard, Modules, Bugs, Activity) display the BottomNav and show the drawer icon in the Toolbar.
  - Create/Detail screens hide the BottomNav and show a back arrow in the Toolbar.

## 📦 Module & Version System
- **Modules**: Represent apps/packages. The `package_name` field is critical for identifying and mapping APKs.
- **APK as Source of Truth**: There is no manual version input. The system extracts `versionName`, `versionCode`, and `packageName` directly from the APK file.
- **Version Flow**:
  1. User selects APK.
  2. Data is extracted locally.
  3. Validated via Supabase Edge Function (`validate-apk`).
  4. Uploaded directly to Cloudflare R2 using a signed URL.
  5. Confirmation trigger (`confirm-upload`).
  6. Database record inserted.
- **Rules**:
  - Uploaded `packageName` must perfectly match the module's `package_name`.
  - Same `version_name` increments `build_number`. New `version_name` resets `build_number` to 1.
  - Avoid orphan uploads. No public upload endpoints.

## 🐛 Bug Reporting System
- **Hierarchy**: Workspace → Module → Version → Testing Session → Bug Reports.
- **Drafting Flow**:
  - Implements a multi-bug draft system.
  - Media/screenshots are compressed and cached locally during drafting.
  - *No upload happens while drafting.*
- **Submission Flow**:
  1. User taps "Submit All Bugs".
  2. Creates a testing session (only if one doesn't exist).
  3. Calls `prepare-bug-screenshot-upload-batch` to get signed URLs.
  4. Uploads images to R2 in parallel (max concurrency 2-3) using memory-safe streaming.
  5. Inserts `bug_reports` into the database in a batch.
  6. Clears local cache.

## ⚙️ Development Principles
- **Backend Simplicity**: Keep backend simple. Do not over-engineer.
- **Efficiency**: Avoid unnecessary API calls. Prefer batch operations (e.g., batch insert).
- **Memory Safety**: Avoid memory-heavy operations like `readBytes()`. Use streaming uploads.
- **Small Steps**: Make small, safe changes. Confirm stability before proceeding.
