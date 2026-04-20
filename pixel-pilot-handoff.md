# Pixel Pilot — Claude Code Handoff

**Last updated:** April 20, 2026
**Author:** Nate Robertson
**Status:** Pre-scaffolding, ready to build

---

## The One-Liner

Pixel Pilot is an Android customization suite for GrapheneOS users, launching as the inaugural project of Arcus Foundry Labs. Tagline: **"Customization without compromise."**

---

## Why This Exists

GrapheneOS users have limited options for customizing their phones. Existing tools are either abandoned, non-FOSS, or feature-limited. Pixel Pilot fills that gap with a modern, actively maintained, privacy-respecting customization app.

Specifically, the path to this project:

1. User wanted to set a GIF as wallpaper on GrapheneOS. Stock Android doesn't support this.
2. MP4 as wallpaper: also needs a third-party app.
3. Evaluated options (Alynx Live Wallpaper — dormant since 2020, UndeadWallpaper — active but no clock/theme control, Paperize — only plays animated preview but flattens on apply, no video support).
4. Concluded off-the-shelf options are inadequate. Rolling our own is tractable given 300-500 lines of Kotlin.
5. Scope expanded to include color theme control since Material You picks up weird colors from video wallpapers (Zombillie → red theme incident).

---

## Brand Positioning

**Parent:** Arcus Foundry Labs (new sub-brand being launched with Pixel Pilot as flagship)

**Labs location on the web:** `arcusfoundry.com/labs/` — path, not subdomain
- Reason: reuse existing site infrastructure (Cloudflare Worker nav/footer, design system, SEO authority)
- Consistent with existing pattern (`/sparkforge/`, `/sparkforge/starter`)
- Sibling to `/tools/` which lives in Claude Code context

**Pixel Pilot project page:** `arcusfoundry.com/labs/pixel-pilot`

**Relationship to other Arcus Foundry properties:**
- `/sparkforge/` — the paid SaaS product
- `/tools/` — lead-gen utilities for SMB prospects (managed in Claude Code)
- `/labs/` — FOSS developer-facing experiments (this, Pixel Pilot being the first)

---

## Visual Identity

Inherits Arcus Foundry design language:
- Primary: `#c3d95e` (lime green)
- Background: `#23270f` (dark olive)
- Same typography, same component patterns as the main site
- Global nav via existing Worker: `afmenufooter.royal-silence-7f1a.workers.dev`

In-app default theme is AF lime/olive. Users can change via the color picker feature (see Features below).

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Video:** Media3 ExoPlayer (not legacy ExoPlayer 2)
- **Min SDK:** 33 (Android 13)
- **Target SDK:** 36 (current)
- **Build:** Gradle with Kotlin DSL
- **No Google Play Services dependencies** (GrapheneOS-friendly)
- **No Firebase, no analytics, no tracking**
- **License:** MIT
- **Package:** `com.arcusfoundry.labs.pixelpilot`
- **Repo:** `github.com/Arcusfoundry/pixel-pilot` (under the existing Arcusfoundry org; tagged with `labs` GitHub topic for grouping alongside future Labs repos). Org rename to `arcus-foundry` is a separate future initiative.

---

## Distribution

- **Primary:** GitHub Releases (signed APKs, Obtainium-friendly)
- **Secondary:** F-Droid submission post-launch
- **Tertiary:** Google Play Store — same APK, free, with optional "Support Development" in-app purchase tiers ($3/$5/$10/$25)

**Not doing:**
- Paid-only Play Store version
- Feature gates based on distribution channel
- Dual-licensing schemes

**Rationale:** FOSS reputation matters more than modest Play Store revenue. Pixel Pilot is a calling-card project for Arcus Foundry Labs, not a revenue center. Tip jar model respects the GrapheneOS community.

---

## Feature Scope

### v1 (Launch)

