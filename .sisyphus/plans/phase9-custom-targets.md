# Phase 9: Custom Daily Macro/Micro Targets

## TL;DR

> **Quick Summary**: Add user-configurable daily macro/micro targets (Protein, Carbs, Fats, Fiber, Sodium) to the Profile screen, persist them in DataStore, and wire them into Dashboard analytics so progress bars use custom targets instead of formula-based defaults.
> 
> **Deliverables**:
> - 5 new DataStore preference keys + UserProfile data class expansion
> - "DAILY TARGETS" input section on ProfileScreen with 5 OutlinedTextFields
> - Dashboard progress bars dynamically sourced from custom targets (formula fallback for P/C/F when unset)
> - Conditional Fiber/Sodium progress bars on Dashboard (appear only when target > 0)
> 
> **Estimated Effort**: Short (4 files modified, ~150 lines added)
> **Parallel Execution**: YES — 2 waves + verification
> **Critical Path**: Task 1 → Task 2 or 3 (parallel) → Task 4

---

## Context

### Original Request
Phase 9 of SavageStats: Upgrade the Profile section to allow users to set custom daily macro and micro targets, which feed into the Dashboard Analytics. Update DataStore, ProfileScreen.kt, and DashboardViewModel.kt.

### Interview Summary
**Key Discussions**:
- Custom targets override the formula-based targets from Phase 8's `macroTargetsForGoal()`
- Targets default to 0f, meaning "use formula fallback" for P/C/F and "don't show bar" for Fiber/Sodium
- Save button on ProfileScreen saves all targets atomically with existing profile data
- Target input fields are OPTIONAL — save button must NOT require them

**Research Findings**:
- `AnalyticsUiState` currently lacks fiber/sodium fields — must be added
- `MacroProgressRow` composable hardcodes "g" unit — needs a `unit` parameter for Sodium's "mg"
- `DailyLog` already stores fiber and sodium data — available via `todayLog?.fiber`
- `ProfileViewModel` has a clean pattern for input StateFlows + init pre-fill that we replicate exactly

### Metis Review
**Identified Gaps** (addressed):
- AnalyticsUiState missing fiber/sodium fields → Added to Task 3
- Save button `enabled` guard must NOT change → Explicit guardrail
- MacroProgressRow hardcodes "g" unit → Task 3 adds `unit` parameter
- Sodium units must be labeled "mg" consistently → Enforced in both Profile and Dashboard
- ProfileSummaryCard doesn't show targets → Deferred (low value, keeps scope tight)
- No formula fallback for Fiber/Sodium → Intentional: bars hidden when target = 0

---

## Work Objectives

### Core Objective
Enable users to set custom daily nutrition targets in their Profile that replace the generic formula-based targets in Dashboard Analytics progress bars.

### Concrete Deliverables
- `UserProfileManager.kt`: 5 new DataStore keys + expanded UserProfile data class (9 fields total)
- `ProfileViewModel.kt`: 5 new input StateFlows + onChange handlers + saveProfile() expansion
- `ProfileScreen.kt`: "DAILY TARGETS" card section with 5 OutlinedTextFields
- `DashboardViewModel.kt`: Conditional target override in analyticsUiState combine block
- `DashboardScreen.kt`: Conditional Fiber/Sodium progress bars + unit parameter on MacroProgressRow

### Definition of Done
- [ ] `gradlew.bat assembleDebug` exits 0 (clean compile)
- [ ] `gradlew.bat testDebugUnitTest` exits 0 (existing tests still pass)
- [ ] Profile screen shows "DAILY TARGETS" section with 5 input fields
- [ ] Dashboard progress bars use custom targets when set, formula fallback when not
- [ ] Fiber/Sodium progress bars appear only when their custom target > 0

### Must Have
- 5 DataStore preference keys (targetProtein, targetCarbs, targetFats, targetFiber, targetSodium)
- UserProfile data class with 5 new Float fields defaulting to 0f
- DAILY TARGETS section in ProfileScreen between Goal selector and Save button
- KeyboardType.Decimal on all target input fields
- Conditional override: `if (profile.targetX > 0f) profile.targetX else formula.targetX` for P/C/F
- Fiber/Sodium progress bars ONLY shown when target > 0f
- MacroProgressRow `unit` parameter for "mg" on Sodium
- All new Float writes wrapped in existing `sanitizeFloat()` 

