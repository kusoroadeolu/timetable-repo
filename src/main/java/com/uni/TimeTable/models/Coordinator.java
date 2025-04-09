package com.uni.TimeTable.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "coordinators")
public class Coordinator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // Plain text for demo; use BCrypt in production

    @OneToMany(mappedBy = "coordinator")
    private List<CoordinatorAssignment> assignments;

    // Default constructor
    public Coordinator() {}

    // Constructor
    public Coordinator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<CoordinatorAssignment> getAssignments() { return assignments; }
    public void setAssignments(List<CoordinatorAssignment> assignments) { this.assignments = assignments; }
}