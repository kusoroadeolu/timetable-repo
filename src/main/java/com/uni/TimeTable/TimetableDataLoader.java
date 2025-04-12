package com.uni.TimeTable;

import com.uni.TimeTable.models.*;
import com.uni.TimeTable.repository.*;
import com.uni.TimeTable.service.TimetableService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class TimetableDataLoader implements CommandLineRunner {

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private LecturerRepository lecturerRepository;

    @Autowired
    private CoordinatorRepository coordinatorRepository;

    @Override
    public void run(String... args) throws Exception {
        String excelPath = "src/main/resources/timetable.xlsx";
        loadTimetableFromExcel(excelPath);
    }

    private void loadTimetableFromExcel(String excelPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Step 1: Extract Course Details (Rows 2-8)
            Map<String, CourseDetails> courseMap = new HashMap<>();
            for (int rowNum = 1; rowNum <= 7; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                String codeDesc = getStringCellValue(row.getCell(1));
                String unitsStatus = getStringCellValue(row.getCell(2));

                if (codeDesc != null && unitsStatus != null) {
                    String[] codeParts = codeDesc.split(" - ");
                    String code = codeParts[0].trim();
                    String name = codeParts.length > 1 ? codeParts[1].trim() : code;
                    String[] unitsParts = unitsStatus.split(" ");
                    int units = Integer.parseInt(unitsParts[0].trim());

                    courseMap.put(code, new CourseDetails(name, units));
                }
            }

            // Step 2: Parse Schedule Grid (Rows 10+)
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            int[] courseColIndices = {5, 6, 7, 8, 9};
            int[] locationColIndices = {7, 8, 9, 10, 11};

            Authentication auth = new AuthenticationMock("coord1");
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Look up or create department by name
            Department csDept = departmentRepository.findByName("Computer Science")
                    .orElseGet(() -> {
                        Department dept = new Department();
                        dept.setName("Computer Science");
                        return departmentRepository.save(dept);
                    });

            Long defaultLecturerId = 1L;
            lecturerRepository.findById(defaultLecturerId)
                    .orElseThrow(() -> new IllegalArgumentException("Default lecturer not found"));

            for (int rowNum = 9; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                String timeSlot = getStringCellValue(row.getCell(3));
                if (timeSlot == null || timeSlot.equals("Break")) continue;

                String[] times = timeSlot.split("-");
                String startTime = times[0].trim().replace(":", "");
                String endTime = times[1].trim().replace(":", "");

                for (int dayIdx = 0; dayIdx < days.length; dayIdx++) {
                    String courseCode = getStringCellValue(row.getCell(courseColIndices[dayIdx]));
                    String location = getStringCellValue(row.getCell(locationColIndices[dayIdx]));
                    if (courseCode == null || courseCode.isEmpty()) continue;

                    CourseDetails details = courseMap.get(courseCode);
                    if (details == null) {
                        System.err.println("Course not found: " + courseCode + " at row " + (rowNum + 1));
                        continue;
                    }

                    try {
                        timetableService.scheduleCourse(
                                courseCode,
                                details.name,
                                csDept.getId(),
                                1,
                                startTime,
                                endTime,
                                days[dayIdx],
                                location != null ? location : "",
                                defaultLecturerId,
                                details.units,
                                null,
                                auth
                        );
                        System.out.println("Scheduled: " + courseCode + " on " + days[dayIdx] + " at " + timeSlot);
                    } catch (Exception e) {
                        System.err.println("Error scheduling " + courseCode + " on " + days[dayIdx] + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
            default: return null;
        }
    }

    private static class CourseDetails {
        String name;
        int units;

        CourseDetails(String name, int units) {
            this.name = name;
            this.units = units;
        }
    }

    private static class AuthenticationMock implements Authentication {
        private final String name;

        public AuthenticationMock(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public Object getCredentials() { return null; }
        @Override public Object getDetails() { return null; }
        @Override public Object getPrincipal() { return name; }
        @Override public boolean isAuthenticated() { return true; }
        @Override public void setAuthenticated(boolean isAuthenticated) {}
        @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() { return java.util.Collections.emptyList(); }
    }
}