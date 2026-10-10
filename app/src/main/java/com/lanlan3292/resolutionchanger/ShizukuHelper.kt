package com.lanlan3292.resolutionchanger

import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 通过反射调用 Shizuku.newProcess 执行 shell 命令。
 * 不依赖 AIDL / UserService，避免 aidl 编译问题。
 */
object ShizukuHelper {

    private const val TAG = "ShizukuHelper"

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

    fun bindUserService() {
        // no-op（反射方案无需 bind）
    }

    fun unbindUserService() {
        // no-op
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

    suspend fun setResolution(width: Int, height: Int, density: Int): Boolean {
        val r1 = exec("wm size ${width}x${height}")
        val r2 = exec("wm density $density")
        Log.i(TAG, "size: $r1 | density: $r2")
        return !r1.startsWith("ERROR") && !r2.startsWith("ERROR")
    }

    suspend fun resetResolution(): Boolean {
        val r1 = exec("wm size reset")
        val r2 = exec("wm density reset")
        Log.i(TAG, "reset size: $r1 | density: $r2")
        return !r1.startsWith("ERROR") && !r2.startsWith("ERROR")
    }
}
