package com.lanlan3292.resolutionchanger

import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"

    data class DisplayState(
        val width: Int,
        val height: Int,
        val density: Int
    )

    private var newProcessMethod: Method? = null

    init {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            newProcessMethod = method
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve newProcess", e)
        }
    }

    fun isReady(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0
    }

    fun requestPermission(requestCode: Int = 1001) {
        if (Shizuku.isPreV11()) return
        if (Shizuku.checkSelfPermission() != 0) {
            Shizuku.requestPermission(requestCode)
        }
    }

    private fun createProcess(command: String): Process? {
        val method = newProcessMethod ?: return null
        return try {
            val cmd = arrayOf("sh", "-c", command)
            method.invoke(null, cmd, null, null) as? Process
        } catch (e: Exception) {
            Log.e(TAG, "newProcess invoke failed", e)
            null
        }
    }

    suspend fun exec(command: String): String = suspendCoroutine { cont ->
        try {
            if (!isReady()) {
                cont.resume("ERROR: Shizuku not ready")
                return@suspendCoroutine
            }
            val process = createProcess(command)
            if (process == null) {
                cont.resume("ERROR: failed to create process")
                return@suspendCoroutine
            }
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            process.waitFor()
            process.destroy()
            val result = buildString {
                append(stdout.trim())
                if (stderr.isNotBlank()) {
                    if (isNotEmpty()) append('\n')
                    append(stderr.trim())
                }
            }
            cont.resume(result.ifBlank { "OK" })
        } catch (e: Exception) {
            Log.e(TAG, "exec failed", e)
            cont.resume("ERROR: ${e.message}")
        }
    }

    /**
     * 读取当前逻辑分辨率与密度。
     * wm size 输出示例：
     *   Physical size: 1440x3200
     *   Override size: 1080x2400
     * 优先使用 Override，没有则用 Physical。
     */
    suspend fun getCurrentDisplayState(): DisplayState? {
        val sizeOut = exec("wm size")
        val densityOut = exec("wm density")
        if (sizeOut.startsWith("ERROR") || densityOut.startsWith("ERROR")) {
            return null
        }

        val sizeRegex = Regex("""(?:Override|Physical) size:\s*(\d+)x(\d+)""")
        val densityRegex = Regex("""(?:Override|Physical) density:\s*(\d+)""")

        // 优先 Override
        val overrideSize = Regex("""Override size:\s*(\d+)x(\d+)""").find(sizeOut)
        val physicalSize = Regex("""Physical size:\s*(\d+)x(\d+)""").find(sizeOut)
        val sizeMatch = overrideSize ?: physicalSize

        val overrideDensity = Regex("""Override density:\s*(\d+)""").find(densityOut)
        val physicalDensity = Regex("""Physical density:\s*(\d+)""").find(densityOut)
        val densityMatch = overrideDensity ?: physicalDensity

        if (sizeMatch == null || densityMatch == null) {
            Log.w(TAG, "parse failed size=$sizeOut density=$densityOut")
            return null
        }

        val w = sizeMatch.groupValues[1].toInt()
        val h = sizeMatch.groupValues[2].toInt()
        val d = densityMatch.groupValues[1].toInt()
        return DisplayState(w, h, d)
    }

    suspend fun setResolution(width: Int, height: Int, density: Int): Boolean {
        val r1 = exec("wm size ${width}x${height}")
        val r2 = exec("wm density $density")
        Log.i(TAG, "size: $r1 | density: $r2")
        return !r1.startsWith("ERROR") && !r2.startsWith("ERROR")
    }
}
