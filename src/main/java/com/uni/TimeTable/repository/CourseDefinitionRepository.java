package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.CourseDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseDefinitionRepository extends JpaRepository<CourseDefinition, Long> {
    Optional<CourseDefinition> findByCode(String code);
    List<CourseDefinition> findByDepartmentId(Long departmentId);
    List<CourseDefinition> findByYear(Integer year);
    List<CourseDefinition> findByDepartmentIdAndYear(Long departmentId, Integer year);
}