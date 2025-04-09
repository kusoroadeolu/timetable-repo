package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Student, Long> {
    Student findByUsername(String username);
}