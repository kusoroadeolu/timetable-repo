package com.uni.TimeTable.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lecturer")
@Getter
@Setter
public class Lecturer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "lecturer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LecturerAvailability> availabilities = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LecturerType type;

    public enum LecturerType {
        FULL_TIME,
        ADJUNCT
    }

    public boolean isAvailable(Course.DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        for (LecturerAvailability availability : availabilities) {
            if (availability.getDayOfWeek() == dayOfWeek) {
                LocalTime availStart = availability.getStartTime();
                LocalTime availEnd = availability.getEndTime();
                if (!startTime.isBefore(availStart) && !endTime.isAfter(availEnd)) {
                    return true;
                }
            }
        }
        return false;
    }
}