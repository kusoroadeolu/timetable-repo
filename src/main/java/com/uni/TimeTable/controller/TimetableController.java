package com.uni.TimeTable.controller;

import com.uni.TimeTable.DTO.RoomDTO;
import com.uni.TimeTable.models.*;
import com.uni.TimeTable.exception.ConflictException;
import com.uni.TimeTable.repository.*;
import com.uni.TimeTable.service.TimetableService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TimetableController {

    private static final Logger logger = LoggerFactory.getLogger(TimetableController.class);

    private final TimetableService timetableService;
    private final LecturerRepository lecturerRepository;
    private final CourseDefinitionRepository courseDefinitionRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerAvailabilityRepository lecturerAvailabilityRepository;
    private final SchoolRepository schoolRepository;
    private final BuildingRepository buildingRepository;

    private static final List<String> DAYS_OF_WEEK = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final List<Integer> YEARS = Arrays.asList(1, 2, 3, 4, 5);

    @GetMapping({"/", "/timetable"})
    public String home(Model model) {
        return "home";
    }

    @GetMapping("/overseer/timetable")
    public String viewTimetable(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Authentication auth,
            Model model) {

        // Fetch data
        List<School> schools = timetableService.getAllSchools();
        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        List<Integer> years = Arrays.asList(1, 2, 3, 4);
        List<Course> courses = timetableService.getTimetable(schoolId, departmentId, year, null, auth);

        // If a department filter is applied, only include that department
        List<Department> filteredDepartments;
        if (departmentId != null) {
            filteredDepartments = departments.stream()
                    .filter(dept -> dept.getId().equals(departmentId))
                    .collect(Collectors.toList());
        } else {
            filteredDepartments = departments;
        }

        // Group courses by department, only for the filtered departments
        Map<Long, List<Course>> coursesByDept = filteredDepartments.stream()
                .collect(Collectors.toMap(
                        Department::getId,
                        dept -> courses.stream()
                                .filter(course -> course.getCourseDefinition().getDepartment().getId().equals(dept.getId()))
                                .collect(Collectors.toList())
                ));

        // Add attributes to the model
        model.addAttribute("schools", schools);
        model.addAttribute("departments", departments);
        model.addAttribute("filteredDepartments", filteredDepartments);
        model.addAttribute("years", years);
        model.addAttribute("coursesByDept", coursesByDept);
        model.addAttribute("selectedSchoolId", schoolId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYear", year);

        return "overseer-timetable";
    }

    @PostMapping("/overseer/remove-course")
    @ResponseBody
    public String removeCourse(@RequestParam Long courseId, Authentication auth) {
        System.out.println("Received request to remove course with ID: " + courseId);
        try {
            timetableService.removeCourse(courseId, auth);
            return "Course removed successfully";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error removing course: " + e.getMessage(), e);
        }
    }

    @GetMapping("/overseer/departments-by-school")
    @ResponseBody
    public List<Map<String, Object>> getDepartmentsBySchool(@RequestParam(required = false) Long schoolId) {
        System.out.println("Fetching departments for schoolId: " + schoolId);
        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        System.out.println("Found " + departments.size() + " departments: " + departments);
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
        System.out.println("Fetching CourseDefinitions for departmentId: " + departmentId + ", year: " + year);
        List<CourseDefinition> courseDefinitions;
        if (departmentId != null && year != null) {
            courseDefinitions = courseDefinitionRepository.findByDepartmentIdAndYear(departmentId, year);
        } else if (departmentId != null) {
            courseDefinitions = courseDefinitionRepository.findByDepartmentId(departmentId);
        } else if (year != null) {
            courseDefinitions = courseDefinitionRepository.findByYear(year);
        } else {
            courseDefinitions = courseDefinitionRepository.findAll();
        }
        System.out.println("Found " + courseDefinitions.size() + " CourseDefinitions: " + courseDefinitions);
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

        // Validate that endTime is after startTime
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
        System.out.println("Fetching rooms for buildingId: " + buildingId);
        List<Room> rooms = roomRepository.findByBuildingId(buildingId);
        System.out.println("Found " + rooms.size() + " rooms: " + rooms);
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

    @GetMapping("/overseer/schedule-timetable")
    public String getScheduleTimetable(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Model model) {
        System.out.println("Loading schedule-timetable with schoolId: " + schoolId + ", departmentId: " + departmentId + ", year: " + year);

        List<CourseDefinition> courseDefinitions;
        if (departmentId != null && year != null) {
            courseDefinitions = courseDefinitionRepository.findByDepartmentIdAndYear(departmentId, year);
        } else if (departmentId != null) {
            courseDefinitions = courseDefinitionRepository.findByDepartmentId(departmentId);
        } else if (year != null) {
            courseDefinitions = courseDefinitionRepository.findByYear(year);
        } else {
            courseDefinitions = courseDefinitionRepository.findAll();
        }
        System.out.println("Initial CourseDefinitions: " + courseDefinitions);

        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        System.out.println("Initial Departments: " + departments);

        model.addAttribute("courseDefinitions", courseDefinitions);
        model.addAttribute("schools", timetableService.getAllSchools());
        model.addAttribute("departments", departments);
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
            @RequestParam Long courseDefinitionId,
            @RequestParam Long schoolId,
            @RequestParam Long departmentId,
            @RequestParam Integer year,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String dayOfWeek,
            @RequestParam(required = false) String elearningLink,
            @RequestParam Long roomId,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new IllegalArgumentException("School not found"));
            if (!department.getSchool().getId().equals(schoolId)) {
                throw new IllegalArgumentException("Selected department does not belong to the selected school.");
            }

            timetableService.scheduleTimetable(courseDefinitionId, departmentId, year, startTime, endTime,
                    dayOfWeek, null, elearningLink, roomId, auth);
            redirectAttributes.addFlashAttribute("success", "Timetable scheduled successfully!");
        } catch (ConflictException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/overseer/schedule-timetable";
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
            // Log the exception to confirm this path is executed
            System.out.println("Caught IllegalArgumentException: " + e.getMessage());

            String errorMessage = e.getMessage();
            if (errorMessage.contains("End time must be after start time.")) {
                model.addAttribute("error", "Error reassigning course: End time must be after start time.");
            } else if (errorMessage.contains("Room capacity")) {
                model.addAttribute("error", "Error reassigning course: Room capacity is less than the number of students.");
            } else {
                model.addAttribute("error", "Error reassigning course: " + e.getMessage());
            }
            // Repopulate the model for the view
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
            // Log the exception to confirm this path is executed
            System.out.println("Caught Exception: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error reassigning course: " + e.getMessage());
            return "redirect:/overseer/timetable";
        }
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
            return "redirect:/overseer/finalize-timetable";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error finalizing timetable: " + e.getMessage());
            return "redirect:/overseer/finalize-timetable";
        }
    }

    @GetMapping("/overseer/upload-planner")
    public String showUploadPlannerForm(Model model) {
        model.addAttribute("schools", timetableService.getAllSchools());
        return "overseer-upload-planner";
    }

    @PostMapping("/overseer/upload-planner/academic-planner")
    @Transactional
    public String uploadAcademicPlanner(
            @RequestParam("file") MultipartFile file,
            @RequestParam("schoolId") Long schoolId,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please upload a CSV file.");
            return "redirect:/overseer/upload-planner";
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> {
                    redirectAttributes.addFlashAttribute("error", "Selected school not found.");
                    return new IllegalArgumentException("School not found");
                });

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            int rowCount = 0;
            int successfulRows = 0;
            List<String> errors = new ArrayList<>();
            List<String> successes = new ArrayList<>();

            String[] expectedHeaders = {
                    "Programme(Department)", "Level(Year)", "Course Code", "Course Title", "Credit Units",
                    "Status (Compulsory/Elective)", "Faculty", "Faculty Status", "e-mail address",
                    "No of Students Taking Course for First Time", "No of Carryover Students", "Total No of Students Taking Course"
            };

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (firstLine) {
                    if (!Arrays.equals(data, expectedHeaders)) {
                        redirectAttributes.addFlashAttribute("error", "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
                        return "redirect:/overseer/upload-planner";
                    }
                    firstLine = false;
                    continue;
                }

                if (data.length != 12) {
                    errors.add("Row " + (rowCount + 2) + ": Invalid CSV format, expected 12 columns");
                    rowCount++;
                    continue;
                }

                try {
                    String departmentName = data[0].trim();
                    String levelYear = data[1].trim();
                    String courseCode = data[2].trim();
                    String courseTitle = data[3].trim();
                    String creditUnits = data[4].trim();
                    String status = data[5].trim();
                    String lecturerName = data[6].trim();
                    String facultyStatus = data[7].trim();
                    String lecturerEmail = data[8].trim();
                    String firstTimeStudentsStr = data[9].trim();
                    String carryoverStudentsStr = data[10].trim();
                    String totalStudentsStr = data[11].trim();

                    int year = Integer.parseInt(levelYear);
                    if (year < 1 || year > 5) {
                        throw new IllegalArgumentException("Year must be between 1 and 5: " + year);
                    }

                    int credits = Integer.parseInt(creditUnits);
                    if (credits < 1 || credits > 10) {
                        throw new IllegalArgumentException("Credits must be between 1 and 10: " + credits);
                    }

                    if (!status.equalsIgnoreCase("Compulsory") && !status.equalsIgnoreCase("Elective")) {
                        throw new IllegalArgumentException("Status must be 'Compulsory' or 'Elective': " + status);
                    }

                    int firstTimeStudents = Integer.parseInt(firstTimeStudentsStr);
                    int carryoverStudents = Integer.parseInt(carryoverStudentsStr);
                    int totalStudents = Integer.parseInt(totalStudentsStr);
                    if (firstTimeStudents < 0 || carryoverStudents < 0 || totalStudents < 0) {
                        throw new IllegalArgumentException("Student numbers cannot be negative.");
                    }
                    if (totalStudents != (firstTimeStudents + carryoverStudents)) {
                        throw new IllegalArgumentException("Total students must equal first-time plus carryover students.");
                    }

                    // Case-insensitive department lookup and creation
                    Optional<Department> existingDept = departmentRepository.findByNameIgnoreCase(departmentName);
                    Department department;
                    if (existingDept.isPresent()) {
                        department = existingDept.get();
                        if (!department.getSchool().getId().equals(schoolId)) {
                            throw new IllegalArgumentException("Department " + departmentName + " already exists in a different school.");
                        }
                    } else {
                        Department newDept = new Department();
                        newDept.setName(departmentName);
                        newDept.setSchool(school);
                        department = departmentRepository.save(newDept);
                        logger.info("Created new department: {} for schoolId: {}", departmentName, schoolId);
                    }

                    if (lecturerName == null || lecturerName.isEmpty()) {
                        throw new IllegalArgumentException("Lecturer name (Faculty) cannot be empty.");
                    }
                    if (lecturerEmail == null || lecturerEmail.isEmpty()) {
                        throw new IllegalArgumentException("Lecturer email cannot be empty.");
                    }
                    if (!lecturerEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                        throw new IllegalArgumentException("Invalid email format: " + lecturerEmail);
                    }
                    if (facultyStatus == null || facultyStatus.isEmpty()) {
                        throw new IllegalArgumentException("Faculty Status cannot be empty.");
                    }

                    Lecturer.LecturerType lecturerType = "Active".equalsIgnoreCase(facultyStatus) ?
                            Lecturer.LecturerType.FULL_TIME : Lecturer.LecturerType.ADJUNCT;

                    Lecturer lecturer = null;
                    List<Lecturer> existingLecturers = lecturerRepository.findByName(lecturerName);
                    if (!existingLecturers.isEmpty()) {
                        lecturer = existingLecturers.stream()
                                .filter(l -> l.getEmail().equals(lecturerEmail))
                                .findFirst()
                                .orElse(null);
                        if (lecturer != null) {
                            if (lecturer.getType() != lecturerType) {
                                lecturer.setType(lecturerType);
                                lecturer = lecturerRepository.save(lecturer);
                                logger.info("Updated lecturer type: name={}, email={}, type={}",
                                        lecturerName, lecturerEmail, lecturerType);
                            } else {
                                logger.info("Lecturer matched: name={}, email={}", lecturerName, lecturerEmail);
                            }
                        }
                    }

                    if (lecturer == null) {
                        if (lecturerRepository.findByEmail(lecturerEmail).isPresent()) {
                            throw new IllegalArgumentException("Email " + lecturerEmail + " is already used by another lecturer.");
                        }
                        lecturer = new Lecturer();
                        lecturer.setName(lecturerName);
                        lecturer.setEmail(lecturerEmail);
                        lecturer.setType(lecturerType);
                        lecturer = lecturerRepository.save(lecturer);
                        logger.info("Created new lecturer: name={}, email={}, type={}",
                                lecturerName, lecturerEmail, lecturerType);
                    }

                    CourseDefinition courseDefinition = courseDefinitionRepository.findByCode(courseCode)
                            .orElseGet(() -> new CourseDefinition());
                    courseDefinition.setCode(courseCode);
                    courseDefinition.setName(courseTitle);
                    courseDefinition.setCredits(credits);
                    courseDefinition.setStatus(CourseDefinition.CourseStatus.valueOf(status.toUpperCase()));
                    courseDefinition.setDepartment(department);
                    courseDefinition.setYear(year);
                    courseDefinition.setLecturer(lecturer);
                    courseDefinition.setFirstTimeStudents(firstTimeStudents);
                    courseDefinition.setCarryoverStudents(carryoverStudents);
                    courseDefinition.setTotalStudents(totalStudents);
                    courseDefinition = courseDefinitionRepository.save(courseDefinition);
                    logger.info("CourseDefinition saved: code={}, title={}", courseCode, courseTitle);

                    successes.add("Row " + (rowCount + 2) + ": Course " + courseCode + " (" + departmentName + ", Year " + year + ")");
                    successfulRows++;
                    rowCount++;
                } catch (Exception e) {
                    logger.error("Error processing row {}: {}", rowCount + 2, e.getMessage());
                    errors.add("Row " + (rowCount + 2) + ": " + e.getMessage());
                    rowCount++;
                    continue;
                }
            }

            String successMessage = "Academic planner uploaded successfully! " + successfulRows + " rows processed.";
            if (!successes.isEmpty()) {
                successMessage += " Processed rows: " + String.join("; ", successes);
            }
            if (!errors.isEmpty()) {
                successMessage += " However, some rows failed: " + String.join("; ", errors);
                redirectAttributes.addFlashAttribute("error", successMessage);
            } else {
                redirectAttributes.addFlashAttribute("success", successMessage);
            }
            logger.info(successMessage);
        } catch (Exception e) {
            logger.error("Error processing academic planner CSV: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error processing academic planner CSV: " + e.getMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

    @PostMapping("/overseer/upload-planner/rooms")
    @Transactional
    public String uploadRooms(
            @RequestParam("file") MultipartFile file,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please upload a CSV file.");
            return "redirect:/overseer/upload-planner";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            int batchSize = 1000;
            List<Room> roomsBatch = new ArrayList<>();
            int rowCount = 0;
            int successfulRows = 0;
            List<String> errors = new ArrayList<>();
            List<String> successes = new ArrayList<>();
            Set<String> roomNamesInCsv = new HashSet<>();

            String[] expectedHeaders = {"buildingName", "roomName", "capacity"};

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (firstLine) {
                    if (!Arrays.equals(data, expectedHeaders)) {
                        redirectAttributes.addFlashAttribute("error", "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
                        return "redirect:/overseer/upload-planner";
                    }
                    firstLine = false;
                    continue;
                }

                if (data.length < 3) {
                    errors.add("Row " + (rowCount + 2) + ": Invalid CSV format, expected 3 columns");
                    rowCount++;
                    continue;
                }

                try {
                    String buildingName = data[0].trim();
                    String roomName = data[1].trim();
                    int capacity;
                    try {
                        capacity = Integer.parseInt(data[2].trim());
                        if (capacity <= 0) {
                            throw new IllegalArgumentException("Capacity must be a positive integer.");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid capacity format: " + data[2].trim());
                    }

                    // Check for duplicates within the CSV
                    String roomKey = roomName + ":" + buildingName;
                    if (!roomNamesInCsv.add(roomKey)) {
                        throw new IllegalArgumentException("Duplicate room name found in CSV: " + roomName + " in " + buildingName);
                    }

                    // Case-insensitive building lookup
                    Optional<Building> buildingOpt = buildingRepository.findByNameIgnoreCase(buildingName);
                    Building building;
                    if (buildingOpt.isPresent()) {
                        building = buildingOpt.get();
                    } else {
                        building = new Building();
                        building.setName(buildingName);
                        building = buildingRepository.save(building);
                        logger.info("Created new building: {}", buildingName);
                    }

                    // Check if room already exists in the database
                    Optional<Room> existingRoom = roomRepository.findByNameAndBuildingId(roomName, building.getId());
                    if (existingRoom.isPresent()) {
                        Room room = existingRoom.get();
                        // Update capacity if different
                        if (room.getCapacity() != capacity) {
                            room.setCapacity(capacity);
                            roomsBatch.add(room);
                            successes.add("Row " + (rowCount + 2) + ": Updated room " + roomName + " in " + buildingName + " with capacity " + capacity);
                        } else {
                            successes.add("Row " + (rowCount + 2) + ": Room " + roomName + " in " + buildingName + " already exists, skipped");
                        }
                    } else {
                        Room room = new Room();
                        room.setName(roomName);
                        room.setCapacity(capacity);
                        room.setBuilding(building);
                        roomsBatch.add(room);
                        successes.add("Row " + (rowCount + 2) + ": Created room " + roomName + " in " + buildingName + " with capacity " + capacity);
                    }

                    successfulRows++;
                    rowCount++;

                    if (roomsBatch.size() >= batchSize) {
                        roomRepository.saveAll(roomsBatch);
                        roomsBatch.clear();
                    }
                } catch (Exception e) {
                    errors.add("Row " + (rowCount + 2) + ": " + e.getMessage());
                    rowCount++;
                    continue;
                }
            }

            if (!roomsBatch.isEmpty()) {
                roomRepository.saveAll(roomsBatch);
            }

            String successMessage = "Rooms uploaded successfully! " + successfulRows + " rows processed.";
            if (!successes.isEmpty()) {
                successMessage += " Details: " + String.join("; ", successes);
            }
            if (!errors.isEmpty()) {
                successMessage += " However, some rows failed: " + String.join("; ", errors);
                redirectAttributes.addFlashAttribute("error", successMessage);
            } else {
                redirectAttributes.addFlashAttribute("success", successMessage);
            }
            logger.info(successMessage);
        } catch (Exception e) {
            logger.error("Error processing rooms CSV: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error processing rooms CSV: " + e.getMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

    @PostMapping("/overseer/upload-planner/lecturer-availability")
    @Transactional
    public String uploadLecturerAvailability(
            @RequestParam("file") MultipartFile file,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please upload a CSV file.");
            return "redirect:/overseer/upload-planner";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            int batchSize = 1000;
            List<LecturerAvailability> availabilityBatch = new ArrayList<>();
            int rowCount = 0;
            int successfulRows = 0;
            int addedCount = 0;
            int skippedCount = 0;

            Map<String, List<Integer>> errorMap = new HashMap<>();
            Map<String, Integer> errorCounts = new HashMap<>();

            String[] expectedHeaders = {"lecturerEmail", "dayOfWeek", "startTime", "endTime"};

            while ((line = reader.readLine()) != null) {
                // Remove comments (anything after //) before splitting
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex).trim();
                }

                String[] data = line.split(",");
                if (firstLine) {
                    if (!Arrays.equals(data, expectedHeaders)) {
                        redirectAttributes.addFlashAttribute("error", "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
                        return "redirect:/overseer/upload-planner";
                    }
                    firstLine = false;
                    continue;
                }

                if (data.length < 4) {
                    String errorMsg = "Invalid CSV format, expected 4 columns";
                    errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge(errorMsg, 1, Integer::sum);
                    logger.error("Row {}: {}", rowCount + 2, errorMsg);
                    rowCount++;
                    continue;
                }

                try {
                    String lecturerEmail = data[0].trim();
                    String dayOfWeek = data[1].trim();
                    String startTime = data[2].trim();
                    String endTime = data[3].trim();

                    // Validate email format
                    if (!lecturerEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                        String errorMsg = "Invalid email format: " + lecturerEmail;
                        errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge(errorMsg, 1, Integer::sum);
                        logger.error("Row {}: {}", rowCount + 2, errorMsg);
                        rowCount++;
                        continue;
                    }

                    // Validate day of week
                    Course.DayOfWeek parsedDayOfWeek;
                    try {
                        parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
                    } catch (IllegalArgumentException e) {
                        String errorMsg = "Invalid dayOfWeek: " + dayOfWeek + ". Expected: MONDAY, TUESDAY, ..., SATURDAY";
                        errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge(errorMsg, 1, Integer::sum);
                        logger.error("Row {}: {}", rowCount + 2, errorMsg);
                        rowCount++;
                        continue;
                    }

                    // Validate time format with regex before parsing
                    String timePattern = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$";
                    if (!startTime.matches(timePattern)) {
                        String errorMsg = "Invalid startTime format: " + startTime + ". Expected: HH:mm (e.g., 09:00)";
                        errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge(errorMsg, 1, Integer::sum);
                        logger.error("Row {}: {}", rowCount + 2, errorMsg);
                        rowCount++;
                        continue;
                    }
                    if (!endTime.matches(timePattern)) {
                        String errorMsg = "Invalid endTime format: " + endTime + ". Expected: HH:mm (e.g., 11:00)";
                        errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge(errorMsg, 1, Integer::sum);
                        logger.error("Row {}: {}", rowCount + 2, errorMsg);
                        rowCount++;
                        continue;
                    }

                    // Parse times
                    LocalTime parsedStartTime, parsedEndTime;
                    try {
                        parsedStartTime = LocalTime.parse(startTime);
                        parsedEndTime = LocalTime.parse(endTime);
                        if (parsedEndTime.isBefore(parsedStartTime) || parsedEndTime.equals(parsedStartTime)) {
                            throw new IllegalArgumentException("End time must be after start time.");
                        }
                    } catch (Exception e) {
                        String errorMsg = "Invalid time format: " + e.getMessage();
                        errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge(errorMsg, 1, Integer::sum);
                        logger.error("Row {}: {}", rowCount + 2, errorMsg);
                        rowCount++;
                        continue;
                    }

                    // Find lecturer
                    Optional<Lecturer> lecturerOpt = lecturerRepository.findByEmail(lecturerEmail);
                    if (lecturerOpt.isEmpty()) {
                        String errorMsg = "Lecturer not found: " + lecturerEmail;
                        errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge(errorMsg, 1, Integer::sum);
                        logger.error("Row {}: {}", rowCount + 2, errorMsg);
                        rowCount++;
                        continue;
                    }
                    Lecturer lecturer = lecturerOpt.get();

                    // Check for existing identical availability (idempotency)
                    Optional<LecturerAvailability> existingAvailability = lecturerAvailabilityRepository
                            .findByLecturerIdAndDayOfWeekAndTimes(
                                    lecturer.getId(), parsedDayOfWeek, parsedStartTime, parsedEndTime);
                    if (existingAvailability.isPresent()) {
                        skippedCount++;
                        logger.info("Row {}: Availability for {} on {} {}-{} already exists, skipped",
                                rowCount + 2, lecturerEmail, dayOfWeek, startTime, endTime);
                        successfulRows++;
                        rowCount++;
                        continue;
                    }

                    // Check for overlapping availability
                    List<LecturerAvailability> overlapping = lecturerAvailabilityRepository
                            .findOverlappingByLecturerIdAndDayOfWeek(
                                    lecturer.getId(), parsedDayOfWeek, parsedStartTime, parsedEndTime);
                    if (!overlapping.isEmpty()) {
                        LecturerAvailability conflict = overlapping.get(0);
                        String errorMsg = "Overlapping availability for " + lecturerEmail + " on " + dayOfWeek + ": " +
                                conflict.getStartTime() + "-" + conflict.getEndTime();
                        errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge(errorMsg, 1, Integer::sum);
                        logger.error("Row {}: {}", rowCount + 2, errorMsg);
                        rowCount++;
                        continue;
                    }

                    // Create new availability
                    LecturerAvailability availability = new LecturerAvailability();
                    availability.setLecturer(lecturer);
                    availability.setDayOfWeek(parsedDayOfWeek);
                    availability.setStartTime(parsedStartTime);
                    availability.setEndTime(parsedEndTime);
                    availabilityBatch.add(availability);

                    addedCount++;
                    logger.info("Row {}: Added availability for {} on {} {}-{}",
                            rowCount + 2, lecturerEmail, dayOfWeek, startTime, endTime);
                    successfulRows++;
                    rowCount++;

                    if (availabilityBatch.size() >= batchSize) {
                        lecturerAvailabilityRepository.saveAll(availabilityBatch);
                        availabilityBatch.clear();
                    }
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge(errorMsg, 1, Integer::sum);
                    logger.error("Row {}: {}", rowCount + 2, errorMsg);
                    rowCount++;
                    continue;
                }
            }

            if (!availabilityBatch.isEmpty()) {
                lecturerAvailabilityRepository.saveAll(availabilityBatch);
            }

            // Build concise summary message for successes
            String successSummary = "Lecturer availability uploaded successfully! " + successfulRows + " rows processed.";
            if (addedCount > 0 || skippedCount > 0) {
                List<String> successDetails = new ArrayList<>();
                if (addedCount > 0) {
                    successDetails.add("Added " + addedCount + " availabilities");
                }
                if (skippedCount > 0) {
                    successDetails.add("Skipped " + skippedCount + " duplicates");
                }
                successSummary += " " + String.join(", ", successDetails) + ".";
            }
            redirectAttributes.addFlashAttribute("success", successSummary);

            if (!errorMap.isEmpty()) {
                List<String> errorSummary = new ArrayList<>();
                int errorTypeLimit = 5;
                int totalErrorTypes = errorMap.size();
                int displayedErrorTypes = 0;
                for (Map.Entry<String, List<Integer>> entry : errorMap.entrySet()) {
                    if (displayedErrorTypes >= errorTypeLimit) {
                        break;
                    }
                    String errorMsg = entry.getKey();
                    List<Integer> rows = entry.getValue();
                    int count = errorCounts.get(errorMsg);
                    List<Integer> exampleRows = rows.size() > 5 ? rows.subList(0, 5) : rows;
                    String rowExamples = exampleRows.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                    errorSummary.add(errorMsg + ": " + count + " rows (e.g., rows " + rowExamples +
                            (rows.size() > 5 ? ", ...)" : ")"));
                    displayedErrorTypes++;
                }
                String errorMessage = "Some rows failed: " + String.join("; ", errorSummary) + ".";
                if (totalErrorTypes > errorTypeLimit) {
                    errorMessage += " Additional errors logged (" + (totalErrorTypes - errorTypeLimit) + " more types).";
                }
                errorMessage += " Check logs for details.";
                redirectAttributes.addFlashAttribute("error", errorMessage);
            }
            logger.info(successSummary + (errorMap.isEmpty() ? "" : " Errors: " + errorMap.toString()));
        } catch (Exception e) {
            logger.error("Error processing lecturer availability CSV: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error processing lecturer availability CSV: " + e.getMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

    @PostMapping("/overseer/upload-planner/timetable-sheet")
    @Transactional
    public String uploadTimetableSheet(
            @RequestParam("file") MultipartFile file,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please upload a CSV file.");
            return "redirect:/overseer/upload-planner";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            List<Course> coursesToUpdate = new ArrayList<>();
            int rowCount = 0;
            Map<String, List<Integer>> errorMap = new HashMap<>();
            Map<String, Integer> errorCounts = new HashMap<>();
            int successfulRows = 0;

            String[] expectedHeaders = {"Course Code", "Department", "Year", "DayOfWeek", "StartTime", "EndTime", "RoomName"};

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (firstLine) {
                    if (!Arrays.equals(data, expectedHeaders)) {
                        redirectAttributes.addFlashAttribute("error", "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
                        return "redirect:/overseer/upload-planner";
                    }
                    firstLine = false;
                    continue;
                }

                if (data.length != 7) {
                    String errorMsg = "Invalid CSV format, expected 7 columns";
                    errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge(errorMsg, 1, Integer::sum);
                    logger.error("Row {}: {}", rowCount + 2, errorMsg);
                    rowCount++;
                    continue;
                }

                try {
                    String courseCode = data[0].trim();
                    String departmentName = data[1].trim();
                    int year = Integer.parseInt(data[2].trim());
                    String dayOfWeek = data[3].trim();
                    LocalTime startTime = LocalTime.parse(data[4].trim());
                    LocalTime endTime = LocalTime.parse(data[5].trim());
                    String roomName = data[6].trim();

                    Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);

                    if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
                        throw new IllegalArgumentException("End time must be after start time.");
                    }

                    CourseDefinition courseDefinition = courseDefinitionRepository.findByCode(courseCode)
                            .orElseThrow(() -> new IllegalArgumentException("Course code not found: " + courseCode));
                    Department department = departmentRepository.findByName(departmentName)
                            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentName));

                    if (!courseDefinition.getDepartment().getName().equals(departmentName) || courseDefinition.getYear() != year) {
                        throw new IllegalArgumentException("Course " + courseCode + " does not belong to " + departmentName + " Year " + year);
                    }

                    Room room = roomRepository.findByName(roomName)
                            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomName));

                    // Create a new Course instead of overwriting
                    Course newCourse = new Course();
                    newCourse.setCourseDefinition(courseDefinition);
                    newCourse.setStatus(Course.CourseInstanceStatus.DRAFT);
                    newCourse.setDayOfWeek(parsedDayOfWeek);
                    newCourse.setStartTime(startTime);
                    newCourse.setEndTime(endTime);
                    newCourse.setRoom(room);

                    Lecturer lecturer = courseDefinition.getLecturer();
                    if (lecturer != null) {
                        if (!lecturer.isAvailable(parsedDayOfWeek, startTime, endTime)) {
                            throw new IllegalArgumentException("Lecturer " + lecturer.getEmail() + " is not available on " + dayOfWeek + " from " + startTime + " to " + endTime);
                        }
                        List<Course> conflictingLecturerCourses = courseRepository.findByLecturerAndDayOfWeekAndTimeOverlap(
                                lecturer.getId(), parsedDayOfWeek, startTime, endTime);
                        if (!conflictingLecturerCourses.isEmpty()) {
                            Course conflict = conflictingLecturerCourses.get(0);
                            throw new IllegalArgumentException("Lecturer " + lecturer.getEmail() + " is already scheduled for " +
                                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                                    conflict.getStartTime() + " to " + conflict.getEndTime());
                        }
                    }

                    List<Course> conflictingRoomCourses = courseRepository.findByDayOfWeekAndRoomAndTimeOverlap(room, parsedDayOfWeek, startTime, endTime);
                    if (!conflictingRoomCourses.isEmpty()) {
                        Course conflict = conflictingRoomCourses.get(0);
                        throw new IllegalArgumentException("Room " + room.getName() + " is already booked for " +
                                conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                                conflict.getStartTime() + " to " + conflict.getEndTime());
                    }

                    int studentCount = courseDefinition.getTotalStudents() != null ? courseDefinition.getTotalStudents() : 0;
                    if (studentCount > room.getCapacity()) {
                        throw new IllegalArgumentException("Room capacity (" + room.getCapacity() + ") is less than student count (" + studentCount + ")");
                    }

                    coursesToUpdate.add(newCourse);
                    successfulRows++;
                    logger.info("Created new course {} with schedule: {} {}-{}", courseCode, dayOfWeek, startTime, endTime);
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    errorMap.computeIfAbsent(errorMsg, k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge(errorMsg, 1, Integer::sum);
                    logger.error("Row {}: {}", rowCount + 2, errorMsg);
                }
                rowCount++;
            }

            if (!coursesToUpdate.isEmpty()) {
                courseRepository.saveAll(coursesToUpdate);
            }

            String successSummary = "Timetable sheet uploaded successfully! " + successfulRows + " courses scheduled.";
            redirectAttributes.addFlashAttribute("success", successSummary);

            if (!errorMap.isEmpty()) {
                List<String> errorSummary = new ArrayList<>();
                int errorTypeLimit = 5;
                int totalErrorTypes = errorMap.size();
                int displayedErrorTypes = 0;
                for (Map.Entry<String, List<Integer>> entry : errorMap.entrySet()) {
                    if (displayedErrorTypes >= errorTypeLimit) break;
                    String errorMsg = entry.getKey();
                    List<Integer> rows = entry.getValue();
                    int count = errorCounts.get(errorMsg);
                    List<Integer> exampleRows = rows.size() > 5 ? rows.subList(0, 5) : rows;
                    String rowExamples = exampleRows.stream().map(String::valueOf).collect(Collectors.joining(", "));
                    errorSummary.add(errorMsg + ": " + count + " rows (e.g., rows " + rowExamples + (rows.size() > 5 ? ", ...)" : ")"));
                    displayedErrorTypes++;
                }
                String errorMessage = "Some rows failed: " + String.join("; ", errorSummary) + ".";
                if (totalErrorTypes > errorTypeLimit) errorMessage += " Additional errors logged (" + (totalErrorTypes - errorTypeLimit) + " more types).";
                errorMessage += " Check logs for details.";
                redirectAttributes.addFlashAttribute("error", errorMessage);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing timetable sheet CSV: " + e.getMessage());
        }
        return "redirect:/overseer/upload-planner";
    }

    @GetMapping("/coordinator/view-timetable")
    public String getCoordinatorTimetable(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String dayOfWeek,
            Authentication auth, Model model) {
        List<CoordinatorAssignment> assignments = timetableService.getCoordinatorAssignments(auth);
        System.out.println("Assignments for " + auth.getName() + ": " + assignments); // Debug log
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

    @GetMapping("/student/departments-by-school")
    @ResponseBody
    public List<Map<String, Object>> getDepartmentsBySchoolForStudent(@RequestParam(required = false) Long schoolId) {
        System.out.println("Fetching departments for student with schoolId: " + schoolId);
        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        System.out.println("Found " + departments.size() + " departments for student: " + departments);
        return departments.stream()
                .map(dept -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", dept.getId());
                    item.put("name", dept.getName());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/student/courses")
    public String getStudentCourses(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Model model) {
        model.addAttribute("schools", timetableService.getAllSchools());
        model.addAttribute("departments", timetableService.getDepartmentsBySchool(schoolId));
        model.addAttribute("years", YEARS);

        if (schoolId != null && departmentId != null && year != null) {
            List<CourseDefinition> courses = timetableService.getStudentCourseDefinition(departmentId, year);
            model.addAttribute("courses", courses);
            model.addAttribute("selectedSchoolId", schoolId);
            model.addAttribute("selectedDepartmentId", departmentId);
            model.addAttribute("selectedYear", year);
        } else if (schoolId != null && departmentId != null) {
            model.addAttribute("selectedSchoolId", schoolId);
            model.addAttribute("selectedDepartmentId", departmentId);
        } else if (schoolId != null) {
            model.addAttribute("selectedSchoolId", schoolId);
        }

        return "student-courses";
    }
}