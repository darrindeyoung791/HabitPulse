package io.github.darrindeyoung791.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.style.accentDerivedLicenseHueResolver
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import io.github.darrindeyoung791.habitpulse.ui.screens.Md3ScrollbarOverlay
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.PressedCorner
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsExpandableListSurface
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsGroupItemGap
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback

class OpenSourceLicensesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                OpenSourceLicensesScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OpenSourceLicensesScreen() {
    val context = LocalContext.current
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val listState = rememberLazyListState()
    var licenseDialogLibrary by remember { mutableStateOf<Library?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_open_source_licenses),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val libs = libraries?.libraries.orEmpty()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(SettingsGroupItemGap)
            ) {
                itemsIndexed(libs) { index, library ->
                    LibraryExpandableRow(
                        index = index,
                        count = libs.size,
                        library = library,
                        onLicenseContentRequest = { licenseDialogLibrary = it }
                    )
                }
            }

            Md3ScrollbarOverlay(
                listState = listState,
                modifier = Modifier.matchParentSize()
            )
        }
    }

    licenseDialogLibrary?.let { library ->
        LicenseContentDialog(
            library = library,
            onDismiss = { licenseDialogLibrary = null }
        )
    }
}

@Composable
private fun LibraryExpandableRow(
    index: Int,
    count: Int,
    library: Library,
    onLicenseContentRequest: (Library) -> Unit
) {
    var expanded by remember(library.uniqueId) { mutableStateOf(false) }

    val supporting = listOfNotNull(
        library.author.takeIf { it.isNotBlank() },
        library.artifactVersion?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    SettingsExpandableListSurface(
        index = index,
        count = count,
        expanded = expanded,
        onToggle = { expanded = !expanded },
        headline = library.name,
        supportingText = supporting.takeIf { it.isNotBlank() },
        badgeContent = {
            if (library.licenses.isNotEmpty()) {
                LicenseBadgeRow(library = library)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LibraryActions(
                library = library,
                onLicenseContentRequest = onLicenseContentRequest
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LicenseBadgeRow(library: Library) {
    val hueResolver = accentDerivedLicenseHueResolver()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        library.licenses.forEach { license ->
            val hue = license.spdxId?.let { hueResolver.colorFor(it) }
                ?: license.name.let { hueResolver.colorFor(it) }
                ?: MaterialTheme.colorScheme.primary
            LicenseBadge(
                text = license.name,
                hue = hue
            )
        }
    }
}

/**
 * License badge pill — visually identical to the original aboutlibraries Traditional row:
 * 50% corner pill, hue color at 15% alpha container, `labelSmall` 11sp Medium text.
 */
@Composable
private fun LicenseBadge(text: String, hue: Color) {
    val container = hue.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = hue
        )
    }
}

/**
 * Action chips mirroring the original aboutlibraries `LibraryActions`: outlined Source /
 * Website / Sponsor chips and a filled License chip, but with the listitem spec's press
 * corner-radius grow (`PressedCorner`) and press vibration.
 */
@Composable
private fun LibraryActions(
    library: Library,
    onLicenseContentRequest: (Library) -> Unit
) {
    val context = LocalContext.current
    val source = library.scm?.url?.takeIf { it.isNotBlank() }
    val website = library.website?.takeIf { it.isNotBlank() }
    val sponsor = library.funding.firstOrNull()?.url?.takeIf { it.isNotBlank() }
    val licenseUrl = library.licenses.firstOrNull()?.url
    val licensePresent = library.licenses.firstOrNull() != null

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!source.isNullOrBlank()) {
            LibraryActionChip(label = "Source", filled = false) {
                context.startActivity(
                    Intent(context, WebViewActivity::class.java).apply {
                        putExtra(WebViewActivity.EXTRA_INITIAL_URL, source)
                    }
                )
            }
        }
        if (!website.isNullOrBlank()) {
            LibraryActionChip(label = "Website", filled = false) {
                context.startActivity(
                    Intent(context, WebViewActivity::class.java).apply {
                        putExtra(WebViewActivity.EXTRA_INITIAL_URL, website)
                    }
                )
            }
        }
        if (!sponsor.isNullOrBlank()) {
            LibraryActionChip(label = "Sponsor", filled = false) {
                context.startActivity(
                    Intent(context, WebViewActivity::class.java).apply {
                        putExtra(WebViewActivity.EXTRA_INITIAL_URL, sponsor)
                    }
                )
            }
        }
        if (licensePresent) {
            LibraryActionChip(label = "View license", filled = true) {
                if (!licenseUrl.isNullOrBlank()) {
                    context.startActivity(
                        Intent(context, WebViewActivity::class.java).apply {
                            putExtra(WebViewActivity.EXTRA_INITIAL_URL, licenseUrl)
                        }
                    )
                } else {
                    onLicenseContentRequest(library)
                }
            }
        }
    }
}

@Composable
private fun LibraryActionChip(
    label: String,
    filled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    PressVibrationFeedback(interactionSource = interactionSource)

    val corner by animateDpAsState(
        targetValue = if (pressed) PressedCorner else 8.dp,
        label = "actionChipCorner"
    )
    val shape = RoundedCornerShape(corner)

    val container = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
    val content = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (filled) Color.Transparent else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .clip(shape)
            .background(container)
            .border(width = if (filled) 0.dp else 1.dp, color = border, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = content
            )
        )
    }
}

@Composable
private fun LicenseContentDialog(
    library: Library,
    onDismiss: () -> Unit
) {
    val content = library.strippedLicenseContent.takeIf { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(library.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content ?: library.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_confirm))
            }
        }
    )
}