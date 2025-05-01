package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCapacityGreaterThanEqual(int capacity);

    Optional<Room> findByName(String name);
}