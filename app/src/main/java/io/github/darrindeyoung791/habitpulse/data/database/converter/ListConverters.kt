package io.github.darrindeyoung791.habitpulse.data.database.converter

import androidx.room.TypeConverter
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod
import org.json.JSONArray

/**
 * 列表类型转换器
 * 用于将 List<Int> 和 List<String> 转换为 JSON 字符串存储
 */
class ListConverters {
    
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }
    
    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val result = mutableListOf<Int>()
        val jsonArray = JSONArray(value)
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getInt(i))
        }
        return result
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val result = mutableListOf<String>()
        val jsonArray = JSONArray(value)
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }
}
