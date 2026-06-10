## 1. Extract reusable DndRangeSlider component

- [x] 1.1 Identify DndRangeSlider code in `ReminderSettingsScreen.kt` (RangeSlider with 30-min step, time display, toLinear/snapToStep/stepToTime helpers)
- [x] 1.2 Extract DndRangeSlider into a standalone Composable file at `ui/screens/dnd/DndRangeSlider.kt`
- [x] 1.3 Update `ReminderSettingsScreen.kt` to import and use the extracted DndRangeSlider
- [x] 1.4 Verify ReminderSettingsScreen still works correctly after extraction (build succeeds)

## 2. Create step indicator composable

- [x] 2.1 Create `ui/screens/welcome/WelcomeStepIndicator.kt` with a Composable that renders "① ② ③" step indicators
- [x] 2.2 Current step should be visually distinct (primary color, filled circle/bold vs outline/dim)
- [x] 2.3 Completed steps should also be visually distinct (checkmark or secondary color)

## 3. Refactor WelcomeActivity to multi-step state management

- [x] 3.1 Add `currentStep` state variable in WelcomeActivity (using `rememberSaveable` for rotation safety, default = 1)
- [x] 3.2 Define step callbacks: `onNotificationNext`, `onSkip`, `onAIComplete`, `onDisagree`
- [x] 3.3 Update `setContent` to pass `currentStep` and callbacks to WelcomeScreen
- [x] 3.4 Keep existing `onAgree` behavior: Step 1 agree → advance to Step 2 (not directly to permission request)

## 4. Update WelcomeScreen to render multi-step content

- [x] 4.1 Modify WelcomeScreen signature to accept `currentStep: Int`, `onNotificationNext`, `onSkip`, `onAIComplete` plus existing callbacks
- [x] 4.2 Add `when (currentStep)` block to render the appropriate step content inside Scaffold
- [x] 4.3 Add WelcomeStepIndicator at the bottom of steps 2 and 3
- [x] 4.4 Include "下一步"/"跳过" or "完成"/"跳过" buttons depending on step

## 5. Add TOS/Privacy links to Step 1 (Welcome step)

- [x] 5.1 Add clickable "使用条款" and "隐私政策" text links below the permission list
- [x] 5.2 On click, show Snackbar with "即将推出" message
- [x] 5.3 Add `SnackbarHostState` to the Scaffold for Snackbar display
- [x] 5.4 Add string resources for all locales

## 6. Create WelcomeNotificationStep composable

- [x] 6.1 Create `ui/screens/welcome/WelcomeNotificationStep.kt`
- [x] 6.2 Compose the step with title, description, DND toggle switch
- [x] 6.3 Include extracted DndRangeSlider (conditionally visible when DND enabled)
- [x] 6.4 Expose saved state via callbacks: `onNext(dndEnabled, dndStart, dndEnd)`, `onSkip()`
- [x] 6.5 Add string resources for all locales

## 7. Create WelcomeAIStep composable

- [x] 7.1 Create `ui/screens/welcome/WelcomeAIStep.kt`
- [x] 7.2 Compose the step with: title, description, API endpoint field, API key field (password toggle), model dropdown
- [x] 7.3 Pre-fill endpoint with default `https://open.bigmodel.cn/api/paas/v4/chat/completions` and model with `glm-4-flash-250414`
- [x] 7.4 Expose saved state: `onComplete(endpoint, apiKey, model)`, `onSkip()`
- [x] 7.5 Add string resources for all locales

## 8. Wire up step completion data flow

- [x] 8.1 WelcomeActivity uses `UserPreferences.getInstance()` directly to save DND and AI settings
- [x] 8.2 Step 2 next: calls `UserPreferences.setDndEnabled()`, `setDndStartTime()`, `setDndEndTime()` then advances to Step 3
- [x] 8.3 Step 2 skip: advances to Step 3 without saving
- [x] 8.4 Step 3 complete: calls `UserPreferences.setLlmApiEndpoint()`, `setLlmApiKey()`, `setLlmModelName()` then triggers `completeOnboarding()` + permission request + MainActivity
- [x] 8.5 Step 3 skip: triggers `completeOnboarding()` + permission request + MainActivity without saving AI settings

## 9. Add string resources for all locales

- [x] 9.1 Add to `values/strings.xml` (Simplified Chinese): step titles, step descriptions, TOS/Privacy link texts, Snackbar message
- [x] 9.2 Add to `values-en-rUS/strings.xml`: English translations
- [x] 9.3 Add to `values-zh-rHK/strings.xml`: Traditional Chinese (HK) translations
- [x] 9.4 Add to `values-zh-rTW/strings.xml`: Traditional Chinese (TW) translations

## 10. Verify and test

- [x] 10.1 LauncherActivity routes to WelcomeActivity for first launch (unchanged)
- [x] 10.2 "不同意" → limited mode → MainActivity flow (unchanged logic)
- [x] 10.3 Step 1 → Step 2 → Step 3 → MainActivity flow (implemented)
- [x] 10.4 DND settings saved via UserPreferences (same DataStore keys)
- [x] 10.5 AI settings saved via UserPreferences (same DataStore keys)
- [x] 10.6 Run existing unit tests to ensure no regression (4 test classes, all pass)
- [x] 10.7 Build the project to verify no compilation errors
