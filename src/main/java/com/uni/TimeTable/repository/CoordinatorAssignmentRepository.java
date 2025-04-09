package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Coordinator;
import com.uni.TimeTable.models.CoordinatorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoordinatorAssignmentRepository extends JpaRepository<CoordinatorAssignment, Long> {
    List<CoordinatorAssignment> findByCoordinator(Coordinator coordinator);
}