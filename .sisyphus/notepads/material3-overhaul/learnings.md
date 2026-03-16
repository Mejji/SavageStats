# Material 3 Overhaul — Learnings & Conventions

## Design Patterns (Material 3)

### Cards
- Use `ElevatedCard` with default elevation (not `Card` with border)
- `RoundedCornerShape(16.dp)` for rounded corners
- Apply `Modifier.fillMaxWidth()` for responsive width

### Typography
- Standard sentence case (not ALL CAPS)
- Balanced font weights: `headlineLarge` for headers, `bodyLarge` for body text
- Remove excessive letter-spacing

### Buttons
- `Button` (filled) for primary actions
- `OutlinedButton` for secondary actions
- Use `Modifier.fillMaxWidth()` for full-width buttons
- Default `RoundedCornerShape(12.dp)` from Material 3

### Input Fields
- `OutlinedTextField` with Material 3 defaults
- `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)` for numeric inputs
- Use `fillMaxWidth()` for responsive inputs

### Activity Selectors
- Replace custom pill chips with `FilterChip`
- `selected` parameter controls state
- `onClick` callback for selection

### Colors
- Use Material 3 semantic colors: `primary`, `secondary`, `surface`, `background`, `surfaceVariant`
- Avoid hardcoded color references

### Spacing
- Generous padding: 24.dp around screen edges
- 16.dp between major sections
- 8.dp between related items

---

## Existing Codebase Patterns

### ViewModel Integration
- ViewModels already use `StateFlow` for UI state
- Collect flows with `.collectAsState()` in Composables
- ViewModel factories already implemented as inner `Factory` classes

### Navigation
- 3-tab bottom NavigationBar in MainActivity.kt
- Direct routing to screens (no NavHost, just conditional display)
- Selection state managed in MainActivity

### Room Database
- DailyLog entity: date, proteinGrams, activityDurationMinutes, activityType, sleepHours
- Repository pattern: LogRepository wraps DailyLogDao

### DataStore
- UserProfileManager exposes `Flow<UserProfile>` (age, weight, height, goal)
- Converted to StateFlow in ViewModels

---

## Known Issues

### Markdown Rendering
- AI Coach responses include markdown bold markers `**text**` showing as literal text
- Need to strip or properly render markdown in CoachScreen.kt response display
