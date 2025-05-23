package com.cookandroid.challengers.data
//
//import androidx.room.Dao
//import androidx.room.Query
//
////서버로부터 스케줄 받아옴. for삭제를 위해
//@Dao
//interface ExerciseScheduleDao {
//    @Query("""
//        SELECT es.* FROM exercise_schedule es
//        JOIN exercise_reps er ON es.id = er.schedule_id
//        WHERE es.exercise_plan_id = :planId AND er.exercise_id = :exerciseId
//        LIMIT 1
//    """)
//    suspend fun getScheduleByPlanAndExercise(planId: Long, exerciseId: Long): ExerciseSchedule?
//}