package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.OneRepMaxDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.PlanLastSession
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** The routines as a list: reading them, creating, ordering, archiving and deleting them. */
class PlanRepository(
    private val workoutDao: WorkoutDao,
    private val userDao: UserDao,
    private val oneRepMaxDao: OneRepMaxDao
) {
    fun getAllPlansWithDetails(): Flow<List<PlanWithDetails>> = workoutDao.getAllPlansWithDetails()

    suspend fun savePlan(plan: WorkoutPlanEntity): Long = workoutDao.insertPlan(plan)

    suspend fun savePlans(plans: List<WorkoutPlanEntity>) = workoutDao.updatePlans(plans)

    suspend fun updatePlan(plan: WorkoutPlanEntity) = workoutDao.updatePlan(plan)

    suspend fun setPlanActive(planId: Int, active: Boolean) = workoutDao.setPlanActive(planId, active)

    /** Deletes the routine; its workouts stay in the history, moved to the hidden "Custom Workout" plan. */
    suspend fun deletePlan(plan: WorkoutPlanEntity) {
        val user = userDao.getUser().first()
        if (user != null) {
            val allPlans = workoutDao.getAllPlans().first()
            val customPlan = allPlans.find { it.nome == "Custom Workout" && it.note == "SYSTEM_PLAN" }
                ?: allPlans.find { it.nome == "Custom Workout" && it.note == null && !it.isActive } // Legacy
                ?: run {
                    val newId = workoutDao.insertPlan(
                        WorkoutPlanEntity(
                            userId = user.id,
                            nome = "Custom Workout",
                            note = "SYSTEM_PLAN",
                            dataInizio = Clock.System.now().toEpochMilliseconds(),
                            isActive = false
                        )
                    )
                    workoutDao.getAllPlans().first().find { it.id == newId.toInt() }!!
                }

            if (customPlan.note == null) {
                workoutDao.updatePlan(customPlan.copy(note = "SYSTEM_PLAN"))
            }

            workoutDao.reassignSessions(plan.id, customPlan.id, plan.nome)
        }
        workoutDao.deletePlan(plan)
        workoutDao.deleteEmptyRoutineGroups()
    }

    // Routine groups

    fun getRoutineGroups(): Flow<List<RoutineGroupEntity>> = workoutDao.getRoutineGroups()

    fun getLastSessionTimes(): Flow<List<PlanLastSession>> = workoutDao.getLastSessionTimes()

    /** Creates a group at the end of the list and moves [planIds] into it; returns its id. */
    suspend fun createRoutineGroup(name: String, colorRole: Int, customColor: Int?, planIds: List<Int>): Int {
        val groups = workoutDao.getRoutineGroups().first()
        val id = workoutDao.insertRoutineGroup(
            RoutineGroupEntity(
                name = name,
                ordine = (groups.maxOfOrNull { it.ordine } ?: -1) + 1,
                colorRole = colorRole,
                customColor = customColor
            )
        ).toInt()
        moveRoutinesToGroup(planIds, id)
        return id
    }

    /** Moves [planIds] into the group [groupId], or out of any group when it is null; empty groups go. */
    suspend fun moveRoutinesToGroup(planIds: List<Int>, groupId: Int?) {
        if (planIds.isEmpty()) return
        workoutDao.setPlansGroup(planIds, groupId)
        workoutDao.deleteEmptyRoutineGroups()
    }

    suspend fun updateRoutineGroup(group: RoutineGroupEntity) = workoutDao.updateRoutineGroups(listOf(group))

    /** Saves [orderedGroups] with their place in the list as their order. */
    suspend fun reorderRoutineGroups(orderedGroups: List<RoutineGroupEntity>) =
        workoutDao.updateRoutineGroups(orderedGroups.mapIndexed { index, group -> group.copy(ordine = index) })

    /** Archives (or brings back) every routine of the group; they stay in it. */
    suspend fun setRoutineGroupActive(groupId: Int, active: Boolean) = workoutDao.setRoutineGroupActive(groupId, active)

    /** Takes every routine out of the group and removes it; the routines themselves stay. */
    suspend fun dissolveRoutineGroup(groupId: Int) {
        workoutDao.clearRoutineGroup(groupId)
        workoutDao.deleteEmptyRoutineGroups()
    }

    /** What a shared file of these routines could carry besides the routines themselves. */
    data class ExportOptions(val hasImages: Boolean, val hasMaximums: Boolean)

    suspend fun exportOptionsFor(planIds: List<Int>): ExportOptions {
        val plans = workoutDao.getPlansWithDetails(planIds)
        val hasImages = plans.any { it.plan.imageUri != null || it.images.isNotEmpty() }
        val hasMaximums = plans.any { plan ->
            plan.exercises.any { it.isAdvanced && oneRepMaxDao.getCurrentOnce(it.exercise.id) != null }
        }
        return ExportOptions(hasImages = hasImages, hasMaximums = hasMaximums)
    }
}
