package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.rememberDeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.screens.welcome.WelcomeAIStep
import io.github.darrindeyoung791.habitpulse.ui.screens.welcome.WelcomeMergedStep
import io.github.darrindeyoung791.habitpulse.ui.screens.welcome.WelcomeNotificationStep
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    currentStep: Int,
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
    isLimitedMode: Boolean = false,
    onNotificationNext: (reminderEnabled: Boolean, dndEnabled: Boolean, dndStart: String, dndEnd: String, persistentEnabled: Boolean) -> Unit,
    onSkip: () -> Unit,
    onAIComplete: (endpoint: String, apiKey: String, model: String, streamingEnabled: Boolean) -> Unit,
    onPrevious: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val deviceForm = rememberDeviceFormInfo()
    val shouldUseSplitLayout = deviceForm.isLandscape
    val isTablet = deviceForm.isLargeWindow

    val secondaryTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Normal
    )

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(animationSpec = tween(300)) { width -> width / 3 } + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(animationSpec = tween(300)) { width -> -width / 3 } + fadeOut(animationSpec = tween(300))
                } else {
                    slideInHorizontally(animationSpec = tween(300)) { width -> -width / 3 } + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(animationSpec = tween(300)) { width -> width / 3 } + fadeOut(animationSpec = tween(300))
                }
            },
            label = "welcome_steps"
        ) { step ->
            when (step) {
                1 -> {
                    if (shouldUseSplitLayout) {
                        WelcomeStep1SplitLayout(
                            paddingValues = paddingValues,
                            isLimitedMode = isLimitedMode,
                            secondaryTextStyle = secondaryTextStyle,
                            onAgree = onAgree,
                            onDisagree = onDisagree
                        )
                    } else {
                        WelcomeStep1PortraitLayout(
                            paddingValues = paddingValues,
                            isLimitedMode = isLimitedMode,
                            secondaryTextStyle = secondaryTextStyle,
                            onAgree = onAgree,
                            onDisagree = onDisagree
                        )
                    }
                }
                2 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (isTablet) {
                            WelcomeMergedStep(
                                onComplete = onAIComplete,
                                onSkip = onSkip,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            WelcomeNotificationStep(
                                initialReminderEnabled = true,
                                onNext = onNotificationNext,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                3 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        WelcomeAIStep(
                            onComplete = onAIComplete,
                            onSkip = onSkip,
                            onBack = onPrevious,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep1SplitLayout(
    paddingValues: PaddingValues,
    isLimitedMode: Boolean,
    secondaryTextStyle: androidx.compose.ui.text.TextStyle,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    val logoDelay = 0
    val descriptionDelay = 150
    val permissionsTitleDelay = 250
    val permission1Delay = 300
    val permission2Delay = 350
    val primaryButtonDelay = 450
    val secondaryButtonDelay = 500

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 600, delayMillis = logoDelay)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500, delayMillis = descriptionDelay)
                )
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500, delayMillis = descriptionDelay)
                )
            ) {
                Text(
                    text = stringResource(R.string.welcome_description),
                    style = secondaryTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (isLimitedMode) {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 400, delayMillis = 300)
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.welcome_limited_mode),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500, delayMillis = permissionsTitleDelay)
                )
            ) {
                Text(
                    text = stringResource(R.string.welcome_permissions_title, stringResource(R.string.app_name)),
                    style = secondaryTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 400, delayMillis = permission1Delay)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.welcome_permission_notification_title),
                        style = secondaryTextStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 400, delayMillis = permission2Delay)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SettingsBackupRestore,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.welcome_permission_background_title),
                        style = secondaryTextStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        initialScale = 0.95f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 400, delayMillis = primaryButtonDelay)
                    )
                ) {
                    Button(
                        onClick = onAgree,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_agree_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 400, delayMillis = secondaryButtonDelay)
                    )
                ) {
                    TextButton(
                        onClick = onDisagree,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_disagree_button),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep1PortraitLayout(
    paddingValues: PaddingValues,
    isLimitedMode: Boolean,
    secondaryTextStyle: androidx.compose.ui.text.TextStyle,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    val logoDelay = 0
    val descriptionDelay = 150
    val permissionsTitleDelay = 250
    val permission1Delay = 300
    val permission2Delay = 350
    val primaryButtonDelay = 450
    val secondaryButtonDelay = 500

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 48.dp, horizontal = 24.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 600, delayMillis = logoDelay)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500, delayMillis = descriptionDelay)
                )
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500, delayMillis = descriptionDelay)
                )
            ) {
                Text(
                    text = stringResource(R.string.welcome_description),
                    style = secondaryTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (isLimitedMode) {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 400, delayMillis = 300)
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.welcome_limited_mode),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 500, delayMillis = permissionsTitleDelay)
                )
            ) {
                Text(
                    text = stringResource(R.string.welcome_permissions_title, stringResource(R.string.app_name)),
                    style = secondaryTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 400, delayMillis = permission1Delay)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.welcome_permission_notification_title),
                        style = secondaryTextStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 400, delayMillis = permission2Delay)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SettingsBackupRestore,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.welcome_permission_background_title),
                        style = secondaryTextStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 400, delayMillis = primaryButtonDelay)
                )
            ) {
                Button(
                    onClick = onAgree,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.welcome_agree_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 400, delayMillis = secondaryButtonDelay)
                )
            ) {
                TextButton(
                    onClick = onDisagree,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.welcome_disagree_button),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    HabitPulseTheme {
        WelcomeScreen(
            currentStep = 1,
            onAgree = {},
            onDisagree = {},
            isLimitedMode = false,
            onNotificationNext = { _, _, _, _, _ -> },
            onSkip = {},
            onAIComplete = { _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.ORIENTATION_LANDSCAPE)
@Composable
private fun WelcomeScreenLandscapePreview() {
    HabitPulseTheme {
        WelcomeScreen(
            currentStep = 1,
            onAgree = {},
            onDisagree = {},
            isLimitedMode = false,
            onNotificationNext = { _, _, _, _, _ -> },
            onSkip = {},
            onAIComplete = { _, _, _, _ -> }
        )
    }
}
