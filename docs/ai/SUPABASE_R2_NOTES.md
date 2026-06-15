# Supabase & Cloudflare R2 Notes

## ☁️ Cloudflare R2
PrecisionLayer uses Cloudflare R2 for all object storage to ensure cost-efficiency and high performance.
- **Private Bucket**: The R2 bucket is strictly private. There are NO public upload or download endpoints.
- **Database Tracking**: Only store the relative `file_path` in the Postgres database (e.g., `bugs/{workspace_id}/...`). Do NOT store full public URLs in the DB.
- **Interaction Method**: All client interaction with storage relies on short-lived presigned URLs generated securely by Supabase Edge Functions.

## ⚡ Supabase Edge Functions
- **Deployment Flag**: Edge Functions must be deployed using the `--no-verify-jwt` flag. 
  *(Example: `npx supabase functions deploy <name> --no-verify-jwt`)*
- **Manual Authentication**: Because the native JWT verification is disabled at the routing layer, every Edge Function MUST manually verify the user token using `supabase.auth.getUser(token)`.
- **Security & Authorization**: Functions must confirm not only who the user is, but also verify their `workspace_members` association before granting presigned URLs or performing mutations.

## 🛡️ Security Guidelines
- **No Orphan Uploads**: Edge Functions map uploads to database records immediately or utilize confirmation hooks.
- **Path-Based Restrictions**: R2 file paths should always inherently include the `workspace_id` to enforce multitenancy at the file level.

## 🗄️ Database & Schema Requirements
- **Strict Foreign Keys**: Always use composite foreign keys linking `(entity_id, workspace_id)` back to parent tables when associating cross-workspace records (e.g., `bug_reports` linking to `testing_sessions`). This guarantees database-level integrity for multi-tenancy.
- **Strict RLS**: Never use `(auth.role() = 'authenticated')` globally for data manipulation. All INSERT/UPDATE/DELETE/SELECT policies must actively check the `workspace_members` table against the row's `workspace_id`.
