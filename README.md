# Pixel Pilot

Customization without compromise.

Pixel Pilot is an Android customization app for GrapheneOS users who want control over what's on their screen. Pick from a library of 21 procedurally-generated animated wallpapers, bring your own MP4, or paste a YouTube URL to download and use locally. Apply color tint, speed, size, dim, or rainbow cycling across everything uniformly.

## Requirements

- Android 13 (API 33) or newer
- Works on GrapheneOS. Works on stock Android.

## Install

**Via [Obtainium](https://obtainium.imranr.dev/):** Point at this repository. Obtainium pulls signed APKs from GitHub Releases.

**Via GitHub Releases:** Download the latest signed APK from the Releases tab and install directly.

**Via F-Droid:** Coming after initial release.

## Features

- 21 procedurally-generated animated wallpapers across Classics, Abstract, Tech, Nature, and Energy categories
- User-provided MP4 wallpapers via the system file picker
- YouTube download-to-local: paste a URL, the app fetches the video to local storage, then plays it offline from there. No ongoing network traffic.
- Unified controls: speed, size, dim, color tint (none / static / rainbow cycling) apply across every source
- Material You color theme integration (experimental)
- No Google Play Services. No Firebase. No analytics. No tracking.

## Build

Builds happen in GitHub Actions. Push to `main` and a debug APK is produced as a workflow artifact. Local builds require Android Studio or a full Android SDK toolchain.

## License

MIT. See [LICENSE](LICENSE).

## Status

Early development. APIs, features, and defaults may change between releases. Provided as-is under the Arcus Foundry Labs ethos: built because nothing better existed, no support SLA, PRs welcome.

Part of [Arcus Foundry Labs](https://arcusfoundry.com/labs).
