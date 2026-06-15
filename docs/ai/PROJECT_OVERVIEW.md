# PrecisionLayer Project Overview

## 🎯 What is PrecisionLayer?
PrecisionLayer is a multi-workspace SaaS testing platform designed for streamlined app testing, module management, and secure bug reporting.

## 🌟 Core Product Features
- **Workspace Management**: Organizations create workspaces to manage apps and modules.
- **App Version Control**: Testers upload APK builds for testing.
- **Bug Tracking**: Bugs are reported against specific APK versions, supported by multimedia evidence (screenshots/videos).
- **Secure Storage**: All sensitive files (APKs and media) are handled by Cloudflare R2 using temporary signed URLs.
- **Backend Infrastructure**: Powered entirely by Supabase.
- **Client**: Android native application built with Kotlin.

## 💻 Tech Stack
- **Android Client**: Kotlin, MVVM, Coroutines/Flow, Retrofit, Glide.
- **UI Architecture**: Single Activity architecture (`MainActivity`) with Fragment-based navigation.
- **Backend / Database**: Supabase (Postgres, Auth).
- **Serverless**: Supabase Edge Functions (Deno/TypeScript).
- **Object Storage**: Cloudflare R2 (Private buckets).

## 🏢 Workspace System
Multi-workspace support is foundational to the app. Everything revolves around the current active workspace.
- **Key Tables**: `workspaces`, `workspace_members`, `profiles`.
- **Golden Rule**: All major entities (modules, versions, testing sessions, bugs) must be strictly workspace-scoped. Always think "multi-workspace first" when designing queries or flows.
