package com.arcusfoundry.labs.pixelpilot.render.animations

import com.arcusfoundry.labs.pixelpilot.render.Animation

object AnimationRegistry {

    val all: List<Animation> = listOf(
        GraphPaperAnimation,
        ParticlesAnimation,
        GradientWavesAnimation,
        BokehAnimation,
        FloatingOrbsAnimation,
        MatrixRainAnimation,
        GridPulseAnimation,
        CircuitsAnimation,
        CyberRainAnimation,
        AuroraAnimation,
        StarfieldAnimation,
        FirefliesAnimation,
        SoundWavesAnimation,
        NeonPulseAnimation,
        SmokeAnimation,
        FlyingToastersAnimation,
        BouncingDvdAnimation,
        Pipes2DAnimation,
        Pipes3DAnimation,
        MystifyAnimation,
        BezierAnimation
    )

    val byId: Map<String, Animation> = all.associateBy { it.id }

    val byCategory: Map<String, List<Animation>> = all.groupBy { it.category }

    fun get(id: String): Animation? = byId[id]

    val default: Animation = MatrixRainAnimation
}
