package com.uni.TimeTable.controller;

import com.uni.TimeTable.models.*;
import com.uni.TimeTable.exception.ConflictException;
import com.uni.TimeTable.repository.*;
import com.uni.TimeTable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TimetableController {

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
    private static final List<Integer> YEARS = Arrays.asList(1, 2, 3, 4);

    @GetMapping({"/", "/timetable"})
    public String home(Model model) {
        return "home";
    }

    @GetMapping("/overseer/timetable")
    public String getOverseerTimetable(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Authentication auth, Model model) {
        List<School> schools = timetableService.getAllSchools();
        model.addAttribute("schools", schools);

        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        if (departmentId != null) {
            departments = departments.stream()
                    .filter(dept -> dept.getId().equals(departmentId))
                    .collect(Collectors.toList());
        }

        model.addAttribute("departments", departments);
        model.addAttribute("years", YEARS);

        model.addAttribute("selectedSchoolId", schoolId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYear", year);

        List<Course> courses = timetableService.getTimetable(schoolId, departmentId, year, null, auth);
        Map<Long, List<Course>> coursesByDept = new HashMap<>();
        for (Department dept : departments) {
            List<Course> deptCourses = courses.stream()
                    .filter(course -> course.getCourseDefinition().getDepartment().getId().equals(dept.getId()))
                    .collect(Collectors.toList());
            System.out.println("Courses for dept " + dept.getId() + ": " + deptCourses.size());
            deptCourses.forEach(course -> System.out.println("Course: " + course.getCourseDefinition().getCode() +
                    ", Day: " + course.getDayOfWeek() +
                    ", Status: " + course.getStatus() +
                    ", Lecturer: " + (course.getCourseDefinition().getLecturer() != null ? course.getCourseDefinition().getLecturer().getName() : "N/A")));
            coursesByDept.put(dept.getId(), deptCourses);
        }
        model.addAttribute("coursesByDept", coursesByDept);

        return "overseer-timetable";
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

    @GetMapping("/overseer/schedule-timetable")
    public String getScheduleTimetable(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Model model) {
        System.out.println("Loading schedule-timetable with schoolId: " + schoolId + ", departmentId: " + departmentId + ", year: " + year);

        // Filter CourseDefinition options based on departmentId and year
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

        // Filter departments based on schoolId
        List<Department> departments = timetableService.getDepartmentsBySchool(schoolId);
        System.out.println("Initial Departments: " + departments);

        // Add attributes to the model
        model.addAttribute("courseDefinitions", courseDefinitions);
        model.addAttribute("schools", timetableService.getAllSchools());
        model.addAttribute("departments", departments);
        model.addAttribute("lecturers", lecturerRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
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
            @RequestParam Long lecturerId,
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
                    dayOfWeek, lecturerId, elearningLink, roomId, auth);
            redirectAttributes.addFlashAttribute("success", "Timetable scheduled successfully!");
        } catch (ConflictException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/overseer/schedule-timetable";
    }

    @GetMapping("/overseer/reassign-room")
    public String getReassignRoom(Model model) {
        List<Course> courses = courseRepository.findAll();
        model.addAttribute("courses", courses);
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("daysOfWeek", DAYS_OF_WEEK);
        return "overseer-reassign-room";
    }

    @PostMapping("/overseer/reassign-room")
    public String reassignRoom(
            @RequestParam Long courseId,
            @RequestParam Long roomId,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            timetableService.reassignRoom(courseId, roomId, dayOfWeek, startTime, endTime, auth);
            redirectAttributes.addFlashAttribute("success", "Room reassigned successfully!");
        } catch (ConflictException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/overseer/reassign-room";
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
            int batchSize = 1000;
            List<Course> coursesBatch = new ArrayList<>();
            int rowCount = 0;
            int successfulRows = 0;
            List<String> errors = new ArrayList<>();

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
                    String lecturerEmail = data[8].trim();
                    String firstTimeStudentsStr = data[9].trim();
                    String carryoverStudentsStr = data[10].trim();
                    String totalStudentsStr = data[11].trim();

                    int year = Integer.parseInt(levelYear);
                    if (year < 1 || year > 5) {
                        throw new IllegalArgumentException("Year must be between 1 and 5: " + year);
                    }

                    int credits = Integer.parseInt(creditUnits);
                    if (credits <= 0) {
                        throw new IllegalArgumentException("Credits must be a positive integer: " + credits);
                    }

                    if (!status.equals("Compulsory") && !status.equals("Elective")) {
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

                    Department department = departmentRepository.findByName(departmentName)
                            .filter(dept -> dept.getSchool().getId().equals(schoolId))
                            .orElseGet(() -> {
                                Department newDept = new Department();
                                newDept.setName(departmentName);
                                newDept.setSchool(school);
                                return departmentRepository.save(newDept);
                            });

                    Lecturer lecturer = lecturerRepository.findByEmail(lecturerEmail)
                            .orElseThrow(() -> new IllegalArgumentException("Lecturer not found: " + lecturerEmail));

                    CourseDefinition courseDefinition = courseDefinitionRepository.findByCode(courseCode)
                            .orElseGet(() -> new CourseDefinition());
                    courseDefinition.setCode(courseCode);
                    courseDefinition.setName(courseTitle);
                    courseDefinition.setCredits(credits);
                    courseDefinition.setStatus(CourseDefinition.CourseStatus.valueOf(status));
                    courseDefinition.setDepartment(department);
                    courseDefinition.setYear(year);
                    courseDefinition.setLecturer(lecturer);
                    courseDefinition = courseDefinitionRepository.save(courseDefinition);

                    Optional<Course> existingCourse = courseRepository.findByCourseDefinition(courseDefinition);
                    Course course;
                    if (existingCourse.isPresent()) {
                        course = existingCourse.get();
                        course.setFirstTimeStudents(firstTimeStudents);
                        course.setCarryoverStudents(carryoverStudents);
                        course.setTotalStudents(totalStudents);
                        course.setStatus(Course.CourseInstanceStatus.DRAFT);
                    } else {
                        course = new Course();
                        course.setCourseDefinition(courseDefinition);
                        course.setStatus(Course.CourseInstanceStatus.DRAFT);
                        course.setFirstTimeStudents(firstTimeStudents);
                        course.setCarryoverStudents(carryoverStudents);
                        course.setTotalStudents(totalStudents);
                    }
                    coursesBatch.add(course);

                    successfulRows++;
                    rowCount++;

                    if (coursesBatch.size() >= batchSize) {
                        courseRepository.saveAll(coursesBatch);
                        coursesBatch.clear();
                    }
                } catch (Exception e) {
                    errors.add("Row " + (rowCount + 2) + ": " + e.getMessage());
                    rowCount++;
                    continue;
                }
            }

            if (!coursesBatch.isEmpty()) {
                courseRepository.saveAll(coursesBatch);
            }

            String successMessage = "Academic planner uploaded successfully! " + successfulRows + " rows processed.";
            if (!errors.isEmpty()) {
                successMessage += " However, some rows failed: " + String.join("; ", errors);
                redirectAttributes.addFlashAttribute("error", successMessage);
            } else {
                redirectAttributes.addFlashAttribute("success", successMessage);
            }
        } catch (Exception e) {
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
            Set<String> roomNames = new HashSet<>();

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    String[] headers = line.split(",");
                    if (!Arrays.equals(headers, new String[]{"buildingName", "roomName", "capacity"})) {
                        redirectAttributes.addFlashAttribute("error", "Invalid CSV headers. Expected: buildingName,roomName,capacity");
                        return "redirect:/overseer/upload-planner";
                    }
                    firstLine = false;
                    continue;
                }

                String[] data = line.split(",");
                if (data.length < 3) {
                    redirectAttributes.addFlashAttribute("error", "Invalid CSV format at row " + (rowCount + 2));
                    return "redirect:/overseer/upload-planner";
                }

                String buildingName = data[0].trim();
                String roomName = data[1].trim();
                int capacity;
                try {
                    capacity = Integer.parseInt(data[2].trim());
                    if (capacity <= 0) {
                        throw new IllegalArgumentException("Capacity must be a positive integer.");
                    }
                } catch (NumberFormatException e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid capacity format at row " + (rowCount + 2));
                    return "redirect:/overseer/upload-planner";
                }

                if (!roomNames.add(roomName + ":" + buildingName)) {
                    redirectAttributes.addFlashAttribute("error", "Duplicate room name found in CSV at row " + (rowCount + 2) + ": " + roomName);
                    return "redirect:/overseer/upload-planner";
                }

                Building building = buildingRepository.findByName(buildingName)
                        .orElseGet(() -> {
                            Building newBuilding = new Building();
                            newBuilding.setName(buildingName);
                            return buildingRepository.save(newBuilding);
                        });

                Room room = new Room();
                room.setName(roomName);
                room.setCapacity(capacity);
                room.setBuilding(building);
                roomsBatch.add(room);

                rowCount++;

                if (roomsBatch.size() >= batchSize) {
                    roomRepository.saveAll(roomsBatch);
                    roomsBatch.clear();
                }
            }

            if (!roomsBatch.isEmpty()) {
                roomRepository.saveAll(roomsBatch);
            }

            redirectAttributes.addFlashAttribute("success", "Rooms uploaded successfully! " + rowCount + " rows processed.");
        } catch (Exception e) {
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

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    String[] headers = line.split(",");
                    if (!Arrays.equals(headers, new String[]{"lecturerEmail", "dayOfWeek", "startTime", "endTime"})) {
                        redirectAttributes.addFlashAttribute("error", "Invalid CSV headers. Expected: lecturerEmail,dayOfWeek,startTime,endTime");
                        return "redirect:/overseer/upload-planner";
                    }
                    firstLine = false;
                    continue;
                }

                String[] data = line.split(",");
                if (data.length < 4) {
                    redirectAttributes.addFlashAttribute("error", "Invalid CSV format at row " + (rowCount + 2));
                    return "redirect:/overseer/upload-planner";
                }

                String lecturerEmail = data[0].trim();
                String dayOfWeek = data[1].trim();
                String startTime = data[2].trim();
                String endTime = data[3].trim();

                Course.DayOfWeek parsedDayOfWeek;
                try {
                    parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
                } catch (IllegalArgumentException e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid day of week at row " + (rowCount + 2) + ": " + dayOfWeek);
                    return "redirect:/overseer/upload-planner";
                }

                LocalTime parsedStartTime, parsedEndTime;
                try {
                    parsedStartTime = LocalTime.parse(startTime);
                    parsedEndTime = LocalTime.parse(endTime);
                    if (parsedEndTime.isBefore(parsedStartTime) || parsedEndTime.equals(parsedStartTime)) {
                        throw new IllegalArgumentException("End time must be after start time.");
                    }
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid time format at row " + (rowCount + 2) + ": " + e.getMessage());
                    return "redirect:/overseer/upload-planner";
                }

                Lecturer lecturer = lecturerRepository.findByEmail(lecturerEmail)
                        .orElseThrow(() -> new IllegalArgumentException("Lecturer not found: " + lecturerEmail));

                LecturerAvailability availability = new LecturerAvailability();
                availability.setLecturer(lecturer);
                availability.setDayOfWeek(parsedDayOfWeek);
                availability.setStartTime(parsedStartTime);
                availability.setEndTime(parsedEndTime);
                availabilityBatch.add(availability);

                rowCount++;

                if (availabilityBatch.size() >= batchSize) {
                    lecturerAvailabilityRepository.saveAll(availabilityBatch);
                    availabilityBatch.clear();
                }
            }

            if (!availabilityBatch.isEmpty()) {
                lecturerAvailabilityRepository.saveAll(availabilityBatch);
            }

            redirectAttributes.addFlashAttribute("success", "Lecturer availability uploaded successfully! " + rowCount + " rows processed.");
        } catch (Exception e) {
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
            List<String> errors = new ArrayList<>();

            String[] expectedHeaders = {
                    "Course Code", "Department", "Year", "DayOfWeek", "StartTime", "EndTime", "RoomName"
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

                if (data.length != 7) {
                    errors.add("Row " + (rowCount + 2) + ": Invalid CSV format, expected 7 columns");
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

                    Course.DayOfWeek parsedDayOfWeek;
                    try {
                        parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
                    }

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

                    Course course = courseRepository.findByCourseDefinition(courseDefinition)
                            .orElseGet(() -> {
                                Course newCourse = new Course();
                                newCourse.setCourseDefinition(courseDefinition);
                                newCourse.setStatus(Course.CourseInstanceStatus.DRAFT);
                                newCourse.setFirstTimeStudents(0);
                                newCourse.setCarryoverStudents(0);
                                newCourse.setTotalStudents(0);
                                return newCourse;
                            });

                    Lecturer lecturer = courseDefinition.getLecturer();
                    if (lecturer != null) {
                        if (!lecturer.isAvailable(parsedDayOfWeek, startTime, endTime)) {
                            throw new IllegalArgumentException("Lecturer " + lecturer.getEmail() + " is not available on " + dayOfWeek + " from " + startTime + " to " + endTime);
                        }
                        List<Course> conflictingLecturerCourses = courseRepository.findByLecturerAndDayOfWeekAndTimeOverlap(
                                lecturer.getId(), parsedDayOfWeek, startTime, endTime);
                        conflictingLecturerCourses.removeIf(c -> c.getId() != null && c.getId().equals(course.getId()));
                        if (!conflictingLecturerCourses.isEmpty()) {
                            Course conflict = conflictingLecturerCourses.get(0);
                            throw new IllegalArgumentException("Lecturer " + lecturer.getEmail() + " is already scheduled for " +
                                    conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                                    conflict.getStartTime() + " to " + conflict.getEndTime());
                        }
                    }

                    List<Course> conflictingRoomCourses = courseRepository.findByDayOfWeekAndRoomAndTimeOverlap(room, parsedDayOfWeek, startTime, endTime);
                    conflictingRoomCourses.removeIf(c -> c.getId() != null && c.getId().equals(course.getId()));
                    if (!conflictingRoomCourses.isEmpty()) {
                        Course conflict = conflictingRoomCourses.get(0);
                        throw new IllegalArgumentException("Room " + room.getName() + " is already booked for " +
                                conflict.getCourseDefinition().getCode() + " on " + dayOfWeek + " from " +
                                conflict.getStartTime() + " to " + conflict.getEndTime());
                    }

                    int studentCount = course.getTotalStudents();
                    if (studentCount > room.getCapacity()) {
                        throw new IllegalArgumentException("Room capacity (" + room.getCapacity() + ") is less than student count (" + studentCount + ")");
                    }

                    course.setDayOfWeek(parsedDayOfWeek);
                    course.setStartTime(startTime);
                    course.setEndTime(endTime);
                    course.setRoom(room);
                    coursesToUpdate.add(course);

                    rowCount++;
                } catch (Exception e) {
                    errors.add("Row " + (rowCount + 2) + ": " + e.getMessage());
                    rowCount++;
                    continue;
                }
            }

            if (!coursesToUpdate.isEmpty()) {
                courseRepository.saveAll(coursesToUpdate);
            }

            String successMessage = "Timetable sheet uploaded successfully! " + coursesToUpdate.size() + " courses scheduled.";
            if (!errors.isEmpty()) {
                successMessage += " However, some rows failed: " + String.join("; ", errors);
                redirectAttributes.addFlashAttribute("error", successMessage);
            } else {
                redirectAttributes.addFlashAttribute("success", successMessage);
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
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String dayOfWeek,
            Model model) {
        model.addAttribute("departments", timetableService.getDepartmentsBySchool(null));
        model.addAttribute("years", YEARS);
        model.addAttribute("daysOfWeek", DAYS_OF_WEEK);

        if (departmentId == null || year == null) {
            return "student-timetable";
        }

        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedDayOfWeek", dayOfWeek);

        List<Course> courses = timetableService.getTimetable(null, departmentId, year, dayOfWeek, null);
        model.addAttribute("courses", courses);

        return "student-timetable";
    }

    @GetMapping("/student/courses")
    public String getStudentCourses(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            Model model) {
        model.addAttribute("departments", timetableService.getDepartmentsBySchool(null));
        model.addAttribute("years", YEARS);

        if (departmentId != null && year != null) {
            List<Course> courses = timetableService.getStudentCourses(departmentId, year);
            model.addAttribute("courses", courses);
            model.addAttribute("selectedDepartmentId", departmentId);
            model.addAttribute("selectedYear", year);
        }

        return "student-courses";
    }
}