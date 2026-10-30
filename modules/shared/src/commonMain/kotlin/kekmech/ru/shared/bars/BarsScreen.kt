package kekmech.ru.shared.bars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_bars_shared.BarsController
import kekmech.ru.feature_bars_shared.BarsUiState

/** The BARS section: draws what [controller] says and reports the taps to it. */
@Composable
public fun BarsScreen(controller: BarsController) {
    val state by controller.state.collectAsState()
    when (val current = state) {
        BarsUiState.Starting -> Centered { CircularProgressIndicator() }
        is BarsUiState.Failed -> FailedView(current.message, onRetry = { controller.start() })
        is BarsUiState.SignIn -> SignInView(current, onSignIn = { login, password -> controller.signIn(login, password) })
        is BarsUiState.Code -> CodeView(
            state = current,
            onSubmit = { controller.submitCode(it) },
            onResend = { controller.resendCode(it) },
            onBack = { controller.backToSignIn() },
        )
        is BarsUiState.Content -> ContentView(
            state = current,
            onSemester = { controller.selectSemester(it) },
            onTab = { controller.selectTab(it) },
            onRefresh = { controller.refresh() },
            onSignOut = { controller.signOut() },
        )
    }
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().safeContentPadding(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun FailedView(message: String, onRetry: () -> Unit) {
    Centered {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}

@Composable
private fun SignInView(state: BarsUiState.SignIn, onSignIn: (String, String) -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("БАРС МЭИ", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Войдите тем же логином и паролем, что и на сайте БАРС. Пароль уходит только в БАРС и нигде не сохраняется.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            singleLine = true,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            enabled = !state.busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(state.error)
        Button(
            onClick = { onSignIn(login, password) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Войти")
        }
    }
}

@Composable
private fun CodeView(
    state: BarsUiState.Code,
    onSubmit: (String) -> Unit,
    onResend: (kekmech.ru.lib_bars_parser.BarsTwoFactorProvider) -> Unit,
    onBack: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val length = state.provider.codeLength
    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Код подтверждения", style = MaterialTheme.typography.headlineMedium)
        state.info?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        OutlinedTextField(
            value = code,
            onValueChange = { typed ->
                code = typed.filter { it.isDigit() }.take(length)
                // The page of BARS sends the code as soon as it is complete
                if (code.length == length) onSubmit(code)
            },
            label = { Text("Код ($length цифр)") },
            singleLine = true,
            enabled = !state.busy,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(state.error)
        Button(
            onClick = { onSubmit(code) },
            enabled = !state.busy && code.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Войти")
        }
        state.page.providers.forEach { provider ->
            TextButton(onClick = { onResend(provider) }, enabled = !state.busy) { Text(provider.caption) }
        }
        OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
internal fun ErrorText(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
