package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TimetableRepository
import com.example.ui.TimetableViewModel
import com.example.ui.components.UpdateMarkdownBlock
import com.example.ui.components.UpdateMarkdownParser
import com.example.ui.components.UpdateMarkdownSpan
import com.example.ui.dialogs.ANDROID_2027_NOTICE_MARKDOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportantNoticeUnitTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("timetable_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `notice markdown contains both expected clickable links`() {
        val blocks = UpdateMarkdownParser.parseBlocks(ANDROID_2027_NOTICE_MARKDOWN)
        assertEquals(1, blocks.size)
        val textBlock = blocks.first() as UpdateMarkdownBlock.TextBlock
        val links = textBlock.spans.filterIsInstance<UpdateMarkdownSpan.Link>()
        assertEquals(2, links.size)
        assertEquals("詳細", links[0].title)
        assertEquals("https://share.google/KBQ1j704V23MfyXco", links[0].url)
        assertEquals("2027年以降のインストール方法", links[1].title)
        assertEquals("https://share.google/XWYJi7lAEkFMgIRDL", links[1].url)
    }

    @Test
    fun `startup shows important notice after onboarding until dontShowAgain is checked`() {
        val repo = TimetableRepository.getInstance(app)
        assertFalse(repo.isImportantNotice2027Dismissed())

        // During initial onboarding, dialog does not appear on top of onboarding
        val vmDuringOnboarding = TimetableViewModel(app)
        assertFalse(vmDuringOnboarding.uiState.value.showImportantNoticeDialog)

        // Completing onboarding triggers the important notice dialog
        vmDuringOnboarding.completeOnboarding()
        assertTrue(vmDuringOnboarding.uiState.value.showImportantNoticeDialog)

        // Closing without checking "二度と表示しない" closes for current session only
        vmDuringOnboarding.dismissImportantNoticeDialog(dontShowAgain = false)
        assertFalse(vmDuringOnboarding.uiState.value.showImportantNoticeDialog)
        assertFalse(repo.isImportantNotice2027Dismissed())

        // On next launch, dialog appears again
        val vmSecondLaunch = TimetableViewModel(app)
        assertTrue(vmSecondLaunch.uiState.value.showImportantNoticeDialog)

        // Closing with "二度と表示しない" persists preference
        vmSecondLaunch.dismissImportantNoticeDialog(dontShowAgain = true)
        assertFalse(vmSecondLaunch.uiState.value.showImportantNoticeDialog)
        assertTrue(repo.isImportantNotice2027Dismissed())

        // On third launch, dialog stays hidden automatically
        val vmThirdLaunch = TimetableViewModel(app)
        assertFalse(vmThirdLaunch.uiState.value.showImportantNoticeDialog)
        assertTrue(vmThirdLaunch.uiState.value.isImportantNoticePermanentlyDismissed)

        // Can still be opened manually from Other > ガイド・更新情報
        vmThirdLaunch.openImportantNoticeDialog()
        assertTrue(vmThirdLaunch.uiState.value.showImportantNoticeDialog)
        assertTrue(vmThirdLaunch.uiState.value.isImportantNoticePermanentlyDismissed)
    }
}
