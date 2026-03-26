# CineVault — Premium Movie Streaming Platform

A production-grade full-stack movie streaming application with an Android user app, React admin dashboard, and NestJS backend API.

## Architecture

```
┌─────────────────────┐     ┌─────────────────────┐
│   Android App       │     │   Admin Dashboard    │
│   (Kotlin/Compose)  │     │   (React/TypeScript) │
└────────┬────────────┘     └────────┬────────────┘
         │                           │
         └───────────┬───────────────┘
                     │ REST API
         ┌───────────▼───────────────┐
         │   Backend API (NestJS)    │
         └───────────┬───────────────┘
                     │
         ┌───────────▼───────────────┐
         │   MongoDB + Redis         │
         └───────────────────────────┘
```

## Modules

| Module | Stack | Directory |
|--------|-------|-----------|
| Backend API | NestJS, MongoDB, Mongoose, JWT, S3/R2, Swagger | `backend/` |
| Android App | Kotlin, Jetpack Compose, ExoPlayer, Retrofit, Hilt | `android/` |
| Admin Dashboard | React 18, Vite, TypeScript, Tailwind, React Query | `admin/` |

## Design System

| Token | Value |
|-------|-------|
| Background | `#0A0A0A` |
| Surface | `#141414` |
| Gold Accent | `#F5A623` |
| Display Font | Playfair Display |
| Body Font | DM Sans |
| Mono Font | JetBrains Mono |

## Quick Start

### Prerequisites

- Node.js 18+
- MongoDB 6+
- Android Studio Hedgehog+ (for Android app)
- JDK 17

### Backend

```bash
cd backend
cp .env.example .env    # configure environment variables
npm install
npm run start:dev       # http://localhost:3000
```

Swagger docs available at `http://localhost:3000/api/docs`.

### Admin Dashboard

```bash
cd admin
npm install
npm run dev             # http://localhost:3001
```

### Android App

1. Open `android/` in Android Studio
2. Sync Gradle
3. Update `BASE_URL` in `NetworkModule.kt` to your backend URL
4. Run on device or emulator (API 26+)

### Docker (Backend + Database)

```bash
docker compose up -d
```

## Features

### User App (Android)
- Splash, onboarding, auth (login/register/forgot password)
- Home feed with hero banner carousel and dynamic sections
- Movie/series detail with episodes, cast, reviews
- HLS video player with quality/speed controls and progress tracking
- Search with autocomplete, filters, trending
- Watchlist, continue watching, watch history
- Profile management with PIN protection

### Admin Dashboard
- Dashboard analytics with charts
- Movie & series CRUD with cast, streaming sources, genres
- User management with suspend/unsuspend
- Banner management with active toggles
- Review moderation (approve/reject)
- Push notification composer

### Backend API
- JWT auth with refresh tokens
- 17 modules: Auth, Users, Profiles, Movies, Series, Banners, HomeSections, Search, WatchProgress, Watchlist, Reviews, Notifications, Upload, Streaming, Admin, Analytics, Health
- File upload to S3/R2
- HLS streaming support
- Role-based access control (user/admin)

## License

Private — All rights reserved.
