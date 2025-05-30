package com.uni.TimeTable.service;

import com.uni.TimeTable.models.*;
import com.uni.TimeTable.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    private final LecturerRepository lecturerRepository;
    private final CourseDefinitionRepository courseDefinitionRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final BuildingRepository buildingRepository;
    private final LecturerAvailabilityRepository lecturerAvailabilityRepository;

    public static class UploadResult {
        private final String successMessage;
        private final String errorMessage;

        public UploadResult(String successMessage, String errorMessage) {
            this.successMessage = successMessage;
            this.errorMessage = errorMessage;
        }

        public String getSuccessMessage() {
            return successMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    @Transactional
    public UploadResult uploadAcademicPlanner(MultipartFile file, Long schoolId) {
        if (file.isEmpty()) {
            return new UploadResult(null, "Please upload a CSV file.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

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
                        return new UploadResult(null, "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
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
                    int credits = Integer.parseInt(creditUnits);
                    int firstTimeStudents = Integer.parseInt(firstTimeStudentsStr);
                    int carryoverStudents = Integer.parseInt(carryoverStudentsStr);
                    int totalStudents = Integer.parseInt(totalStudentsStr);

                    validateAcademicPlannerRow(year, credits, firstTimeStudents, carryoverStudents, totalStudents, status, lecturerName, lecturerEmail, facultyStatus);

                    Lecturer.LecturerType lecturerType = "Active".equalsIgnoreCase(facultyStatus)
                            ? Lecturer.LecturerType.FULL_TIME
                            : Lecturer.LecturerType.ADJUNCT;

                    Department department = departmentRepository.findByNameIgnoreCase(departmentName)
                            .orElseGet(() -> {
                                Department newDept = new Department();
                                newDept.setName(departmentName);
                                newDept.setSchool(school);
                                return departmentRepository.save(newDept);
                            });

                    Lecturer lecturer = lecturerRepository.findByEmail(lecturerEmail)
                            .orElseGet(() -> {
                                Lecturer newLecturer = new Lecturer();
                                newLecturer.setName(lecturerName);
                                newLecturer.setEmail(lecturerEmail);
                                newLecturer.setType(lecturerType);
                                return lecturerRepository.save(newLecturer);
                            });

                    CourseDefinition courseDefinition = new CourseDefinition();
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
                    courseDefinitionRepository.save(courseDefinition);

                    successes.add("Row " + (rowCount + 2) + ": Course " + courseCode + " (" + departmentName + ", Year " + year + ")");
                    successfulRows++;
                } catch (Exception e) {
                    errors.add("Row " + (rowCount + 2) + ": " + e.getMessage());
                }
                rowCount++;
            }

            String successMessage = "Academic planner uploaded successfully! " + successfulRows + " rows processed.";
            if (!successes.isEmpty()) successMessage += " Processed rows: " + String.join("; ", successes);
            String errorMessage = errors.isEmpty() ? null : "However, some rows failed: " + String.join("; ", errors);
            return new UploadResult(successMessage, errorMessage);
        } catch (Exception e) {
            return new UploadResult(null, "Error processing academic planner CSV: " + e.getMessage());
        }
    }

    @Transactional
    public UploadResult uploadRooms(MultipartFile file) {
        if (file.isEmpty()) {
            return new UploadResult(null, "Please upload a CSV file.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
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
                        return new UploadResult(null, "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
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
                    int capacity = Integer.parseInt(data[2].trim());

                    if (capacity <= 0) throw new IllegalArgumentException("Capacity must be a positive integer.");
                    if (!roomNamesInCsv.add(roomName + ":" + buildingName)) throw new IllegalArgumentException("Duplicate room name found.");

                    Building building = buildingRepository.findByNameIgnoreCase(buildingName)
                            .orElseGet(() -> {
                                Building newBuilding = new Building();
                                newBuilding.setName(buildingName);
                                return buildingRepository.save(newBuilding);
                            });

                    Room room = roomRepository.findByNameAndBuildingId(roomName, building.getId())
                            .orElseGet(() -> {
                                Room newRoom = new Room();
                                newRoom.setName(roomName);
                                newRoom.setCapacity(capacity);
                                newRoom.setBuilding(building);
                                return newRoom;
                            });

                    if (room.getCapacity() != capacity) {
                        room.setCapacity(capacity);
                        roomsBatch.add(room);
                        successes.add("Row " + (rowCount + 2) + ": Updated room " + roomName);
                    } else {
                        successes.add("Row " + (rowCount + 2) + ": Room " + roomName + " already exists, skipped");
                    }

                    successfulRows++;
                } catch (Exception e) {
                    errors.add("Row " + (rowCount + 2) + ": " + e.getMessage());
                }
                rowCount++;
            }

            if (!roomsBatch.isEmpty()) roomRepository.saveAll(roomsBatch);

            String successMessage = "Rooms uploaded successfully! " + successfulRows + " rows processed.";
            if (!successes.isEmpty()) successMessage += " Details: " + String.join("; ", successes);
            String errorMessage = errors.isEmpty() ? null : "However, some rows failed: " + String.join("; ", errors);
            return new UploadResult(successMessage, errorMessage);
        } catch (Exception e) {
            return new UploadResult(null, "Error processing rooms CSV: " + e.getMessage());
        }
    }

    @Transactional
    public UploadResult uploadLecturerAvailability(MultipartFile file) {
        if (file.isEmpty()) {
            return new UploadResult(null, "Please upload a CSV file.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            List<LecturerAvailability> availabilityBatch = new ArrayList<>();
            int rowCount = 0;
            int successfulRows = 0;
            int addedCount = 0;
            int skippedCount = 0;
            Map<String, List<Integer>> errorMap = new HashMap<>();
            Map<String, Integer> errorCounts = new HashMap<>();

            String[] expectedHeaders = {"lecturerEmail", "dayOfWeek", "startTime", "endTime"};

            while ((line = reader.readLine()) != null) {
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) line = line.substring(0, commentIndex).trim();
                String[] data = line.split(",");
                if (firstLine) {
                    if (!Arrays.equals(data, expectedHeaders)) {
                        return new UploadResult(null, "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
                    }
                    firstLine = false;
                    continue;
                }

                if (data.length < 4) {
                    errorMap.computeIfAbsent("Invalid CSV format, expected 4 columns", k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge("Invalid CSV format, expected 4 columns", 1, Integer::sum);
                    rowCount++;
                    continue;
                }

                try {
                    String lecturerEmail = data[0].trim();
                    String dayOfWeek = data[1].trim();
                    String startTime = data[2].trim();
                    String endTime = data[3].trim();

                    if (!lecturerEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                        errorMap.computeIfAbsent("Invalid email format: " + lecturerEmail, k -> new ArrayList<>()).add(rowCount + 2);
                        errorCounts.merge("Invalid email format: " + lecturerEmail, 1, Integer::sum);
                        rowCount++;
                        continue;
                    }

                    Course.DayOfWeek parsedDayOfWeek = Course.DayOfWeek.valueOf(dayOfWeek);
                    LocalTime parsedStartTime = LocalTime.parse(startTime);
                    LocalTime parsedEndTime = LocalTime.parse(endTime);

                    if (parsedEndTime.isBefore(parsedStartTime) || parsedEndTime.equals(parsedStartTime)) {
                        throw new IllegalArgumentException("End time must be after start time.");
                    }

                    Lecturer lecturer = lecturerRepository.findByEmail(lecturerEmail)
                            .orElseThrow(() -> new IllegalArgumentException("Lecturer not found: " + lecturerEmail));

                    if (lecturerAvailabilityRepository.findByLecturerIdAndDayOfWeekAndTimes(lecturer.getId(), parsedDayOfWeek, parsedStartTime, parsedEndTime).isPresent()) {
                        skippedCount++;
                        successfulRows++;
                        rowCount++;
                        continue;
                    }

                    if (!lecturerAvailabilityRepository.findOverlappingByLecturerIdAndDayOfWeek(lecturer.getId(), parsedDayOfWeek, parsedStartTime, parsedEndTime).isEmpty()) {
                        throw new IllegalArgumentException("Overlapping availability for " + lecturerEmail);
                    }

                    LecturerAvailability availability = new LecturerAvailability();
                    availability.setLecturer(lecturer);
                    availability.setDayOfWeek(parsedDayOfWeek);
                    availability.setStartTime(parsedStartTime);
                    availability.setEndTime(parsedEndTime);
                    availabilityBatch.add(availability);
                    addedCount++;
                    successfulRows++;
                } catch (Exception e) {
                    errorMap.computeIfAbsent(e.getMessage(), k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge(e.getMessage(), 1, Integer::sum);
                }
                rowCount++;
            }

            if (!availabilityBatch.isEmpty()) lecturerAvailabilityRepository.saveAll(availabilityBatch);

            String successMessage = "Lecturer availability uploaded successfully! " + successfulRows + " rows processed.";
            if (addedCount > 0 || skippedCount > 0) {
                successMessage += " Added " + addedCount + ", skipped " + skippedCount + " duplicates.";
            }
            String errorMessage = null;
            if (!errorMap.isEmpty()) {
                List<String> errorSummary = errorMap.entrySet().stream()
                        .limit(5)
                        .map(e -> e.getKey() + ": " + errorCounts.get(e.getKey()) + " rows")
                        .collect(Collectors.toList());
                errorMessage = "Some rows failed: " + String.join("; ", errorSummary) + ".";
                if (errorMap.size() > 5) errorMessage += " Additional errors logged.";
            }
            return new UploadResult(successMessage, errorMessage);
        } catch (Exception e) {
            return new UploadResult(null, "Error processing lecturer availability CSV: " + e.getMessage());
        }
    }

    @Transactional
    public UploadResult uploadTimetableSheet(MultipartFile file) {
        if (file.isEmpty()) {
            return new UploadResult(null, "Please upload a CSV file.");
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
                        return new UploadResult(null, "Invalid CSV headers. Expected: " + String.join(",", expectedHeaders));
                    }
                    firstLine = false;
                    continue;
                }

                if (data.length != 7) {
                    errorMap.computeIfAbsent("Invalid CSV format, expected 7 columns", k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge("Invalid CSV format, expected 7 columns", 1, Integer::sum);
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
                    Room room = roomRepository.findByName(roomName)
                            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomName));

                    if (!courseDefinition.getDepartment().getName().equals(departmentName) || courseDefinition.getYear() != year) {
                        throw new IllegalArgumentException("Course mismatch with department or year.");
                    }

                    Course newCourse = new Course();
                    newCourse.setCourseDefinition(courseDefinition);
                    newCourse.setStatus(Course.CourseInstanceStatus.DRAFT);
                    newCourse.setDayOfWeek(parsedDayOfWeek);
                    newCourse.setStartTime(startTime);
                    newCourse.setEndTime(endTime);
                    newCourse.setRoom(room);

                    Lecturer lecturer = courseDefinition.getLecturer();
                    if (lecturer != null && !lecturer.isAvailable(parsedDayOfWeek, startTime, endTime)) {
                        throw new IllegalArgumentException("Lecturer unavailable.");
                    }

                    if (!courseRepository.findByDayOfWeekAndRoomAndTimeOverlap(room, parsedDayOfWeek, startTime, endTime).isEmpty()) {
                        throw new IllegalArgumentException("Room conflict.");
                    }

                    int studentCount = courseDefinition.getTotalStudents() != null ? courseDefinition.getTotalStudents() : 0;
                    if (studentCount > room.getCapacity()) {
                        throw new IllegalArgumentException("Room capacity exceeded.");
                    }

                    coursesToUpdate.add(newCourse);
                    successfulRows++;
                } catch (Exception e) {
                    errorMap.computeIfAbsent(e.getMessage(), k -> new ArrayList<>()).add(rowCount + 2);
                    errorCounts.merge(e.getMessage(), 1, Integer::sum);
                }
                rowCount++;
            }

            if (!coursesToUpdate.isEmpty()) courseRepository.saveAll(coursesToUpdate);

            String successMessage = "Timetable sheet uploaded successfully! " + successfulRows + " courses scheduled.";
            String errorMessage = null;
            if (!errorMap.isEmpty()) {
                List<String> errorSummary = errorMap.entrySet().stream()
                        .limit(5)
                        .map(e -> e.getKey() + ": " + errorCounts.get(e.getKey()) + " rows")
                        .collect(Collectors.toList());
                errorMessage = "Some rows failed: " + String.join("; ", errorSummary) + ".";
                if (errorMap.size() > 5) errorMessage += " Additional errors logged.";
            }
            return new UploadResult(successMessage, errorMessage);
        } catch (Exception e) {
            return new UploadResult(null, "Error processing timetable sheet CSV: " + e.getMessage());
        }
    }

    private void validateAcademicPlannerRow(int year, int credits, int firstTimeStudents, int carryoverStudents,
                                            int totalStudents, String status, String lecturerName, String lecturerEmail,
                                            String facultyStatus) {
        if (year < 1 || year > 5) {
            throw new IllegalArgumentException("Year must be between 1 and 5: " + year);
        }
        if (credits < 1 || credits > 10) {
            throw new IllegalArgumentException("Credits must be between 1 and 10: " + credits);
        }
        if (firstTimeStudents < 0 || carryoverStudents < 0 || totalStudents < 0) {
            throw new IllegalArgumentException("Student numbers cannot be negative.");
        }
        if (totalStudents != (firstTimeStudents + carryoverStudents)) {
            throw new IllegalArgumentException("Total students must equal first-time plus carryover students.");
        }
        if (!status.equalsIgnoreCase("Compulsory") && !status.equalsIgnoreCase("Elective")) {
            throw new IllegalArgumentException("Status must be 'Compulsory' or 'Elective': " + status);
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
    }
}

