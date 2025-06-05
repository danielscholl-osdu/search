# Fork Management Template

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GitHub Issues](https://img.shields.io/github/issues/danielscholl-osdu/osdu-fork-template)](https://github.com/danielscholl-osdu/osdu-fork-template/issues)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/danielscholl-osdu/osdu-fork-template/pulls)

## 🤖 AI-Driven Development

[![Claude Ready](https://img.shields.io/badge/Claude%20Code-Ready-orange?logo=anthropic)](https://github.com/danielscholl/pr-generator-agent/blob/main/CLAUDE.md)
[![Copilot-Ready](https://img.shields.io/badge/Copilot%20Agent-Ready-8A2BE2?logo=github)](https://github.com/danielscholl-osdu/osdu-fork-template/blob/main/.github/copilot-instructions.md)
[![Template CI](https://img.shields.io/badge/Template%20CI-Active-green?logo=github)](https://github.com/danielscholl-osdu/osdu-fork-template/actions)

This project follows an AI-driven development workflow:
- 🤖 **Built with AI** - Developed and maintained using Claude Code and GitHub Copilot
- 📋 **AI Task Assignment** - Issues labeled with `copilot` are designed for AI implementation
- 📚 **AI-Friendly Documentation** - Comprehensive guides for AI agents in [CLAUDE.md](CLAUDE.md)
- 🔄 **Automated Workflows** - GitHub Actions with AI-enhanced PR descriptions and conflict resolution
- 🎯 **AI-First Architecture** - Designed with clear patterns for AI understanding and modification

This repository provides an automated template for managing long-lived forks of upstream repositories, ensuring controlled synchronization and release management. To understand conceptionally what is trying to be accomplished in this effort, see the [Overview OnePager](doc/overview.md). For detailed design and requirements, see the [Product Requirements Document](doc/prd.md).

## Features

This template automates the process of maintaining a fork while keeping it updated with upstream changes. When you create a repository from this template, it will:

- Set up a structured branch strategy for controlled upstream synchronization
- Configure automated workflows to handle syncing, validation, and releases
- Enforce branch protection rules to maintain repository integrity
- Manage releases with semantic versioning and upstream tracking

## Prerequisites

Before starting, ensure you have:
- GitHub account with repository creation permissions
- (Optional) Personal Access Token (PAT) for full automation:
  - Create a secret named `GH_TOKEN` in your repository
  - Required scopes: `repo`, `workflow`, `admin:repo_hook`
  - Without PAT: Manual configuration of branch protection and secrets required

## Quick Start

### 1. Create New Repository
1. Click the "Use this template" button above
2. Choose a name and owner for your new repository
3. Create repository

### 2. Initialize Repository
1. Go to Actions → Select "Initialize Fork" → Click "Run workflow" (if not already running)
2. An initialization issue will appear in the Issues tab
3. Follow the instructions in the issue from the bot to complete setup

## Branch Structure

The permanent branches control how upstream updates flow through validation before reaching the main branch:

```
             ┌────────────────────────┐
             │ fork_upstream          │
             │ (Tracks Upstream)      │
             └────────────────────────┘
                      ↓
             ┌───────────────────────┐
             │ fork_integration      │
             │ (Conflict Resolution) │
             └───────────────────────┘
                      ↓
             ┌───────────────────────┐
             │ main                  │
             │ (Stable)              │
             └───────────────────────┘
              ↑                     ↑
        Feature Branches       Certified Tags
        (Feature1, etc.)      (Downstream Pull)
```

## Automated Workflows

These workflows keep your fork in sync, enforce validation rules, and manage releases automatically:

### 1. Upstream Sync
- Scheduled automatic sync from upstream repository
- Manual sync available via Actions tab
- Automated conflict detection and notification
- [Details →](doc/sync-workflow.md)

### 2. Validation
- Enforces commit format and branch status
- Prevents merging of invalid PRs
- Ensures code quality and consistency
- [Details →](doc/validation-workflow.md)

### 3. Release Management
- Automated versioning and changelogs
- Tracks upstream versions with release tags
- [Details →](doc/release-workflow.md)

## Development Workflow

```mermaid
gitGraph
    checkout main
    commit id: "Init Repo" tag: "0.0.0"

    branch upstream
    checkout upstream
    commit id: "Upstream Sync 1" tag: "upstream-v1.0.0"

    checkout main
    branch integration
    checkout integration


    merge upstream 


    commit id: "Bugfix 1"

    checkout upstream
    commit id: "Upstream Sync 2" tag: "upstream-v2.0.0"

    checkout integration
    merge upstream


    commit id: "Bugfix 2"

    checkout main
    commit id: "Feature Work 1" tag: "0.0.1"
    commit id: "Feature Work 2" tag: "0.1.0"

    merge integration tag: "2.0.0"

    commit id: "Feature Work 3" tag: "2.1.1"
    commit id: "Feature Work 4" tag: "2.1.2"

```

### 1. Feature Development
1. Branch from main: `git checkout -b feature/my-feature main`
2. Make changes and test
3. Use conventional commits:
   ```
   feat: new feature
   fix: bug fix
   feat!: breaking change
   ```
4. Create PR → Review → Merge

### 2. Upstream Sync Process
1. Auto-sync PR created daily
2. Review changes
3. Resolve conflicts if needed
4. Merge sync PR

### 3. Release Process
1. Merge to main with conventional commits
2. Release Please handles versioning and changelog
3. Release includes upstream version tracking
