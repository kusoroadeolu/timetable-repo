package com.uni.TimeTable.service;

import com.uni.TimeTable.models.*;
import com.uni.TimeTable.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.uni.TimeTable.exception.ConflictException;

import java.util.Comparator;
import java.util.List;

@Service
public class TimetableService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final CoordinatorRepository coordinatorRepository;
    private final CoordinatorAssignmentRepository assignmentRepository;
    private final LecturerRepository lecturerRepository;

    public TimetableService(CourseRepository courseRepository,
                            DepartmentRepository departmentRepository,
                            CoordinatorRepository coordinatorRepository,
                            CoordinatorAssignmentRepository assignmentRepository,
                            LecturerRepository lecturerRepository) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.coordinatorRepository = coordinatorRepository;
        this.assignmentRepository = assignmentRepository;
        this.lecturerRepository = lecturerRepository;
    }

    @Transactional
    public Course scheduleCourse(String code, String name, Long departmentId, int year,
                                 String startTime, String endTime, String dayOfWeek,
                                 String location, Long lecturerId, Integer credits, String elearningLink,
                                 Authentication auth)
            throws IllegalArgumentException, ConflictException {
        Coordinator coordinator = coordinatorRepository.findByUsername(auth.getName());
        if (coordinator == null) throw new IllegalArgumentException("Invalid coordinator");

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        Lecturer lecturer = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found: " + lecturerId));

        List<CoordinatorAssignment> assignments = assignmentRepository.findByCoordinator(coordinator);
        boolean isAssigned = assignments.stream().anyMatch(a ->
                a.getDepartment().getId().equals(departmentId) && a.getYear() == year);
        if (!isAssigned) throw new IllegalArgumentException("You are not assigned to this department and year");

        List<Course> conflicts = courseRepository.findConflictingCourses(startTime, endTime, dayOfWeek, location);
        if (!conflicts.isEmpty()) {
            Course conflict = conflicts.getFirst();
            throw new ConflictException("Time slot conflicts with " + conflict.getCode());
        }

        if (credits == null) throw new IllegalArgumentException("Credits must be specified");
        Course course = new Course(code, name, department, year, startTime, endTime, dayOfWeek, location,
                coordinator, lecturer, credits, elearningLink);
        return courseRepository.save(course);
    }

    //Display timetable by department and year
    public List<Course> getTimetable(Long departmentId, int year) {
        return courseRepository.findByDepartmentIdAndYear(departmentId, year);
    }

    //Display timetable by department and year and day of the week
    public List<Course> getTimetable(Long departmentId, Integer year, String dayOfWeek) {
        List<Course> courses;
        if (dayOfWeek != null && !dayOfWeek.trim().isEmpty()) {
            courses = courseRepository.findByDepartmentIdAndYearAndDayOfWeek(departmentId, year, dayOfWeek);
            System.out.println("Filtering by dayOfWeek: " + dayOfWeek);
            System.out.println("Courses found: " + courses.size());
        } else {
            courses = courseRepository.findByDepartmentIdAndYear(departmentId, year);
            System.out.println("No dayOfWeek filter applied, fetching all courses for dept: " + departmentId + ", year: " + year);
        }
        courses.sort(Comparator.comparing(Course::getStartTime));
        return courses;
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