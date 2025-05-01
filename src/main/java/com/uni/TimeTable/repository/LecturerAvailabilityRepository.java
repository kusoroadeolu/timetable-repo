package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.LecturerAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LecturerAvailabilityRepository extends JpaRepository<LecturerAvailability, Long> {
    @Query("SELECT la FROM LecturerAvailability la WHERE la.lecturer.id = :lecturerId")
    List<LecturerAvailability> findByLecturerId(Long lecturerId);
}