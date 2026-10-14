package com.project.tracker.controller;

import com.project.tracker.dto.HabitLogDTO;
import com.project.tracker.dto.HabitLogResponseDTO;
import com.project.tracker.entity.HabitLog;
import com.project.tracker.service.HabitLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/logs")
public class HabitLogController {
    private final HabitLogService habitLogService;

    public HabitLogController(HabitLogService habitLogService) {
        this.habitLogService = habitLogService;
    }

    @PostMapping
    public HabitLogResponseDTO createOrUpdateLog(@Valid @RequestBody HabitLogDTO request) {
        return habitLogService.saveAndUpdateLog(request.getCategoryName(), request.getDate(), request.getStatus());
    }

    @GetMapping
    public List<HabitLogResponseDTO> getLogs(@RequestParam(name = "month") String month) {
        return habitLogService.getLogsForMonth(YearMonth.parse(month));
    }
}
