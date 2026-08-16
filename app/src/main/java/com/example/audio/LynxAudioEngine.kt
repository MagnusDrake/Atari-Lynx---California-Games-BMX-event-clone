package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sin
import kotlin.random.Random

/**
 * Authentic 8-bit Sound Generator & Synthesizer for Lynx California Games BMX
 * Uses Android AudioTrack to produce classic pulse waves, triangle basslines,
 * noise drums, and chiptune melodies in real time without external audio files.
 */
class LynxAudioEngine {

    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var isRunning = false
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var musicEnabled: Boolean = true
    var sfxEnabled: Boolean = true
    var masterVolume: Float = 0.85f

    // Active sound effects queue
    private val activeSfx = ConcurrentLinkedQueue<SfxInstance>()

    // Musical note frequencies in Hz
    companion object {
        val NOTE_C3 = 130.81f
        val NOTE_D3 = 146.83f
        val NOTE_E3 = 164.81f
        val NOTE_F3 = 174.61f
        val NOTE_G3 = 196.00f
        val NOTE_A3 = 220.00f
        val NOTE_B3 = 246.94f
        val NOTE_C4 = 261.63f
        val NOTE_CS4 = 277.18f
        val NOTE_D4 = 293.66f
        val NOTE_DS4 = 311.13f
        val NOTE_E4 = 329.63f
        val NOTE_F4 = 349.23f
        val NOTE_FS4 = 369.99f
        val NOTE_G4 = 392.00f
        val NOTE_GS4 = 415.30f
        val NOTE_A4 = 440.00f
        val NOTE_AS4 = 466.16f
        val NOTE_B4 = 493.88f
        val NOTE_C5 = 523.25f
        val NOTE_D5 = 587.33f
        val NOTE_E5 = 659.25f
        val NOTE_G5 = 783.99f
        val NOTE_A5 = 880.00f
        val REST = 0.0f
    }

    // Classic California Games BMX Chiptune Pattern (BPM ~138, 16th notes)
    // Melody + Bassline + Percussion
    private val bmxMelody = floatArrayOf(
        NOTE_E4, REST, NOTE_G4, NOTE_A4,  REST, NOTE_A4, NOTE_B4, NOTE_D5,
        NOTE_B4, NOTE_A4, NOTE_G4, NOTE_E4, NOTE_D4, NOTE_E4, NOTE_G4, REST,
        NOTE_A4, REST, NOTE_C5, NOTE_D5,  REST, NOTE_D5, NOTE_E5, NOTE_G5,
        NOTE_E5, NOTE_D5, NOTE_B4, NOTE_G4, NOTE_A4, NOTE_B4, NOTE_A4, REST,
        NOTE_E4, NOTE_E4, NOTE_G4, NOTE_A4,  REST, NOTE_B4, NOTE_A4, NOTE_G4,
        NOTE_E4, REST, NOTE_D4, NOTE_E4,  NOTE_G4, NOTE_E4, NOTE_D4, NOTE_C4,
        NOTE_D4, NOTE_D4, NOTE_E4, NOTE_G4,  NOTE_A4, NOTE_B4, NOTE_D5, NOTE_E5,
        NOTE_D5, NOTE_B4, NOTE_A4, NOTE_G4,  NOTE_A4, REST, REST, REST
    )

    private val bmxBass = floatArrayOf(
        NOTE_E3, REST, NOTE_E3, REST,  NOTE_G3, REST, NOTE_A3, REST,
        NOTE_E3, REST, NOTE_E3, REST,  NOTE_D3, REST, NOTE_E3, REST,
        NOTE_A3, REST, NOTE_A3, REST,  NOTE_C4, REST, NOTE_D4, REST,
        NOTE_A3, REST, NOTE_A3, REST,  NOTE_G3, REST, NOTE_A3, REST,
        NOTE_C3, REST, NOTE_C3, REST,  NOTE_D3, REST, NOTE_D3, REST,
        NOTE_E3, REST, NOTE_E3, REST,  NOTE_B3, REST, NOTE_E3, REST,
        NOTE_D3, REST, NOTE_D3, REST,  NOTE_G3, REST, NOTE_B3, REST,
        NOTE_A3, REST, NOTE_A3, REST,  NOTE_E3, REST, REST, REST
    )

    fun start() {
        if (isRunning) return
        isRunning = true

        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 10)

            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            audioTrack?.play()

