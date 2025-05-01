package com.uni.TimeTable.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coordinator_assignment")
@Getter
@Setter
@NoArgsConstructor
public class CoordinatorAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "coordinator_id", nullable = false)
    private Coordinator coordinator;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private int year;

    // Constructor
    public CoordinatorAssignment(Coordinator coordinator, Department department, int year) {
        this.coordinator = coordinator;
        this.department = department;
        this.year = year;
    }

}