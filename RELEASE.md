# Forge OS — Release Guide

This document describes how to produce a signed release build of Forge OS:
via GitHub Actions (the current distribution path: signed APKs attached to
GitHub Releases) or locally. Play Store listing material is kept under
`fastlane/` for when it is needed.

---

## 1. One-time setup

### 1.0 CI releases (the normal path)

Distributed APKs are built by `.github/workflows/release.yml` and must be
signed with the SAME key every time. If the key changes, Android refuses
to update an existing install (the "must uninstall first" problem).

One-time setup (already done on the dev machine - see
`RELEASE_SIGNING_SETUP.md` in the repo root; it is gitignored and holds
the passwords):

1. A release keystore (`forge-release.jks`, alias `forge`) was generated.
2. These GitHub repository secrets were added (Settings -> Secrets and
   variables -> Actions -> New repository secret):
   - `FORGE_KEYSTORE_BASE64` - contents of `forge-release.jks.base64.txt`
   - `FORGE_KEYSTORE_PASSWORD` - from `RELEASE_SIGNING_SETUP.md`
   - `FORGE_KEY_ALIAS` - `forge`
3. To cut a release: `git tag v1.0.1 && git push origin v1.0.1`. The
   workflow builds a signed `ForgeOS-<version>.apk`, verifies the
   signature, and attaches it to a GitHub Release. `versionCode` is the
   workflow run number (always increases); `versionName` comes from the
   tag.

Keep the keystore and its password safe forever, never commit them, and
never install debug builds over release installs.

### 1.1 Create a release keystore

```bash
keytool -genkey -v \
  -keystore forge-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias forge
```

Keep `forge-release.jks` and the passwords **off** version control.

### 1.2 Configure signing

Create `app/keystore.properties` (gitignored):

```
storeFile=../forge-release.jks
storePassword=your-store-password
keyAlias=forge
keyPassword=your-key-password
```

The provided `app/build.gradle` already reads this file and applies it as the
`release` signing config when present.

---

## 2. Build a release AAB

```bash
./gradlew clean
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

For an installable APK (sideload / testing):

```bash
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

---

## 3. ProGuard / R8

Release builds run R8 with `app/proguard-rules.pro`. Forge ships with rules for
Retrofit, Gson, Room, Hilt, Chaquopy, Kotlinx-Serialization, and our own
`@Serializable` data classes. If you add a new third-party library, add
`-keep` rules here and re-test the release build before shipping.

---

## 4. Play Store listing

Listing copy lives under `fastlane/metadata/android/en-US/`:

```
title.txt              — store title (≤ 30 chars)
short_description.txt  — short description (≤ 80 chars)
full_description.txt   — long description
```

Required graphics (produce these from the in-app theme):

| Asset                  | Size           | Notes                              |
|------------------------|----------------|------------------------------------|
| Hi-res icon            | 512×512 PNG    | No alpha                           |
| Feature graphic        | 1024×500 PNG   | Used at the top of the listing     |
| Phone screenshots ×4+  | 1080×1920 PNG  | At least 2 required                |

You can drop these into `fastlane/metadata/android/en-US/images/` and use
`fastlane supply` to push the listing.

---

## 5. Release checklist

- [ ] Version handled (CI: automatic from tag + run number; local builds: bump `versionCode`/`versionName` in `app/build.gradle`)
- [ ] All `// TODO(release)` comments resolved
- [ ] Crash-free on a fresh install (no API key) → onboarding flow shown
- [ ] Crash-free with API key set → chat works
- [ ] Heartbeat, cron, plugins, sub-agents all green in Status screen
- [ ] ProGuard build does not strip `@Serializable` data classes
- [ ] `app-release.aab` is < 100 MB
- [ ] Internal-track upload tested on a real device before promoting
