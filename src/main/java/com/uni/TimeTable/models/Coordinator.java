package com.uni.TimeTable.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "coordinator")
@Getter
@Setter
@NoArgsConstructor
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


    // Constructor
    public Coordinator(String username, String password) {
        this.username = username;
        this.password = password;
    }
}