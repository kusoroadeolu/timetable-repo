package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    List<Lecturer> findByName(String lecturerName);
    Optional<Lecturer> findByEmail(String lecturerEmail);
}