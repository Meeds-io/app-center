# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Application Center is a Meeds/eXo Platform add-on that manages and displays external applications. It is a Maven multi-module project with a Java backend and a Vue 2 frontend.

## Modules

- **`app-center-services`** — Backend: Spring Boot services, Spring Data JPA entities/DAOs, REST controllers, Liquibase migrations.
- **`app-center-webapps`** — Frontend: Vue 2 + Vuetify portlets bundled via Webpack, plus portal configuration XML.
- **`app-center-packaging`** — Addon packaging.

## Build Commands

### Full build
```bash
mvn clean install
```

### Backend only
```bash
mvn clean install -pl app-center-services
```

### Frontend
From `app-center-webapps/`:
```bash
npm install          # first time
npm run build        # production build
npm run watch        # dev watch mode
npm run lint         # eslint --fix
```

### Run a single Java test
```bash
mvn test -pl app-center-services -Dtest=ApplicationCenterServiceTest
```

## Backend Architecture

Layered Spring Boot stack:

```
REST (@RestController)  →  Service (@Service)  →  Storage (@Component)  →  DAO (Spring Data JPA)
ApplicationRest            ApplicationCenterService   ApplicationCenterStorage   ApplicationDAO
ApplicationFavoriteRest                                                           FavoriteApplicationDAO
```

- REST base path: `/app-center/rest/`
- Access control via eXo's `UserACL` and `@Secured("users")` / `@Secured("admins")`
- File images stored via eXo `FileService` (namespace `appCenter`)
- Translations via `TranslationService` with `ApplicationTranslationPlugin`
- PWA shortcuts via `AppCenterPwaShortcutPlugin`
- Default applications seeded from `applications.json` via `ApplicationCenterInjectService`
- DB schema managed by Liquibase: `db.changelogs/app-center-changelog-1.0.0.xml`
- JPA entity list registered in `jpa-entities.idx`

## Frontend Architecture

Multiple independent Webpack entry points, each compiled to an AMD bundle at `js/[name].bundle.js`:

| Entry | Purpose |
|---|---|
| `appCenterCommon` | Shared components (AppItem, AppShortcut) and JS service modules |
| `adminSetup` | Admin portlet for managing applications |
| `myApplications` | End-user portlet listing their applications |
| `appLauncher` | Drawer launcher overlay |
| `appCenterTopbarApplication` | Topbar button/icon |
| `appCenterTopbarPinnedApplication` | Pinned apps row in topbar |
| `appCenterUserSettings` | User preferences drawer |
| `quickActionExtensions` | Registers quick-action extensions |
| `appCenterTopbarExtension` | Registers topbar extension slots |

`vue` and `vuetify` are declared as Webpack externals — they are provided by the platform at runtime.

### Vue app conventions

- Each portlet has `main.js` (entry) → `initComponents.js` (component registration) → `services.js` (imports JS service modules).
- i18n is loaded asynchronously via `exoi18n.loadLanguageAsync(lang, urls)` before mounting.
- Cross-portlet communication uses `extensionRegistry` and DOM `CustomEvent`s (e.g. `extension-QuickAction-Extension-updated`).
- Locale files are in `src/main/resources/locale/` under `addon/` and `portlet/`.

### Frontend API calls

JS service files in `application-common/js/` call the backend REST API directly with `fetch()` using `credentials: 'include'`. Base path is `/app-center/rest/`.

## Testing

Tests use JUnit 5 + Mockito + Spring Boot Test. All tests are pure unit/mock tests — no running server required.
