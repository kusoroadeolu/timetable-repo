package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Optional<Building> findByName(String buildingName);
}
