# ==========================================
# U Copy ProGuard Rules
# ==========================================

# Keep the model classes used for Kotlin Serialization (JSON)
-keep class com.umkt.ucopy.model.** { *; }
-keep class com.umkt.ucopy.data.** { *; }
-keep class com.umkt.ucopy.utils.BackupHelper$BackupData { *; }

# Kotlin Serialization Rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.umkt.ucopy.**$$serializer { *; }
-keepclassmembers class com.umkt.ucopy.** {
    *** Companion;
}
-keepclasseswithmembers class com.umkt.ucopy.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# Markwon (Markdown Library)
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Compose Markdown
-keep class dev.jeziellago.compose.markdowntext.** { *; }

# About Libraries
-keep class com.mikepenz.aboutlibraries.** { *; }
-dontwarn com.mikepenz.aboutlibraries.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# AndroidX Core
-keep class androidx.core.app.CoreComponentFactory { *; }

# General attributes (keep annotations and line numbers for debugging)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile


# ---- U Copy release keep rules ----
-keep class com.umkt.ucopy.ui.NoteWidgetProvider { *; }
-keep class com.umkt.ucopy.ui.WidgetTodoService { *; }
-keep class com.umkt.ucopy.ui.WidgetConfigActivity { *; }
-keep class com.umkt.ucopy.ui.WidgetAddTodoActivity { *; }
-keep class com.umkt.ucopy.ui.QuickSettingsTileService { *; }
-keep class com.umkt.ucopy.ui.OpenAppTileService { *; }
-keep class com.umkt.ucopy.ui.BootReceiver { *; }
-keep class com.umkt.ucopy.ui.ClipboardListenerService { *; }

# kotlinx.serialization
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes *Annotation*, InnerClasses

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
