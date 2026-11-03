package com.emanuel5014.trainable.data.repository

import androidx.room.withTransaction
import com.emanuel5014.trainable.data.local.GymDatabase
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(private val database: GymDatabase) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = database.withTransaction { block() }
}
