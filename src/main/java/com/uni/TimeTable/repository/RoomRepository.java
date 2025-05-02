package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCapacityGreaterThanEqual(int capacity);

    Optional<Room> findByName(String name);
    @Query("SELECT r FROM Room r WHERE r.name = :name AND r.building.id = :buildingId")
    Optional<Room> findByNameAndBuildingId(@Param("name") String name, @Param("buildingId") Long buildingId);

    @Query("SELECT r FROM Room r WHERE r.building.id = :buildingId")
    List<Room> findByBuildingId(@Param("buildingId") Long buildingId);

    @Query("SELECT r FROM Room r WHERE r.building.id = :buildingId AND NOT EXISTS (" +
            "SELECT c FROM Course c WHERE c.room = r AND c.dayOfWeek = :dayOfWeek AND " +
            "(c.startTime < :endTime AND c.endTime > :startTime))")
    List<Room> findAvailableRoomsByBuildingAndTime(
            @Param("buildingId") Long buildingId,
            @Param("dayOfWeek") Course.DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}