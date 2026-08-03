package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsTemplateScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val templateValue by userPreferences.notificationTemplateFlow.collectAsStateWithLifecycle(initialValue = null)

    var editedTemplate by remember { mutableStateOf(templateValue ?: context.getString(R.string.notification_default_template)) }

    NewSettingsScaffold(
        title = stringResource(id = R.string.notification_template_settings_title),
        onBack = onBack,
        onHelp = onOpenHelp
    ) {
        OutlinedTextField(
            value = editedTemplate,
            onValueChange = { editedTemplate = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(12.dp),
            label = { Text(stringResource(id = R.string.notification_template_label)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.notification_template_variable_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        userPreferences.setNotificationTemplate(editedTemplate)
                        Toast.makeText(
                            context,
                            context.getString(R.string.notification_template_save_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.dialog_confirm))
            }
            TextButton(
                onClick = {
                    scope.launch {
                        userPreferences.resetNotificationTemplate()
                        editedTemplate = context.getString(R.string.notification_default_template)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.notification_template_reset))
            }
        }
    }
}