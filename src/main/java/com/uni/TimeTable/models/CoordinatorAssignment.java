package com.uni.TimeTable.models;

import jakarta.persistence.*;

@Entity
@Table(name = "coordinator_assignments")
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

    // Default constructor
    public CoordinatorAssignment() {}

    // Constructor
    public CoordinatorAssignment(Coordinator coordinator, Department department, int year) {
        this.coordinator = coordinator;
        this.department = department;
        this.year = year;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Coordinator getCoordinator() { return coordinator; }
    public void setCoordinator(Coordinator coordinator) { this.coordinator = coordinator; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}