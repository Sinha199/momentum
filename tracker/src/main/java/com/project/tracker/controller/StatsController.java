package com.project.tracker.controller;

import com.project.tracker.service.StreakService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {
    private final StreakService streakService;

    public StatsController(StreakService streakService) {
        this.streakService = streakService;
    }
    @GetMapping("/streak")
    public Map<String,Integer> streak(){
        return streakService.allStreaks();
    }

}
