package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReportItem(
    val id: String,
    val timestamp: String,
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val lastAction: String,
    val lastCoordinates: String,
    val memoryUsageMb: String,
    val androidVersion: String,
    val deviceModel: String
)

object CrashTracker {
    private const val TAG = "CrashTracker"
    private const val PREF_NAME = "mock_gps_crash_tracker"
    private const val KEY_REPORTS_JSON = "crash_reports_json"
    private const val KEY_LAST_ACTION = "last_user_action"
    private const val KEY_LAST_COORDS = "last_coordinates"
    private const val KEY_TOTAL_CRASH_COUNT = "total_crash_count"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var database: AppDatabase? = null
    private var isInitialized = false

    var currentAction: String = "App Started"
        private set
    var currentCoords: String = "Lat: 0.0, Lng: 0.0"
        private set

    fun init(context: Context) {
        if (isInitialized) return
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        try {
            database = AppDatabase.getDatabase(app)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AppDatabase in CrashTracker", e)
        }
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(throwable, "Uncaught Exception on Thread [${thread.name}]")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record uncaught exception", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        Log.i(TAG, "Global Exception Handler & CrashTracker initialized successfully")
    }

    fun updateState(action: String, coordinates: String? = null) {
        currentAction = action
        coordinates?.let { currentCoords = it }
        prefs?.edit()?.apply {
            putString(KEY_LAST_ACTION, currentAction)
            putString(KEY_LAST_COORDS, currentCoords)
            apply()
        }
    }

    fun recordCrash(throwable: Throwable, causeTag: String = "App Abnormality") {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val fullStackTrace = sw.toString()
        val swTopLines = fullStackTrace.lines().take(25).joinToString("\n")

        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)
        val memStr = "$usedMem MB / $maxMem MB"

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timeStr = dateFormat.format(Date())
        val crashId = "CRASH_${System.currentTimeMillis()}"

