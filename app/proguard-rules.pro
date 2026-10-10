# Shizuku
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }

# Keep newProcess for reflection
-keepclassmembers class rikka.shizuku.Shizuku {
    *** newProcess(...);
}
