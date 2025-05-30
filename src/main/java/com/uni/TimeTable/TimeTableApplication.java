package com.uni.TimeTable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableTransactionManagement
@ComponentScan(basePackages = "com.uni.TimeTable")
public class TimeTableApplication {

	public static void main(String[] args) {
	SpringApplication.run(TimeTableApplication.class, args);
	}
}

