package oneclick.client.apps.user.core.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import oneclick.client.apps.user.core.generated.resources.*
import oneclick.client.apps.user.core.ui.screens.LoginScreenTestTags.PASSWORD_PLACEHOLDER
import oneclick.client.apps.user.core.ui.screens.LoginScreenTestTags.PASSWORD_TEXT_FIELD
import oneclick.client.apps.user.core.ui.screens.LoginScreenTestTags.USERNAME_PLACEHOLDER
import oneclick.client.apps.user.core.ui.screens.LoginScreenTestTags.USERNAME_TEXT_FIELD
import oneclick.client.apps.user.ui.components.Body
import oneclick.client.apps.user.ui.components.DefaultButton
import oneclick.client.apps.user.ui.components.DialogBox
import oneclick.client.apps.user.ui.components.ScreenBox
import oneclick.client.apps.user.ui.models.Field
import oneclick.client.apps.user.ui.previews.dev.ScreenPreviewComposable
import oneclick.client.apps.user.ui.previews.providers.base.PreviewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    state: LoginScreenState,
    onEvent: (event: LoginEvent) -> Unit,
) {
    ScreenBox {
        DialogBox(header = stringResource(Res.string.loginScreen_title_register)) {
            UsernameTextField(
                username = state.username.text,
                isUsernameValid = state.username.isValid != false,
                onUsernameChange = { newUsername -> onEvent(LoginEvent.UsernameChanged(newUsername)) },
            )

            PasswordTextField(
                password = state.password.text,
                isPasswordValid = state.password.isValid != false,
                onPasswordChange = { newPassword -> onEvent(LoginEvent.PasswordChanged(newPassword)) },
            )

            DefaultButton(
                text = stringResource(Res.string.loginScreen_registerButton_register),
                isEnabled = state.isRegisterButtonEnabled,
                isLoading = state.isLoading,
                onClick = { onEvent(LoginEvent.RegisterButtonClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UsernameTextField(
    username: String,
    isUsernameValid: Boolean,
    onUsernameChange: (String) -> Unit
) {
    OutlinedTextField(
        placeholder = {
            Body(
                text = stringResource(Res.string.loginScreen_usernameTextField_username),
                modifier = Modifier.testTag(USERNAME_PLACEHOLDER)
            )
        },
        value = username,
        onValueChange = onUsernameChange,
        isError = !isUsernameValid,
        maxLines = 1,
        modifier = Modifier
            .semantics {
                contentType = ContentType.Username
            }
            .testTag(USERNAME_TEXT_FIELD),
    )
}

@Composable
private fun PasswordTextField(
    password: String,
    isPasswordValid: Boolean,
    onPasswordChange: (String) -> Unit
) {
    val showPassword = remember { mutableStateOf(false) }

    OutlinedTextField(
        placeholder = {
            Body(
                text = stringResource(Res.string.loginScreen_passwordTextField_password),
                modifier = Modifier.testTag(PASSWORD_PLACEHOLDER)
            )
        },
        trailingIcon = {
            IconButton(onClick = { showPassword.value = !showPassword.value }) {
                Icon(
                    imageVector = if (showPassword.value) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null,
                )
            }
        },
        value = password,
        onValueChange = onPasswordChange,
        visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
        isError = !isPasswordValid,
        maxLines = 1,
        modifier = Modifier
            .semantics {
                contentType = ContentType.Password
                password()
            }
            .testTag(PASSWORD_TEXT_FIELD),
    )
}

object LoginScreenTestTags {
    const val USERNAME_TEXT_FIELD = "LoginScreen.Username.TextField"
    const val USERNAME_PLACEHOLDER = "LoginScreen.Username.Placeholder"
    const val PASSWORD_TEXT_FIELD = "LoginScreen.Password.TextField"
    const val PASSWORD_PLACEHOLDER = "LoginScreen.Password.Placeholder"
}

sealed interface LoginEvent {
    data class UsernameChanged(val newUsername: String) : LoginEvent
    data class PasswordChanged(val newPassword: String) : LoginEvent

    data object RegisterButtonClicked : LoginEvent
}

@Immutable
data class LoginScreenState(
    val username: Field = Field(),
    val password: Field = Field(),
    val isRegisterButtonEnabled: Boolean = false,
    val isLoading: Boolean = false,
)

@Composable
fun LoginScreenPreview(previewModel: PreviewModel<LoginScreenState>) {
    ScreenPreviewComposable(previewModel) {
        LoginScreen(
            state = previewModel.model,
            onEvent = {}
        )
    }
}
