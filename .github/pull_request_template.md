<!--
Title: Conventional Commits, for example "feat(monitor): claim due sources with a lease".
Target: develop (release PRs: develop -> main). Rules: CONTRIBUTING.md.
-->

## Summary

<!-- What does this PR change, and why? One short paragraph. -->

## Phase · PRD IDs

<!-- For example: Phase 4 · NFR-SEC-1 · U12, U13 -->

Closes #

## Changes

-

## Tests

<!-- New or changed tests (IDs from PRD §14 where they exist) and how you ran them locally. -->

- [ ] `./mvnw verify` passes locally (backend changes)
- [ ] `pnpm lint && pnpm typecheck && pnpm build` pass locally (frontend changes)

## Evidence

<!-- CI run link, screenshots, curl output or logs that show the change working. -->

## Checklist

- [ ] Title and commits follow Conventional Commits (`type(scope): subject`)
- [ ] The branch is rebased on the latest `develop` and CI is green
- [ ] Scope matches the phase; no unrelated changes
- [ ] No secrets, credentials or personal data in code, config, tests or logs
- [ ] Flyway migrations already merged are untouched (new changes use a new version)
- [ ] Docs, `.env.example` and the README are updated where behavior or configuration changed
- [ ] Reviewer requested (someone other than the author)