**Core wallpaper engine:**
- MP4 video as live wallpaper
- GIF as live wallpaper
- Static image with smart cropper
- Mute audio by default
- Loop seamlessly
- Pause when screen off (battery)
- Center-crop scaling with user adjustment (scale, position, zoom)

**Color theme control:**
- Color wheel picker (HSV)
- Hex input field
- Saved palette library (default: AF lime/olive pre-loaded)
- Apply via wallpaper-priming technique (generate solid-color or noise-textured image to drive Material You extraction to the chosen color)
- Eyedropper from current wallpaper

**Scaffolding:**
- Main activity with Compose UI
- WallpaperService for live wallpaper rendering
- GL renderer for video crop (or accept stretch for v1)
- SharedPreferences for state persistence
- Wallpaper picker integration (system intent)

### v2 (Post-launch, if traction)

- Per-screen wallpapers (home vs lock different)
- Time-based video/image rotation
- Battery-aware playback profiles
- Privileged mode via Shizuku for direct theme overlay control (RRO)
- Wallpaper schedule with sunrise/sunset
- Lock screen clock positioning hacks (if feasible without root)

### v3 (Speculative)

- Icon pack support
- Status bar customization
- Font override
- AOD customization
- Per-app wallpaper triggers
- Cross-device sync (optional paid tier, self-hosted or Tailscale-based)

---

## Key Technical Decisions

### Wallpaper rendering
Use `WallpaperService.Engine` with `SurfaceHolder` → Media3 ExoPlayer via `setVideoSurface()`. Loop enabled, volume zeroed via `setVolume(0f)`. For GIF, use Coil's GIF decoder rendered to the same surface.

### Center-crop math
For v1, can either use a custom GL renderer (like Alynx did) OR accept Media3's built-in `VideoScalingMode.SCALE_TO_FIT_WITH_CROPPING` and calculate a matrix. Favor Media3 built-in for simplicity; roll our own GL only if scaling quality is inadequate.

### Theme color injection (the clever bit)
Android 12+ Material You extracts colors from wallpaper. Direct overlay control (`CHANGE_OVERLAY_PACKAGES`) is privileged-only. Workaround for v1: generate a small (e.g., 2x2 or solid-gradient) bitmap primed with the user's chosen color and set it as wallpaper via `WallpaperManager.setBitmap()`. Android's Monet extraction produces a theme built around that color.

For v2, support Shizuku for privileged theme control as an opt-in.

### SAF for file access
Use Storage Access Framework (content URIs via `ACTION_OPEN_DOCUMENT`) instead of `READ_EXTERNAL_STORAGE`. Persist URI permissions via `takePersistableUriPermission()`. Zero sensitive permissions in v1 manifest beyond `BIND_WALLPAPER`.

### Release automation
GitHub Actions workflow that builds signed release APK on version tag push. Signing key stored as GitHub secret. Output attached to GitHub Release so Obtainium picks it up automatically.

---

## Naming Convention

- **App name:** Pixel Pilot
- **Tagline:** Customization without compromise.
- **Supporting language options:** "Take the stick on your Pixel." / "Fly your Pixel your way." (optional, use sparingly)
- **Tone:** Confident, playful, technical-adjacent. Not corporate. Not cutesy.
- **Icon concept:** Paper airplane + pixel grid motif OR pilot wings with pixelated edge (to be designed — not scope of Claude Code handoff, will be handled separately)

---

## Writing Style (Nate's Preferences)

**Apply to:** README, in-app copy, release notes, commit messages where appropriate.

- No em dashes
- No AI-sounding jargon
- No filler words like "just"
- No "not X, but Y" constructions
- Direct and confident
- Technical precision over marketing fluff

---

## What Claude Code Should Build First

