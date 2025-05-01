package com.uni.TimeTable.repository;

import com.uni.TimeTable.models.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
}