            synthJob = scope.launch {
                synthesizeAudioLoop()
            }
        } catch (e: Throwable) {
            // Graceful fallback for Robolectric or environments where AudioTrack is unavailable
            audioTrack = null
        }
    }

    fun stop() {
        isRunning = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private suspend fun synthesizeAudioLoop() {
        val chunkSize = 512
        val pcmBuffer = ShortArray(chunkSize)
        var phaseMelody = 0.0
        var phaseBass = 0.0
        var phaseArp = 0.0

        var noteStep = 0
        val samplesPer16th = (sampleRate * 60) / (138 * 4) // ~138 BPM
        var sampleCounterInStep = 0

        val random = Random(42)

        while (isRunning) {
            for (i in 0 until chunkSize) {
                sampleCounterInStep++
                if (sampleCounterInStep >= samplesPer16th) {
                    sampleCounterInStep = 0
                    noteStep = (noteStep + 1) % bmxMelody.size
                }

                var sampleVal = 0.0

                // 1. Music (Melody Pulse + Bass Triangle + Arpeggios)
                if (musicEnabled) {
                    val melodyFreq = bmxMelody[noteStep]
                    val bassFreq = bmxBass[noteStep]

                    // Pulse wave for melody (25% duty cycle - authentic 8-bit sound)
                    if (melodyFreq > 0) {
                        phaseMelody += (2.0 * Math.PI * melodyFreq) / sampleRate
                        if (phaseMelody > 2.0 * Math.PI) phaseMelody -= 2.0 * Math.PI
                        val pulse = if (phaseMelody < Math.PI * 0.5) 0.18 else -0.18
                        sampleVal += pulse
                    }

                    // Bass Triangle wave
                    if (bassFreq > 0) {
                        phaseBass += (2.0 * Math.PI * bassFreq) / sampleRate
                        if (phaseBass > 2.0 * Math.PI) phaseBass -= 2.0 * Math.PI
                        val tri = (2.0 / Math.PI) * Math.asin(sin(phaseBass)) * 0.22
                        sampleVal += tri
                    }

                    // Noise Hi-Hat / Snare on 16th steps
                    if (sampleCounterInStep < samplesPer16th / 4) {
                        val isSnare = (noteStep % 4 == 2)
                        val isKick = (noteStep % 4 == 0)
                        if (isSnare) {
                            val noise = (random.nextDouble() * 2.0 - 1.0) * 0.12 * (1.0 - sampleCounterInStep.toDouble() / (samplesPer16th / 4))
                            sampleVal += noise
                        } else if (isKick && sampleCounterInStep < samplesPer16th / 8) {
                            val kickFreq = 120.0 * (1.0 - sampleCounterInStep.toDouble() / (samplesPer16th / 8))
                            phaseArp += (2.0 * Math.PI * kickFreq) / sampleRate
                            sampleVal += sin(phaseArp) * 0.25
                        }
                    }
                }

                // 2. Active SFX mixing
                if (sfxEnabled && !activeSfx.isEmpty()) {
                    val iterator = activeSfx.iterator()
                    while (iterator.hasNext()) {
                        val sfx = iterator.next()
                        val sfxSample = sfx.nextSample(sampleRate)
                        if (sfx.isFinished()) {
                            iterator.remove()
                        } else {
                            sampleVal += sfxSample
                        }
                    }
                }

                // Clamp to 16-bit PCM
                val finalSample = (sampleVal * masterVolume * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                pcmBuffer[i] = finalSample
            }

            audioTrack?.write(pcmBuffer, 0, chunkSize)
        }
    }

    // ==========================================
    // SOUND EFFECTS (Authentic 8-Bit Lynx SFX)
    // ==========================================

    fun playPedalTick() {
        if (!sfxEnabled) return
        activeSfx.add(PedalTickSfx())
    }

    fun playJump() {
        if (!sfxEnabled) return
        activeSfx.add(JumpSfx())
    }

    fun playTrickSuccess(trickName: String) {
        if (!sfxEnabled) return
        activeSfx.add(TrickSuccessSfx(trickName))
    }

    fun playMudSplash() {
        if (!sfxEnabled) return
        activeSfx.add(MudSplashSfx())
    }

    fun playCrash() {
        if (!sfxEnabled) return
        activeSfx.add(CrashSfx())
    }

    fun playCountdownBeep(highPitch: Boolean) {
        if (!sfxEnabled) return
        activeSfx.add(BeepSfx(if (highPitch) 880f else 440f, 0.15f))
    }

    fun playFinishCheer() {
        if (!sfxEnabled) return
        activeSfx.add(VictoryFanfareSfx())
    }

    fun playButtonTap() {
        if (!sfxEnabled) return
        activeSfx.add(BeepSfx(587f, 0.04f))
    }

    // SFX Implementation classes
    interface SfxInstance {
        fun nextSample(sampleRate: Int): Double
        fun isFinished(): Boolean
    }

    private class PedalTickSfx : SfxInstance {
        private var sampleCount = 0
        private val totalSamples = 400
        private var phase = 0.0

        override fun nextSample(sampleRate: Int): Double {
            sampleCount++
            val freq = 450.0 + (totalSamples - sampleCount) * 2.0
            phase += (2.0 * Math.PI * freq) / sampleRate
            val decay = 1.0 - (sampleCount.toDouble() / totalSamples)
            return (if (sin(phase) > 0) 0.25 else -0.25) * decay
        }

        override fun isFinished() = sampleCount >= totalSamples
    }

    private class JumpSfx : SfxInstance {
        private var sampleCount = 0
        private val totalSamples = 3200
        private var phase = 0.0

        override fun nextSample(sampleRate: Int): Double {
            sampleCount++
            // Frequency slides up from 180Hz to 680Hz
            val progress = sampleCount.toDouble() / totalSamples
            val freq = 180.0 + progress * 500.0
            phase += (2.0 * Math.PI * freq) / sampleRate
            val decay = 1.0 - (progress * progress)
            val sq = if (sin(phase) > 0.1) 0.35 else -0.35
            return sq * decay
        }

        override fun isFinished() = sampleCount >= totalSamples
    }

    private class TrickSuccessSfx(val trickName: String) : SfxInstance {
        private var sampleCount = 0
        private val totalSamples = 7000
        private var phase = 0.0

        override fun nextSample(sampleRate: Int): Double {
            sampleCount++
            val segment = (sampleCount * 4) / totalSamples
            val freq = when (segment) {
                0 -> 523.25 // C5
                1 -> 659.25 // E5
                2 -> 783.99 // G5
                else -> 1046.50 // C6
            }
            phase += (2.0 * Math.PI * freq) / sampleRate
            val progress = sampleCount.toDouble() / totalSamples
            val decay = (1.0 - progress) * 0.4
            return (if (sin(phase) > 0) 0.3 else -0.3) * decay
        }

        override fun isFinished() = sampleCount >= totalSamples
    }

    private class MudSplashSfx : SfxInstance {
        private var sampleCount = 0
        private val totalSamples = 4500
        private val random = Random(123)

        override fun nextSample(sampleRate: Int): Double {
            sampleCount++
            val progress = sampleCount.toDouble() / totalSamples
            val noise = (random.nextDouble() * 2.0 - 1.0)
            val decay = (1.0 - progress) * 0.3
            return noise * decay
        }

        override fun isFinished() = sampleCount >= totalSamples
    }

    private class CrashSfx : SfxInstance {
        private var sampleCount = 0
        private val totalSamples = 12000
        private val random = Random(999)
        private var phase = 0.0

        override fun nextSample(sampleRate: Int): Double {
            sampleCount++
            val progress = sampleCount.toDouble() / totalSamples
            // Low pitch boom dropping to 40Hz + crunch noise
            val freq = 220.0 * (1.0 - progress) + 30.0
            phase += (2.0 * Math.PI * freq) / sampleRate
            val boom = sin(phase) * 0.4
            val noise = (random.nextDouble() * 2.0 - 1.0) * 0.4
            val decay = (1.0 - progress) * 0.5
            return (boom + noise) * decay
        }

        override fun isFinished() = sampleCount >= totalSamples
    }

    private class BeepSfx(private val freq: Float, durationSec: Float) : SfxInstance {
        private var sampleCount = 0
        private val totalSamples = (22050 * durationSec).toInt()
        private var phase = 0.0

        override fun nextSample(sampleRate: Int): Double {
            sampleCount++
            phase += (2.0 * Math.PI * freq) / sampleRate
            val decay = 1.0 - (sampleCount.toDouble() / totalSamples)
            return (if (sin(phase) > 0) 0.25 else -0.25) * decay
        }

        override fun isFinished() = sampleCount >= totalSamples
    }

    private class VictoryFanfareSfx : SfxInstance {
        private var sampleCount = 0
        private val totalSamples = 22050 * 2
        private var phase = 0.0

        override fun nextSample(sampleRate: Int): Double {
            sampleCount++
            val sec = sampleCount.toDouble() / 22050.0
            val freq = when {
                sec < 0.2 -> 523.25 // C5
                sec < 0.4 -> 659.25 // E5
                sec < 0.6 -> 783.99 // G5
                sec < 0.9 -> 1046.50 // C6
                sec < 1.1 -> 880.00 // A5
                sec < 1.3 -> 987.77 // B5
                else -> 1046.50 // C6 hold
            }
            phase += (2.0 * Math.PI * freq) / sampleRate
            val sq = if (sin(phase) > 0.1) 0.35 else -0.35
            return sq * 0.4
        }

        override fun isFinished() = sampleCount >= totalSamples
    }
}
