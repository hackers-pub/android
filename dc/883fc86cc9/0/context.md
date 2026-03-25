# Session Context

## User Prompts

### Prompt 1

Bump up to v1.0.1, and auto increment version code

### Prompt 2

# Create Commit

Create a commit with auto-generated message from staged changes.

**Arguments**: `` (optional commit message override)

## Instructions

1. Check for staged changes:
   - Run `git diff --cached --stat` to see staged files summary
   - If no staged changes, inform the user and suggest `git add`

2. Gather information about staged changes:
   - Run `git diff --cached` to see the actual diff
   - Run `git status` to see overall status

3. Generate commit message:
   - If `` is prov...

### Prompt 3

Base directory for this skill: /Users/kodingwarrior/.claude/skills/release-tag

# Release Tag

Create an annotated git tag based on the version in `package.json`.

**Arguments**: `<message>` (optional — annotation message for the tag)

## Instructions

### Phase 1: Read Version

1. Read `package.json` and extract the `version` field.
2. Construct the tag name as `v{version}` (e.g., `v0.3.0`).

### Phase 2: Validate

1. Check if the tag already exists:
   ```bash
   git tag --list 'v{version}'
...

### Prompt 4

[Request interrupted by user]

### Prompt 5

Okay correction. not 1.0.1, 1.1.0

