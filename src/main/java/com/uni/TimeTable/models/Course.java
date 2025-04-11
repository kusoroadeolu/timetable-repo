package com.uni.TimeTable.models;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code; // Replaces name for timetable display
    private String name; // Full name for details
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String location;
    private int year;
    private Integer credits; // New field
    @ManyToOne
    private Department department;
    @ManyToOne
    private Coordinator coordinator;
    @ManyToOne
    private Lecturer lecturer; // New field
    private String elearningLink; // Replaces syllabusLink

    // Constructors
    public Course() {}
    public Course(String code, String name, Department department, int year, String startTime, String endTime,
                  String dayOfWeek, String location, Coordinator coordinator, Lecturer lecturer, Integer credits, String elearningLink) {
        this.code = code;
        this.name = name;
        this.department = department;
        this.year = year;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
        this.location = location;
        this.coordinator = coordinator;
        this.lecturer = lecturer;
        this.credits = credits;
        this.elearningLink = elearningLink;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Coordinator getCoordinator() { return coordinator; }
    public void setCoordinator(Coordinator coordinator) { this.coordinator = coordinator; }
    public Lecturer getLecturer() { return lecturer; }
    public void setLecturer(Lecturer lecturer) { this.lecturer = lecturer; }
    public String getElearningLink() { return elearningLink; }
    public void setElearningLink(String elearningLink) { this.elearningLink = elearningLink; }
}