package io.github.darrindeyoung791.habitpulse.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用户偏好设置存储
 * 
 * 使用 DataStore 存储用户设置，支持异步读取和写入
 */

// DataStore 实例（扩展属性）
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * 用户偏好设置键
 */
object PreferencesKeys {
    /**
     * 是否开启开屏广告以支持 HabitPulse
     */
    val SHOW_SPLASH_AD = booleanPreferencesKey("show_splash_ad")
}

/**
 * 用户偏好设置管理器
 * 
 * 提供单例访问入口，封装 DataStore 操作
 */
class UserPreferences(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: UserPreferences? = null
        
        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * 是否显示开屏广告的 Flow
     * 默认值为 false（不显示）
     */
    val showSplashAdFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_SPLASH_AD] ?: false
    }
    
    /**
     * 设置是否显示开屏广告
     * 
     * @param show true 为显示，false 为不显示
     */
    suspend fun setShowSplashAd(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SPLASH_AD] = show
        }
    }
}
