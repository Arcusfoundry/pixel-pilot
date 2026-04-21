# Scene Settings & Gear Button — Port Handoff

**Source:** SparkforgeOS (reverted in v2.1.14 — belongs here instead).
**Goal:** Let each animation declare its own config knobs (e.g. 3D Text's word). A gear icon appears on any picker tile whose animation has settings; tapping it opens a contextual panel where the user edits that animation's parameters.

## Why

Global `RenderParams` (speed/scale/dim/tint) aren't enough for animations with unique knobs. Per-animation settings are orthogonal to the global ones — they belong *to* the animation, not *across* it. 3D Text's word is the motivating example: hard-coded is wrong, global doesn't fit.

## Shape of the feature

1. Each `Animation` optionally exposes a `settings` schema: a list of typed fields (text, int-range, color, enum).
2. `AnimationPicker` shows a small gear icon overlaid on any tile whose animation has `settings.isNotEmpty()`. The rest of the tile UI is unchanged.
3. Tapping the gear:
   - If that animation isn't the current one, selects it first.
   - Then opens a settings sheet/panel scoped to that animation.
4. Values are stored in `WallpaperPreferences` under a keyspace like `scene:<animationId>:<key>`.
5. On change: persist, re-initialize the animation with the new values so the preview/wallpaper reflects the edit.

## SparkforgeOS reference (JS — for shape only, don't port line-for-line)

Three sites carried the feature:

- `backgrounds.js` — `3d-text` entry had a `settings` field: `{ word: { type: 'text', label: 'Text', default: 'SparkforgeOS', maxLength: 40 } }`
- `screensavers.js` — `3d-text.init` received a `cfg` param: `init(w, h, cfg)`; read `cfg.word` (fallback to default), measured text, sized offscreen texture to fit. Draw uses that texture every frame.
- `hub.html` — state (`sceneSettings` map + `sceneSettingsOpen` bool), helpers (`getSceneConfig`, `setSceneSetting`, `renderSceneSettings`, `toggleSceneSettings`), CSS for the gear button (`.bg-tile-gear` — absolute positioned, 26×26 circular, only shown on `.has-settings` tiles) and the slide-up panel (`.scene-settings` — max-height + opacity transitions). Gear onclick called `toggleSceneSettings(bgId)`; if that bg wasn't active, selected + opened in one motion.

All of that is reverted from SparkforgeOS as of v2.1.14 — nothing to pull.

## Port plan (Kotlin / Compose)

### 1. Extend the Animation interface

```kotlin
// render/Animation.kt
interface Animation {
    val id: String
    val displayName: String
    val category: String
    val defaultBackground: Int
    val legacy: Boolean get() = false

    // NEW — animations with per-scene knobs declare their schema here.
    // Default empty list means "no settings, no gear on the tile."
    val settings: List<SettingSpec> get() = emptyList()

    fun initialize(
        width: Int, height: Int, scale: Float,
        config: SceneConfig = SceneConfig.EMPTY  // NEW — animation-scoped values
    ): Any

    fun draw(
        canvas: Canvas, width: Int, height: Int, state: Any,
        timeMs: Long, speed: Float, scale: Float, tintColor: Int?
    )
}
```

### 2. Setting schema types

```kotlin
// render/SettingSpec.kt
sealed class SettingSpec {
    abstract val key: String
    abstract val label: String

    data class Text(
        override val key: String,
        override val label: String,
        val default: String,
        val maxLength: Int = 60
    ) : SettingSpec()

    data class IntRange(
        override val key: String, override val label: String,
        val default: Int, val min: Int, val max: Int, val step: Int = 1
    ) : SettingSpec()

    data class Color(
        override val key: String, override val label: String,
        val default: Int
    ) : SettingSpec()

    data class Choice(
        override val key: String, override val label: String,
        val default: String, val options: List<Pair<String, String>>  // id to label
    ) : SettingSpec()
}

// Value bag passed to initialize(). Animations read with defaults folded in.
class SceneConfig(private val values: Map<String, Any?>) {
    fun string(key: String, default: String): String =
        (values[key] as? String)?.takeIf { it.isNotEmpty() } ?: default
    fun int(key: String, default: Int): Int = (values[key] as? Int) ?: default
    fun color(key: String, default: Int): Int = (values[key] as? Int) ?: default

    companion object { val EMPTY = SceneConfig(emptyMap()) }
}
```

### 3. Preferences — per-animation scope

Add to `WallpaperPreferences`:

```kotlin
// Scene-scoped keys are namespaced: "scene:<animationId>:<settingKey>".
fun sceneConfig(animationId: String, specs: List<SettingSpec>): SceneConfig {
    val map = mutableMapOf<String, Any?>()
    for (spec in specs) {
        val k = "scene:$animationId:${spec.key}"
        map[spec.key] = when (spec) {
            is SettingSpec.Text -> prefs.getString(k, spec.default)
            is SettingSpec.IntRange -> prefs.getInt(k, spec.default)
            is SettingSpec.Color -> prefs.getInt(k, spec.default)
            is SettingSpec.Choice -> prefs.getString(k, spec.default)
        }
    }
    return SceneConfig(map)
}

fun setSceneValue(animationId: String, key: String, value: Any) {
    val k = "scene:$animationId:$key"
    prefs.edit {
        when (value) {
            is String -> putString(k, value)
            is Int -> putInt(k, value)
            is Float -> putFloat(k, value)
            is Boolean -> putBoolean(k, value)
        }
    }
}
```

Add `"scene:"` to the change-listener sensitivity list (or handle with a dedicated listener that re-initializes only the affected animation).

### 4. UI — gear button + sheet

In `AnimationPicker`:

- On each card whose `animation.settings.isNotEmpty()`, overlay a `FilledIconButton` (top-right of the thumbnail, ~28.dp). Pass click through with `pointerInput` to stop the card's main click handler from firing.
- On click: set `openSettingsFor` state to the animation's id (in `WallpaperViewModel`). If that id isn't currently selected, also call `selectAnimation(id)` first.

In `MainScreen` (or a new component `SceneSettingsSheet`):

- `ModalBottomSheet` bound to `openSettingsFor != null`. Title: `"${animation.displayName} Settings"`.
- Render one field per `SettingSpec`: `OutlinedTextField` for Text, `Slider` for IntRange, `ColorWheel` (already exists!) for Color, `SingleChoiceSegmentedButtonRow` for Choice.
- On commit (IME done / slider release / color picked): `viewModel.setSceneValue(animationId, key, value)` → persists and triggers re-initialize on the renderer.

### 5. Hooking re-init

`ProceduralRenderer` (or whatever orchestrates animation lifecycle) needs to call `animation.initialize(w, h, scale, prefs.sceneConfig(animation.id, animation.settings))` whenever:

- The active animation changes.
- A scene-scoped pref for the active animation changes (SharedPreferences listener, filter key by `"scene:$activeId:"`).

Everything else (speed/scale/dim/tint) already flows through per-frame draw args and doesn't require re-init.

## Suggested first target

3D Text is the motivating case, but it doesn't exist in Pixel Pilot yet (it lives in SparkforgeOS per the split). Two options:

1. **Port 3D Text to Pixel Pilot too** (the JS lives in SparkforgeOS's `screensavers.js` — re-implement using Android `Canvas.drawTextOnPath` + Matrix-based 3D rotation, or pre-render to a `Bitmap` and use `Canvas.drawBitmapMesh` for the 2-triangle affine warp). Then the first real user of scene settings is the word field.
2. **Pick an existing animation** — e.g., `BouncingDvdAnimation` with a configurable word (currently "DVD"), or `MatrixRainAnimation` with a configurable charset. Gets the infrastructure shipped against a real knob without a new animation port.

Option 2 is the cheaper first-ship. Option 1 matches the original arc. Both land the system the same way.

## Things NOT to port

- The SparkforgeOS DOM approach (innerHTML strings, classList toggling). Compose state is cleaner — use `remember` + a `SceneSettingsSheet` composable, not imperative DOM.
- The `max-height` slide-up CSS trick. `ModalBottomSheet` does the slide natively.
- The `escapeAttr` helper. Compose text doesn't need HTML escaping.

## Acceptance

- [ ] `Animation` interface gains optional `settings` + `SceneConfig` parameter on `initialize`.
- [ ] At least one animation uses it (3D Text ported OR an existing one retrofitted).
- [ ] Gear icon renders on picker tiles only when the animation has settings.
- [ ] Tapping gear on a non-selected tile selects + opens the sheet.
- [ ] Editing a value in the sheet persists and re-initializes the animation (visible change in preview).
- [ ] Relaunching the app restores the user's edited value.
