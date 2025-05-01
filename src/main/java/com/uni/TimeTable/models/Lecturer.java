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

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<LecturerAvailability> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(List<LecturerAvailability> availabilities) {
        this.availabilities = availabilities;
    }

    public boolean isAvailable(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        Course.DayOfWeek parsedDayOfWeek;
        try {
            parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
        } catch (IllegalArgumentException e) {
            return false; // Invalid day, treat as unavailable
        }
        return isAvailable(parsedDayOfWeek, startTime, endTime);
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