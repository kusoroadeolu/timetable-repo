package com.uni.TimeTable.controller;

import com.uni.TimeTable.DTO.RoomDTO;
import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.models.CourseDefinition;
import com.uni.TimeTable.models.Department;
import com.uni.TimeTable.models.Room;
import com.uni.TimeTable.repository.CourseDefinitionRepository;
import com.uni.TimeTable.repository.RoomRepository;
import com.uni.TimeTable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class RoomManagementController {

    private final TimetableService timetableService;
    private final RoomRepository roomRepository;
    private final CourseDefinitionRepository courseDefinitionRepository;


    @GetMapping("/overseer/departments-by-school")
    @ResponseBody
    public List<Map<String, Object>> getDepartmentsBySchool(@RequestParam(required = false) Long schoolId) {
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

    @GetMapping("/overseer/course-definitions-by-dept-and-year")
    @ResponseBody
    public List<Map<String, Object>> getCourseDefinitionsByDeptAndYear(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year) {
        List<CourseDefinition> courseDefinitions = (departmentId != null && year != null)
                ? courseDefinitionRepository.findByDepartmentIdAndYear(departmentId, year)
                : (departmentId != null)
                ? courseDefinitionRepository.findByDepartmentId(departmentId)
                : (year != null)
                ? courseDefinitionRepository.findByYear(year)
                : courseDefinitionRepository.findAll();
        return courseDefinitions.stream()
                .map(courseDef -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", courseDef.getId());
                    item.put("name", courseDef.getCode() + " - " + courseDef.getName());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/overseer/available-rooms-by-building")
    @ResponseBody
    public List<Map<String, Object>> getAvailableRoomsByBuilding(
            @RequestParam Long buildingId,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
        LocalTime parsedStartTime = LocalTime.parse(startTime);
        LocalTime parsedEndTime = LocalTime.parse(endTime);

        if (parsedEndTime.isBefore(parsedStartTime) || parsedEndTime.equals(parsedStartTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        List<Room> availableRooms = roomRepository.findAvailableRoomsByBuildingAndTime(
                buildingId, parsedDayOfWeek, parsedStartTime, parsedEndTime);

        return availableRooms.stream()
                .map(room -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", room.getId());
                    item.put("name", room.getName());
                    item.put("capacity", room.getCapacity());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/overseer/rooms-by-building")
    @ResponseBody
    public List<Map<String, Object>> getRoomsByBuilding(@RequestParam Long buildingId) {
        List<Room> rooms = roomRepository.findByBuildingId(buildingId);
        return rooms.stream()
                .map(room -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", room.getId());
                    item.put("name", room.getName());
                    item.put("capacity", room.getCapacity());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/overseer/available-rooms")
    @ResponseBody
    public List<RoomDTO> getAvailableRooms(
            @RequestParam Long courseId,
            @RequestParam(required = false) String dayOfWeek,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Long buildingId,
            Authentication auth) {
        Course course = timetableService.getCourseById(courseId, auth);
        return timetableService.getAvailableRooms(course, dayOfWeek, startTime, endTime, buildingId);
    }
}