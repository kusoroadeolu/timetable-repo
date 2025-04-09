package com.uni.TimeTable.controller;

import com.uni.TimeTable.models.CoordinatorAssignment;
import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.exception.ConflictException;
import com.uni.TimeTable.service.TimetableService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/coordinator/timetable")
    public String getCoordinatorTimetable(Authentication auth,
                                          @RequestParam(required = false) Long departmentId,
                                          @RequestParam(required = false) Integer year,
                                          Model model) {
        // Fetch assignments using the Authentication object
        List<CoordinatorAssignment> assignments = timetableService.getCoordinatorAssignments(auth);

        Long selectedDepartmentId = departmentId;
        Integer selectedYear = year;

        // Preselect the first assignment if no departmentId or year is provided
        if (departmentId == null && year == null && !assignments.isEmpty()) {
            selectedDepartmentId = assignments.getFirst().getDepartment().getId();
            selectedYear = assignments.getFirst().getYear();
        }

        // Load courses if a department and year are selected
        if (selectedDepartmentId != null && selectedYear != null) {
            List<Course> courses = timetableService.getTimetable(selectedDepartmentId, selectedYear);
            System.out.println("Courses for Dept " + selectedDepartmentId + ", Year " + selectedYear + ": " + courses.size());
            model.addAttribute("courses", courses);
        }

        // Initialize newCourse with the selected year
        Course newCourse = new Course();
        if (selectedYear != null) {
            newCourse.setYear(selectedYear); // Set year explicitly to avoid default 0
        }

        // Add attributes to the model
        model.addAttribute("newCourse", newCourse);
        model.addAttribute("selectedDepartmentId", selectedDepartmentId);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("assignments", assignments);

        return "coordinator-timetable";
    }

    @PostMapping("/coordinator/schedule")
    public String scheduleCourse(@ModelAttribute("newCourse") Course newCourse,
                                 @RequestParam(required = false) Long departmentId,
                                 Authentication auth,
                                 Model model) {
        try {
            if (departmentId == null) {
                throw new IllegalArgumentException("Please select a department before adding a course");
            }

            //Invoke the timetable service method to schedule a course with the required parameters
            timetableService.scheduleCourse(newCourse.getName(), departmentId, newCourse.getYear(),
                    newCourse.getStartTime(), newCourse.getEndTime(), newCourse.getDayOfWeek(),
                    newCourse.getLocation(), auth);
            return "redirect:/coordinator/timetable?departmentId=" + departmentId + "&year=" + newCourse.getYear();
        } catch (IllegalArgumentException | ConflictException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assignments", timetableService.getCoordinatorAssignments(auth));
            model.addAttribute("courses", timetableService.getTimetable(departmentId, newCourse.getYear()));
            model.addAttribute("selectedDepartmentId", departmentId);
            model.addAttribute("selectedYear", newCourse.getYear());
            return "coordinator-timetable";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("assignments", timetableService.getCoordinatorAssignments(auth));
            model.addAttribute("courses", timetableService.getTimetable(departmentId, newCourse.getYear()));
            model.addAttribute("selectedDepartmentId", departmentId);
            model.addAttribute("selectedYear", newCourse.getYear());
            return "coordinator-timetable";
        }
    }

    //Remove a course from the timetable
    @PostMapping("/coordinator/remove-course")
    public String removeCourse(@RequestParam Long courseId,
                               @RequestParam Long departmentId,
                               @RequestParam int year,
                               Authentication auth) {
        try {
            timetableService.removeCourse(courseId, auth);
        } catch (IllegalArgumentException e) {
            // Log if needed
        }
        return "redirect:/coordinator/timetable?departmentId=" + departmentId + "&year=" + year;
    }


    @GetMapping("/student/timetable")
    public String getStudentTimetable(@RequestParam(required = false) Long departmentId,
                                      @RequestParam(required = false) Integer year,
                                      Model model) {
        model.addAttribute("departments", timetableService.getAllDepartments());
        if (departmentId != null && year != null) {
            model.addAttribute("courses", timetableService.getTimetable(departmentId, year));
            model.addAttribute("selectedDepartmentId", departmentId);
            model.addAttribute("selectedYear", year);
        }
        return "student-timetable";
    }
}