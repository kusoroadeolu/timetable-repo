package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Coordinator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoordinatorRepository extends JpaRepository<Coordinator, Long> {
    Coordinator findByUsername(String username);
}