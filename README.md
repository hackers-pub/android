# Hackers' Pub Android

An Android client for [Hackers' Pub](https://hackers.pub), a fediverse-compatible social network for developers.

## Features

- **Timeline**: View your personal home timeline with posts from people you follow
- **Explore**: Browse local and global/federated timelines
- **Notifications**: Stay updated with follows, mentions, replies, quotes, shares, and reactions
- **Search**: Find posts and users across the fediverse
- **Compose**: Create posts with Markdown support and visibility controls
- **Profiles**: View user profiles and their posts
- **Post Details**: See full posts with replies and reactions
- **Authentication**: Secure login via email verification code

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Networking**: Apollo GraphQL Client
- **Caching**: Apollo Normalized Cache with SQLite
- **Image Loading**: Coil
- **State Management**: StateFlow + ViewModel
- **Navigation**: Navigation Compose
- **Local Storage**: DataStore Preferences

## Project Structure

```
app/src/main/java/pub/hackers/android/
├── data/
│   ├── local/          # SessionManager (DataStore)
│   └── repository/     # HackersPubRepository
├── di/                 # Hilt modules
├── domain/
│   └── model/          # Domain models
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # Screen composables & ViewModels
│   └── theme/          # Material theme
├── HackersPubApplication.kt
└── MainActivity.kt
```

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 35

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/hackerspub-android.git
   cd hackerspub-android
   ```

2. Generate GraphQL code:
   ```bash
   ./gradlew generateApolloSources
   ```

3. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```

4. Or open in Android Studio and run on a device/emulator.

## GraphQL

The app uses Apollo GraphQL to communicate with the Hackers' Pub API at `https://hackers.pub/graphql`.

- Schema: `app/src/main/graphql/pub/hackers/android/schema.graphqls`
- Operations: `app/src/main/graphql/pub/hackers/android/operations.graphql`

To update the schema:
```bash
./gradlew downloadHackerspubApolloSchemaFromIntrospection
```

## Architecture

The app follows Clean Architecture principles with three main layers:

1. **Data Layer**: Repository, GraphQL client, local storage
2. **Domain Layer**: Business models and use cases
3. **Presentation Layer**: ViewModels and Composables

### Data Flow

```
UI (Compose) → ViewModel → Repository → Apollo Client → GraphQL API
                              ↓
                        Apollo Cache (SQLite)
```

## Screens

| Screen | Description |
|--------|-------------|
| Timeline | Personal home feed with pull-to-refresh and infinite scroll |
| Explore | Local/Global timeline tabs |
| Notifications | All notification types with appropriate icons |
| Search | Post and user search with query input |
| Settings | User info, sign out, cache management |
| Sign In | Two-step email verification flow |
| Compose | New post creation with visibility options |
| Post Detail | Full post view with replies and reactions |
| Profile | User profile with bio and posts |

## License

MIT License
