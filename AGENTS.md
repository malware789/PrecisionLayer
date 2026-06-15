# AI Agent Guidelines for PrecisionLayer

Welcome! You are assisting with the PrecisionLayer project, a multi-workspace SaaS testing platform.

## 📚 Required Reading Before Coding
To understand the project rules, agents MUST read the following files in this exact order before writing any code:
1. `AGENTS.md` (This file)
2. `docs/ai/PROJECT_OVERVIEW.md`
3. `docs/ai/ARCHITECTURE_RULES.md`
4. `docs/ai/CURRENT_STATUS.md`
5. `docs/ai/PENDING_WORK.md`
6. `docs/ai/AI_TASK_BOARD.md`

*If your task touches Supabase, Edge Functions, RLS, authentication, APK upload, screenshot upload, or Cloudflare R2, you must also read:*
7. `docs/ai/SUPABASE_R2_NOTES.md`

## 🤝 Multi-Agent Workflow
We coordinate multiple AI agents in this repository. Follow this workflow:

**Before Coding:**
- Check `docs/ai/AI_TASK_BOARD.md`.
- Confirm no active task owns the same files or the same feature area you intend to modify.
- Add/update your active task entry on the board.
- List the expected files/areas you will edit to establish a file lock.

**After Coding:**
- Update `docs/ai/AI_CHANGELOG.md`.
- Update `docs/ai/CURRENT_STATUS.md` if project status/features changed.
- Update `docs/ai/PENDING_WORK.md` if task status changed.
- Update `docs/ai/AI_TASK_BOARD.md` to mark your task as completed and release file locks.
- Tell the user exactly which files changed in your response.

## 🚦 Safe vs. Unsafe Parallel Work

**Safe Examples (Good for parallel agents):**
- One AI handles Profile flow while another handles Invitation flow.
- One AI handles Bug Details media viewing while another handles the global Activity screen.

**Unsafe Examples (Do NOT do this):**
- Two AIs editing `MainActivity.kt` at the same time.
- Two AIs editing `main_nav_graph.xml` at the same time.
- Two AIs editing the same repository/viewmodel flow at the same time.
- One AI changing Supabase RLS while another changes Android repository code depending on those exact policies.

## 🌿 Branch Naming Conventions
Always use separate Git branches for parallel work. Use descriptive names:
- `feature/profile-flow-ai`
- `feature/invitation-flow-ai`
- `feature/bug-details-ai`
- `feature/activity-screen-ai`
- `fix/workspace-switch-ai`

## 🤖 AI Agent Behavior Rules
To ensure smooth collaboration and maintain project integrity, follow these rules strictly:
1. **Architecture Compliance**: Adhere strictly to the rules outlined in `docs/ai/ARCHITECTURE_RULES.md`.
2. **Documentation Maintenance**: Keep the AI documentation suite up to date as outlined in the workflow.
3. **Database & Backend Safety**:
   - Do NOT make Supabase schema or RLS changes unless explicitly required by the user.
   - For any approved Supabase changes, you MUST create a SQL migration file or clearly document the manual SQL executed.
4. **No App Code Changes for Documentation**: When asked to update AI docs, do not change application code unless explicitly combined with a coding task.

## 🏗️ Project Identity & Stack
- **Project**: PrecisionLayer
- **Stack**: Android (Kotlin), Supabase (Auth, Postgres, Edge Functions), Cloudflare R2 (Storage).
- **Core Principle**: Multi-workspace first. All primary data must be strictly workspace-scoped.

## ⚠️ Important Context Note
**Ignore `RouteManagmentActivity.kt`**: This file is completely unrelated to PrecisionLayer. It was mistakenly shared from another project/chat. Do not use it as project context or try to integrate with it.

## 🛠️ Build & Test Commands
*(Standard Android Gradle commands apply)*
- Clean project: `./gradlew clean`
- Build Debug APK: `./gradlew assembleDebug`
- Run Unit Tests: `./gradlew testDebugUnitTest`

For Supabase Edge Functions:
- Deploy: `npx supabase functions deploy <function_name> --no-verify-jwt`