### Must NOT Have (Guardrails)
- **NO changes to Save button `enabled` condition** — target fields are optional, must not block save
- **NO deletion or modification of `macroTargetsForGoal()` function body** — stays as formula fallback
- **NO changes to `calculateWeeklyConsistency()` logic** — consistency score stays protein-only
- **NO changes to `DashboardViewModel.Factory` constructor signature** — profileManager already present
- **NO changes to `MainActivity.kt`** — all wiring already exists
- **NO new dependencies in build.gradle.kts**
- **NO refactoring OutlinedTextField into reusable composable** — copy the existing pattern exactly
- **NO fixing the multiple-dot validation bug** — pre-existing, keep consistent with current pattern
- **NO unit test files or test dependency additions** — verification via compile + existing test suite
- **NO calorie target field** — calories are derived (P×4 + C×4 + F×9), not a separate target
- **NO input range validation** (e.g., max protein 500g) — none exists today, don't add it
- **NO "Reset to Formula Defaults" button** — out of scope

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: YES (minimal — only ExampleUnitTest with 2+2=4)
- **Automated tests**: None (no ViewModel tests, no Compose tests, no test utilities)
- **Framework**: JUnit (for existing trivial test only)
- **Approach**: Compilation verification + structural grep assertions + existing test suite pass

### QA Policy
Every task includes agent-executed QA scenarios using Bash commands (gradlew, grep, ast_grep_search).
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Compilation**: `gradlew.bat assembleDebug` — exit code 0
- **Structural**: `grep` counts for expected patterns (field counts, function counts, keyword presence)
- **Regression**: `gradlew.bat testDebugUnitTest` — exit code 0 (existing tests still pass)

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — foundation):
└── Task 1: Expand UserProfile data class + DataStore keys [quick]

Wave 2 (After Wave 1 — run BOTH in parallel):
├── Task 2: ProfileViewModel + ProfileScreen target inputs [unspecified-high]
└── Task 3: DashboardViewModel + DashboardScreen analytics wiring [unspecified-high]

Wave 3 (After Wave 2 — verification):
└── Task 4: Integration verification + compile check [quick]

Wave FINAL (After ALL tasks — independent review):
├── Task F1: Plan compliance audit [oracle]
├── Task F2: Code quality review [unspecified-high]
├── Task F3: Real manual QA [unspecified-high]
└── Task F4: Scope fidelity check [deep]

Critical Path: Task 1 → Task 2 (or 3) → Task 4
Parallel Speedup: ~25% (Wave 2 runs 2 tasks simultaneously)
Max Concurrent: 2 (Wave 2)
```

### Dependency Matrix

| Task | Depends On | Blocks | Wave |
|------|-----------|--------|------|
| 1 | — | 2, 3 | 1 |
| 2 | 1 | 4 | 2 |
| 3 | 1 | 4 | 2 |
| 4 | 2, 3 | F1-F4 | 3 |
| F1-F4 | 4 | — | FINAL |

### Agent Dispatch Summary

- **Wave 1**: **1 task** — T1 → `quick`
- **Wave 2**: **2 tasks** — T2 → `unspecified-high`, T3 → `unspecified-high`
- **Wave 3**: **1 task** — T4 → `quick`
- **Wave FINAL**: **4 tasks** — F1 → `oracle`, F2 → `unspecified-high`, F3 → `unspecified-high`, F4 → `deep`

---

## TODOs

- [ ] 1. Expand UserProfile Data Class & UserProfileManager DataStore Keys

  **What to do**:
  1. Add 5 new fields to `UserProfile` data class (after existing 4 fields):
     - `targetProtein: Float = 0f`
     - `targetCarbs: Float = 0f`
     - `targetFats: Float = 0f`
     - `targetFiber: Float = 0f`
     - `targetSodium: Float = 0f`
  2. Add 5 new keys to the private `Keys` object (after existing 4 keys):
     - `val TARGET_PROTEIN = floatPreferencesKey("target_protein")`
     - `val TARGET_CARBS = floatPreferencesKey("target_carbs")`
     - `val TARGET_FATS = floatPreferencesKey("target_fats")`
     - `val TARGET_FIBER = floatPreferencesKey("target_fiber")`
     - `val TARGET_SODIUM = floatPreferencesKey("target_sodium")`
  3. Update the `userProfile` Flow mapping (lines 35-42) to read new keys with `?: 0f` defaults:
     - `targetProtein = prefs[Keys.TARGET_PROTEIN] ?: 0f`
     - Same pattern for all 5 fields
  4. Update `updateProfile()` (lines 68-75) to write all 5 new fields inside the existing `edit {}` block:
     - `prefs[Keys.TARGET_PROTEIN] = sanitizeFloat(profile.targetProtein)`
     - Same pattern for all 5 fields
  5. Do NOT add individual `updateTargetX()` functions — only `updateProfile()` is used

  **Must NOT do**:
  - Do NOT rename or reorder existing fields
  - Do NOT add DataStore migration code (new keys simply return defaults)
  - Do NOT add individual update functions for each target

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single file, straightforward data class + key additions following existing pattern exactly
  - **Skills**: `[]`
    - No specialized skills needed — pure Kotlin DataStore key additions

  **Parallelization**:
  - **Can Run In Parallel**: NO (foundation task)
  - **Parallel Group**: Wave 1 (solo)
  - **Blocks**: Tasks 2, 3
  - **Blocked By**: None (can start immediately)

  **References**:

  **Pattern References** (existing code to follow):
  - `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt:16-21` — UserProfile data class: copy field declaration pattern (`val x: Float = 0f`)
  - `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt:28-33` — Keys object: copy `floatPreferencesKey("key_name")` pattern
  - `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt:35-42` — userProfile Flow: copy `prefs[Keys.X] ?: 0f` pattern for each new field
  - `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt:68-75` — updateProfile(): copy `prefs[Keys.X] = sanitizeFloat(profile.x)` pattern for each new Float field

  **WHY Each Reference Matters**:
  - Lines 16-21: Shows exact constructor parameter format with default values
  - Lines 28-33: Shows the floatPreferencesKey naming convention (snake_case strings)
  - Lines 35-42: Shows how to read from DataStore with null-coalescing defaults
  - Lines 68-75: Shows atomic write block with sanitizeFloat wrapping

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: UserProfile data class has 9 fields
    Tool: Bash (grep)
    Preconditions: Task 1 changes applied to UserProfileManager.kt
    Steps:
      1. Run: grep -c "val target" app/src/main/java/com/example/savagestats/data/UserProfileManager.kt
      2. Assert: output is "5"
      3. Run: grep -c "val " app/src/main/java/com/example/savagestats/data/UserProfileManager.kt (within data class)
      4. Verify UserProfile has age, weight, height, goal + 5 target fields
    Expected Result: 5 target fields present, 9 total fields in UserProfile
    Failure Indicators: Count less than 5, or missing field names
    Evidence: .sisyphus/evidence/task-1-userprofile-fields.txt

  Scenario: DataStore keys expanded to 9
    Tool: Bash (grep)
    Preconditions: Same file modified
    Steps:
      1. Run: grep -c "PreferencesKey" app/src/main/java/com/example/savagestats/data/UserProfileManager.kt
      2. Assert: output is "9" (4 existing: int+float+float+string + 5 new float keys)
    Expected Result: 9 preference key declarations
    Failure Indicators: Count not 9
    Evidence: .sisyphus/evidence/task-1-datastore-keys.txt

  Scenario: updateProfile writes all 9 fields
    Tool: Bash (grep)
    Preconditions: Same file modified
    Steps:
      1. Run: grep "prefs\[Keys\." app/src/main/java/com/example/savagestats/data/UserProfileManager.kt | wc -l
      2. Assert: count is 9 (within updateProfile function)
    Expected Result: 9 prefs write lines in updateProfile
    Failure Indicators: Count not 9
    Evidence: .sisyphus/evidence/task-1-update-profile-writes.txt

  Scenario: Compilation succeeds
    Tool: Bash
    Preconditions: All changes saved
    Steps:
      1. Run: gradlew.bat assembleDebug
      2. Assert: exit code 0, output contains "BUILD SUCCESSFUL"
    Expected Result: Clean compilation
    Failure Indicators: BUILD FAILED, any compile errors
    Evidence: .sisyphus/evidence/task-1-compile.txt
  ```

  **Commit**: YES
  - Message: `feat(profile): add custom macro target fields to UserProfile and DataStore`
  - Files: `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt`
  - Pre-commit: `gradlew.bat assembleDebug`

