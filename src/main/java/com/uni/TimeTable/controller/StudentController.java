package com.uni.TimeTable.controller;

import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class StudentController {

    private final TimetableService timetableService;
    private static final List<String> DAYS_OF_WEEK = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final List<Integer> YEARS = Arrays.asList(1, 2, 3, 4, 5);

    @GetMapping("/student/timetable")
    public String getStudentTimetable(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String dayOfWeek,
            Model model) {
        model.addAttribute("schools", timetableService.getAllSchools());
        model.addAttribute("departments", timetableService.getDepartmentsBySchool(schoolId));
        model.addAttribute("years", YEARS);
        model.addAttribute("daysOfWeek", DAYS_OF_WEEK);

        if (schoolId != null && departmentId != null && year != null) {
            List<Course> courses = timetableService.getTimetable(schoolId, departmentId, year, dayOfWeek, null);
            model.addAttribute("courses", courses);
            model.addAttribute("selectedSchoolId", schoolId);
            model.addAttribute("selectedDepartmentId", departmentId);
            model.addAttribute("selectedYear", year);
            model.addAttribute("selectedDayOfWeek", dayOfWeek);
        } else if (schoolId != null && departmentId != null) {
            model.addAttribute("selectedSchoolId", schoolId);
            model.addAttribute("selectedDepartmentId", departmentId);
        } else if (schoolId != null) {
            model.addAttribute("selectedSchoolId", schoolId);
        }

        return "student-timetable";
    }
}