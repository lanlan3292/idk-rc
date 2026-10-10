# Shizuku
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class com.lanlan3292.resolutionchanger.service.ShellUserService { *; }
-keepclassmembers class * extends android.os.Binder {
    public static final *** DESCRIPTOR;
    public *** asBinder();
}