- [ ] 2. Add Target Input Fields to ProfileViewModel & ProfileScreen

  **What to do**:

  **ProfileViewModel.kt changes:**
  1. Add 5 new MutableStateFlow/StateFlow pairs for target inputs (after existing input state, around line 34):
     - `private val _targetProteinInput = MutableStateFlow("")` / `val targetProteinInput: StateFlow<String> = _targetProteinInput.asStateFlow()`
     - Same for: `targetCarbsInput`, `targetFatsInput`, `targetFiberInput`, `targetSodiumInput`
  2. Add 5 `onTargetXChanged` handler functions following the existing `onWeightChanged` pattern (lines 70-73):
     ```kotlin
     fun onTargetProteinChanged(value: String) {
         if (value.all { it.isDigit() || it == '.' }) {
             _targetProteinInput.value = value
         }
     }
     ```
     Same pattern for all 5 target fields.
  3. Update the `init` block pre-fill (lines 44-62) to also populate target inputs:
     ```kotlin
     if (_targetProteinInput.value.isEmpty() && profile.targetProtein > 0f) {
         _targetProteinInput.value = profile.targetProtein.toString()
     }
     ```
     Same pattern for all 5. Note: only pre-fill if > 0f (0f means "unset").
  4. Update `saveProfile()` (lines 86-103) to include targets in the UserProfile constructor:
     ```kotlin
     profileManager.updateProfile(
         UserProfile(
             age = age,
             weight = weight,
             height = height,
             goal = goal,
             targetProtein = _targetProteinInput.value.toFloatOrNull() ?: 0f,
             targetCarbs = _targetCarbsInput.value.toFloatOrNull() ?: 0f,
             targetFats = _targetFatsInput.value.toFloatOrNull() ?: 0f,
             targetFiber = _targetFiberInput.value.toFloatOrNull() ?: 0f,
             targetSodium = _targetSodiumInput.value.toFloatOrNull() ?: 0f
         )
     )
     ```

  **ProfileScreen.kt changes:**
  5. Add 5 new state collections at the top of ProfileScreen composable (after line 56):
     ```kotlin
     val targetProteinInput by viewModel.targetProteinInput.collectAsStateWithLifecycle()
     val targetCarbsInput by viewModel.targetCarbsInput.collectAsStateWithLifecycle()
     val targetFatsInput by viewModel.targetFatsInput.collectAsStateWithLifecycle()
     val targetFiberInput by viewModel.targetFiberInput.collectAsStateWithLifecycle()
     val targetSodiumInput by viewModel.targetSodiumInput.collectAsStateWithLifecycle()
     ```
  6. Add a "DAILY TARGETS" `ElevatedCard` section BETWEEN the Goal selector section (ends at line 239) and the Save button (starts at line 242). Use the exact same card styling as the "BODY METRICS" card (lines 101-189):
     ```kotlin
     ElevatedCard(
         shape = RoundedCornerShape(16.dp),
         colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
         elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
         modifier = Modifier.fillMaxWidth()
     ) {
         Column(
             modifier = Modifier.padding(20.dp),
             verticalArrangement = Arrangement.spacedBy(16.dp)
         ) {
             Text(
                 text = "DAILY TARGETS",
                 style = MaterialTheme.typography.titleMedium.copy(
                     fontWeight = FontWeight.Bold,
                     letterSpacing = 1.5.sp
                 ),
                 color = SavageRed
             )
             Text(
                 text = "SET YOUR OWN BAR. OR DON'T. DEFAULTS AREN'T FOR QUITTERS.",
                 style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                 color = TextMuted
             )
             // ... 5 OutlinedTextFields below
         }
     }
     ```
  7. Add 5 OutlinedTextFields inside the card for: PROTEIN (G), CARBS (G), FATS (G), FIBER (G), SODIUM (MG). Each follows the exact same styling as the existing Body Metrics fields (lines 124-144). Use `KeyboardType.Decimal`. Placeholders: "e.g. 180", "e.g. 250", "e.g. 70", "e.g. 30", "e.g. 2300". Wire each to its corresponding `viewModel::onTargetXChanged`.
  8. **CRITICAL**: Do NOT modify the Save button `enabled` condition on lines 247-248. Target fields are optional.

  **Must NOT do**:
  - Do NOT change Save button enabled guard
  - Do NOT refactor OutlinedTextField into a reusable composable
  - Do NOT add input range validation
  - Do NOT modify ProfileSummaryCard
  - Do NOT fix the multi-dot validation bug

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Two files modified with substantial UI additions (~100 lines), requires precise pattern matching against existing code
  - **Skills**: `[]`
    - No specialized skills needed — pure Kotlin/Compose following existing patterns verbatim

  **Skills Evaluated but Omitted**:
  - `brainstorming`: Design decisions already made in planning session
  - `frontend-design`: Not a design task — copying existing visual patterns exactly
  - `frontend-ui-ux`: Same reason — no creative UI decisions

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 3)
  - **Parallel Group**: Wave 2 (with Task 3)
  - **Blocks**: Task 4
  - **Blocked By**: Task 1

  **References**:

  **Pattern References** (existing code to follow):
  - `app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt:21-34` — MutableStateFlow + asStateFlow() input state declaration pattern. Copy exactly for 5 new target inputs.
  - `app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt:44-62` — Init block pre-fill pattern: `if (input.value.isEmpty() && profile.field > 0f) { input.value = profile.field.toString() }`. Copy for 5 target fields.
  - `app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt:70-73` — onWeightChanged validation: `if (value.all { it.isDigit() || it == '.' })`. Copy for all 5 onTargetXChanged handlers.
  - `app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt:86-103` — saveProfile() builds UserProfile and calls profileManager.updateProfile(). Add 5 new fields to UserProfile constructor.
  - `app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt:101-189` — ElevatedCard with OutlinedTextFields: exact card shape, colors, elevation, padding, text field styling. Copy for "DAILY TARGETS" card.
  - `app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt:124-144` — Single OutlinedTextField with full color config: focusedBorderColor=SavageRed, unfocusedBorderColor=DarkSurfaceVariant, etc. Copy for each target field.
  - `app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt:247-248` — Save button enabled condition: `ageInput.isNotEmpty() && weightInput.isNotEmpty() && heightInput.isNotEmpty() && selectedGoal.isNotEmpty()`. DO NOT CHANGE THIS.

  **API/Type References**:
  - `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt:16-21` — UserProfile data class with 5 new target fields (from Task 1). Used in saveProfile() constructor.

  **WHY Each Reference Matters**:
  - ProfileViewModel patterns: Ensures identical state management approach — no architectural deviation
  - ProfileScreen card/field patterns: Ensures pixel-perfect visual consistency with existing UI
  - Save button guard: CRITICAL guardrail — accidentally adding target checks would break UX for existing users

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: ProfileViewModel has 5 target input StateFlows
    Tool: Bash (grep)
    Preconditions: Task 2 changes applied
    Steps:
      1. Run: grep -c "targetProteinInput\|targetCarbsInput\|targetFatsInput\|targetFiberInput\|targetSodiumInput" app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt
      2. Assert: count >= 10 (each appears as private _x and public x)
    Expected Result: All 5 target input flows declared
    Failure Indicators: Count less than 10
    Evidence: .sisyphus/evidence/task-2-viewmodel-flows.txt

  Scenario: ProfileViewModel has 5 onTargetXChanged handlers
    Tool: Bash (grep)
    Preconditions: Same file modified
    Steps:
      1. Run: grep -c "fun onTarget.*Changed" app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt
      2. Assert: count is 5
    Expected Result: 5 onChange handler functions
    Failure Indicators: Count not 5
    Evidence: .sisyphus/evidence/task-2-viewmodel-handlers.txt

  Scenario: ProfileViewModel saveProfile includes all targets
    Tool: Bash (grep)
    Preconditions: Same file modified
    Steps:
      1. Run: grep "targetProtein\|targetCarbs\|targetFats\|targetFiber\|targetSodium" app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt
      2. Assert: targetProtein, targetCarbs, targetFats, targetFiber, targetSodium all appear in saveProfile context
    Expected Result: All 5 targets in UserProfile constructor within saveProfile()
    Failure Indicators: Any target missing from saveProfile
    Evidence: .sisyphus/evidence/task-2-save-profile.txt

  Scenario: ProfileScreen has DAILY TARGETS section with 5 fields
    Tool: Bash (grep)
    Preconditions: ProfileScreen.kt modified
    Steps:
      1. Run: grep -c "DAILY TARGETS" app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt
      2. Assert: count >= 1
      3. Run: grep -c "OutlinedTextField" app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt
      4. Assert: count >= 8 (3 existing + 5 new)
    Expected Result: "DAILY TARGETS" section present with 5 new text fields
    Failure Indicators: Missing section header or field count < 8
    Evidence: .sisyphus/evidence/task-2-profile-screen-fields.txt

  Scenario: Save button enabled condition UNCHANGED
    Tool: Bash (grep)
    Preconditions: ProfileScreen.kt modified
    Steps:
      1. Run: grep -A2 "enabled =" app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt
      2. Assert: condition still only checks ageInput, weightInput, heightInput, selectedGoal
      3. Assert: NO target field names appear in enabled condition
    Expected Result: Save button guard unchanged — targets are optional
    Failure Indicators: Any "target" string near "enabled ="
    Evidence: .sisyphus/evidence/task-2-save-button-guard.txt

  Scenario: Compilation succeeds
    Tool: Bash
    Preconditions: All Task 2 changes saved
    Steps:
      1. Run: gradlew.bat assembleDebug
      2. Assert: exit code 0, output contains "BUILD SUCCESSFUL"
    Expected Result: Clean compilation
    Failure Indicators: BUILD FAILED
    Evidence: .sisyphus/evidence/task-2-compile.txt
  ```

  **Commit**: YES
  - Message: `feat(profile): add daily target input section to Profile screen`
  - Files: `app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt`, `app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt`
  - Pre-commit: `gradlew.bat assembleDebug`

- [ ] 3. Wire Custom Targets into Dashboard Analytics (ViewModel + Screen)

  **What to do**:

  **DashboardViewModel.kt changes:**
  1. Expand `AnalyticsUiState` data class (lines 333-342) with 4 new fields:
     - `todayFiber: Float = 0f`
     - `todaySodium: Float = 0f`
     - `fiberTarget: Float = 0f`
     - `sodiumTarget: Float = 0f`
  2. Update the `analyticsUiState` combine block (lines 99-119) to add conditional target overrides AFTER the existing `val targets = macroTargetsForGoal(...)` call:
     ```kotlin
     val effectiveProteinTarget = if (profile.targetProtein > 0f) profile.targetProtein else targets.proteinTarget
     val effectiveCarbsTarget = if (profile.targetCarbs > 0f) profile.targetCarbs else targets.carbsTarget
     val effectiveFatsTarget = if (profile.targetFats > 0f) profile.targetFats else targets.fatsTarget
     val effectiveFiberTarget = profile.targetFiber   // no formula fallback — 0 means "don't show"
     val effectiveSodiumTarget = profile.targetSodium  // no formula fallback — 0 means "don't show"
     ```
  3. Update the `AnalyticsUiState(...)` construction in the combine block to use effective targets:
     - `proteinTarget = effectiveProteinTarget`
     - `carbsTarget = effectiveCarbsTarget`
     - `fatsTarget = effectiveFatsTarget`
     - `fiberTarget = effectiveFiberTarget`
     - `sodiumTarget = effectiveSodiumTarget`
     - `todayFiber = todayLog?.fiber ?: 0f`
     - `todaySodium = todayLog?.sodium ?: 0f`
  4. Update the `calculateWeeklyConsistency` call to pass `effectiveProteinTarget` instead of `targets.proteinTarget`:
     - `val weeklyConsistency = calculateWeeklyConsistency(recentLogs, effectiveProteinTarget)`
  5. Do NOT modify `macroTargetsForGoal()` function body (lines 344-373)
  6. Do NOT modify `calculateWeeklyConsistency()` logic (lines 375-379)

  **DashboardScreen.kt changes:**
  7. Add `unit: String = "g"` parameter to `MacroProgressRow` composable (line 585-588):
     ```kotlin
     @Composable
     private fun MacroProgressRow(
         label: String,
         current: Float,
         target: Float,
         unit: String = "g",
     )
     ```
  8. Update the text in MacroProgressRow (line 595) to use the `unit` parameter:
     - From: `"$label: ${formatFloatValue(current)}g / ${formatFloatValue(safeTarget)}g"`
     - To: `"$label: ${formatFloatValue(current)}$unit / ${formatFloatValue(safeTarget)}$unit"`
  9. Add conditional Fiber progress bar in the "TODAY'S FUEL" card (after existing Fats row, around line 514):
     ```kotlin
     if (analytics.fiberTarget > 0f) {
         MacroProgressRow(
             label = "Fiber",
             current = analytics.todayFiber,
             target = analytics.fiberTarget
         )
     }
     ```
  10. Add conditional Sodium progress bar (after Fiber):
      ```kotlin
      if (analytics.sodiumTarget > 0f) {
          MacroProgressRow(
              label = "Sodium",
              current = analytics.todaySodium,
              target = analytics.sodiumTarget,
              unit = "mg"
          )
      }
      ```

  **Must NOT do**:
  - Do NOT delete or modify `macroTargetsForGoal()` function body
  - Do NOT modify `calculateWeeklyConsistency()` logic
  - Do NOT change DashboardViewModel.Factory constructor
  - Do NOT modify the Savage Score or Weekly Activity sections
  - Do NOT touch the WeeklyActivityBars composable

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Two files modified, ViewModel logic change + conditional UI additions, requires understanding of combine flow
  - **Skills**: `[]`
    - No specialized skills needed — ViewModel logic + 2 conditional Compose rows

  **Skills Evaluated but Omitted**:
  - `brainstorming`: Logic decisions already made
  - `systematic-debugging`: Not debugging, building
  - `frontend-design`: Minimal UI addition (2 conditional rows copying existing pattern)

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 2)
  - **Parallel Group**: Wave 2 (with Task 2)
  - **Blocks**: Task 4
  - **Blocked By**: Task 1

  **References**:

  **Pattern References** (existing code to follow):
  - `app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt:99-119` — analyticsUiState combine block: this is the EXACT block being modified. Study the flow: 4 inputs combined → targets computed → AnalyticsUiState constructed. Insert conditional overrides between target computation and state construction.
  - `app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt:333-342` — AnalyticsUiState data class: add 4 new fields here following the exact pattern of existing fields.
  - `app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt:344-373` — macroTargetsForGoal() function: DO NOT MODIFY. This is the formula fallback. Read it to understand what values it produces so you can correctly wire the conditional override.
  - `app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt:375-379` — calculateWeeklyConsistency(): DO NOT MODIFY logic. Only change the argument passed to it (use effectiveProteinTarget).
  - `app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt:500-514` — Existing MacroProgressRow calls for Protein, Carbs, Fats. Add Fiber and Sodium calls after Fats (line 514) with conditional rendering.
  - `app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt:584-608` — MacroProgressRow composable: add `unit` parameter, update text format string on line 595.

  **API/Type References**:
  - `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt:16-21` — UserProfile data class (from Task 1). The combine block reads `profile.targetProtein`, `profile.targetCarbs`, etc.
  - `app/src/main/java/com/example/savagestats/data/DailyLog.kt` — DailyLog entity: has `fiber: Float` and `sodium: Float` fields. Accessed via `todayLog?.fiber`.

  **WHY Each Reference Matters**:
  - Combine block (99-119): This is the surgical insertion point. Must understand the 4-way combine to add overrides correctly.
  - macroTargetsForGoal (344-373): Must NOT be modified — read-only reference to understand fallback values.
  - MacroProgressRow (584-608): The composable being extended with unit parameter. Must preserve existing behavior for P/C/F.
  - DailyLog: Confirms fiber/sodium field names and types for `todayLog?.fiber` access.

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: AnalyticsUiState has fiber/sodium fields
    Tool: Bash (grep)
    Preconditions: Task 3 changes applied to DashboardViewModel.kt
    Steps:
      1. Run: grep "todayFiber\|todaySodium\|fiberTarget\|sodiumTarget" app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt
      2. Assert: all 4 field names appear in AnalyticsUiState data class
    Expected Result: 4 new fields in AnalyticsUiState
    Failure Indicators: Any of the 4 fields missing
    Evidence: .sisyphus/evidence/task-3-analytics-state-fields.txt

  Scenario: Combine block uses conditional target override
    Tool: Bash (grep)
    Preconditions: Same file modified
    Steps:
      1. Run: grep "profile.targetProtein" app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt
      2. Assert: at least one line shows conditional override pattern (if profile.targetProtein > 0f)
      3. Run: grep "effectiveProteinTarget\|effectiveCarbsTarget\|effectiveFatsTarget" app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt
      4. Assert: all 3 effective target variables present
    Expected Result: Conditional override logic present in combine block
    Failure Indicators: Missing profile.targetX references or effectiveX variables
    Evidence: .sisyphus/evidence/task-3-conditional-override.txt

  Scenario: macroTargetsForGoal function body UNCHANGED
    Tool: Bash (grep)
    Preconditions: Same file modified
    Steps:
      1. Run: grep -A30 "private fun macroTargetsForGoal" app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt
      2. Assert: function still contains "Build Muscle", "Lose Fat", "Athletic Conditioning", "Endurance" branches
      3. Assert: multiplier values unchanged (e.g., 2.2f, 4.0f, 1.0f for Build Muscle)
    Expected Result: Formula function completely preserved
    Failure Indicators: Any multiplier changed, any branch removed
    Evidence: .sisyphus/evidence/task-3-formula-preserved.txt

  Scenario: DashboardScreen conditionally renders Fiber/Sodium bars
    Tool: Bash (grep)
    Preconditions: DashboardScreen.kt modified
    Steps:
      1. Run: grep "fiberTarget > 0f" app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt
      2. Assert: conditional check present
      3. Run: grep "sodiumTarget > 0f" app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt
      4. Assert: conditional check present
      5. Run: grep -c "MacroProgressRow" app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt
      6. Assert: count >= 5 (3 original + Fiber + Sodium)
    Expected Result: Fiber and Sodium bars conditionally rendered
    Failure Indicators: Missing conditionals or MacroProgressRow count < 5
    Evidence: .sisyphus/evidence/task-3-conditional-bars.txt

  Scenario: MacroProgressRow has unit parameter
    Tool: Bash (grep)
    Preconditions: DashboardScreen.kt modified
    Steps:
      1. Run: grep "unit: String" app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt
      2. Assert: parameter declaration found with default "g"
      3. Run: grep 'unit = "mg"' app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt
      4. Assert: Sodium call passes unit = "mg"
    Expected Result: Unit parameter exists with "g" default, Sodium uses "mg"
    Failure Indicators: Missing unit parameter or Sodium still using "g"
    Evidence: .sisyphus/evidence/task-3-unit-parameter.txt

  Scenario: Compilation succeeds
    Tool: Bash
    Preconditions: All Task 3 changes saved
    Steps:
      1. Run: gradlew.bat assembleDebug
      2. Assert: exit code 0, output contains "BUILD SUCCESSFUL"
    Expected Result: Clean compilation
    Failure Indicators: BUILD FAILED
    Evidence: .sisyphus/evidence/task-3-compile.txt
  ```

  **Commit**: YES
  - Message: `feat(dashboard): wire custom targets into analytics progress bars`
  - Files: `app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt`, `app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt`
  - Pre-commit: `gradlew.bat assembleDebug`

