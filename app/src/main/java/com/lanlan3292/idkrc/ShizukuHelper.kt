package com.lanlan3292.resolutionchanger

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.lanlan3292.resolutionchanger.service.IShellService
import com.lanlan3292.resolutionchanger.service.ShellUserService
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"
    private var shellService: IShellService? = null
    private var isBinding = false

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            "com.lanlan3292.resolutionchanger",
            ShellUserService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(false)
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "UserService connected")
            shellService = IShellService.Stub.asInterface(binder)
            isBinding = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "UserService disconnected")
            shellService = null
            isBinding = false
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
        if (shellService != null || isBinding) return
        if (!Shizuku.pingBinder()) return
        isBinding = true
        try {
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (e: Exception) {
            Log.e(TAG, "bindUserService failed", e)
            isBinding = false
        }
    }

    fun unbindUserService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, connection, true)
        } catch (_: Exception) {
        }
        shellService = null
    }

    suspend fun exec(command: String): String = suspendCoroutine { cont ->
        val service = shellService
        if (service == null) {
            cont.resume("ERROR: UserService not connected")
            return@suspendCoroutine
        }
        try {
            val result = service.exec(command)
            cont.resume(result)
        } catch (e: Exception) {
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
        return !r1.startsWith("ERROR") && !r2.startsWith("ERROR")
    }
}
