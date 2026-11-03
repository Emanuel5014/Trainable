package com.emanuel5014.trainable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "physical_checks")
data class PhysicalCheckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val peso: Float?,
    val note: String?,
    val fotoFilenames: String // Nomi delle foto salvate sul disco divisi da virgole
)