        val report = CrashReportItem(
            id = crashId,
            timestamp = timeStr,
            errorType = "${throwable.javaClass.simpleName} ($causeTag)",
            message = throwable.message ?: "No error message provided",
            stackTrace = swTopLines,
            lastAction = currentAction,
            lastCoordinates = currentCoords,
            memoryUsageMb = memStr,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER.capitalize(Locale.US)} ${Build.MODEL}"
        )

        val entity = CrashReportEntity(
            id = crashId,
            timestamp = timeStr,
            errorType = report.errorType,
            message = report.message,
            stackTrace = report.stackTrace,
            lastAction = report.lastAction,
            lastCoordinates = report.lastCoordinates,
            memoryUsageMb = report.memoryUsageMb,
            androidVersion = report.androidVersion,
            deviceModel = report.deviceModel
        )

        // Save synchronously to Room database before process termination
        try {
            database?.crashReportDao()?.insertCrashReportBlocking(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash to Room Database", e)
        }

        saveReport(report)
    }

    fun logNonFatalError(tag: String, message: String, exception: Throwable? = null) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timeStr = dateFormat.format(Date())

        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)

        val sw = StringWriter()
        exception?.printStackTrace(PrintWriter(sw))
        val stack = if (exception != null) sw.toString().lines().take(15).joinToString("\n") else "No stack trace"
        val errorId = "ERR_${System.currentTimeMillis()}"

        val report = CrashReportItem(
            id = errorId,
            timestamp = timeStr,
            errorType = "Warning / Abnormality ($tag)",
            message = message,
            stackTrace = stack,
            lastAction = currentAction,
            lastCoordinates = currentCoords,
            memoryUsageMb = "$usedMem MB / $maxMem MB",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER.capitalize(Locale.US)} ${Build.MODEL}"
        )

        val entity = CrashReportEntity(
            id = errorId,
            timestamp = timeStr,
            errorType = report.errorType,
            message = report.message,
            stackTrace = report.stackTrace,
            lastAction = report.lastAction,
            lastCoordinates = report.lastCoordinates,
            memoryUsageMb = report.memoryUsageMb,
            androidVersion = report.androidVersion,
            deviceModel = report.deviceModel
        )

        // Asynchronously or blocking write to Room database
        try {
            CoroutineScope(Dispatchers.IO).launch {
                database?.crashReportDao()?.insertCrashReport(entity)
            }
        } catch (e: Exception) {
            try {
                database?.crashReportDao()?.insertCrashReportBlocking(entity)
            } catch (dbEx: Exception) {
                Log.e(TAG, "Failed to save error to Room", dbEx)
            }
        }

        saveReport(report)
    }

    private fun saveReport(report: CrashReportItem) {
        val p = prefs ?: return
        try {
            val jsonArrayStr = p.getString(KEY_REPORTS_JSON, "[]") ?: "[]"
            val jsonArray = JSONArray(jsonArrayStr)

            val newObj = JSONObject().apply {
                put("id", report.id)
                put("timestamp", report.timestamp)
                put("errorType", report.errorType)
                put("message", report.message)
                put("stackTrace", report.stackTrace)
                put("lastAction", report.lastAction)
                put("lastCoordinates", report.lastCoordinates)
                put("memoryUsageMb", report.memoryUsageMb)
                put("androidVersion", report.androidVersion)
                put("deviceModel", report.deviceModel)
            }

            // Keep max 30 latest crash reports in prefs cache
            val updatedArray = JSONArray()
            updatedArray.put(newObj)
            for (i in 0 until minOf(jsonArray.length(), 29)) {
                updatedArray.put(jsonArray.getJSONObject(i))
            }

            val count = p.getInt(KEY_TOTAL_CRASH_COUNT, 0) + 1
            p.edit()
                .putString(KEY_REPORTS_JSON, updatedArray.toString())
                .putInt(KEY_TOTAL_CRASH_COUNT, count)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save report JSON", e)
        }
    }

    fun getCrashReports(): List<CrashReportItem> {
        // Try Room database first
        try {
            val dbList = database?.crashReportDao()?.getCrashReportsSnapshot()
            if (!dbList.isNullOrEmpty()) {
                return dbList.map { entity ->
                    CrashReportItem(
                        id = entity.id,
                        timestamp = entity.timestamp,
                        errorType = entity.errorType,
                        message = entity.message,
                        stackTrace = entity.stackTrace,
                        lastAction = entity.lastAction,
                        lastCoordinates = entity.lastCoordinates,
                        memoryUsageMb = entity.memoryUsageMb,
                        androidVersion = entity.androidVersion,
                        deviceModel = entity.deviceModel
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Room snapshot fetch exception, falling back to prefs", e)
        }

        val p = prefs ?: return emptyList()
        val list = mutableListOf<CrashReportItem>()
        try {
            val jsonArrayStr = p.getString(KEY_REPORTS_JSON, "[]") ?: "[]"
            val jsonArray = JSONArray(jsonArrayStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CrashReportItem(
                        id = obj.optString("id", ""),
                        timestamp = obj.optString("timestamp", ""),
                        errorType = obj.optString("errorType", "Unknown"),
                        message = obj.optString("message", ""),
                        stackTrace = obj.optString("stackTrace", ""),
                        lastAction = obj.optString("lastAction", "N/A"),
                        lastCoordinates = obj.optString("lastCoordinates", "N/A"),
                        memoryUsageMb = obj.optString("memoryUsageMb", "N/A"),
                        androidVersion = obj.optString("androidVersion", "N/A"),
                        deviceModel = obj.optString("deviceModel", "N/A")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading crash reports", e)
        }
        return list
    }

    fun getTotalCrashCount(): Int {
        return prefs?.getInt(KEY_TOTAL_CRASH_COUNT, 0) ?: 0
    }

    fun clearReports() {
        try {
            database?.crashReportDao()?.clearCrashReportsBlocking()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear Room crash reports", e)
        }
        prefs?.edit()?.apply {
            remove(KEY_REPORTS_JSON)
            putInt(KEY_TOTAL_CRASH_COUNT, 0)
            apply()
        }
    }

    fun generateFormattedBugReport(): String {
        val reports = getCrashReports()
        val sb = StringBuilder()
        sb.appendLine("=========================================")
        sb.appendLine(" MOCK GPS PRO - SYSTEM DIAGNOSTIC REPORT")
        sb.appendLine(" Generated At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine(" Total Incident Logs: ${reports.size}")
        sb.appendLine(" Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
        sb.appendLine(" Last Tracked Action: $currentAction")
        sb.appendLine(" Last Tracked Target: $currentCoords")
        sb.appendLine(" Storage: Persistent Room DB + Cached SharedPreferences")
        sb.appendLine("=========================================\n")

        if (reports.isEmpty()) {
            sb.appendLine("STATUS: ALL SYSTEMS OPERATIONAL - NO CRASHES DETECTED.")
        } else {
            reports.forEachIndexed { idx, rep ->
                sb.appendLine("--- INCIDENT #${idx + 1} [${rep.timestamp}] ---")
                sb.appendLine("Error Type  : ${rep.errorType}")
                sb.appendLine("Message     : ${rep.message}")
                sb.appendLine("Last Action : ${rep.lastAction}")
                sb.appendLine("Coordinates : ${rep.lastCoordinates}")
                sb.appendLine("Memory Usage: ${rep.memoryUsageMb}")
                sb.appendLine("Device Spec : ${rep.deviceModel} | ${rep.androidVersion}")
                sb.appendLine("Stack Trace :")
                sb.appendLine(rep.stackTrace.indented("  "))
                sb.appendLine("-----------------------------------------\n")
            }
        }
        return sb.toString()
    }

    private fun String.indented(prefix: String): String {
        return this.lines().joinToString("\n") { "$prefix$it" }
    }
}

