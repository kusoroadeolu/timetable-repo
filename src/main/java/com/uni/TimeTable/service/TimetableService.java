package com.uni.TimeTable.service;

import com.uni.TimeTable.exception.ConflictException;
import com.uni.TimeTable.models.*;
import com.uni.TimeTable.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final LecturerRepository lecturerRepository;
    private final CourseDefinitionRepository courseDefinitionRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public void scheduleTimetable(
            Long courseDefinitionId,
            Long departmentId,
            Integer year,
            String startTime,
            String endTime,
            String dayOfWeek,
            Long lecturerId,
            String elearningLink,
            Long roomId,
            Authentication auth) throws ConflictException {
        CourseDefinition courseDefinition = courseDefinitionRepository.findById(courseDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("CourseDefinition not found"));

        // Removed validation check for departmentId and year since the UI ensures they match

        Lecturer lecturer = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
        LocalTime parsedStartTime = LocalTime.parse(startTime);
        LocalTime parsedEndTime = LocalTime.parse(endTime);

        if (parsedEndTime.isBefore(parsedStartTime) || parsedEndTime.equals(parsedStartTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        // Check lecturer availability
        if (!lecturer.isAvailable(parsedDayOfWeek, parsedStartTime, parsedEndTime)) {
            throw new ConflictException("Lecturer " + lecturer.getEmail() + " is not available on " + dayOfWeek + " from " + startTime + " to " + endTime);
        }

        // Check for lecturer conflicts
        List<Course> conflictingLecturerCourses = courseRepository.findByLecturerAndDayOfWeekAndTimeOverlap(
                lecturer.getId(), parsedDayOfWeek, parsedStartTime, parsedEndTime);
        if (!conflictingLecturerCourses.isEmpty()) {
            Course conflict = conflictingLecturerCourses.get(0);
            throw new ConflictException("Lecturer " + lecturer.getEmail() + " is already scheduled for " +
                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                    conflict.getStartTime() + " to " + conflict.getEndTime());
        }

        // Check for room conflicts
        List<Course> conflictingRoomCourses = courseRepository.findByDayOfWeekAndRoomAndTimeOverlap(
                room, parsedDayOfWeek, parsedStartTime, parsedEndTime);
        if (!conflictingRoomCourses.isEmpty()) {
            Course conflict = conflictingRoomCourses.get(0);
            throw new ConflictException("Room " + room.getName() + " is already booked for " +
                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                    conflict.getStartTime() + " to " + conflict.getEndTime());
        }

        // Check room capacity
        Course existingCourse = courseRepository.findByCourseDefinition(courseDefinition)
                .orElseGet(() -> {
                    Course newCourse = new Course();
                    newCourse.setCourseDefinition(courseDefinition);
                    newCourse.setStatus(Course.CourseInstanceStatus.DRAFT);
                    newCourse.setFirstTimeStudents(0);
                    newCourse.setCarryoverStudents(0);
                    newCourse.setTotalStudents(0);
                    return newCourse;
                });

        int studentCount = existingCourse.getTotalStudents();
        if (studentCount > room.getCapacity()) {
            throw new IllegalArgumentException("Room capacity (" + room.getCapacity() + ") is less than student count (" + studentCount + ")");
        }

        // Update the course
        existingCourse.setDayOfWeek(parsedDayOfWeek);
        existingCourse.setStartTime(parsedStartTime);
        existingCourse.setEndTime(parsedEndTime);
        existingCourse.setRoom(room);
        existingCourse.setStatus(Course.CourseInstanceStatus.DRAFT);

        // Update the lecturer in the CourseDefinition if necessary
        if (!courseDefinition.getLecturer().getId().equals(lecturerId)) {
            courseDefinition.setLecturer(lecturer);
            courseDefinitionRepository.save(courseDefinition);
        }

        // Update the elearningLink in the CourseDefinition if provided
        if (elearningLink != null && !elearningLink.isEmpty()) {
            courseDefinition.setElearningLink(elearningLink);
            courseDefinitionRepository.save(courseDefinition);
        }

        courseRepository.save(existingCourse);
    }

    @Transactional
    public void reassignRoom(Long courseId, Long roomId, String dayOfWeek, String startTime, String endTime, Authentication auth) throws ConflictException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
        LocalTime parsedStartTime = LocalTime.parse(startTime);
        LocalTime parsedEndTime = LocalTime.parse(endTime);

        if (parsedEndTime.isBefore(parsedStartTime) || parsedEndTime.equals(parsedStartTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        Lecturer lecturer = course.getCourseDefinition().getLecturer();
        if (lecturer != null) {
            if (!lecturer.isAvailable(parsedDayOfWeek, parsedStartTime, parsedEndTime)) {
                throw new ConflictException("Lecturer " + lecturer.getEmail() + " is not available on " + dayOfWeek + " from " + startTime + " to " + endTime);
            }
            List<Course> conflictingLecturerCourses = courseRepository.findByLecturerAndDayOfWeekAndTimeOverlap(
                    lecturer.getId(), parsedDayOfWeek, parsedStartTime, parsedEndTime);
            conflictingLecturerCourses.removeIf(c -> c.getId().equals(courseId));
            if (!conflictingLecturerCourses.isEmpty()) {
                Course conflict = conflictingLecturerCourses.get(0);
                throw new ConflictException("Lecturer " + lecturer.getEmail() + " is already scheduled for " +
                        conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                        conflict.getStartTime() + " to " + conflict.getEndTime());
            }
        }

        List<Course> conflictingRoomCourses = courseRepository.findByDayOfWeekAndRoomAndTimeOverlap(
                room, parsedDayOfWeek, parsedStartTime, parsedEndTime);
        conflictingRoomCourses.removeIf(c -> c.getId().equals(courseId));
        if (!conflictingRoomCourses.isEmpty()) {
            Course conflict = conflictingRoomCourses.get(0);
            throw new ConflictException("Room " + room.getName() + " is already booked for " +
                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                    conflict.getStartTime() + " to " + conflict.getEndTime());
        }

        int studentCount = course.getTotalStudents();
        if (studentCount > room.getCapacity()) {
            throw new IllegalArgumentException("Room capacity (" + room.getCapacity() + ") is less than student count (" + studentCount + ")");
        }

        course.setRoom(room);
        course.setDayOfWeek(parsedDayOfWeek);
        course.setStartTime(parsedStartTime);
        course.setEndTime(parsedEndTime);
        courseRepository.save(course);
    }

    @Transactional
    public void finalizeTimetable(Authentication auth, Long departmentId, Integer year) {
        List<Course> draftCourses;
        if (departmentId != null && year != null) {
            draftCourses = courseRepository.findByCourseDefinitionDepartmentIdAndCourseDefinitionYearAndStatus(
                    departmentId, year, Course.CourseInstanceStatus.DRAFT
            );
        } else if (departmentId != null) {
            draftCourses = courseRepository.findByCourseDefinitionDepartmentIdAndStatus(
                    departmentId, Course.CourseInstanceStatus.DRAFT
            );
        } else if (year != null) {
            draftCourses = courseRepository.findByCourseDefinitionYearAndStatus(
                    year, Course.CourseInstanceStatus.DRAFT
            );
        } else {
            draftCourses = courseRepository.findByStatus(Course.CourseInstanceStatus.DRAFT);
        }

        if (draftCourses.isEmpty()) {
            throw new IllegalStateException("No draft courses found to finalize for the selected criteria.");
        }

        for (Course course : draftCourses) {
            course.setStatus(Course.CourseInstanceStatus.FINALIZED);
            courseRepository.save(course);
        }
    }

    public List<School> getAllSchools() {
        return schoolRepository.findAll();
    }

    public List<Department> getDepartmentsBySchool(Long schoolId) {
        if (schoolId == null) {
            return departmentRepository.findAll();
        }
        return departmentRepository.findBySchoolId(schoolId);
    }

    public List<Course> getTimetable(Long schoolId, Long departmentId, Integer year, String dayOfWeek, Authentication auth) {
        List<Course> courses;
        if (departmentId != null && year != null) {
            courses = courseRepository.findByCourseDefinitionDepartmentIdAndCourseDefinitionYearAndStatus(
                    departmentId, year, Course.CourseInstanceStatus.FINALIZED);
        } else if (departmentId != null) {
            courses = courseRepository.findByCourseDefinitionDepartmentIdAndStatus(
                    departmentId, Course.CourseInstanceStatus.FINALIZED);
        } else if (year != null) {
            courses = courseRepository.findByCourseDefinitionYearAndStatus(
                    year, Course.CourseInstanceStatus.FINALIZED);
        } else {
            courses = courseRepository.findByStatus(Course.CourseInstanceStatus.FINALIZED);
        }

        if (schoolId != null) {
            courses = courses.stream()
                    .filter(course -> course.getCourseDefinition().getDepartment().getSchool().getId().equals(schoolId))
                    .collect(Collectors.toList());
        }

        if (dayOfWeek != null && !dayOfWeek.isEmpty()) {
            Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
            courses = courses.stream()
                    .filter(course -> course.getDayOfWeek() == parsedDayOfWeek)
                    .collect(Collectors.toList());
        }

        return courses;
    }

    public List<CoordinatorAssignment> getCoordinatorAssignments(Authentication auth) {
        // Placeholder for fetching coordinator assignments
        return new ArrayList<>();
    }

    public List<Course> getStudentCourses(Long departmentId, Integer year) {
        if (departmentId == null || year == null) {
            return new ArrayList<>();
        }
        return courseRepository.findByCourseDefinitionDepartmentIdAndCourseDefinitionYearAndStatus(
                departmentId, year, Course.CourseInstanceStatus.FINALIZED);
    }
}