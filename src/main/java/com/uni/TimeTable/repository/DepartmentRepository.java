package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    List<Department> findBySchoolId(Long schoolId);

    @Query("SELECT d FROM Department d WHERE LOWER(d.name) = LOWER(:name)")
    Optional<Department> findByNameIgnoreCase(@Param("name") String name);
}