package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var darkTheme by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                // Appearance section
                ListItem(
                    headlineContent = { Text("深色模式") },
                    supportingContent = { Text("切换应用主题颜色") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { darkTheme = it }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // About section
                ListItem(
                    headlineContent = { Text("应用版本") },
                    supportingContent = { Text(versionName) },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                ListItem(
                    headlineContent = { Text("开发者") },
                    supportingContent = { Text("darrindeyoung791") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HabitPulseTheme {
        SettingsScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenDarkPreview() {
    HabitPulseTheme(darkTheme = true) {
        SettingsScreen()
    }
}
