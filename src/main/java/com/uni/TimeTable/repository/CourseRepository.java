package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.models.CourseDefinition;
import com.uni.TimeTable.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE (:schoolId IS NULL OR cd.department.school.id = :schoolId) " +
            "AND (:departmentId IS NULL OR cd.department.id = :departmentId) " +
            "AND (:year IS NULL OR cd.year = :year)")
    List<Course> findBySchoolIdAndDepartmentIdAndYear(
            @Param("schoolId") Long schoolId,
            @Param("departmentId") Long departmentId,
            @Param("year") Integer year);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE cd.department.id = :departmentId " +
            "AND cd.year = :year " +
            "AND c.status = :status")
    List<Course> findByDepartmentIdAndYearAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("year") Integer year,
            @Param("status") String status);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE cd.department.id = :departmentId " +
            "AND c.status = :status")
    List<Course> findByDepartmentIdAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("status") String status);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE cd.year = :year " +
            "AND c.status = :status")
    List<Course> findByYearAndStatus(
            @Param("year") Integer year,
            @Param("status") String status);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE c.status = :status")
    List<Course> findByStatus(@Param("status") String status);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE cd.lecturer.id = :lecturerId " +
            "AND c.dayOfWeek = :dayOfWeek " +
            "AND ((c.startTime <= :endTime AND c.endTime >= :startTime) OR (c.startTime >= :startTime AND c.endTime <= :endTime))")
    List<Course> findByLecturerAndDayOfWeekAndTimeOverlap(
            @Param("lecturerId") Long lecturerId,
            @Param("dayOfWeek") Course.DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE c.room = :room " +
            "AND c.dayOfWeek = :dayOfWeek " +
            "AND ((c.startTime <= :endTime AND c.endTime >= :startTime) OR (c.startTime >= :startTime AND c.endTime <= :endTime))")
    List<Course> findByDayOfWeekAndRoomAndTimeOverlap(
            @Param("room") Room room,
            @Param("dayOfWeek") Course.DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE c.courseDefinition = :courseDefinition")
    Optional<Course> findByCourseDefinition(
            @Param("courseDefinition") CourseDefinition courseDefinition);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.courseDefinition cd " +
            "JOIN FETCH cd.lecturer " +
            "LEFT JOIN FETCH c.room " +
            "WHERE c.id = :id")
    Optional<Course> findByIdWithFetch(@Param("id") Long id);

    @Query("SELECT c FROM Course c WHERE c.status = :status")
    List<Course> findByStatus(Course.CourseInstanceStatus status);

    // Updated method name to reflect the relationship path
    @Query("SELECT c FROM Course c WHERE c.courseDefinition.department.id = :departmentId AND c.courseDefinition.year = :year AND c.status = :status")
    List<Course> findByCourseDefinitionDepartmentIdAndCourseDefinitionYearAndStatus(
            Long departmentId, Integer year, Course.CourseInstanceStatus status
    );

    // Updated method name to reflect the relationship path
    @Query("SELECT c FROM Course c WHERE c.courseDefinition.department.id = :departmentId AND c.status = :status")
    List<Course> findByCourseDefinitionDepartmentIdAndStatus(
            Long departmentId, Course.CourseInstanceStatus status
    );

    // Updated method name to reflect the relationship path
    @Query("SELECT c FROM Course c WHERE c.courseDefinition.year = :year AND c.status = :status")
    List<Course> findByCourseDefinitionYearAndStatus(
            Integer year, Course.CourseInstanceStatus status
    );
}