package com.androos.player
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.BassBoost
class AudioEffectsController(sessionId: Int) {
    private val enhancer = LoudnessEnhancer(sessionId)
    private val bass = BassBoost(0, sessionId)
    fun setGain(gain: Int) { enhancer.setTargetGain(gain); enhancer.enabled = true }
    fun setBass(s: Short) { bass.setStrength(s); bass.enabled = true }
}
