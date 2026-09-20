package com.klortek.velora

import android.content.Intent
import android.os.Parcel
import androidx.test.platform.app.InstrumentationRegistry
import com.klortek.velora.livetv.LiveTvChannelSourceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class LiveTvPlayerIntentTest {
    @Test fun sourceChoicesSurviveAndroidIntentParcelWithNullSlots() {
        val ids = listOf("A", "B", "C")
        val intent = JellyfinVideoPlayerActivity.createIntent(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            itemId = "A", isLiveTv = true, liveTvMediaSourceId = "S2",
            liveTvChannelIds = ids,
            liveTvChannelNames = listOf("Canal A", "Canal B", "Canal C"),
            liveTvChannelMediaSourceIds = listOf("S2", null, "S3")
        )
        val parcel = Parcel.obtain()
        try {
            intent.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = Intent.CREATOR.createFromParcel(parcel)
            val sources = LiveTvChannelSourceState(
                restored.getStringArrayListExtra("live_tv_channel_ids").orEmpty(),
                restored.getStringArrayListExtra("live_tv_channel_media_source_ids").orEmpty()
            )
            assertEquals("S2", restored.getStringExtra("live_tv_media_source_id"))
            assertEquals("S2", sources.mediaSourceIdFor("A"))
            assertNull(sources.mediaSourceIdFor("B"))
            assertEquals("S3", sources.mediaSourceIdFor("C"))
            assertEquals(listOf("S2", null, "S3"), sources.mediaSourceIdsFor(ids))
        } finally { parcel.recycle() }
    }
}
