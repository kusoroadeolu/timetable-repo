package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.models.CourseDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseDefinitionRepository extends JpaRepository<CourseDefinition, Long> {
    Optional<CourseDefinition> findByCode(String code);
    List<CourseDefinition> findByDepartmentId(Long departmentId);
    List<CourseDefinition> findByYear(Integer year);
    List<CourseDefinition> findByDepartmentIdAndYear(Long departmentId, Integer year);

    @Query("SELECT DISTINCT c.courseDefinition FROM Course c WHERE c.courseDefinition.department.id = :departmentId " +
            "AND c.courseDefinition.year = :year " +
            "AND c.status = :status")
    List<CourseDefinition> findCourseDefinitionsByDepartmentIdAndYearAndCourseStatus(
            @Param("departmentId") Long departmentId,
            @Param("year") Integer year,
            @Param("status") Course.CourseInstanceStatus status);
}