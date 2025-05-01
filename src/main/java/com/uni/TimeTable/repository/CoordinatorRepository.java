package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Coordinator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CoordinatorRepository extends JpaRepository<Coordinator, Long> {
    @Query("SELECT c FROM Coordinator c WHERE c.username = :username")
    Coordinator findByUsername(String username);
}