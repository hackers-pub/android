package pub.hackers.android.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.data.local.StoredAccount
import pub.hackers.android.ui.theme.AppTypographyDefaults
import pub.hackers.android.ui.theme.LightAppColors
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `RevokePasskeyDialog renders title and confirm-cancel buttons`() {
        composeRule.setContent {
            TestTheme {
                RevokePasskeyDialog(
                    passkeyId = "passkey-123",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Remove Passkey").assertIsDisplayed()
        composeRule.onNodeWithText("Are you sure you want to remove this passkey?").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `RevokePasskeyDialog confirm button passes passkeyId to callback`() {
        val confirmed = mutableListOf<String>()
        composeRule.setContent {
            TestTheme {
                RevokePasskeyDialog(
                    passkeyId = "passkey-xyz",
                    onConfirm = { id -> confirmed += id },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Remove").performClick()

        assertEquals(listOf("passkey-xyz"), confirmed)
    }

    @Test
    fun `RevokePasskeyDialog cancel button triggers onDismiss`() {
        var dismissed = 0
        composeRule.setContent {
            TestTheme {
                RevokePasskeyDialog(
                    passkeyId = "id",
                    onConfirm = {},
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, dismissed)
    }

    @Test
    fun `AccountSwitcherMenu opens a dropdown with other accounts and Add Account`() {
        val switched = mutableListOf<String>()
        var addClicks = 0
        val bob = StoredAccount("bob-token", "bob-id", "bob", "@bob@hackers.pub", "Bob", "b.png")
        composeRule.setContent {
            TestTheme {
                AccountSwitcherMenu(
                    accounts = listOf(bob),
                    enabled = true,
                    onSwitchAccount = { switched += it },
                    onAddAccountClick = { addClicks++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Switch Account").performClick()
        composeRule.onNodeWithText("Bob").performClick()
        composeRule.onNodeWithContentDescription("Switch Account").performClick()
        composeRule.onNodeWithText("Add Account").performClick()

        assertEquals(listOf("bob-id"), switched)
        assertEquals(1, addClicks)
    }
}

@Composable
private fun TestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides LightAppColors,
        LocalAppTypography provides AppTypographyDefaults,
    ) {
        MaterialTheme(content = content)
    }
}
