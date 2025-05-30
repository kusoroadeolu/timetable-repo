package com.uni.TimeTable.controller;

import com.uni.TimeTable.DTO.ActivityDto;
import com.uni.TimeTable.mapper.ActivityMapper;
import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.models.Activity;
import com.uni.TimeTable.repository.ActivityRepository;
import com.uni.TimeTable.service.TimetableService;
import com.uni.TimeTable.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.uni.TimeTable.utils.DateTimeUtils.getRelativeTime;

@Controller
@RequestMapping("/overseer")
@RequiredArgsConstructor
public class DashboardController {

    private final TimetableService timetableService;
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final DateTimeUtils dateTimeUtils;

    @GetMapping("/dashboard")
    public String viewDashboard(Model model, Authentication auth) {
        List<Course> courses = timetableService.getTimetable(null, null, null, null, auth);
        String currentUri = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        model.addAttribute("currentUri", currentUri);

        // Calculate stats
        long totalCourses = courses.size();
        long draftCoursesToFinalize = courses.stream()
                .filter(course -> course.getStatus() == Course.CourseInstanceStatus.DRAFT)
                .count();

        // Fetch and map recent activities
        List<Activity> recentActivities = activityRepository.findAllRecentActivities(LocalDateTime.now().minusWeeks(1));
        List<ActivityDto> activityDtos = recentActivities.stream()
                .map(activityMapper::toDto)
                .toList();

        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("draftCoursesToFinalize", draftCoursesToFinalize);
        model.addAttribute("recentActivities", activityDtos);
        return "overseer-dashboard";
    }


}
