# servdesk documentation

Where things are written down, and which document answers which kind of question.

| If you want to know… | Read |
| --- | --- |
| What servdesk is, how to run it, how to test it | [`../README.md`](../README.md) |
| Why the code is shaped the way it is, and what breaks if you change it | [`../CLAUDE.md`](../CLAUDE.md) |
| What a domain term means, and which synonyms to avoid | [`../CONTEXT.md`](../CONTEXT.md) |
| Why a specific hard-to-reverse decision was made | [`adr/`](adr/) |
| How work gets tracked, triaged and landed | [`agents/`](agents/) |
| What the API does, exactly | `../src/main/resources/static/openapi/servdesk-api.yaml`, browsable at `/docs/index.html` |

## Architecture decision records

ADRs record decisions that would be expensive to reverse, along with the options rejected and the
reasoning. They are historical documents: an ADR is amended when reality diverges from it, and
superseded rather than rewritten when a decision is genuinely reversed.

| ADR | Decision | Status |
| --- | --- | --- |
| [0001](adr/0001-ticket-subtypes-composed-not-inherited.md) | Ticket subtypes compose with a shared `Ticket` record instead of using JPA inheritance | Accepted, amended 2026-07-21 for the cross-subtype read surface |
| [0002](adr/0002-postgresql-only-product-owns-its-database.md) | PostgreSQL exclusively; the database ships as part of the product | Accepted |

New ADRs get the next number, a short imperative title, and the same shape: the decision up front,
then the options considered and rejected, then the consequences that follow from it.

## Working agreements

These describe how this repository is worked on — by humans and by coding agents alike.

| Document | Covers |
| --- | --- |
| [`agents/issue-tracker.md`](agents/issue-tracker.md) | GitHub Issues as the tracker, and the `gh` commands for each operation |
| [`agents/triage-labels.md`](agents/triage-labels.md) | The five triage labels and what each one means |
| [`agents/pr-workflow.md`](agents/pr-workflow.md) | Branch and pull request per issue; never a direct commit to `master` |
| [`agents/domain.md`](agents/domain.md) | How to consume the glossary and ADRs before changing code |
