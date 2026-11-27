# Third-party notices

SourceRipple is licensed under the [Apache License 2.0](LICENSE). It depends on the open-source
components below, each under its own license. Only licenses compatible with Apache-2.0 are used
(MIT, ISC, Apache-2.0, BSD, 0BSD, SIL OFL for fonts). This list covers direct dependencies;
transitive dependencies are listed by `./mvnw dependency:tree` and `pnpm licenses list`.

Source files copied from other projects are listed under "Copied source" below. When one is
copied, its original copyright and license notice is kept and recorded here.

## Backend (bundled in the application image)

| Component | License |
|---|---|
| Spring Boot, Spring Framework, Spring Security, Spring Data JPA, Spring Session | Apache-2.0 |
| Hibernate ORM, Hibernate Validator | Apache-2.0 |
| Flyway (core and PostgreSQL database support) | Apache-2.0 |
| PostgreSQL JDBC Driver | BSD-2-Clause |
| HikariCP | Apache-2.0 |
| Micrometer (core and Prometheus registry) | Apache-2.0 |
| springdoc-openapi | Apache-2.0 |
| OkHttp, Okio | Apache-2.0 |
| jsoup | MIT |
| crawler-commons | Apache-2.0 |
| java-diff-utils | Apache-2.0 |
| Jackson | Apache-2.0 |
| Apache Tomcat (embedded) | Apache-2.0 |
| Kotlin standard library (OkHttp dependency) | Apache-2.0 |

## Backend (tests only, not distributed)

| Component | License |
|---|---|
| JUnit 5 | EPL-2.0 |
| Mockito | MIT |
| AssertJ | Apache-2.0 |
| Testcontainers | MIT |
| MockWebServer (OkHttp) | Apache-2.0 |
| JaCoCo | EPL-2.0 |

## Frontend (bundled in the application image)

| Component | License |
|---|---|
| React, React DOM | MIT |
| React Router | MIT |
| TanStack Query | MIT |
| Zustand | MIT |
| openapi-fetch | MIT |
| React Hook Form, @hookform/resolvers | MIT |
| Zod | MIT |
| Tailwind CSS | MIT |
| clsx | MIT |
| tailwind-merge | MIT |
| class-variance-authority | Apache-2.0 |
| Radix UI primitives (Dialog, Dropdown Menu, Label, Popover, Slot, Tabs, Tooltip) | MIT |
| lucide-react | ISC |
| Sonner | MIT |
| Motion | MIT |
| Geist Sans and Geist Mono fonts (via Fontsource) | SIL Open Font License 1.1 |

## Copied source

| Component | Where | License |
|---|---|---|
| shadcn/ui components (button, input, label, dialog, sheet, dropdown-menu, tabs, tooltip, popover, badge, skeleton, sonner), Copyright (c) 2023 shadcn | `frontend/src/components/ui/`, adapted to the Midnight tokens | MIT |

## Frontend (build and development tools, not distributed)

| Component | License |
|---|---|
| TypeScript | Apache-2.0 |
| Vite, @vitejs/plugin-react | MIT |
| ESLint, typescript-eslint, eslint-plugin-react-hooks, eslint-plugin-react-refresh | MIT |
| Prettier, prettier-plugin-tailwindcss | MIT |
| openapi-typescript | MIT |

## Container images and services (used unmodified, not redistributed in our image)

| Component | License |
|---|---|
| Eclipse Temurin (JRE base image) | GPL-2.0 with Classpath Exception |
| Node.js (build stage) | MIT |
| Maven (build stage) | Apache-2.0 |
| PostgreSQL | PostgreSQL License |
| Prometheus | Apache-2.0 |
| Grafana | AGPL-3.0 (run as a separate, unmodified container) |

## Design inspiration

The dashboard's design language takes interaction *patterns* from the Hatchet project
(MIT-licensed). No Hatchet code, names, logos, screenshots or copy are included.
