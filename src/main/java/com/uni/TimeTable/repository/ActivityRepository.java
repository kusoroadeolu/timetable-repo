package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Integer> {

    @Query("SELECT a FROM Activity a WHERE a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<Activity> findAllRecentActivities(@Param("startDate")LocalDateTime startDate);

}
