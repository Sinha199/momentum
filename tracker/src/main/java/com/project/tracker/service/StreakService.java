package com.project.tracker.service;

import com.project.tracker.entity.Category;
import com.project.tracker.entity.CategoryType;
import com.project.tracker.repository.CategoryRepository;
import com.project.tracker.repository.HabitLogRepository;
import com.project.tracker.stats.StreakCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class StreakService {
    private static final Logger log = LoggerFactory.getLogger(StreakService.class);
    private static final String KEY = "streaks";
    private static final int LOOKBACK_DAYS = 366;

    private final HabitLogRepository habitLogRepository;

    public StreakService(HabitLogRepository habitLogRepository, CategoryRepository categoryRepository, StringRedisTemplate redis) {
        this.habitLogRepository = habitLogRepository;
        this.categoryRepository = categoryRepository;
        this.redis = redis;
    }

    private final CategoryRepository categoryRepository;
    private final StringRedisTemplate redis;

    public int recompute(Category habit) {
        LocalDate today = LocalDate.now();
        Set<LocalDate> doneDays = new HashSet<>(habitLogRepository.findDoneDates(habit.getId(),today.minusDays(LOOKBACK_DAYS)));
        int streak = StreakCalculator.currentStreak(doneDays,today);
        try{
            redis.opsForZSet().add(KEY,habit.getName(),streak);
        }catch(RuntimeException e){
            log.warn("ZADD {} {} failed, streak not cached: {}",KEY, habit.getName(), e.getMessage());
        }
        return streak;

    }
    public Map<String,Integer> allStreaks(){
        Map<String,Integer> result = new LinkedHashMap<>();
        for(Category category : categoryRepository.findAll()){
            Double score = null;
            try{
                score = redis.opsForZSet().score(KEY,category.getName());
            }catch(RuntimeException e){
                log.warn("ZADD {} {} failed, streak not cached: {}",KEY, category.getName(),e.getMessage());
            }
            result.put(category.getName(),score != null ? score.intValue():recompute(category));
        }
        return result;
    }
}
