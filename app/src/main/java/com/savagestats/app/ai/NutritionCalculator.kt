package com.savagestats.app.ai

/**
 * Mifflin-St Jeor TDEE + macro target engine.
 *
 * No gender field exists in UserProfile yet, so we use a gender-neutral
 * midpoint BMR (average of male +5 / female -161 offsets = -78).
 * This can be refined once a sex field is added to the profile.
 */
object NutritionCalculator {

    // Activity multiplier constants — no activity level in profile yet,
    // so we default to "lightly active" (1.375) as a safe baseline.
    const val ACTIVITY_SEDENTARY = 1.2f
    const val ACTIVITY_LIGHT = 1.375f
    const val ACTIVITY_MODERATE = 1.55f
    const val ACTIVITY_ACTIVE = 1.725f
    const val ACTIVITY_VERY_ACTIVE = 1.9f

    /**
     * Mifflin-St Jeor BMR (gender-neutral midpoint).
     * Male:   (10 * kg) + (6.25 * cm) - (5 * age) + 5
     * Female: (10 * kg) + (6.25 * cm) - (5 * age) - 161
     * Midpoint offset: (5 + -161) / 2 = -78
     */
    fun calculateBMR(weightKg: Float, heightCm: Float, age: Int): Float {
        if (weightKg <= 0f || heightCm <= 0f || age <= 0) return 0f
        return (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 78f
    }

    /**
     * TDEE adjusted for goal-specific calorie surplus/deficit.
     * Custom goals fall back to maintenance (TDEE) as a safe baseline.
     */
    fun calculateDailyCalories(
        bmr: Float,
        activityMultiplier: Float = ACTIVITY_LIGHT,
        goal: String
    ): Float {
        if (bmr <= 0f) return 0f
        val tdee = bmr * activityMultiplier
        val normalized = goal.lowercase()
        return when {
            normalized.contains("lose fat")          -> tdee - 500f
            normalized.contains("build muscle")      -> tdee + 300f
            normalized.contains("athletic") ||
            normalized.contains("endurance")         -> tdee + 200f
            normalized.contains("maintenance")       -> tdee
            else                                     -> tdee  // Custom → maintenance baseline
        }
    }

    /**
     * Macro split from target calories + body weight.
     *   Protein : 2.2 g/kg body weight
     *   Fats    : 25 % of total calories  (9 kcal/g)
     *   Carbs   : remaining calories      (4 kcal/g)
     */
    fun calculateTargetMacros(
        targetCalories: Float,
        weightKg: Float
    ): MacroTargets {
        if (targetCalories <= 0f || weightKg <= 0f) {
            return MacroTargets(0f, 0f, 0f)
        }
        val protein = weightKg * 2.2f
        val fatCalories = targetCalories * 0.25f
        val fats = fatCalories / 9f
        val carbCalories = (targetCalories - (protein * 4f) - fatCalories).coerceAtLeast(0f)
        val carbs = carbCalories / 4f
        return MacroTargets(protein = protein, carbs = carbs, fats = fats)
    }

    data class MacroTargets(
        val protein: Float,
        val carbs: Float,
        val fats: Float,
    )
}
