# Material 3 Premium UI Overhaul Plan

**Goal**: Replace brutalist "SAVAGE STATS" design with clean, modern Material 3 aesthetic across all screens.

**Status**: In Progress

---

## Implementation Tasks

### Phase 1: Theme System Redesign
- [ ] T1.1: Update Color.kt with Material 3 color palette (primary, secondary, surface, background)
- [ ] T1.2: Update Type.kt with standard case typography (no ALL CAPS, balanced font weights)
- [ ] T1.3: Update Theme.kt to use Material 3 color scheme properly

**Parallelizable**: No (subsequent tasks depend on theme)
**Dependencies**: None
**File Scope**: `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt`

---

### Phase 2: Dashboard Screen Redesign
- [ ] T2.1: Redesign DashboardScreen.kt with Material 3 components
  - Replace custom headers with standard case typography
  - Use ElevatedCard for summary section
  - Add verticalScroll + generous padding (24.dp)
  - Replace pill chips with Material 3 FilterChip
  - Update button styling to Material 3 filled button
  - Make inputs responsive with fillMaxWidth

**Parallelizable**: No (depends on Phase 1 theme)
**Dependencies**: T1.1, T1.2, T1.3
**File Scope**: `ui/DashboardScreen.kt`

---

### Phase 3: Profile Screen Redesign
- [ ] T3.1: Redesign ProfileScreen.kt with Material 3 components
  - Standard case "Your Stats" header
  - Wrap Age/Weight/Height inputs in single ElevatedCard
  - Responsive OutlinedTextField with proper keyboard types
  - Material 3 segmented button or radio group for Goal
  - Full-width Material 3 filled button for save

**Parallelizable**: Yes (can run parallel with T2.1 after Phase 1)
**Dependencies**: T1.1, T1.2, T1.3
**File Scope**: `ui/ProfileScreen.kt`

---

### Phase 4: AI Coach Screen Redesign
- [ ] T4.1: Redesign CoachScreen.kt with Material 3 components
  - Standard case "The Savage Coach" header with subtitle
  - Weekly logs in scrollable ElevatedCard
  - Deep primary color button for "ROAST MY WEEK" with elevation
  - AI response in distinct Card with surface variant background (chat bubble style)
  - Subtle shadows throughout

**Parallelizable**: Yes (can run parallel with T2.1, T3.1 after Phase 1)
**Dependencies**: T1.1, T1.2, T1.3
**File Scope**: `ui/CoachScreen.kt`

---

### Phase 5: Markdown Rendering Fix
- [ ] T5.1: Fix AI Coach response rendering to strip markdown artifacts (** bold markers showing as literal text)

**Parallelizable**: Yes (independent of UI redesign)
**Dependencies**: None
**File Scope**: `ui/CoachScreen.kt` (response display logic)

---

## Verification Requirements

**After each task**:
- [ ] `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
- [ ] `lsp_diagnostics` → ZERO errors
- [ ] Manual read of changed files → logic matches requirements
- [ ] Visual verification via `/playwright` or manual device testing

**Final acceptance criteria**:
- [ ] All 4 screens use Material 3 design language
- [ ] ElevatedCard, FilterChip, proper elevation/shadows throughout
- [ ] Standard case typography, no ALL CAPS
- [ ] Generous padding and responsive layouts
- [ ] AI Coach responses render clean text without ** artifacts
- [ ] App builds and runs successfully
