# Minimal Invoice

A private-by-default, offline-first native Android invoice maker built with Kotlin, Jetpack Compose, Material 3 and Room.

## Features
- Local invoice storage (Room); no account, backend, internet permission or cloud sync.
- Invoice list with outstanding balance and paid/unpaid status.
- Create invoices with client, editable line items, quantity, rate, tax, due date and notes.
- Invoice preview, mark paid/unpaid, delete and share as a PDF using Android PdfDocument.
- Business name, currency symbol and default tax rate settings.
- System light/dark theme.
- Android 8.0 (API 26)+.

## GitHub Actions builds
Every push to `main` and manual workflow dispatch builds:
- Debug APK: `Minimal-Invoice.apk`
- Release AAB: `Minimal-Invoice.aab`

Open **Actions → Build Minimal Invoice → latest run → Artifacts** and download `minimal-invoice-apk`. The artifact contains both files.

Pushing a tag beginning with `v` (for example `v1.0.0`) also creates a GitHub Release with both files attached.

## Install
Extract the downloaded artifact ZIP on Android, tap `Minimal-Invoice.apk`, allow installation from that file manager/browser if prompted, then install. The AAB is intended for Play Console or bundle-based distribution; it is not directly installable.

## Local build
Use JDK 17 and Gradle 8.7:
```bash
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug bundleRelease
```

## Toolchain
- Android Gradle Plugin 8.6.1 + Gradle 8.7 + JDK 17.
- Kotlin 2.0.20 with matching Compose compiler plugin.
- Room 2.6.1 via KSP 2.0.20-1.0.25.
- Target SDK 34, minimum SDK 26.

The GitHub workflow generates the official Gradle Wrapper using the pinned Gradle 8.7 installation before invoking `./gradlew`.
