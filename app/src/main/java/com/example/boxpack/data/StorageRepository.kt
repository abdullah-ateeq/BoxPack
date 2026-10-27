package com.example.boxpack.data

import android.content.Context
import android.content.SharedPreferences
import com.example.boxpack.model.AppSettings
import com.example.boxpack.model.LengthUnit
import com.example.boxpack.model.OrientationMode
import com.example.boxpack.model.SavedCalculation
import com.example.boxpack.model.StackingMode
import org.json.JSONArray
import org.json.JSONObject

class StorageRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("boxpack_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SETTINGS = "app_settings"
        private const val KEY_HISTORY = "history_calculations"
        private const val KEY_LAST_CALC = "last_calc_state"
        private const val MAX_HISTORY_ITEMS = 10
    }

    fun getSettings(): AppSettings {
        val jsonStr = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        return try {
            val json = JSONObject(jsonStr)
            AppSettings(
                defaultUnit = LengthUnit.valueOf(json.optString("defaultUnit", LengthUnit.CENTIMETERS.name)),
                defaultOrientationMode = OrientationMode.valueOf(json.optString("defaultOrientationMode", OrientationMode.AUTOMATIC.name)),
                defaultBoxGapMm = json.optDouble("defaultBoxGapMm", 0.0),
                defaultWallThicknessMm = json.optDouble("defaultWallThicknessMm", 0.0),
                defaultStackingMode = StackingMode.valueOf(json.optString("defaultStackingMode", StackingMode.ALLOWED.name)),
                showContainerDiagram = json.optBoolean("showContainerDiagram", true),
                showPackingGrid = json.optBoolean("showPackingGrid", true),
                showOrientationComparison = json.optBoolean("showOrientationComparison", true),
                showCalculationBreakdown = json.optBoolean("showCalculationBreakdown", true),
                showUtilization = json.optBoolean("showUtilization", true),
                showVolumeComparison = json.optBoolean("showVolumeComparison", true),
                decimalPlaces = json.optInt("decimalPlaces", 2),
                liveCalculation = json.optBoolean("liveCalculation", true),
                haptics = json.optBoolean("haptics", true),
                animations = json.optBoolean("animations", true),
                confirmDelete = json.optBoolean("confirmDelete", true),
                restoreLastCalculation = json.optBoolean("restoreLastCalculation", true),
                themeMode = json.optString("themeMode", "SYSTEM"),
                dynamicColor = json.optBoolean("dynamicColor", true)
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        val json = JSONObject().apply {
            put("defaultUnit", settings.defaultUnit.name)
            put("defaultOrientationMode", settings.defaultOrientationMode.name)
            put("defaultBoxGapMm", settings.defaultBoxGapMm)
            put("defaultWallThicknessMm", settings.defaultWallThicknessMm)
            put("defaultStackingMode", settings.defaultStackingMode.name)
            put("showContainerDiagram", settings.showContainerDiagram)
            put("showPackingGrid", settings.showPackingGrid)
            put("showOrientationComparison", settings.showOrientationComparison)
            put("showCalculationBreakdown", settings.showCalculationBreakdown)
            put("showUtilization", settings.showUtilization)
            put("showVolumeComparison", settings.showVolumeComparison)
            put("decimalPlaces", settings.decimalPlaces)
            put("liveCalculation", settings.liveCalculation)
            put("haptics", settings.haptics)
            put("animations", settings.animations)
            put("confirmDelete", settings.confirmDelete)
            put("restoreLastCalculation", settings.restoreLastCalculation)
            put("themeMode", settings.themeMode)
            put("dynamicColor", settings.dynamicColor)
        }
        prefs.edit().putString(KEY_SETTINGS, json.toString()).apply()
    }

    fun resetSettings(): AppSettings {
        val default = AppSettings()
        saveSettings(default)
        return default
    }

    fun getHistory(): List<SavedCalculation> {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SavedCalculation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedCalculation(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        timestamp = obj.getLong("timestamp"),
                        containerWidthMm = obj.getDouble("containerWidthMm"),
                        containerDepthMm = obj.getDouble("containerDepthMm"),
                        containerHeightMm = obj.getDouble("containerHeightMm"),
                        wallThicknessMm = obj.getDouble("wallThicknessMm"),
                        boxWidthMm = obj.getDouble("boxWidthMm"),
                        boxDepthMm = obj.getDouble("boxDepthMm"),
                        boxHeightMm = obj.getDouble("boxHeightMm"),
                        clearanceWidthMm = obj.getDouble("clearanceWidthMm"),
                        clearanceDepthMm = obj.getDouble("clearanceDepthMm"),
                        clearanceHeightMm = obj.getDouble("clearanceHeightMm"),
                        gapWidthMm = obj.getDouble("gapWidthMm"),
                        gapDepthMm = obj.getDouble("gapDepthMm"),
                        gapHeightMm = obj.getDouble("gapHeightMm"),
                        orientationMode = OrientationMode.valueOf(obj.getString("orientationMode")),
                        capacity = obj.getInt("capacity"),
                        across = obj.getInt("across"),
                        deep = obj.getInt("deep"),
                        layers = obj.getInt("layers"),
                        utilizationPercent = obj.getDouble("utilizationPercent"),
                        unit = LengthUnit.valueOf(obj.optString("unit", LengthUnit.CENTIMETERS.name))
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCalculation(calc: SavedCalculation): List<SavedCalculation> {
        val current = getHistory().toMutableList()
        // If an item with the same id exists, remove it first
        current.removeAll { it.id == calc.id }
        // Prepend newest
        current.add(0, calc)
        // Keep max 10
        val trimmed = if (current.size > MAX_HISTORY_ITEMS) current.take(MAX_HISTORY_ITEMS) else current

        val array = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("timestamp", item.timestamp)
                put("containerWidthMm", item.containerWidthMm)
                put("containerDepthMm", item.containerDepthMm)
                put("containerHeightMm", item.containerHeightMm)
                put("wallThicknessMm", item.wallThicknessMm)
                put("boxWidthMm", item.boxWidthMm)
                put("boxDepthMm", item.boxDepthMm)
                put("boxHeightMm", item.boxHeightMm)
                put("clearanceWidthMm", item.clearanceWidthMm)
                put("clearanceDepthMm", item.clearanceDepthMm)
                put("clearanceHeightMm", item.clearanceHeightMm)
                put("gapWidthMm", item.gapWidthMm)
                put("gapDepthMm", item.gapDepthMm)
                put("gapHeightMm", item.gapHeightMm)
                put("orientationMode", item.orientationMode.name)
                put("capacity", item.capacity)
                put("across", item.across)
                put("deep", item.deep)
                put("layers", item.layers)
                put("utilizationPercent", item.utilizationPercent)
                put("unit", item.unit.name)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
        return trimmed
    }

    fun deleteCalculation(id: String): List<SavedCalculation> {
        val current = getHistory().toMutableList()
        current.removeAll { it.id == id }
        val array = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("timestamp", item.timestamp)
                put("containerWidthMm", item.containerWidthMm)
                put("containerDepthMm", item.containerDepthMm)
                put("containerHeightMm", item.containerHeightMm)
                put("wallThicknessMm", item.wallThicknessMm)
                put("boxWidthMm", item.boxWidthMm)
                put("boxDepthMm", item.boxDepthMm)
                put("boxHeightMm", item.boxHeightMm)
                put("clearanceWidthMm", item.clearanceWidthMm)
                put("clearanceDepthMm", item.clearanceDepthMm)
                put("clearanceHeightMm", item.clearanceHeightMm)
                put("gapWidthMm", item.gapWidthMm)
                put("gapDepthMm", item.gapDepthMm)
                put("gapHeightMm", item.gapHeightMm)
                put("orientationMode", item.orientationMode.name)
                put("capacity", item.capacity)
                put("across", item.across)
                put("deep", item.deep)
                put("layers", item.layers)
                put("utilizationPercent", item.utilizationPercent)
                put("unit", item.unit.name)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
        return current
    }

    fun saveLastCalculationState(jsonString: String) {
        prefs.edit().putString(KEY_LAST_CALC, jsonString).apply()
    }

    fun getLastCalculationState(): String? {
        return prefs.getString(KEY_LAST_CALC, null)
    }
}
