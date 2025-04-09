package com.uni.TimeTable;

import com.uni.TimeTable.models.Coordinator;
import com.uni.TimeTable.models.CoordinatorAssignment;
import com.uni.TimeTable.models.Department;
import com.uni.TimeTable.models.Student;
import com.uni.TimeTable.models.Course;
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
        Department ee = new Department("Electrical Engineering");
        entityManager.persist(cs);
        entityManager.persist(ee);

        Coordinator coord1 = new Coordinator("coord1", "password1");
        Coordinator coord2 = new Coordinator("coord2", "password2");
        Student student = new Student("student1", cs, 1);
        entityManager.persist(coord1);
        entityManager.persist(coord2);
        entityManager.persist(student);

        CoordinatorAssignment assign1 = new CoordinatorAssignment(coord1, cs, 1);
        CoordinatorAssignment assign2 = new CoordinatorAssignment(coord2, ee, 2);
        entityManager.persist(assign1);
        entityManager.persist(assign2);

        Course course1 = new Course("CS101", cs, 1, "09:00", "09:50", "Monday", "Room A", coord1);
        Course course2 = new Course("EE201", ee, 2, "10:00", "10:50", "Tuesday", "Room B", coord2);
        entityManager.persist(course1);
        entityManager.persist(course2);

        System.out.println("Test data loaded!");
    }
}