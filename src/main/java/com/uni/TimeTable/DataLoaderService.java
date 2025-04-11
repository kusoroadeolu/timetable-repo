package com.uni.TimeTable;

import com.uni.TimeTable.models.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataLoaderService {

    private final EntityManager entityManager;

    public DataLoaderService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void loadInitialData() {
        Department cs = new Department("Computer Science");
        entityManager.persist(cs);

        Coordinator coord1 = new Coordinator("coord1", "password1");
        entityManager.persist(coord1);

        Lecturer lec1 = new Lecturer("Dr. Smith", "smith@university.edu");
        entityManager.persist(lec1);

        CoordinatorAssignment assign1 = new CoordinatorAssignment(coord1, cs, 1);
        entityManager.persist(assign1);

        Course course1 = new Course("CS101", "Introduction to Programming", cs, 1, "09:00", "09:50", "Monday", "Room A",
                coord1, lec1, 3, "http://elearning.university.edu/cs101");
        entityManager.persist(course1);

        Course course2 = new Course("CS102", "Data Structures", cs, 1, "10:00", "10:50", "Tuesday", "Room B",
                coord1, lec1, 3, "http://elearning.university.edu/cs102");
        entityManager.persist(course2);
    }
}