# ProGuard / R8 konfiguracija za Peletko

# 1. Zadrži podatke za linije u slučaju crash logova na Play Konzoli
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. ROOM DATABASE
# Zadrži sve podatkovne klase (entitete) i njihova polja tačnim imenima
-keep class com.example.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Zadrži Room anotacije
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# 3. JETPACK COMPOSE
# Compose compiler i runtime pravila
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# 4. COROUTINES
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# 5. OKHTTP & NETWORK
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# 6. MODELI I POMOĆNE KLASE KOJE SE KORISTE U SERIJALIZACIJI / REFLEKSIJI
-keep class com.example.util.IndustryMetrics { *; }
-keep class com.example.util.InventorySummary { *; }
-keep class com.example.data.weather.WeatherResult { *; }
