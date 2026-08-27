package com.josem.misgastos.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun movementTypeToString(value: MovementType): String = value.name
    @TypeConverter fun stringToMovementType(value: String): MovementType = MovementType.valueOf(value)
    @TypeConverter fun reviewStatusToString(value: ReviewStatus): String = value.name
    @TypeConverter fun stringToReviewStatus(value: String): ReviewStatus = ReviewStatus.valueOf(value)
}