- [ ] 4. Final Integration Verification & Compile Check

  **What to do**:
  1. Run `gradlew.bat assembleDebug` to verify full compilation with all changes
  2. Run `gradlew.bat testDebugUnitTest` to verify existing tests still pass
  3. Verify no `TODO`, `FIXME`, or placeholder comments were left behind in modified files
  4. Verify UserProfile field count = 9 (4 original + 5 targets)
  5. Verify AnalyticsUiState field count >= 11 (7 original + 4 new: todayFiber, todaySodium, fiberTarget, sodiumTarget)
  6. Verify updateProfile() in UserProfileManager writes exactly 9 fields
  7. Verify Save button enabled condition has NOT changed
  8. Spot-check: macroTargetsForGoal() function body is unchanged

  **Must NOT do**:
  - Do NOT make any code changes — this is verification only
  - Do NOT add test files

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Pure verification task — run commands, grep files, report results
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (depends on all prior tasks)
  - **Parallel Group**: Wave 3 (solo)
  - **Blocks**: F1-F4 (Final Verification Wave)
  - **Blocked By**: Tasks 2, 3

  **References**:

  **Files to Verify**:
  - `app/src/main/java/com/example/savagestats/data/UserProfileManager.kt` — 9 fields in data class, 9 keys in Keys object, 9 writes in updateProfile()
  - `app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt` — 5 target input flows, 5 onTargetXChanged handlers, targets in saveProfile()
  - `app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt` — DAILY TARGETS section, >= 8 OutlinedTextFields, save button guard unchanged
  - `app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt` — conditional overrides in combine block, 4 new AnalyticsUiState fields, formula function unchanged
  - `app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt` — conditional Fiber/Sodium bars, unit parameter on MacroProgressRow

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Full project compiles
    Tool: Bash
    Steps:
      1. Run: gradlew.bat assembleDebug
      2. Assert: exit code 0, "BUILD SUCCESSFUL"
    Expected Result: Clean build
    Evidence: .sisyphus/evidence/task-4-compile.txt

  Scenario: Existing tests pass
    Tool: Bash
    Steps:
      1. Run: gradlew.bat testDebugUnitTest
      2. Assert: exit code 0, "BUILD SUCCESSFUL"
    Expected Result: No test regression
    Evidence: .sisyphus/evidence/task-4-tests.txt

  Scenario: No TODOs or FIXMEs left behind
    Tool: Bash (grep)
    Steps:
      1. Run: grep -rn "TODO\|FIXME\|HACK\|XXX" app/src/main/java/com/example/savagestats/data/UserProfileManager.kt app/src/main/java/com/example/savagestats/ui/ProfileViewModel.kt app/src/main/java/com/example/savagestats/ui/ProfileScreen.kt app/src/main/java/com/example/savagestats/ui/DashboardViewModel.kt app/src/main/java/com/example/savagestats/ui/DashboardScreen.kt
      2. Assert: no matches (or only pre-existing ones unrelated to Phase 9)
    Expected Result: Clean code, no placeholders
    Evidence: .sisyphus/evidence/task-4-no-todos.txt

  Scenario: All structural assertions pass
    Tool: Bash (grep)
    Steps:
      1. Verify UserProfile has 5 target fields
      2. Verify Keys object has 5 TARGET_ keys
      3. Verify ProfileScreen has >= 8 OutlinedTextFields
      4. Verify DashboardScreen has >= 5 MacroProgressRow calls
      5. Verify Save button guard only checks age/weight/height/goal
    Expected Result: All counts match expectations
    Evidence: .sisyphus/evidence/task-4-structural.txt
  ```

  **Commit**: NO (verification only — no code changes)

---

## Final Verification Wave

> 4 review agents run in PARALLEL. ALL must APPROVE. Rejection → fix → re-run.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, grep for pattern). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in .sisyphus/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `gradlew.bat assembleDebug` + `gradlew.bat testDebugUnitTest`. Review all changed files for: `as Any`, `@Suppress` (beyond existing ones), empty catches, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic variable names.
  Output: `Build [PASS/FAIL] | Tests [PASS/FAIL] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high`
  Start from clean state. Verify compilation succeeds. Use grep/ast_grep_search to verify: (1) ProfileScreen has DAILY TARGETS section with 5 OutlinedTextFields, (2) DashboardViewModel combine block references profile.targetProtein, (3) DashboardScreen conditionally renders fiber/sodium bars, (4) MacroProgressRow has unit parameter. Save evidence to `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual changes (git diff). Verify 1:1 — everything in spec was built, nothing beyond spec was built. Check "Must NOT do" compliance. Detect cross-task contamination. Flag unaccounted changes.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

