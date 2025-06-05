# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a Fork Management Template repository designed to help teams maintain long-lived forks of upstream repositories with automated synchronization and release management. It's specifically designed for OSDU (Open Subsurface Data Universe) projects but can be adapted for any upstream fork management scenario.

## Key Architecture

### Branch Strategy
- `main` - Stable production branch (protected)
- `fork_upstream` - Tracks upstream repository changes
- `fork_integration` - Conflict resolution and testing branch

### Workflow System
The repository uses GitHub Actions workflows for automation:
- **init.yml**: Repository initialization and setup
- **sync.yml**: Daily upstream synchronization with AI-enhanced PR descriptions
- **build.yml**: Build and test automation (supports Java/Maven projects)
- **validate.yml**: PR validation, commit message checks, conflict detection
- **release.yml**: Automated semantic versioning and changelog generation

## Core Documentation

- @docs/adr/README.md - Architectural decisions index
- @docs/project-architect.md - System architecture

## Common Commands

### For Java/Maven Projects (when initialized):
```bash
# Build the project
mvn clean install

# Run tests
mvn test

# Generate test coverage report
mvn clean test org.jacoco:jacoco-maven-plugin:0.8.11:report

# Run a single test
mvn test -Dtest=TestClassName#testMethodName
```

### GitHub CLI Operations:
```bash
# Create a pull request (use GitHub CLI as per .cursor/rules)
gh pr create --title "feat: add new feature" --body "Description"

# Check workflow status
gh workflow view

# View PR status
gh pr status
```

### YAML Workflow Validation:
```bash
# Validate GitHub Actions workflow YAML syntax (requires yq)
yq e '.' .github/workflows/<filename>.yml >/dev/null && echo "YAML is valid"

# Validate all workflow files
for file in .github/workflows/*.yml; do
  yq e '.' "$file" >/dev/null && echo "$file is valid" || echo "$file has errors"
done
```

**Common YAML Issues in GitHub Actions:**
- **Multiline strings in shell scripts**: Use single-line strings or heredoc syntax instead of inline multiline strings
- **Special characters**: Backticks, @symbols, and # can break YAML parsing within strings
- **Indentation**: Ensure consistent spacing in YAML structure
- **Variable interpolation**: Be careful with `${}` syntax in multiline strings

**Fix multiline string assignments:**
```bash
# ❌ Problematic (causes YAML parsing errors)
VARIABLE="line 1
line 2 
line 3"

# ✅ Fixed - Single line
VARIABLE="line 1. line 2. line 3"

# ✅ Fixed - Heredoc (for complex content)
VARIABLE=$(cat <<'EOF'
line 1
line 2
line 3
EOF
)
```

## Development Guidelines

1. **Commits**: Use conventional commits (feat:, fix:, chore:, docs:, etc.)
2. **Branches**: `agent/<issue>-<description>` format
3. **Testing**: Write behavior-driven tests, not implementation tests
4. **Type Safety**: Fix all mypy errors before committing
5. **Documentation**: Update ADRs when architecture changes

## Issue Creation and Labels

When creating GitHub issues, follow the label strategy defined in @docs/label-strategy.md:

1. **Always include one Type label**: bug, enhancement, documentation, etc.
2. **Add Priority when clear**: high-priority, medium-priority, low-priority
3. **Add Component labels**: configuration, dependencies, etc. (when relevant)
4. **Status labels are optional**: needs-triage, blocked, breaking-change
5. **Add copilot label**: When the issue is suitable for GitHub Copilot implementation

Example issue creation:
```bash
gh issue create -t "Add retry logic to storage client" \
  -l "enhancement,medium-priority,copilot" \
  -b "The storage client should retry failed requests..."
```
