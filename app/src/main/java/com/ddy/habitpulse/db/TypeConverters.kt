package com.ddy.habitpulse.db

import androidx.room.TypeConverter
import com.ddy.habitpulse.enums.RepeatCycle
import com.ddy.habitpulse.enums.SupervisionMethod
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HabitTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromRepeatCycle(value: RepeatCycle): String {
        return value.name
    }

    @TypeConverter
    fun toRepeatCycle(value: String): RepeatCycle {
        return RepeatCycle.valueOf(value)
    }

    @TypeConverter
    fun fromSupervisionMethod(value: SupervisionMethod): String {
        return value.name
    }

    @TypeConverter
    fun toSupervisionMethod(value: String): SupervisionMethod {
        return SupervisionMethod.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
    }
}