# Session Context

## User Prompts

### Prompt 1

# Create Pull Request

Create a pull request with auto-generated title and body from commits.

**Arguments**: `` (optional base branch, defaults to `main`)

## Instructions

1. Parse the base branch:
   - If `` is provided, use it as the base branch
   - Otherwise, default to `main`

2. Gather information about the current branch:
   - Run `git branch --show-current` to get the current branch name
   - Run `git log <base>..HEAD --oneline` to see commits not in base
   - Run `git diff <base>...HE...

### Prompt 2

Wait, it should point at hackerspub/android.

### Prompt 3

Okay, it was hackers-pub

### Prompt 4

Okay, I forked

### Prompt 5

<bash-input>git switch main</bash-input>

### Prompt 6

<bash-stdout>Your branch is up to date with 'origin/main'.
Switched to branch 'main'</bash-stdout><bash-stderr></bash-stderr>

### Prompt 7

<bash-input>git switch -c fix/html-highlighting</bash-input>

### Prompt 8

<bash-stdout>Switched to a new branch 'fix/html-highlighting'</bash-stdout><bash-stderr></bash-stderr>

### Prompt 9

For rendering mention, hashtag, link, and so all, Could you hightlight differently? Reference ../hackerspub/web-next

### Prompt 10

<bash-input>git pull upstream main</bash-input>

### Prompt 11

<bash-stdout></bash-stdout><bash-stderr>error: cannot pull with rebase: You have unstaged changes.
error: Please commit or stash them.
</bash-stderr>

### Prompt 12

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

### Prompt 13

<bash-input>git pull upstream main</bash-input>

### Prompt 14

<bash-stdout>From github.com:hackers-pub/android
 * branch            main       -> FETCH_HEAD
 * [new branch]      main       -> upstream/main
Rebasing (1/1)Successfully rebased and updated refs/heads/fix/html-highlighting.</bash-stdout><bash-stderr></bash-stderr>

### Prompt 15

# Create Pull Request

Create a pull request with auto-generated title and body from commits.

**Arguments**: `` (optional base branch, defaults to `main`)

## Instructions

1. Parse the base branch:
   - If `` is provided, use it as the base branch
   - Otherwise, default to `main`

2. Gather information about the current branch:
   - Run `git branch --show-current` to get the current branch name
   - Run `git log <base>..HEAD --oneline` to see commits not in base
   - Run `git diff <base>...HE...

### Prompt 16

Could you give me build script which install in my cellphone?

### Prompt 17

<bash-input>git switch main</bash-input>

### Prompt 18

<bash-stdout>Your branch is up to date with 'origin/main'.
Switched to branch 'main'</bash-stdout><bash-stderr></bash-stderr>

### Prompt 19

git pull upstream main

### Prompt 20

<bash-input>git push</bash-input>

### Prompt 21

<bash-stdout>To github.com:malkoG/hackerspub-android.git
   f5ca675..984cc02  main -> main</bash-stdout><bash-stderr></bash-stderr>

### Prompt 22

<bash-input>git switch -c fix/article-rendering</bash-input>

### Prompt 23

<bash-stdout>Switched to a new branch 'fix/article-rendering'</bash-stdout><bash-stderr></bash-stderr>

### Prompt 24

For rendering article, Could you render article card differently, and create the article detail page? See ../hackerspub/web-next

### Prompt 25

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

### Prompt 26

For article rendering or note rendering, we need to full support for code snippet/heading/list/numbered list. Could you also considerate them?

### Prompt 27

[Request interrupted by user]

### Prompt 28

Continue from where you left off.

### Prompt 29

For article rendering or note rendering, we need to full support for code snippet/heading/list/numbered list. Could you also considerate them?

### Prompt 30

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

