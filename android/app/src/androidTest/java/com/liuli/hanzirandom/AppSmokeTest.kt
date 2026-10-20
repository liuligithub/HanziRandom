package com.liuli.hanzirandom

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun studyShowsCharacterAndTabsSwitch() {
        rule.onNodeWithTag("characterLabel", useUnmergedTree = true).assertExists()

        rule.onNodeWithText("错题本").assertExists()
        rule.onNodeWithTag("mistakesTab").performClick()
        rule.onNodeWithTag("mistakeBookHeader").assertExists()
        rule.onNodeWithTag("mistakeBookEmptyState").assertExists()

        rule.onNodeWithTag("studyTab").performClick()
        rule.onNodeWithTag("characterLabel", useUnmergedTree = true).assertExists()
    }
}
