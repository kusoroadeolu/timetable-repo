
package com.uni.TimeTable.controller;

import com.uni.TimeTable.DTO.RoomDTO;
import com.uni.TimeTable.DTO.TimetableScheduleDTO; // Add this import
import com.uni.TimeTable.models.*;
import com.uni.TimeTable.repository.BuildingRepository;
import com.uni.TimeTable.repository.CourseDefinitionRepository;
import com.uni.TimeTable.repository.DepartmentRepository;
import com.uni.TimeTable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TimetableScheduleController {

    private final TimetableService timetableService;
    private final BuildingRepository buildingRepository;
    private final CourseDefinitionRepository courseDefinitionRepository;
    private final DepartmentRepository departmentRepository;

    private static final List<String> DAYS_OF_WEEK = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final List<Integer> YEARS = Arrays.asList(1, 2, 3, 4, 5);

    @PostMapping("/overseer/remove-course")
    @ResponseBody
    public Map<String, String> removeCourse(@RequestParam Long courseId, Authentication auth) {
        Map<String, String> response = new HashMap<>();
        try {
            timetableService.removeCourse(courseId, auth);
            response.put("status", "success");
            response.put("message", "Course removed successfully");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error removing course: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/overseer/schedule-timetable")
    public String getScheduleTimetable(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Model model) {

        List<CourseDefinition> courseDefinitions = (departmentId != null && year != null)
                ? courseDefinitionRepository.findByDepartmentIdAndYear(departmentId, year)
                : (departmentId != null)
                ? courseDefinitionRepository.findByDepartmentId(departmentId)
                : (year != null)
                ? courseDefinitionRepository.findByYear(year)
                : courseDefinitionRepository.findAll();

        model.addAttribute("schedule", new TimetableScheduleDTO()); // Add the schedule object for form binding
        model.addAttribute("courseDefinitions", courseDefinitions);
        model.addAttribute("schools", timetableService.getAllSchools());
        model.addAttribute("departments", timetableService.getDepartmentsBySchool(schoolId));
        model.addAttribute("buildings", buildingRepository.findAll());
        model.addAttribute("daysOfWeek", DAYS_OF_WEEK);
        model.addAttribute("years", YEARS);
        model.addAttribute("selectedSchoolId", schoolId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYear", year);

        return "overseer-schedule-timetable";
    }

    @PostMapping("/overseer/schedule-timetable")
    public String scheduleTimetable(
            @ModelAttribute("schedule") TimetableScheduleDTO schedule, // Use ModelAttribute for form binding
            @RequestParam(required = false) String elearningLink, // Keep elearningLink as RequestParam since it's optional
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            timetableService.scheduleTimetable(
                    schedule.getCourseDefinitionId(),
                    schedule.getDepartmentId(),
                    schedule.getYear(),
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getDayOfWeek(),
                    null,
                    elearningLink,
                    schedule.getRoomId(),
                    auth
            );
            redirectAttributes.addFlashAttribute("success", "Timetable scheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/overseer/schedule-timetable";
    }

    @GetMapping("/overseer/departments")
    @ResponseBody
    public List<Map<String, Object>> getDepartments(@RequestParam(required = false) Long schoolId) {
        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        return departments.stream()
                .map(dept -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", dept.getId());
                    item.put("name", dept.getName());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/overseer/reassign-course")
    public String showReassignCourse(@RequestParam(required = false) Long courseId, Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (courseId == null) {
            redirectAttributes.addFlashAttribute("error", "Course ID is required to reassign a course.");
            return "redirect:/overseer/timetable";
        }
        Course course = timetableService.getCourseById(courseId, auth);
        Long initialBuildingId = (course.getRoom() != null && course.getRoom().getBuilding() != null)
                ? course.getRoom().getBuilding().getId()
                : null;
        List<RoomDTO> rooms = timetableService.getAvailableRooms(course, null, null, null, initialBuildingId);
        List<DayOfWeek> daysOfWeek = Arrays.asList(DayOfWeek.values());
        List<Building> buildings = buildingRepository.findAll();

        model.addAttribute("course", course);
        model.addAttribute("rooms", rooms);
        model.addAttribute("daysOfWeek", daysOfWeek);
        model.addAttribute("buildings", buildings);

        return "overseer-reassign-course";
    }

    @PostMapping("/overseer/reassign-course")
    public String reassignCourse(
            @RequestParam Long courseId,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam Long roomId,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            timetableService.reassignCourse(courseId, dayOfWeek, startTime, endTime, roomId, auth);
            redirectAttributes.addFlashAttribute("success", "Course reassigned successfully");
            return "redirect:/overseer/timetable";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            Course course = timetableService.getCourseById(courseId, auth);
            Long initialBuildingId = (course.getRoom() != null && course.getRoom().getBuilding() != null)
                    ? course.getRoom().getBuilding().getId()
                    : null;
            List<RoomDTO> rooms = timetableService.getAvailableRooms(course, null, null, null, initialBuildingId);
            List<DayOfWeek> daysOfWeek = Arrays.asList(DayOfWeek.values());
            List<Building> buildings = buildingRepository.findAll();

            model.addAttribute("course", course);
            model.addAttribute("rooms", rooms);
            model.addAttribute("daysOfWeek", daysOfWeek);
            model.addAttribute("buildings", buildings);
            model.addAttribute("selectedDayOfWeek", dayOfWeek);
            model.addAttribute("selectedStartTime", startTime);
            model.addAttribute("selectedEndTime", endTime);
            model.addAttribute("selectedRoomId", roomId);
            return "overseer-reassign-course";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error reassigning course: " + e.getMessage());
            return "redirect:/overseer/timetable";
        }
    }

    @GetMapping("/overseer/finalize-timetable")
    public String showFinalizeTimetableForm(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", List.of(1, 2, 3, 4));
        return "overseer-finalize-timetable";
    }

    @PostMapping("/overseer/finalize-timetable")
    public String finalizeTimetable(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            timetableService.finalizeTimetable(auth, departmentId, year);
            String message = "Timetable finalized successfully!";
            if (departmentId != null && year != null) {
                Department dept = departmentRepository.findById(departmentId).orElseThrow();
                message += " (Department: " + dept.getName() + ", Year: " + year + ")";
            } else if (departmentId != null) {
                Department dept = departmentRepository.findById(departmentId).orElseThrow();
                message += " (Department: " + dept.getName() + ")";
            } else if (year != null) {
                message += " (Year: " + year + ")";
            }
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error finalizing timetable: " + e.getMessage());
        }
        return "redirect:/overseer/finalize-timetable";
    }
}