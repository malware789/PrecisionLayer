# AI Handoff Template

*Whenever an AI agent finishes a session or leaves a task partially complete, fill out this template and append it to the active conversation or save it in a scratchpad to ensure the next agent has full context.*

```markdown
## Current Task
[Briefly describe what the user asked for or what the overarching goal of this session was.]

## Files Changed
- `path/to/file1.kt`
- `path/to/file2.ts`
- `path/to/file3.xml`

## What Was Completed
- [x] Feature A implemented.
- [x] Edge function B deployed.
- [x] Bug C fixed.

## What is Pending / Left to Do
- [ ] Task X still needs UI binding.
- [ ] Edge function Y needs to be deployed.
- [ ] Error Z needs to be debugged.

## How to Test
1. Navigate to Screen X.
2. Click Button Y.
3. Observe Result Z.

## Supabase Changes (If Any)
- **Database**: [List schema changes, new tables, RLS changes]
- **Functions**: [List new or modified edge functions]

## Risks / Notes for Next Agent
- [Mention any fragile code, pending refactors, missing tests, or architectural concerns the next agent should watch out for.]
```
