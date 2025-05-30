package com.uni.TimeTable.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimetableScheduleDTO {
    private Long courseDefinitionId;
    private Long schoolId;
    private Long departmentId;
    private Integer year;
    private String startTime;
    private String endTime;
    private String dayOfWeek;
    private String elearningLink;
    private Long roomId;

}