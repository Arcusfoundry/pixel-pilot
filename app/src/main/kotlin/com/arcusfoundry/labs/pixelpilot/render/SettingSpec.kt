package com.arcusfoundry.labs.pixelpilot.render

/**
 * Per-animation setting schema. Animations declare their own config knobs
 * via a list of SettingSpec. Distinct from the global RenderParams, which
 * applies across all animations.
 */
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
        override val key: String,
        override val label: String,
        val default: Int,
        val min: Int,
        val max: Int,
        val step: Int = 1
    ) : SettingSpec()

    data class Color(
        override val key: String,
        override val label: String,
        val default: Int
    ) : SettingSpec()

    data class Choice(
        override val key: String,
        override val label: String,
        val default: String,
        val options: List<Pair<String, String>>
    ) : SettingSpec()
}

/**
 * Value bag passed to Animation.initialize(). Reads tolerate missing keys
 * and return the caller-supplied default so animations can always fall back.
 */
class SceneConfig(private val values: Map<String, Any?>) {
    fun string(key: String, default: String): String =
        (values[key] as? String)?.takeIf { it.isNotEmpty() } ?: default

    fun int(key: String, default: Int): Int = (values[key] as? Int) ?: default

    fun color(key: String, default: Int): Int = (values[key] as? Int) ?: default

    fun choice(key: String, default: String): String =
        (values[key] as? String)?.takeIf { it.isNotEmpty() } ?: default

    companion object {
        val EMPTY = SceneConfig(emptyMap())
    }
}
