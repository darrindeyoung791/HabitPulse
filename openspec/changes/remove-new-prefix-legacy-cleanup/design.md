## Context

The settings and welcome features were rebuilt as "NewSettings*" / "NewWelcome*" alongside legacy code. Over successive migrations, the legacy `SettingsActivity` (old segmented settings UI) became unreachable: no launcher or navigation entry starts it; it only forwards to the new screens. Its subtree (`ReminderSettingsActivity` + `ui/screens/ReminderSettingsScreen`) is likewise only reachable from that dead activity. The old welcome UI (`WelcomeScreen`/`WelcomeActivity`) was already deleted in a prior commit, leaving only `NewWelcomeActivity`/`NewWelcomeScreen`.

All live settings/welcome classes still carry the `New` prefix, which is now meaningless noise across ~30 files and complicates navigation, docs, and cross-references.

## Goals / Non-Goals

**Goals:**
- Delete all dead legacy settings/welcome code and its manifest registrations
- Rename all live `New*` settings/welcome classes, files, composables, and references by dropping the prefix
- Keep `AGENTS.md`/`QWEN.md` and user-facing docs in sync
- Preserve behavior exactly — pure refactor, zero runtime change

**Non-Goals:**
- Refactoring the new settings/welcome architecture or UI itself
- Removing the still-live `AICreateHabitScreen` legacy AI flow (separate concern)
- Cleaning unused string resources beyond what becomes orphaned by the deletions

## Decisions

**Decision 1: Delete old `SettingsActivity` and `ReminderSettings*` entirely.**
Verified unreachable: no `Intent`/`Class` reference to `SettingsActivity` exists outside its own definition; `ReminderSettingsActivity` is referenced only from `SettingsActivity`; `ReminderSettingsScreen` only from `ReminderSettingsActivity`. Alternative (keeping them) rejected — they duplicate new-screen behavior and would be permanently dead weight.

**Decision 2: Rename in-place by file rename + symbol rename, not content rewrite.**
For each file: rename the file (e.g. `NewSettingsActivity.kt`→`SettingsActivity.kt`), rename the class/composable declarations, and update imports/references. Reuse is done after deletion, so `NewSettingsActivity`→`SettingsActivity` collides with nothing. Alternatives: keep the `New` prefix (rejected: user request), or single mega-rename commit (rejected: keep each rename verifiable).

**Decision 3: Use mechanical find/replace scoped to the settings/welcome feature.**
Rename patterns applied only to the exact identifier tokens (`NewSettings*`, `NewWelcome*`, and the private `*Content` composables inside the renamed Activity files). Careful NOT to touch unrelated identifiers: e.g. `AICreateHabitScreen`, `ReminderReceiver`, `settings_*` string resources, and the `SettingsSectionHeader` shared component keep their names.

**Decision 4: Delete-then-rename ordering avoids collisions.**
Delete `SettingsActivity.kt` first so the name is free for the rename of `NewSettingsActivity.kt`; manifest entry for old `.SettingsActivity` is removed and the renamed activity gets that name. `AndroidManifest.xml` keeps alphabetical-ish registration order consistent with surrounding entries.

**Decision 5: Docs updated last, after code compiles.**
`AGENTS.md`/`QWEN.md` structure trees and feature bullets, `README.md`/`devdoc/readme/README_EN-US.md` trees updated to match final file names. Historical openspec change docs and design guides in `devdoc/ux-design-guide/` are left as historical records.

## Risks / Trade-offs

- [Missed a reference after rename] → Run a global `NewSettings|NewWelcome` grep after the rename and require a clean build (`assembleDebug`) and passing tests.
- [Name collision with future class] → New names are plain, non-prefixed; verified no current conflicts (`SettingsActivity` freed by deletion, `WelcomeScreen` unused since legacy was deleted).
- [Docs drift from code] → Docs updated in the same change; `AGENTS.md`/`QWEN.md` kept byte-consistent with each other.