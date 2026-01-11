package com.androos.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

class AudioEffectsController(private val audioSessionId: Int) {
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private var masterGain: Int = 0
    private val eqBands = FloatArray(5) { 0f }
    private var bassBoostStrength: Int = 0

    init {
        initializeEffects()
    }

    private fun initializeEffects() {
        try {
            // Inicializar LoudnessEnhancer
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            loudnessEnhancer?.enabled = true
            
            // Inicializar Equalizer
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
            
            // Inicializar BassBoost
            bassBoost = BassBoost(0, audioSessionId)
            bassBoost?.enabled = true
            
            // Configurar valores por defecto
            resetToDefaults()
            
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Error initializing audio effects", e)
        }
    }

    fun setMasterGain(gain: Int) {
        masterGain = gain.coerceIn(0, 2000)
        loudnessEnhancer?.setTargetGain(masterGain)
    }

    fun getMasterGain(): Int = masterGain

    fun setEqualizerBand(band: Int, level: Float) {
        if (band in 0..4) {
            eqBands[band] = level
            val bandLevel = (level * 1000).toInt() // Convertir a milibels
            equalizer?.setBandLevel(band.toShort(), bandLevel.toShort())
        }
    }

    fun getEqualizerBand(band: Int): Float = eqBands.getOrElse(band) { 0f }

    fun setBassBoost(strength: Int) {
        bassBoostStrength = strength.coerceIn(0, 1000)
        bassBoost?.setStrength(bassBoostStrength.toShort())
    }

    fun getBassBoost(): Int = bassBoostStrength

    fun resetToDefaults() {
        // Configurar ganancia a nivel medio
        setMasterGain(1000)
        
        // Resetear equalizador a plano
        for (i in 0..4) {
            setEqualizerBand(i, 0f)
        }
        
        // Resetear bass boost
        setBassBoost(0)
    }

    fun release() {
        loudnessEnhancer?.release()
        equalizer?.release()
        bassBoost?.release()
    }

    companion object {
        val EQ_FREQUENCIES = arrayOf(
            "60 Hz",  // Bajo
            "230 Hz", // Bajo-Medio
            "910 Hz", // Medio
            "3.6 kHz", // Medio-Alto
            "14 kHz"  // Alto
        )
    }
}
