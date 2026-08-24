package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTemplateScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val templateValue by userPreferences.notificationTemplateFlow.collectAsStateWithLifecycle(initialValue = null)

    val defaultTemplate = context.getString(R.string.notification_default_template)
    var editedTemplate by remember { mutableStateOf(templateValue ?: defaultTemplate) }

    val hasUnsavedChanges = editedTemplate != (templateValue ?: defaultTemplate)

    val handleBack: () -> Unit = {
        if (hasUnsavedChanges) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_unsaved_changes),
                Toast.LENGTH_SHORT
            ).show()
        }
        onBack()
    }
    BackHandler { handleBack() }

    SettingsScaffold(
        title = stringResource(id = R.string.notification_template_settings_title),
        onBack = handleBack,
        onHelp = onOpenHelp,
        reserveFabSpace = true,
        floatingActionButton = {
            val fabInteractionSource = remember { MutableInteractionSource() }
            ExtendedFloatingActionButton(
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
                interactionSource = fabInteractionSource,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.ai_settings_save)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(id = R.string.ai_settings_save)) }
            )
            PressVibrationFeedback(interactionSource = fabInteractionSource)
        }
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    userPreferences.resetNotificationTemplate()
                    editedTemplate = defaultTemplate
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(stringResource(id = R.string.notification_template_reset))
        }
    }
}