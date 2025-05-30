package com.uni.TimeTable.controller;

import com.uni.TimeTable.service.TimetableService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Controller
public class TimetableController {

    protected static final List<String> DAYS_OF_WEEK = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    protected static final List<Integer> YEARS = Arrays.asList(1, 2, 3, 4, 5);

    private final TimetableService timetableService; // Example dependency

    // Explicit default constructor to satisfy Lombok
    public TimetableController() {
        this(null); // Call the parameterized constructor with null if needed
    }

    // Constructor with dependency injection
    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }


    @GetMapping({"/", "/timetable"})
    public String home(Model model) {
        // Add any service calls if needed, e.g., model.addAttribute("data", timetableService.getSomeData());
        return "home";
    }
}