| After Task | Message | Files | Pre-commit Check |
|-----------|---------|-------|------------------|
| Task 1 | `feat(profile): add custom macro target fields to UserProfile and DataStore` | `UserProfileManager.kt` | `gradlew.bat assembleDebug` |
| Task 2 | `feat(profile): add daily target input section to Profile screen` | `ProfileViewModel.kt`, `ProfileScreen.kt` | `gradlew.bat assembleDebug` |
| Task 3 | `feat(dashboard): wire custom targets into analytics progress bars` | `DashboardViewModel.kt`, `DashboardScreen.kt` | `gradlew.bat assembleDebug` |

---

## Success Criteria

### Verification Commands
```bash
gradlew.bat assembleDebug       # Expected: BUILD SUCCESSFUL
gradlew.bat testDebugUnitTest   # Expected: BUILD SUCCESSFUL (1 test passes)
```

### Final Checklist
- [ ] UserProfile has 9 fields (4 original + 5 targets)
- [ ] UserProfileManager Keys object has 9 keys (4 original + 5 targets)
- [ ] updateProfile() writes all 9 fields with sanitizeFloat() on floats
- [ ] ProfileScreen has "DAILY TARGETS" ElevatedCard with 5 OutlinedTextFields
- [ ] Save button `enabled` condition unchanged (still only checks age/weight/height/goal)
- [ ] DashboardViewModel uses conditional override: custom target > 0 → custom, else → formula
- [ ] AnalyticsUiState has todayFiber, todaySodium, fiberTarget, sodiumTarget fields
- [ ] DashboardScreen shows Fiber bar only when fiberTarget > 0
- [ ] DashboardScreen shows Sodium bar only when sodiumTarget > 0
- [ ] MacroProgressRow accepts `unit` parameter, Sodium shows "mg"
- [ ] macroTargetsForGoal() function body unchanged
- [ ] calculateWeeklyConsistency() logic unchanged
- [ ] No new dependencies in build.gradle.kts
- [ ] No changes to MainActivity.kt
