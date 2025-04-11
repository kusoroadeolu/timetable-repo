package com.uni.TimeTable.controller;

import com.uni.TimeTable.models.CoordinatorAssignment;
import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.exception.ConflictException;
import com.uni.TimeTable.repository.LecturerRepository;
import com.uni.TimeTable.service.TimetableService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Controller
public class TimetableController {

    private final TimetableService timetableService;
    private final LecturerRepository lecturerRepository;

    public TimetableController(TimetableService timetableService, LecturerRepository lecturerRepository) {

        this.timetableService = timetableService;
        this.lecturerRepository = lecturerRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/coordinator/timetable")
    public String getCoordinatorTimetable(Authentication auth,
                                          @RequestParam(required = false) Long departmentId,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) String dayOfWeek,
                                          Model model) {
        if (model.containsAttribute("error")) {
            model.addAttribute("error", model.getAttribute("error"));
        }
        System.out.println("GET /coordinator/timetable - departmentId: " + departmentId + ", year: " + year + ", dayOfWeek: " + dayOfWeek);
        List<CoordinatorAssignment> assignments = timetableService.getCoordinatorAssignments(auth);
        Long selectedDepartmentId = departmentId != null ? departmentId :
                (assignments.isEmpty() ? null : assignments.getFirst().getDepartment().getId());
        Integer selectedYear = year != null ? year : (assignments.isEmpty() ? null : assignments.getFirst().getYear());

        if (selectedDepartmentId != null && selectedYear != null) {
            model.addAttribute("courses", timetableService.getTimetable(selectedDepartmentId, selectedYear, dayOfWeek));
        }

        Course newCourse = new Course();
        if (selectedYear != null) {
            newCourse.setYear(selectedYear);
        }
        newCourse.setCredits(1);
        System.out.println("New Course initialized - dayOfWeek: " + newCourse.getDayOfWeek());

        model.addAttribute("assignments", assignments);
        model.addAttribute("daysOfWeek", Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
        model.addAttribute("newCourse", newCourse);
        model.addAttribute("lecturers", lecturerRepository.findAll());
        model.addAttribute("selectedDepartmentId", selectedDepartmentId);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedDayOfWeek", dayOfWeek);

        return "coordinator-timetable";
    }

    //Allows the coordinator to schedule a course
    @PostMapping("/coordinator/schedule")
    public String scheduleCourse(
            @RequestParam(required = false) Long departmentId,
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam Integer year,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String dayOfWeek,
            @RequestParam String location,
            @RequestParam Long lecturerId,
            @RequestParam Integer credits,
            @RequestParam(required = false) String elearningLink,
            Authentication auth,
            Model model) {
        try {
            if (departmentId == null) throw new IllegalArgumentException("Please select a department");

            // Validate start time is before end time
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("Start time must be before end time.");
            }
            timetableService.scheduleCourse(code, name, departmentId, year, startTime, endTime, dayOfWeek,
                    location, lecturerId, credits, elearningLink, auth);

            // On success, redirect to the timetable view
            String redirectUrl = "/coordinator/timetable?departmentId=" + departmentId + "&year=" + year;
            if (dayOfWeek != null && !dayOfWeek.isEmpty()) {
                redirectUrl += "&dayOfWeek=" + dayOfWeek;
            }
            return "redirect:" + redirectUrl;
        } catch (IllegalArgumentException | ConflictException e) {
            // On conflict or invalid time, stay on the Add Course form
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assignments", timetableService.getCoordinatorAssignments(auth));
            model.addAttribute("courses", timetableService.getTimetable(departmentId, year));
            model.addAttribute("lecturers", lecturerRepository.findAll());
            model.addAttribute("selectedDepartmentId", departmentId);
            model.addAttribute("selectedYear", year);
            model.addAttribute("daysOfWeek", Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
            model.addAttribute("activeTab", "add-course");
            return "coordinator-timetable";
        }
    }

    @GetMapping("/student/timetable")
    public String getStudentTimetable(@RequestParam Long departmentId, @RequestParam Integer year,
                                      @RequestParam(required = false) String dayOfWeek, Model model) {
        model.addAttribute("courses", timetableService.getTimetable(departmentId, year, dayOfWeek));
        model.addAttribute("daysOfWeek", Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedDayOfWeek", dayOfWeek);
        return "student-timetable";
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
            //REMEMBER TO LOG THIS!!
        }
        return "redirect:/coordinator/timetable?departmentId=" + departmentId + "&year=" + year;
    }
}