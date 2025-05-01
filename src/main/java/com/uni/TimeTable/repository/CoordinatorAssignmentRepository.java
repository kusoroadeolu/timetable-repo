package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.CoordinatorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoordinatorAssignmentRepository extends JpaRepository<CoordinatorAssignment, Long> {

    @Query("SELECT ca FROM CoordinatorAssignment ca JOIN ca.coordinator c WHERE c.username = :username")
    List<CoordinatorAssignment> findByUsername(@Param("username") String username);
}