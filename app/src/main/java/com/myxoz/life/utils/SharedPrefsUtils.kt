package com.myxoz.life.utils
import android.content.SharedPreferences
import kotlin.reflect.KClass

// ChatGPT SharedPrefs util
object SharedPrefsUtils {
    fun <T: Any> SharedPreferences.get(key: String, clazz: KClass<T>): T? {
        return when (clazz) {
            String::class -> getString(key, null) as? T?
            Int::class -> if (contains(key)) getInt(key, 0) as? T else null
            Long::class -> if (contains(key)) getLong(key, 0L) as? T else null
            Float::class -> if (contains(key)) getFloat(key, 0f) as? T else null
            Boolean::class -> if (contains(key)) getBoolean(key, false) as? T else null
            Set::class -> getStringSet(key, null) as? T?
            else -> throw IllegalArgumentException("Unsupported type $clazz")
        }
    }

    fun <T: Any> SharedPreferences.Editor.put(key: String, new: T, clazz: KClass<T>) {
        when (clazz) {
            String::class -> putString(key, new as String)
            Int::class -> putInt(key, new as Int)
            Long::class -> putLong(key, new as Long)
            Float::class -> putFloat(key, new as Float)
            Boolean::class -> putBoolean(key, new as Boolean)
            Set::class -> putStringSet(key, new as Set<String>)
            else -> throw IllegalArgumentException("Unsupported type $clazz")
        }
    }

    inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
        val editor = edit()
        editor.block()
        editor.apply()
    }
}
