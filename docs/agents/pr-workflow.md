# PR workflow

Issue-driven work (e.g. `/implement #<n>`) lands via a feature branch and a pull request —
**never commit directly to `master`.**

## Steps

1. **Branch first, before making any change**: `git checkout -b issue-<n>-<short-slug>` off an
   up-to-date `master` (e.g. `issue-3-incident-crud`). One branch per issue.
2. Implement, test, and review on that branch as usual (see the `/implement` skill).
3. Push the branch and open a PR with `gh pr create`, targeting `master`. Put `Closes #<n>` in the
   PR body so merging auto-closes the originating issue — don't close the issue manually as a
   separate step.
4. **Don't merge the PR yourself.** Leave it open for human review; merging is the user's call.

## Why

Established after `/implement #2` committed straight to `master` (no branch existed, so a
retroactive PR would have meant surgery on history that was already pushed). The user asked for
branch+PR going forward specifically so a PR's `Closes #<n>` does the issue-closing automatically
on merge, instead of a separate manual `gh issue close` step.
