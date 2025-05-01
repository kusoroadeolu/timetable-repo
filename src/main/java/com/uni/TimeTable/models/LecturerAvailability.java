package com.uni.TimeTable.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "lecturer_availability")
@Getter
@Setter
public class LecturerAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Course.DayOfWeek dayOfWeek; // Fixed: Use enum

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
}