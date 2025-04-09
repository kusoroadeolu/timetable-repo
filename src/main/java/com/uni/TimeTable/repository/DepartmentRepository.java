package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    //Find a department by its name
    public void findByName(String name);
}
