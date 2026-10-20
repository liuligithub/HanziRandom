package com.liuli.hanzirandom

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val clearPrefsRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement =
            object : Statement() {
                override fun evaluate() {
                    InstrumentationRegistry.getInstrumentation().targetContext
                        .getSharedPreferences("hanzi_random", Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit()
                    base.evaluate()
                }
            }
    }

    @get:Rule
    val rule: RuleChain = RuleChain.outerRule(clearPrefsRule).around(composeRule)

    @After
    fun resetInMemorySettings() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
            .applicationContext as HanziApplication
        app.settingsRepository.setSwipeEnabled(true)
    }

    @Test
    fun studyShowsCharacterAndTabsSwitch() {
        assertCharacterShown()

        composeRule.onNodeWithText("学习").assertExists()
        composeRule.onNodeWithText("错题本").assertExists()
        composeRule.onNodeWithTag("mistakesTab").performClick()
        composeRule.onNodeWithTag("mistakeBookHeader").assertExists()
        composeRule.onNodeWithTag("mistakeBookEmptyState").assertExists()

        composeRule.onNodeWithTag("studyTab").performClick()
        assertCharacterShown()
    }

    @Test
    fun swipeWithGestureEnabled_advancesCharacter() {
        val before = characterText()

        swipeCharacterLeft()

        composeRule.waitUntil(timeoutMillis = 2_000) { characterText() != before }
        assertNotEquals(before, characterText())
    }

    @Test
    fun swipeWithGestureDisabled_doesNotChangeCharacter() {
        val before = characterText()

        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithTag("swipeSwitch").performClick()
        composeRule.onNodeWithTag("settingsDoneButton").performClick()

        swipeCharacterLeft()

        assertEquals(before, characterText())
    }

    private fun swipeCharacterLeft() {
        composeRule.onNodeWithTag("characterCard")
            .performTouchInput {
                swipeLeft(startX = centerX + width * 0.3f, endX = centerX - width * 0.3f)
            }
        composeRule.waitForIdle()
    }

    private fun assertCharacterShown() {
        val text = characterText()
        assertTrue("Study should show a character, got '$text'", text.isNotEmpty())
    }

    private fun characterText(): String =
        composeRule.onNodeWithTag("characterLabel", useUnmergedTree = true)
            .assertExists()
            .fetchSemanticsNode().config
            .getOrElseNullable(SemanticsProperties.Text) { null }
            ?.joinToString("")
            .orEmpty()
}
