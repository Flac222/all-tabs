# Commit Plan: Delivery 2

This document outlines the modular commit strategy to be followed during the development of Delivery 2 features. Each commit represents an atomic, feature-oriented change.

## Phase 1: Setup & Configuration (Branch: `feature/setup-and-styles`)
- `chore(deps): add splash screen and play services auth dependencies`
- `build: configure google-services plugin and firebase setup`
- `style(theme): implement dark mode color palette and typography based on UI mocks`

## Phase 2: Navigation & Core structure (Branch: `feature/navigation-and-di`)
- `feat(navigation): create base NavGraph and routing sealed classes`
- `feat(di): setup FirebaseModule for dependency injection`

## Phase 3: Splash Screen (Branch: `feature/splash-screen`)
- `feat(splash): create SplashViewModel with auth session checking`
- `feat(splash): implement Android Splash Screen API integration in MainActivity`
- `refactor(navigation): dynamically set start destination based on auth state`

## Phase 4: Authentication (Branch: `feature/authentication`)
- `feat(auth): create AuthViewModel handling Google Sign-In intent and states`
- `feat(auth): build Jetpack Compose LoginScreen UI matching design specs`
- `feat(auth): integrate Google Sign-In flow with UI and ViewModel`

## Phase 5: Routing Polish (Branch: `feature/routing-polish`)
- `feat(home): create basic HomeScreen placeholder`
- `fix(navigation): ensure proper routing from Login to Home on success`
- `docs: update documentation and project manifest for Delivery 2 progress`

## Phase 6: API Data Listing (Branch: `feature/api-listing`)
- `feat(api): integrate MusicBrainz API to fetch tab list (CU04)`
- `feat(home): display fetched tabs in HomeScreen LazyColumn`
- `feat(state): implement Loading, Success, and Error UI states for data fetching`

## Phase 7: Detail Screen (Branch: `feature/detail-screen`)
- `feat(navigation): add TabDetail destination to NavGraph`
- `feat(detail): create TabDetailScreen UI to display tab content (CU05)`
- `feat(detail): pass selected tab ID and fetch/display data`
