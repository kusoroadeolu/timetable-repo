package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByDepartmentIdAndYear(Long departmentId, int year);

    @Query("SELECT c FROM Course c WHERE c.dayOfWeek = :dayOfWeek AND c.location = :location " +
            "AND ((c.startTime < :endTime AND c.endTime > :startTime) OR " +
            "(c.startTime = :startTime AND c.endTime = :endTime))")
    List<Course> findConflictingCourses(String startTime, String endTime, String dayOfWeek, String location);
}