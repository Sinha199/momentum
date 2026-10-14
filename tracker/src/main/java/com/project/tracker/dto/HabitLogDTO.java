package com.project.tracker.dto;

import com.project.tracker.entity.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HabitLogDTO {
    private Long userId;
    @NotNull
    //private Long categoryId;
    private String categoryName;
    @NotNull
    private LocalDate date;
    @NotNull
    private Status status;
}
