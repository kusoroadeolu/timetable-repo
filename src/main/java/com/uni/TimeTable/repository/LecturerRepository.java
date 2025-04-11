package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
}
