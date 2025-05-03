package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Coordinator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoordinatorRepository extends JpaRepository<Coordinator, Long> {
    @Query("SELECT c FROM Coordinator c WHERE c.username = :username")
    Optional<Coordinator> findByUsername(String username);

    @Query("SELECT c FROM Coordinator c LEFT JOIN FETCH c.assignments WHERE c.username = :username")
    Optional<Coordinator> findByUsernameWithAssignments(String username);
}