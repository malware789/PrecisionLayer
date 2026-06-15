# AI Task Board

## Purpose
This board coordinates multiple AI agents (e.g., Antigravity, Codex, ChatGPT) working simultaneously in the PrecisionLayer repository. It prevents merge conflicts, overlapping work, and ensures smooth handoffs between different agents and sessions.

## Rules for Parallel AI Work
1. **Single Ownership**: One AI agent should own one isolated task/feature at a time.
2. **No Overlap**: Avoid two agents editing the same file or the same feature area at the same time. Check the **File Locks** table before proceeding.
3. **Branching**: Use separate Git branches for parallel work.
4. **Before Starting**: An agent must register their task in the **Active Tasks** table, including agent/tool name, branch name, task name, expected files/areas to edit, and status.
5. **After Finishing**: The agent must update the status, note the files changed, add testing notes, and append handoff notes before closing out.

---

## Active Tasks

| Agent/Tool | Branch | Task | Files/Area | Status | Last Updated | Notes |
|---|---|---|---|---|---|---|
| | | | | | | |

---

## File Locks
*If you are modifying critical, globally shared files (like navigation graphs or global ViewModels), log them here so other agents know to avoid them.*

| File/Area | Locked By | Reason | Status |
|---|---|---|---|
| | | | |

---

## Completed Tasks

| Date | Agent/Tool | Branch | Task | Files Changed | Notes |
|---|---|---|---|---|---|
| 2026-06-15 | Antigravity | main | Start Invitation & Role Management Flow (Phase 1) | main_nav_graph.xml, MainActivity.kt, InviteTeamMembersFragment, RolesPermissionsFragment, drawer_menu | **Status**: Completed.<br>**Testing Notes**: Implemented UI Skeletons. Configured MainActivity and Navigation Drawer handling. Built successfully via assembleDebug. |
| 2026-06-15 | Antigravity | main | Add Bug to Existing Session | BugListFragment.kt, ReportBugViewModel.kt | **Status**: Completed.<br>**Testing Notes**: Verified BugListFragment FAB properly passes `sessionId` args. Tested `onResume` for list refresh. Tested `ReportBugViewModel` logic properly reuses session with `takeIf { it.isNotBlank() }`. |
| 2026-06-15 | Antigravity | main | Fix PGRST201 Relationship Ambiguity | BugApiService.kt | **Status**: Completed.<br>**Testing Notes**: Updated `bug_reports(count)` to `bug_reports!bug_reports_session_workspace_fkey(count)` to fix the ambiguity caused by multiple foreign keys between `bug_reports` and `testing_sessions`. |
| 2026-06-15 | Antigravity | main | Phase 2 Invitation Client Integration | AuthApiService.kt, AuthRepository.kt, Invitation.kt, InviteTeamMembersFragment.kt, InviteTeamMembersViewModel.kt | **Status**: Completed.<br>**Testing Notes**: Migrated endpoints from legacy `/invitations` to `/workspace_invitations`, integrated `accept-invitation` Edge Function, created ViewModel, and wired UI for invitation sending flow. |

---

## Handoff Notes
*Use this section to drop detailed handoff snippets or context for the next agent picking up your branch.*
