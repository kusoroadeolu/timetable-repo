package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Optional<Building> findByName(String buildingName);

    @Query("SELECT b FROM Building b WHERE LOWER(b.name) = LOWER(:name)")
    Optional<Building> findByNameIgnoreCase(@Param("name") String name);

}
