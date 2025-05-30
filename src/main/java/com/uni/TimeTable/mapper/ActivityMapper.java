package com.uni.TimeTable.mapper;

import com.uni.TimeTable.DTO.ActivityDto;
import com.uni.TimeTable.models.Activity;
import com.uni.TimeTable.utils.DateTimeUtils;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import static com.uni.TimeTable.utils.DateTimeUtils.getRelativeTime;

@Component
@NoArgsConstructor
public class ActivityMapper {

    private DateTimeUtils utils;

    public ActivityDto toDto(Activity activity) {
        String timestamp = getRelativeTime(activity.getCreatedAt());
        return new ActivityDto(activity.getDescription(), timestamp);
    }


}