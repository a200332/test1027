package com.dailybrief.app.ui

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dailybrief.app.R
import com.dailybrief.app.data.Article
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApp::class)
class ActivitySmokeTest {

    @Test
    fun `settings screen inflates and lists every source`() {
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        val activity = controller.get()
        val list = activity.findViewById<RecyclerView>(R.id.sourceList)
        assertEquals(6, list.adapter?.itemCount)
        assertTrue(activity.findViewById<MaterialSwitch>(R.id.digestSwitch).isChecked)
        assertTrue(activity.findViewById<TextView>(R.id.versionText).text.contains("1.0.0"))
        controller.pause().stop().destroy()
    }

    @Test
    fun `detail screen renders article fields`() {
        val article = Article(
            id = "test|https://example.com/a",
            title = "测试新闻标题",
            link = "https://example.com/a",
            summary = "摘要内容",
            content = "完整正文内容",
            imageUrl = null,
            sourceId = "test",
            sourceName = "测试来源",
            category = "科技",
            publishedAt = 1793049608000L,
            author = null
        )
        val intent = DetailActivity.createIntent(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            article
        )
        val controller = Robolectric.buildActivity(DetailActivity::class.java, intent).setup()
        val activity = controller.get()
        assertEquals(
            "测试新闻标题",
            activity.findViewById<TextView>(R.id.detailTitle).text.toString()
        )
        assertEquals(
            "完整正文内容",
            activity.findViewById<TextView>(R.id.detailBody).text.toString()
        )
        assertTrue(
            activity.findViewById<TextView>(R.id.detailMeta).text.contains("测试来源")
        )
        controller.pause().stop().destroy()
    }
}
