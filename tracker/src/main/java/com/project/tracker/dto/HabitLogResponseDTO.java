package com.project.tracker.dto;

import com.project.tracker.entity.HabitLog;
import com.project.tracker.entity.Category;
import com.project.tracker.entity.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class HabitLogResponseDTO {
    private Long id;

    public HabitLogResponseDTO(HabitLog log) {
        this.id = log.getId();
        this.categoryId = log.getCategory().getId();
        this.date = log.getDate();
        this.categoryName = log.getCategory().getName();
        this.status = log.getStatus();
    }

    private Long categoryId;
    private LocalDate date;
    private String categoryName;
    private Status status;

}