### Phase 1: Project scaffolding
1. Initialize Gradle project with Kotlin DSL, Compose, Media3 dependencies
2. Configure `build.gradle.kts` for min SDK 33, target SDK 36
3. Create package structure: `com.arcusfoundry.labs.pixelpilot`
4. Set up signing config (debug for now, release key later)
5. Basic `AndroidManifest.xml` with wallpaper service declaration
6. `wallpaper_info.xml` resource
7. Launcher icon placeholder
8. AF color palette in `colors.xml`
9. Material 3 theme with AF lime/olive defaults

### Phase 2: Core wallpaper service
1. `VideoWallpaperService` extending `WallpaperService`
2. `Engine` subclass with ExoPlayer integration
3. Video surface management
4. Loop + mute configuration
5. Visibility lifecycle (pause on screen off)

### Phase 3: Main activity UI
1. Compose-based `MainActivity`
2. Video picker via SAF
3. Preview area
4. "Set as Wallpaper" button → system wallpaper picker intent
5. Recent videos list (up to 5, persisted to SharedPreferences)

### Phase 4: Color control
1. Color wheel Compose component (roll our own with Canvas, ~150 LOC)
2. Hex input field with validation
3. Saved palettes screen (default: "Arcus Lime" `#c3d95e` and "Dark Olive" `#23270f`)
4. "Apply as theme" button that generates primed bitmap and sets as wallpaper

### Phase 5: GIF + static image support
1. Coil-based GIF rendering on wallpaper surface
2. Static image with in-app cropper

### Phase 6: Release automation
1. GitHub Actions workflow
2. Signed APK build on tag push
3. Release notes template

### Phase 7: Labs website
1. `/labs/index.html` — Labs landing with portfolio grid
2. `/labs/pixel-pilot/index.html` — project page with screenshots, install instructions, GitHub link
3. Both reuse existing AF nav/footer Workers
4. Same design system as rest of arcusfoundry.com

---

## Open Questions for Nate

1. Do we want an onboarding flow on first launch (welcome screen, permission explanations) or drop straight to the picker?
2. Should "Support Development" tip jar IAP be in v1 or added post-launch?
3. Icon design — handled by Nate separately, or does Claude Code stub with a placeholder?
4. Preferred GitHub org display name: "Arcus Foundry Labs" or "AF Labs"?
5. For the primed-wallpaper theme hack, is there concern about users seeing a "wallpaper changed" surprise? (Potential UX: preview a swatch on the lock screen before applying system-wide.)

---

## Out of Scope for Claude Code

- Icon/logo design (handled separately)
- Marketing copy for the Labs landing page beyond functional structure
- Play Store listing copy, screenshots, ASO
- F-Droid submission paperwork
- Podcast episode production tie-ins
- Promotional tweets/social posts

---

## References

- Media3 ExoPlayer docs: https://developer.android.com/media/media3
- WallpaperService API: https://developer.android.com/reference/android/service/wallpaper/WallpaperService
- Material You dynamic color: https://developer.android.com/develop/ui/views/theming/dynamic-colors
- Shizuku (for v2 privileged mode): https://shizuku.rikka.app/
- GrapheneOS Obtainium integration: https://obtainium.imranr.dev/
- UndeadWallpaper (reference implementation to study): https://github.com/maocide/UndeadWallpaper
- Alynx Live Wallpaper (older reference, GL cropping approach): https://github.com/AlynxZhou/alynx-live-wallpaper

---

## Success Criteria for v1

- Installs cleanly on GrapheneOS via Obtainium
- Sets an MP4 as a live wallpaper that loops smoothly without audio
- User can pick a custom color via wheel or hex, apply it, and see Material You pick it up
- Default AF lime/olive theme loads on first install
- No network calls, no analytics, no Google Play Services
- Ships under MIT on GitHub org `arcus-foundry-labs`
- Labs landing page at `arcusfoundry.com/labs/pixel-pilot` is live at or shortly after app launch

---

*This handoff captures the state of planning as of April 20, 2026. Subsequent development decisions should be logged in the repo itself via README updates, commit messages, and issue tracker.*
