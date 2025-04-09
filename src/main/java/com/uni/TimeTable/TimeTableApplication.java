package com.uni.TimeTable;

import com.uni.TimeTable.models.Course;
import com.uni.TimeTable.repository.CourseRepository;
import com.uni.TimeTable.repository.DepartmentRepository;
import com.uni.TimeTable.repository.UserRepository;
import com.uni.TimeTable.service.TimetableService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TimeTableApplication {

	public static void main(String[] args) {
		SpringApplication.run(TimeTableApplication.class, args);
	}

//	@Bean
//	public CommandLineRunner loadData(DataLoaderService dataLoaderService,
//									  TimetableService timetableService,
//									  DepartmentRepository departmentRepo,
//									  UserRepository userRepo,
//									  CourseRepository courseRepo) {
//		return args -> {
//			dataLoaderService.loadInitialData();
//		};
//	}
}

