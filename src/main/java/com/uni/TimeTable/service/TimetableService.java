package com.uni.TimeTable.service;

import com.uni.TimeTable.models.Coordinator;
import com.uni.TimeTable.models.CoordinatorAssignment;
import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.models.Department;
import com.uni.TimeTable.repository.CoordinatorAssignmentRepository;
import com.uni.TimeTable.repository.CoordinatorRepository;
import com.uni.TimeTable.repository.CourseRepository;
import com.uni.TimeTable.repository.DepartmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.uni.TimeTable.exception.ConflictException;

import java.util.List;

@Service
public class TimetableService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final CoordinatorRepository coordinatorRepository;
    private final CoordinatorAssignmentRepository assignmentRepository;

    public TimetableService(CourseRepository courseRepository,
                            DepartmentRepository departmentRepository,
                            CoordinatorRepository coordinatorRepository,
                            CoordinatorAssignmentRepository assignmentRepository) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.coordinatorRepository = coordinatorRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public Course scheduleCourse(String courseName, Long departmentId, int year,
                                 String startTime, String endTime, String dayOfWeek,
                                 String location, Authentication auth)
            throws IllegalArgumentException, ConflictException {
        Coordinator coordinator = coordinatorRepository.findByUsername(auth.getName());
        if (coordinator == null) {
            throw new IllegalArgumentException("Invalid coordinator");
        }

        if (!startTime.matches("\\d{2}:\\d{2}") || !endTime.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Time must be in HH:MM format (e.g., 09:00)");
        }
        int startMinute = Integer.parseInt(startTime.split(":")[0]) * 60 + Integer.parseInt(startTime.split(":")[1]);
        int endMinute = Integer.parseInt(endTime.split(":")[0]) * 60 + Integer.parseInt(endTime.split(":")[1]);
        if (startMinute >= endMinute) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        List<CoordinatorAssignment> assignments = assignmentRepository.findByCoordinator(coordinator);
        System.out.println("Coordinator: " + coordinator.getUsername() + ", Assignments: " + assignments.size());
        for (CoordinatorAssignment a : assignments) {
            System.out.println("Assignment - Dept ID: " + a.getDepartment().getId() + ", Year: " + a.getYear());
        }

        //checks if the coordinator is assigned to the department before editing
        boolean isAssigned = assignments.stream().anyMatch(a ->
                a.getDepartment().getId().equals(departmentId) && a.getYear() == year);
        if (!isAssigned) {
            System.out.println("Not assigned to Dept ID: " + departmentId + ", Year: " + year);
            throw new IllegalArgumentException("You are not assigned to this department and year");
        }

        List<Course> conflicts = courseRepository.findConflictingCourses(startTime, endTime, dayOfWeek, location);
        if (!conflicts.isEmpty()) {
            Course conflict = conflicts.getFirst();
            throw new ConflictException("Time slot " + startTime + "-" + endTime + " on " + dayOfWeek +
                    " at " + location + " conflicts with " + conflict.getName());
        }

        Course course = new Course(courseName, department, year, startTime, endTime, dayOfWeek, location, coordinator);
        return courseRepository.save(course);
    }

    //Display timetable by department and year
    public List<Course> getTimetable(Long departmentId, int year) {
        return courseRepository.findByDepartmentIdAndYear(departmentId, year);
    }


    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional
    public void removeCourse(Long courseId, Authentication auth) throws IllegalArgumentException {
        Coordinator coordinator = coordinatorRepository.findByUsername(auth.getName());
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        List<CoordinatorAssignment> assignments = assignmentRepository.findByCoordinator(coordinator);
        boolean canRemove = assignments.stream().anyMatch(a ->
                a.getDepartment().getId().equals(course.getDepartment().getId()) && a.getYear() == course.getYear());
        if (!canRemove) {
            throw new IllegalArgumentException("You are not authorized to remove this course");
        }

        courseRepository.delete(course);
    }

    public List<CoordinatorAssignment> getCoordinatorAssignments(Authentication auth) {
        Coordinator coordinator = coordinatorRepository.findByUsername(auth.getName());
        return assignmentRepository.findByCoordinator(coordinator);
    }
}