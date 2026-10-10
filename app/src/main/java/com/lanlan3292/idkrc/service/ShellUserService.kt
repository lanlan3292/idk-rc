package com.lanlan3292.resolutionchanger.service

import android.util.Log
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader

@Keep
class ShellUserService : IShellService.Stub() {

    companion object {
        private const val TAG = "ShellUserService"
    }

    constructor() {
        Log.i(TAG, "created, uid=${android.os.Process.myUid()}")
    }

    override fun exec(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            process.waitFor()
            if (stderr.isNotBlank()) Log.w(TAG, "stderr: $stderr")
            stdout.trim() + if (stderr.isNotBlank()) "\n$stderr" else ""
        } catch (e: Exception) {
            Log.e(TAG, "exec failed", e)
            "ERROR: ${e.message}"
        }
    }

    override fun destroy() {
        Log.i(TAG, "destroy")
        System.exit(0)
    }
}
