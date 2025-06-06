package com.example.mobileappproyect_android.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // Formato estándar

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun fromSubscriptionStatus(status: SubscriptionStatus?): String? {
        return status?.name // Almacena el nombre del enum como String
    }

    @TypeConverter
    fun toSubscriptionStatus(statusString: String?): SubscriptionStatus? {
        return statusString?.let { SubscriptionStatus.valueOf(it) }
    }
}