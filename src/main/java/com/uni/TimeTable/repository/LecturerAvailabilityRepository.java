package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.models.LecturerAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerAvailabilityRepository extends JpaRepository<LecturerAvailability, Long> {
    @Query("SELECT la FROM LecturerAvailability la WHERE la.lecturer.id = :lecturerId")
    List<LecturerAvailability> findByLecturerId(Long lecturerId);

    @Query("SELECT la FROM LecturerAvailability la WHERE la.lecturer.id = :lecturerId AND la.dayOfWeek = :dayOfWeek AND la.startTime = :startTime AND la.endTime = :endTime")
    Optional<LecturerAvailability> findByLecturerIdAndDayOfWeekAndTimes(
            @Param("lecturerId") Long lecturerId,
            @Param("dayOfWeek") Course.DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT la FROM LecturerAvailability la WHERE la.lecturer.id = :lecturerId AND la.dayOfWeek = :dayOfWeek " +
            "AND ((la.startTime <= :endTime AND la.endTime >= :startTime) OR (la.startTime >= :startTime AND la.endTime <= :endTime))")
    List<LecturerAvailability> findOverlappingByLecturerIdAndDayOfWeek(
            @Param("lecturerId") Long lecturerId,
            @Param("dayOfWeek") Course.DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}