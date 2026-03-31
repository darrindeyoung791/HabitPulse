package io.github.darrindeyoung791.habitpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

/**
 * Open Source Licenses Activity
 * 
 * Displays all open source libraries and their licenses used in this project.
 * Uses aboutlibraries library to automatically collect dependency information.
 */
class OpenSourceLicensesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display with proper system bar handling
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                OpenSourceLicensesScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen() {
    val context = LocalContext.current

    // Load libraries from generated R.raw.aboutlibraries
    val libraries = produceState<Libs?>(initialValue = null) {
        value = try {
            val json = context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
            Libs.Builder()
                .withJson(json)
                .build()
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_open_source_licenses),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            libraries.value?.let { libs ->
                LibrariesContainer(
                    libraries = libs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
