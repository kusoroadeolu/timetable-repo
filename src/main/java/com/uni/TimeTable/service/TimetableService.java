package com.uni.TimeTable.service;

import com.uni.TimeTable.DTO.RoomDTO;
import com.uni.TimeTable.exception.ConflictException;
import com.uni.TimeTable.models.*;
import com.uni.TimeTable.repository.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final CourseDefinitionRepository courseDefinitionRepository;
    @Getter
    private final RoomRepository roomRepository;
    private final CoordinatorRepository coordinatorRepository;
    private final ActivityRepository activityRepository;

    @Transactional
    public void scheduleTimetable(
            Long courseDefinitionId,
            Long departmentId,
            Integer year,
            String startTime,
            String endTime,
            String dayOfWeek,
            Long lecturerId, // Kept for compatibility, but ignored
            String elearningLink,
            Long roomId,
            Authentication auth) throws ConflictException {
        CourseDefinition courseDefinition = courseDefinitionRepository.findById(courseDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("CourseDefinition not found"));
        Lecturer lecturer = courseDefinition.getLecturer();
        if (lecturer == null) {
            throw new IllegalArgumentException("No lecturer assigned to CourseDefinition: " + courseDefinition.getCode());
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
        LocalTime parsedStartTime = LocalTime.parse(startTime);
        LocalTime parsedEndTime = LocalTime.parse(endTime);

        if (parsedEndTime.isBefore(parsedStartTime) || parsedEndTime.equals(parsedStartTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        if (!lecturer.isAvailable(parsedDayOfWeek, parsedStartTime, parsedEndTime)) {
            throw new ConflictException("Lecturer " + lecturer.getName() + " is not available on " + dayOfWeek + " from " + startTime + " to " + endTime);
        }

        List<Course> conflictingLecturerCourses = courseRepository.findByLecturerAndDayOfWeekAndTimeOverlap(
                lecturer.getId(), parsedDayOfWeek, parsedStartTime, parsedEndTime);
        if (!conflictingLecturerCourses.isEmpty()) {
            Course conflict = conflictingLecturerCourses.getFirst();
            throw new ConflictException("Lecturer " + lecturer.getName() + " is already scheduled for " +
                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                    conflict.getStartTime() + " to " + conflict.getEndTime());
        }

        List<Course> conflictingRoomCourses = courseRepository.findByDayOfWeekAndRoomAndTimeOverlap(
                room, parsedDayOfWeek, parsedStartTime, parsedEndTime);
        if (!conflictingRoomCourses.isEmpty()) {
            Course conflict = conflictingRoomCourses.getFirst();
            throw new ConflictException("Room " + room.getName() + " is already booked for " +
                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                    conflict.getStartTime() + " to " + conflict.getEndTime());
        }

        int studentCount = courseDefinition.getTotalStudents() != null ? courseDefinition.getTotalStudents() : 0;
        if (studentCount > room.getCapacity()) {
            throw new IllegalArgumentException("Room capacity (" + room.getCapacity() + ") is less than student count (" + studentCount + ")");
        }

        Course newCourse = new Course();
        newCourse.setCourseDefinition(courseDefinition);
        newCourse.setStatus(Course.CourseInstanceStatus.DRAFT);
        newCourse.setDayOfWeek(parsedDayOfWeek);
        newCourse.setStartTime(parsedStartTime);
        newCourse.setEndTime(parsedEndTime);
        newCourse.setRoom(room);

        if (elearningLink != null && !elearningLink.isEmpty()) {
            courseDefinition.setElearningLink(elearningLink);
            courseDefinitionRepository.save(courseDefinition);
        }

        courseRepository.save(newCourse);
        String activityDescription =  String.format("Course '%s' scheduled in '%s'", newCourse.getCourseDefinition().getName(), newCourse.getRoom().getName());
        Activity activity = new Activity(activityDescription);
        activityRepository.save(activity);

    }

    @Transactional
    public void removeCourse(Long courseId, Authentication auth) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));
//        if (course.getStatus() != Course.CourseInstanceStatus.DRAFT) {
//            throw new IllegalStateException("Only draft courses can be removed.");
//        }
        String activityDescription =  String.format("Course '%s' removed", course.getCourseDefinition().getName());
        Activity activity = new Activity(activityDescription);
        activityRepository.save(activity);
        courseRepository.delete(course);

    }

    @Transactional
    public void reassignCourse(Long courseId, String dayOfWeek, String startTime, String endTime, Long roomId, Authentication auth) throws ConflictException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));

        Room oldRoom = course.getRoom();

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
            Course conflict = conflictingRoomCourses.getFirst();
            throw new ConflictException("Room " + room.getName() + " is already booked for " +
                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                    conflict.getStartTime() + " to " + conflict.getEndTime());
        }

        int studentCount = course.getCourseDefinition().getTotalStudents() != null ? course.getCourseDefinition().getTotalStudents() : 0;
        if (studentCount > room.getCapacity()) {
            throw new IllegalArgumentException("Room capacity (" + room.getCapacity() + ") is less than student count (" + studentCount + ")");
        }

        // Update course details
        course.setRoom(room);
        course.setDayOfWeek(parsedDayOfWeek);
        course.setStartTime(parsedStartTime);
        course.setEndTime(parsedEndTime);

        String activityDescription =  String.format("Reassigned course '%s' from '%s' to '%s'", course.getCourseDefinition().getName(), oldRoom, room.getName());
        Activity activity = new Activity(activityDescription);
        activityRepository.save(activity);

        courseRepository.save(course);

        // Optional: Log the change or trigger notifications for finalized courses
        // if (course.getStatus() == Course.CourseInstanceStatus.FINALIZED) {
        //     // Log or notify logic here (e.g., send email to students/lecturers)
        // }
    }

    @Transactional(readOnly = true)
    public Course getCourseById(Long courseId, Authentication auth) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));
        // Add authorization check if needed (e.g., ensure overseer has access)
        if (auth != null && !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OVERSEER"))) {
            throw new SecurityException("Unauthorized access to course details");
        }
        return course;
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
            throw new IllegalStateException(
                    "No draft courses found to finalize. Please schedule courses using 'Schedule Timetable' or 'Upload Timetable Sheet'."
            );
        }

        List<Course> incompleteCourses = draftCourses.stream()
                .filter(course -> course.getDayOfWeek() == null ||
                        course.getStartTime() == null ||
                        course.getEndTime() == null ||
                        course.getRoom() == null)
                .collect(Collectors.toList());

        if (!incompleteCourses.isEmpty()) {
            String incompleteCodes = incompleteCourses.stream()
                    .map(course -> course.getCourseDefinition().getCode())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    "Cannot finalize courses missing scheduling details: " + incompleteCodes +
                            ". Please provide day, time, and room details using 'Schedule Timetable' or 'Upload Timetable Sheet'."
            );
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


    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRooms(Course course, String dayOfWeek, String startTime, String endTime, Long buildingId) {
        Long effectiveBuildingId = buildingId != null ? buildingId :
                (course != null && course.getRoom() != null && course.getRoom().getBuilding() != null)
                        ? course.getRoom().getBuilding().getId()
                        : null;

        if (effectiveBuildingId == null) {
            return new ArrayList<>();
        }

        Course.DayOfWeek parsedDayOfWeek = (dayOfWeek != null && !dayOfWeek.isEmpty())
                ? Course.DayOfWeek.valueOf(dayOfWeek)
                : (course != null ? course.getDayOfWeek() : null);
        LocalTime parsedStartTime = (startTime != null && !startTime.isEmpty())
                ? LocalTime.parse(startTime)
                : (course != null ? course.getStartTime() : null);
        LocalTime parsedEndTime = (endTime != null && !endTime.isEmpty())
                ? LocalTime.parse(endTime)
                : (course != null ? course.getEndTime() : null);

        List<Room> rooms;
        if (parsedDayOfWeek == null || parsedStartTime == null || parsedEndTime == null) {
            rooms = roomRepository.findByBuildingId(effectiveBuildingId);
        } else {
            rooms = roomRepository.findAvailableRoomsByBuildingAndTime(
                    effectiveBuildingId, parsedDayOfWeek, parsedStartTime, parsedEndTime);
            if (course != null && course.getRoom() != null) {
                rooms.removeIf(room -> room.getId().equals(course.getRoom().getId()));
            }
        }

        return rooms.stream()
                .map(room -> new RoomDTO(room.getId(), room.getName()))
                .collect(Collectors.toList());
    }

    public List<CourseDefinition> getStudentCourseDefinition(Long departmentId, Integer year) {
        if (departmentId == null || year == null) {
            return List.of();
        }
        return courseDefinitionRepository.findCourseDefinitionsByDepartmentIdAndYearAndCourseStatus(
                departmentId, year, Course.CourseInstanceStatus.FINALIZED);
    }

    public List<CoordinatorAssignment> getCoordinatorAssignments(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return Collections.emptyList();
        }

        String username = auth.getName();
        Coordinator coordinator = coordinatorRepository.findByUsernameWithAssignments(username)
                .orElse(null);
        if (coordinator == null) {
            return Collections.emptyList();
        }

        List<CoordinatorAssignment> assignments = coordinator.getAssignments();
        if (assignments == null) {
            return Collections.emptyList();
        }
        return assignments.stream()
                .filter(a -> a.getDepartment() != null)
                .collect(Collectors.toList());
    }

    public List<Course> getTimetable(Long schoolId, Long departmentId, Integer year, String dayOfWeek, Authentication auth) {
        List<Course> courses;
        boolean isOverseer = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OVERSEER"));

        if (isOverseer) {
            courses = courseRepository.findBySchoolIdAndDepartmentIdAndYear(schoolId, departmentId, year);
        } else {
            courses = courseRepository.findByCourseDefinitionDepartmentIdAndCourseDefinitionYearAndStatus(departmentId, year, Course.CourseInstanceStatus.FINALIZED);
        }

        if (dayOfWeek != null && !dayOfWeek.isEmpty()) {
            Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
            courses = courses.stream()
                    .filter(course -> course.getDayOfWeek() == parsedDayOfWeek)
                    .collect(Collectors.toList());
        }

        return courses;
    }


}