<div align="center">

<img src=".github/media/logo.png" width="120" height="120" alt="MPEI.Hub Logo" />

# «МЭИ.Хаб» · MPEI.Hub

### Smart Campus Ecosystem on Dopamine ⚡

<p align="center">
  <a href="README.md">🇷🇺 Русский</a> &nbsp;•&nbsp; <b>🇬🇧 English</b>
</p>

**Offline Timetable · BARS Portal with 2FA · Lefortovo Offline Map · Glance Widget · OLED Black**

*Everything for a frictionless academic life at NRU "MPEI" — from freshman orientation to diploma defense.*<br>
*Tactile Jetpack Compose, zero-tracker privacy, pure offline-first performance, and zero dependency on Google Play Services.*

<br/>

[![CI](https://img.shields.io/github/actions/workflow/status/ghostoftheeleven/mpei-next/android.yml?branch=main&label=CI&logo=github&color=2ea44f)](https://github.com/ghostoftheeleven/mpei-next/actions)
![Android](https://img.shields.io/badge/Android-8.0+_(API_26–36)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2_·_KMP-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-KMP_WIP-000000?logo=apple&logoColor=white)
<br/>
![Privacy](https://img.shields.io/badge/privacy-152--FZ_·_Zero_Trackers-6f42c1)
![Third-party SDK](https://img.shields.io/badge/third--party_SDK-0-008080)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)
[![Release](https://img.shields.io/github/v/release/ghostoftheeleven/mpei-next?label=Release&color=orange)](https://github.com/ghostoftheeleven/mpei-next/releases)

<br/>

[📥 Download Release APK](https://github.com/ghostoftheeleven/mpei-next/releases/latest) • [📖 Documentation](docs/) • [🗺️ Campus Map](#-navigation--lefortovo-campus) • [🔐 BARS Portal](#-bars-student-portal--2fa) • [🛠️ Building](#-building-and-running)

</div>

---

> [!NOTE]
> **«МЭИ.Хаб» (MPEI.Hub)** is an independent, open-source continuation of the renowned **MpeiX** application originally created by Anton Kolomeytsev. Built by students for students and faculty of NRU "MPEI", focused on uncompromising speed, modern design ergonomics, and absolute data privacy.

---

## 🚀 Features

### 📅 Class Schedule Without Compromises
* **Direct Official API:** fetches schedules directly from the university's production backend (`ts.mpei.ru/api`) with no intermediary proxy servers or scraping middleware.
* **Faculty / Teacher Mode:** dedicated professor view displaying real-time student group rosters per class with one-tap deep navigation into any group's timetable.
* **Dual View Modes:** switch effortlessly between single-day vertical feeds and panoramic multi-day timelines (*Weekly Timeline Grid*).
* **Smart Filtering & Subject Chips:** instant filtering by class types (lectures, seminars, lab works, course projects) and quick-select discipline chips for the current study week.
* **Offline-First Architecture:** all timetable data and personal notes are cached in an encrypted Room SQLite database, remaining instantly accessible even in deep basement laboratories with zero network coverage.
* **Background Monitoring & Alerts:** automated periodic schedule polling via WorkManager with proactive notifications for room relocations, schedule replacements, and reminders sent 24 hours and 5 hours before lab works and seminars.

### 🔐 BARS Student Portal & 2FA
* **Comprehensive 2FA Support:** seamless authentication supporting all official second-factor methods — TOTP authenticators (Google Authenticator, Yandex Key), MAX messenger, Telegram, VK ID, and E-mail OTP.
* **Resilient Session Management:** active session states survive app switching and OTP multitasking with a 5-minute time-to-live (TTL), completely preventing login form resets.
* **Academic Performance Tracking:** continuous evaluation grades, midterm checkpoint weeks, cumulative semester ranking, and expandable discipline cards with detailed milestone statuses.

### 🗺️ Navigation & Lefortovo Campus
* **100% Offline OpenStreetMap:** campus buildings, dormitories, student canteens, sports complex, university clinic, and faculty departments powered by `osmdroid`.
* **Zero API Keys & Zero Play Services:** completely autonomous map rendering requiring no Google APIs, proprietary SDKs, or cloud map billing.
* **Lifecycle-Isolated Map Renderer:** decoupled domain marker states and map views ensure complete GPU and memory reclamation when leaving the screen, preventing Android Activity leaks.
* **Scheme & Hybrid Modes:** rapid switching between vector schema and hybrid satellite imagery optimized for high glare sunlight or low-light evening walks.

### 🎨 Design, Adaptability & System Integration
* **Jetpack Compose + Material 3:** modern tactile UI backed by a custom, cohesive UI Kit design system.
* **Triple Theme Palette:** precisely calibrated Light theme, comfortable Dark theme, and ultra-deep **OLED True Black** for maximum battery longevity on AMOLED displays.
* **Glance Desktop Widget:** interactive home screen widget with quick day toggling and reactive background updates when classes change.
* **Polished Transitions:** native SplashScreen API (Android 12+) coupled with a custom Compose vector animation showcasing the glowing MPEI.Hub emblem rim.
* **Foldables & Desktop Mode:** responsive layout adjustments across foldable form factors (Samsung Galaxy Z Fold/Flip, Google Pixel Fold) without Activity recreation, specialized `layout-sw600dp` tablet grids, Split-Screen multitasking, and Samsung DeX.
* **Bilingual Localization:** full Russian and English localization with instant runtime language switching in settings.

### 🛡️ Paranoid Privacy (Zero-Tracker Policy)
* **Zero Third-Party SDKs & Telemetry:** no Yandex.Metrica, no Firebase or Google Analytics/Crashlytics, no ad networks, and no telemetry libraries whatsoever, on Android and on iOS. Crash reports and usage statistics are not sent anywhere.
* **152-FZ & GDPR-K Compliance:** credentials, session cookies, local notes, and caches remain strictly on the user's physical device in hardware-backed secure storage.
* **DeGoogled OS Ready:** tested and fully operational on privacy-hardened Android distributions (GrapheneOS, CalyxOS, LineageOS, AOSP) and Huawei devices (HarmonyOS / EMUI).

---

## 🍏 Multiplatform Roadmap: iOS & Kotlin Multiplatform

The project is actively transitioning core modules to Kotlin Multiplatform (KMP):
* **lib_bars_parser & lib_bars_client:** the BARS HTML parser and Ktor network client have been refactored into pure KMP modules running across JVM, Android, and Apple iOS runtimes.
* **iOS App Shell:** an initial iOS application shell is included in the repository, automatically building unsigned `.ipa` bundles via GitHub Actions macOS workflows.
* Migration guides and documentation:
  * [Compose Multiplatform Migration Strategy (Android + iOS)](./docs/COMPOSE_MULTIPLATFORM_MIGRATION.md)
  * [Installing iOS .ipa on iPhone from Windows](./docs/IOS.md)

---

## 🏗️ Architecture & Technology Stack

The project adheres to **Clean Architecture** conventions with unidirectional data flow (TEA / ELM):

| Layer / Subsystem | Technology Stack |
|---|---|
| **Language & Toolchain** | Kotlin 2.2.10 / 2.2.21, Java 17/21, AGP 8.12.3, Gradle Composite Builds (`build-logic`) |
| **User Interface** | Jetpack Compose, Material 3, custom `MpeixTheme` token system + UI Kit |
| **State Management (MVI)** | TEA / ELM powered by Vivid Money Elmslie + `lib_elm_compose` |
| **Desktop Widgets** | Jetpack Glance (`glance-appwidget`, `glance-material3`) |
| **Mapping & Geospatial** | OpenStreetMap (`osmdroid 6.1.20`) |
| **Networking** | Retrofit 3, OkHttp 5, Ktor Client (KMP), Kotlinx Serialization, TLS 1.2/1.3 |
| **Persistence & Cache** | Room Database 2.8 (KSP), MMKV, SharedPreferences |
| **Dependency Injection** | Koin 4.1 (`koin-android`, `koin-compose`) |
| **Background Processing** | AndroidX WorkManager 2.10 |
| **Quality Assurance** | JUnit 5 Platform, Kotest (StringSpec, BehaviorSpec), UI Automator Smoke Tests |

For an in-depth breakdown of module boundaries, `api` vs `impl` contracts, and architectural invariants, refer to [ARCHITECTURE.md](./ARCHITECTURE.md).

---

## 🛠️ Building and Running

The project requires no proprietary API tokens and builds locally out-of-the-box with standard Gradle tooling.

### Prerequisites
- **JDK 17** or **JDK 21** (recommended: [Eclipse Adoptium Temurin 21](https://adoptium.net/))
- **Android SDK** (API 34/35/36)
- **Android Studio Ladybug / Meerkat** or newer

### Terminal Commands

```bash
# Clone repository
git clone https://github.com/ghostoftheeleven/mpei-next.git
cd mpei-next

# Build debug APK
./gradlew assembleDebug

# Run unit tests (schedule parser, BARS 2FA, data mappers)
./gradlew :feature_schedule_impl:testDebugUnitTest :feature_bars_impl:testDebugUnitTest

# Assemble release APK
./gradlew assembleRelease --no-configuration-cache
```

> [!TIP]
> **Release Code Signing:**
> Signing keystores are **never committed** to the repository. For release distribution builds, configure the following environment variables or add them to your `~/.gradle/gradle.properties`:
> `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
> For continuous integration secrets, APK/AAB packaging, and fingerprint validation, see [docs/RELEASE.md](./docs/RELEASE.md).

### 💡 Workstation Optimization for 8 GB RAM (Windows)
1. **Gradle Daemon Memory:** `gradle.properties` sets a conservative JVM heap ceiling (`-Xmx2048m`) to prevent OutOfMemory thrashing when running Android Studio alongside the Gradle compilation daemon.
2. **Non-ASCII / Cyrillic Paths:** if your checkout path contains spaces or non-ASCII characters, create an NTFS Directory Junction to a short, clean path:
   ```cmd
   mklink /J C:\mpei-next "C:\Users\<Username>\Documents\Projects\mpei-next"
   ```
   and launch your IDE or command line from `C:\mpei-next`.

---

## 📂 Repository Layout

```text
mpei-next/
├── build-logic/                # Gradle build conventions and internal project plugins
├── gradle/                     # Version Catalog (libs.versions.toml) and Gradle Wrapper
├── docs/                       # Release procedures (RELEASE.md), iOS roadmap (IOS.md), archives
├── third_party/                # Vendored dependencies (local Maven repo, lib_bars)
├── statics/                    # Remote map marker definitions and dynamic BARS configs
├── modules/
│   ├── app/                    # Entry point, Application class, root navigation graph, SplashScreen
│   ├── feature/
│   │   ├── schedule/           # Timetable engine (ts.mpei.ru, Compose UI, Glance, WorkManager)
│   │   ├── bars/               # BARS portal integration (TOTP, MAX, Telegram, VK, Email 2FA)
│   │   ├── map/                # Offline Lefortovo campus map on OpenStreetMap
│   │   ├── dashboard/          # Student home feed, overview cards, and quick actions
│   │   ├── notes/              # Local personal notes attached to academic classes
│   │   ├── search/             # Global search across classrooms, groups, and professors
│   │   ├── app/settings/       # Preferences (theme selection, locale toggle, reminders)
│   │   └── onboarding/         # First-launch setup and initial group selection
│   ├── ui/
│   │   ├── theme/              # Design tokens, typography, and color schemes (including OLED Black)
│   │   ├── kit/                # Reusable UI Kit components (dialogs, segmented controls, switches)
│   │   ├── icons/              # Vector iconography (MpeixIcons)
│   │   └── splash/             # Custom animated splash overlay
│   └── lib/                    # Networking foundation, Room DB, MVI bridge, utility helpers
└── README.md
```

---

## 👥 Authors & Maintainers

* **Rasul Rustamov** ([@ghostoftheeleven](https://github.com/ghostoftheeleven)) — Project lead, architecture & development, NRU "MPEI" student
* **Johnny Wickky** ([@Johnny-Wickky](https://github.com/Johnny-Wickky)) — Co-author, NRU "MPEI" student, information security
* **Anton Kolomeytsev** ([@tonykolomeytsev](https://github.com/tonykolomeytsev)) — Creator of the original MpeiX project

---

## 🤝 Contributing

We welcome pull requests and feature suggestions from students and the broader open-source community:
1. **Fork** the repository.
2. Create your feature branch (`git checkout -b feature/awesome-addition`).
3. Commit your changes using Conventional Commits (`git commit -m 'feat: add awesome addition'`).
4. Push to your branch (`git push origin feature/awesome-addition`).
5. Open a **Pull Request** against `main`.

---

## 📄 License

This project is licensed under the **[MIT License](./LICENSE)**.

*Disclaimer: «МЭИ.Хаб» (MPEI.Hub) is an independent student-led open-source project and is not an official product of the National Research University "MPEI" administration. All registered trademarks and names belong to their respective holders.*
