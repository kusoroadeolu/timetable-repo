package com.uni.TimeTable.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
public class Activity {
    @Id
    @GeneratedValue
    private Integer id;

    @Column(
            nullable = false,
            updatable = false
    )
    private String description;

    @Column(
            updatable = false,
            nullable = false
    )
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    private String timestamp;

    public Activity(String description){
        this.description = description;
    }

}
