package com.emanuel5014.trainable.data.local.relation

/** When a routine was last trained: the timestamp of its last finished workout. */
data class PlanLastSession(
    val planId: Int,
    val lastTimestamp: Long
)
