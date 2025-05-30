package com.uni.TimeTable.controller;

import com.uni.TimeTable.models.*;
import com.uni.TimeTable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TimetableViewController {

    private final TimetableService timetableService;
    private static final List<String> DAYS_OF_WEEK = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final List<String> TIME_SLOTS = Arrays.asList(
            "8:00 AM - 9:00 AM", "9:00 AM - 10:00 AM", "10:00 AM - 11:00 AM",
            "11:00 AM - 12:00 PM", "12:00 PM - 1:00 PM", "1:00 PM - 2:00 PM",
            "2:00 PM - 3:00 PM", "3:00 PM - 4:00 PM", "4:00 PM - 5:00 PM",
            "5:00 PM - 6:00 PM"
    );
    private static final List<Integer> YEARS = Arrays.asList(1, 2, 3, 4, 5);
    private static final Map<String, String> DEPT_COLORS = Map.of(
            "Computer Science", "#60a5fa", "Mathematics", "#f472b6", "Engineering", "#34d399"
    );

    @GetMapping("/overseer/timetable")
    public String viewTimetable(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(defaultValue = "grid") String viewType,
            Authentication auth,
            Model model) {
        String currentUri = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        model.addAttribute("currentUri", currentUri);


        List<School> schools = timetableService.getAllSchools();
        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        List<Course> courses = timetableService.getTimetable(schoolId, departmentId, year, null, auth);

        System.out.println("Retrieved courses: " + courses.size()); // Debug log

        // Apply search filter
        if (searchQuery != null && !searchQuery.isEmpty()) {
            courses = courses.stream()
                    .filter(course -> course.getCourseDefinition().getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            course.getCourseDefinition().getLecturer().getName().toLowerCase().contains(searchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Organize courses by day and time slot for New Timetable view
        Map<String, Map<String, List<Course>>> coursesByDay = new HashMap<>();
        for (String day : DAYS_OF_WEEK) {
            Map<String, List<Course>> daySchedule = new HashMap<>();
            for (String timeSlot : TIME_SLOTS) {
                daySchedule.put(timeSlot, new ArrayList<>());
            }
            coursesByDay.put(day, daySchedule);
        }

        for (Course course : courses) {
            String day = course.getDayOfWeek().toString().toUpperCase();
            LocalTime startTime = course.getStartTime();
            LocalTime endTime = course.getEndTime();

            System.out.println("Processing Course: " + course.getCourseDefinition().getName() +
                    ", Day: " + day +
                    ", Time: " + startTime + " - " + endTime);

            // Convert course times to the display format for logging
            String courseStartStr = startTime.format(TIME_FORMATTER);
            String courseEndStr = endTime.format(TIME_FORMATTER);
            System.out.println("  -> Course time in display format: " + courseStartStr + " - " + courseEndStr);

            // Find all time slots that the course overlaps with
            List<String> matchingSlots = new ArrayList<>();
            for (String slot : TIME_SLOTS) {
                String[] times = slot.split(" - ");
                LocalTime slotStart = LocalTime.parse(times[0], TIME_FORMATTER);
                LocalTime slotEnd = LocalTime.parse(times[1], TIME_FORMATTER);

                // Check if the course overlaps with this slot using standard overlap condition
                if (startTime.isBefore(slotEnd) && endTime.isAfter(slotStart)) {
                    matchingSlots.add(slot);
                    System.out.println("  -> Matched to predefined slot: " + slot);
                }
            }

            if (!matchingSlots.isEmpty()) {
                if (DAYS_OF_WEEK.contains(day)) {
                    for (String finalTimeSlot : matchingSlots) {
                        if (coursesByDay.containsKey(day) && coursesByDay.get(day).containsKey(finalTimeSlot)) {
                            coursesByDay.get(day).get(finalTimeSlot).add(course);
                            System.out.println("  -> Successfully added course to coursesByDay[" + day + "][" + finalTimeSlot + "]");
                        } else {
                            System.err.println("  -> ERROR: Target map/list not found for Day: " + day + ", TimeSlot: " + finalTimeSlot);
                        }
                    }
                } else {
                    System.err.println("  -> ERROR: Day of week " + day + " not found in DAYS_OF_WEEK list.");
                }
            } else {
                System.out.println("  -> No matching predefined time slot for course time: " + courseStartStr + " - " + courseEndStr + ". Skipping course.");
            }
        }

        // DEBUG: Final state of coursesByDay before passing to Thymeleaf
        System.out.println("\n--- DEBUG: Final coursesByDay map state ---");
        coursesByDay.forEach((day, daySchedule) -> {
            System.out.println("Day: " + day);
            daySchedule.forEach((timeSlot, courseList) -> {
                System.out.println("  TimeSlot: " + timeSlot + ", Courses: " + courseList.size());
                courseList.forEach(course -> System.out.println("    - " + course.getCourseDefinition().getName()));
            });
        });
        System.out.println("-------------------------------------------\n");

        model.addAttribute("schools", schools);
        model.addAttribute("departments", departments);
        model.addAttribute("years", YEARS);
        model.addAttribute("timeSlots", TIME_SLOTS);
        model.addAttribute("daysOfWeek", DAYS_OF_WEEK);
        model.addAttribute("coursesByDay", coursesByDay);
        model.addAttribute("courses", courses); // For Old Timetable view
        model.addAttribute("deptColors", DEPT_COLORS);
        model.addAttribute("selectedSchoolId", schoolId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYear", year);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("viewType", viewType);
        model.addAttribute("cacheBuster", System.currentTimeMillis());


        return "overseer-timetable"; // Always return the consolidated template
    }

    @GetMapping("/coordinator/view-timetable")
    public String getCoordinatorTimetable(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String dayOfWeek,
            Authentication auth,
            Model model) {
        List<CoordinatorAssignment> assignments = timetableService.getCoordinatorAssignments(auth);
        if (assignments == null || assignments.isEmpty()) {
            model.addAttribute("error", "No assignments found for this coordinator.");
            return "coordinator-view-timetable";
        }

        assignments = assignments.stream()
                .filter(assign -> assign != null && assign.getDepartment() != null)
                .collect(Collectors.toList());
        if (assignments.isEmpty()) {
            model.addAttribute("error", "No valid assignments found for this coordinator.");
            return "coordinator-view-timetable";
        }

        model.addAttribute("assignments", assignments);
        model.addAttribute("daysOfWeek", DAYS_OF_WEEK);

        if (departmentId != null && year != null) {
            CoordinatorAssignment matchingAssignment = assignments.stream()
                    .filter(a -> a.getDepartment().getId().equals(departmentId) && a.getYear() == year)
                    .findFirst()
                    .orElse(null);

            if (matchingAssignment == null) {
                model.addAttribute("error", "You are not assigned to this department and year.");
                return "coordinator-view-timetable";
            }

            List<Course> courses = timetableService.getTimetable(null, departmentId, year, dayOfWeek, auth);
            model.addAttribute("courses", courses);
            model.addAttribute("selectedDeptYear", matchingAssignment.getDepartment().getName() + " Year " + year);
            model.addAttribute("selectedDayOfWeek", dayOfWeek);
        }

        return "coordinator-view-timetable";
    }

    @GetMapping("/coordinator/assignments")
    @ResponseBody
    public List<Map<String, String>> getCoordinatorAssignmentsAsJson(Authentication auth) {
        List<CoordinatorAssignment> assignments = timetableService.getCoordinatorAssignments(auth);
        if (assignments == null) {
            return List.of();
        }
        return assignments.stream()
                .filter(assign -> assign.getDepartment() != null)
                .map(assign -> {
                    String value = assign.getDepartment().getName() + " Year " + assign.getYear();
                    Map<String, String> item = new HashMap<>();
                    item.put("id", value);
                    item.put("text", value);
                    return item;
                })
                .collect(Collectors.toList());
    }
}