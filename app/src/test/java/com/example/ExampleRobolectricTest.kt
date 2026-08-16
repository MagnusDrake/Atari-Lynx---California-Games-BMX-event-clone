package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.LynxAudioEngine
import com.example.engine.BmxPhysicsEngine
import com.example.model.GamePhase
import com.example.model.RiderPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lynx BMX", appName)
    }

    @Test
    fun `bmx physics engine initialization and jump mechanics`() {
        val audio = LynxAudioEngine()
        val engine = BmxPhysicsEngine(audio)

        assertEquals(engine.startX, engine.posX, 0.01f)
        assertTrue(engine.isOnGround)
        assertEquals(0, engine.score)

        // Simulate pedal acceleration
        engine.gamePhase = GamePhase.PLAYING
        engine.update(
            dt = 0.5f,
            isPedalPressed = true,
            isJumpPressed = false,
            isLeanBack = false,
            isLeanForward = false,
            isTrickUp = false,
            isTrickDown = false
        )

        assertTrue(engine.velX > 0f)
        assertEquals(RiderPose.PEDALING, engine.riderPose)

        // Simulate Jump in 3D (velZ > 0f)
        engine.update(
            dt = 0.1f,
            isPedalPressed = false,
            isJumpPressed = true,
            isLeanBack = false,
            isLeanForward = false,
            isTrickUp = false,
            isTrickDown = false
        )

        assertTrue(!engine.isOnGround)
        assertTrue(engine.velZ > 0f)
    }

    @Test
    fun `bmx score and trick accumulation`() {
        val audio = LynxAudioEngine()
        val engine = BmxPhysicsEngine(audio)
        engine.gamePhase = GamePhase.PLAYING

        engine.addScore(500, "Backflip! +500")
        assertEquals(500, engine.score)
        assertEquals("Backflip! +500", engine.currentTrickBanner)
    }
}

