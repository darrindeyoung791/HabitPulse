package com.ddy.habitpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ddy.habitpulse.ui.theme.HabitPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var showHabitSheet by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Modal bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState()
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HabitPulse") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation icon */ }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("新建习惯") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                onClick = { 
                    isEditing = false
                    showHabitSheet = true 
                }
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                // Empty state when no habits exist
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LibraryAdd,
                        contentDescription = "No habits",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无习惯",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { 
                            isEditing = false
                            showHabitSheet = true 
                        }
                    ) {
                        Text(
                            text = "去新建一个",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    )
    
    // Modal bottom sheet for adding/editing habits
    if (showHabitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHabitSheet = false },
            sheetState = bottomSheetState,
            dragHandle = {
                // 自定义指示条，减小高度以避免遮挡摄像头
                Box(
                    modifier = Modifier
                        .padding(top = 64.dp)
                        .width(32.dp)
                        .height(4.dp) // 减小高度
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isEditing) "编辑习惯" else "新建习惯",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                // Content will be implemented later
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HabitPulseTheme {
        MainScreen()
    }
}