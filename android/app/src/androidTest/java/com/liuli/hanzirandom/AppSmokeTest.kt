package com.liuli.hanzirandom

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private fun assertCharacterShown() {
        val label = composeRule.onNodeWithTag("characterLabel", useUnmergedTree = true)
        label.assertExists()
        val text = label.fetchSemanticsNode().config
            .getOrElseNullable(SemanticsProperties.Text) { null }
            ?.joinToString("")
            .orEmpty()
        assertTrue("Study should show a character, got '$text'", text.isNotEmpty())
    }
}
