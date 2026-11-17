# Security policy

## Supported versions

SourceRipple is in active development. Security fixes are applied to the latest release on
`main`; older versions are not patched.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

1. Use GitHub's private vulnerability reporting: on this repository, open **Security → Report a
   vulnerability**.
2. If that is unavailable, contact the maintainers privately at `security@<project-domain>`
   (the address is published here once the project domain is registered).

Include the affected version or commit, the steps to reproduce, the impact you observed and any
suggested fix. Never include real credentials or personal data.

## What to expect

- Acknowledgement within 3 working days.
- An initial assessment within 7 working days.
- A fix or mitigation plan agreed with you before any public disclosure; credit in the release
  notes if you wish.

## Scope

In scope: this repository's code, the Docker image and Compose stack, and the public preview
deployments. Areas of particular interest include server-side request forgery in URL fetching
and webhook delivery, authorization between organizations and projects, session and CSRF
handling, API key and webhook secret handling, and the scheduler tick endpoint.

Out of scope: denial of service through volume, findings that require a compromised host,
reports from automated scanners without a demonstrated impact, and the free-tier hosting
providers themselves.

Please test only against your own local instance (`docker compose up --build`), not against the
public preview.
