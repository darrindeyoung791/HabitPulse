package io.github.darrindeyoung791.habitpulse.data.database.converter

import androidx.room.TypeConverter
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle

/**
 * 枚举类型转换器
 * 用于将 RepeatCycle 转换为字符串存储
 */
class EnumConverters {
    
    @TypeConverter
    fun fromRepeatCycle(value: RepeatCycle): String {
        return value.name
    }
    
    @TypeConverter
    fun toRepeatCycle(value: String): RepeatCycle {
        return RepeatCycle.valueOf(value)
    }
    
}
