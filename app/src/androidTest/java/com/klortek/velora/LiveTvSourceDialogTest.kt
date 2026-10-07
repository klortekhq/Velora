package com.klortek.velora

import android.view.KeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.klortek.velora.jellyfin.MediaSource
import com.klortek.velora.jellyfin.JellyfinConfig
import com.klortek.velora.livetv.LiveTvChannel
import com.klortek.velora.livetv.LiveTvClient
import com.klortek.velora.livetv.LiveTvChannelGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Executes actual pointer/remote events against the production source picker. */
class LiveTvSourceDialogTest {
    @get:Rule val compose = createComposeRule()

    private fun group(count: Int) = LiveTvChannelGroup("group", (1..count).map { index ->
        LiveTvChannel("channel", "Canal de prueba", MediaSources = listOf(
            MediaSource(Id = "source-$index", Name = "IPTV $index")
        ))
    })

    @Test fun touchOnChannelRowOpensSourcePicker() = checkRowTouch(useSourceButton = false)

    @Test fun touchOnSourceCountOpensSourcePicker() = checkRowTouch(useSourceButton = true)

    private fun checkRowTouch(useSourceButton: Boolean) {
        org.junit.Assume.assumeFalse(BuildConfig.TV_BUILD)
        val testGroup = group(2)
        val open = mutableStateOf(false)
        var selected: String? = null
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val client = LiveTvClient(JellyfinConfig(context))
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                DisposableEffect(client) { onDispose { client.close() } }
                LiveTvChannelRow(
                    channel = testGroup.primary, client = client, channelGroupCount = 2,
                    onClick = { open.value = true }, onShowSources = { open.value = true },
                    onShowProgram = {}, onToggleFavorite = {}
                )
                if (open.value) LiveTvSourceDialog(testGroup, onDismiss = { open.value = false }, onSelect = {
                    selected = it.MediaSources?.single()?.Id
                    open.value = false
                })
            }
        }
        val target = if (useSourceButton) context.getString(R.string.live_tv_source_count, 2) else "Canal de prueba"
        compose.onNodeWithText(target).performTouchInput { click() }
        compose.onNodeWithText("IPTV 2").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals("source-2", selected); assertTrue(!open.value) }
    }

    @Test fun touchSelectsLastSourceAfterScrollingWithoutSelectingOtherSources() {
        org.junit.Assume.assumeFalse(BuildConfig.TV_BUILD)
        val selected = mutableListOf<String?>()
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LiveTvSourceDialog(group(30), onDismiss = {}, onSelect = {
                    selected += it.MediaSources?.single()?.Id
                })
            }
        }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("IPTV 30"))
        capturePicker("long")
        compose.onNodeWithText("IPTV 30").assertIsDisplayed()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf("source-30"), selected) }
    }

    @Test fun cancelClosesLongPickerWithoutStartingPlayback() {
        org.junit.Assume.assumeFalse(BuildConfig.TV_BUILD)
        val open = mutableStateOf(true)
        var played = false
        val cancel = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.live_tv_cancel)
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                if (open.value) LiveTvSourceDialog(group(30), onDismiss = { open.value = false }, onSelect = { played = true })
            }
        }
        compose.onNodeWithText(cancel).assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertTrue(!open.value && !played) }
    }

    @Test fun tvRemoteStartsOnFirstSourceAndCanSelectTheSecond() {
        org.junit.Assume.assumeTrue(BuildConfig.TV_BUILD)
        var selected: String? = null
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LiveTvSourceDialog(group(3), onDismiss = {}, onSelect = { selected = it.MediaSources?.single()?.Id })
            }
        }
        compose.waitForIdle()
        capturePicker("remote")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals("source-2", selected) }
    }

    private fun capturePicker(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir(null))
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            java.io.File(directory, "picker-$name-${BuildConfig.FLAVOR}.png").outputStream().use {
                check(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { screenshot.recycle() }
    }
}
