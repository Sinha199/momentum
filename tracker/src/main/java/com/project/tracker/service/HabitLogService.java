package com.project.tracker.service;

import com.project.tracker.config.AfterCommit;
import com.project.tracker.entity.Category;
import com.project.tracker.entity.HabitLog;
import com.project.tracker.entity.User;
import com.project.tracker.entity.Status;
import com.project.tracker.repository.*;
import com.project.tracker.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import com.project.tracker.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class HabitLogService {

    private static final Logger logger = LoggerFactory.getLogger(HabitLogService.class.getName());
    private final UserRepository userRepository;
    private final HabitLogRepository habitLogRepository;
    private final CategoryRepository categoryRepository;

    private final StreakService streakService;
    private final CacheManager cacheManager;
    public HabitLogService(UserRepository userRepository, HabitLogRepository habitLogRepository, CategoryRepository categoryRepository, StreakService streakService, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.habitLogRepository = habitLogRepository;
        this.categoryRepository = categoryRepository;
        this.streakService = streakService;
        this.cacheManager = cacheManager;
    }

    private static final Long DEFAULT_ID = 1L;

    @Transactional
    public HabitLogResponseDTO saveAndUpdateLog(String categoryName, LocalDate date, Status status) {
        User user = userRepository.getReferenceById(DEFAULT_ID);
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(()->new NoSuchElementException("Category not found " +categoryName ));
        HabitLog log = habitLogRepository.findByUserAndCategoryAndDate(user, category, date).orElse(new HabitLog());
        log.setCategory(category);
        log.setUser(user);
        log.setDate(date);
        log.setStatus(status);
        HabitLog saved = habitLogRepository.save(log);
        YearMonth month = YearMonth.from(date);
        // Rebuild the month on the primary (same write transaction, so it includes the row above)
        // and cache it once the commit succeeds. Don't evict: the next GET would read the replica,
        // which can lag a few ms, and cache the old month.
        List<HabitLogResponseDTO> fresh = new ArrayList<>(habitLogRepository
                .findByUserIdAndDateBetween(DEFAULT_ID, month.atDay(1), month.atEndOfMonth())
                .stream().map(HabitLogResponseDTO::new).toList());
        AfterCommit.run("cache refresh " + month,
                () -> cacheManager.getCache(CacheConfig.LOGS_BY_MONTH).put(month, fresh));
        streakService.recompute(category);
        return new HabitLogResponseDTO(saved);
    }
    /**
     +     * Redis first (key "logsByMonth::2026-10"); only on a miss does this method body run and hit Postgres.
     +     * Returns DTOs, not entities, because that's what gets stored as JSON.
     +     */
    @Cacheable(CacheConfig.LOGS_BY_MONTH)
    @Transactional(readOnly = true)
    public List<HabitLogResponseDTO> getLogsForMonth(YearMonth month) {
        logger.info("Cache miss for {}, reading Postgres",month);
        return new ArrayList<>(habitLogRepository.findByUserIdAndDateBetween(DEFAULT_ID, month.atDay(1), month.atEndOfMonth()).stream().map(HabitLogResponseDTO::new).toList());
    }

    @Transactional(readOnly = true)
    public List<HabitLog> getLogsForLastNdays(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        return habitLogRepository.findByUserIdAndDateBetween(DEFAULT_ID, start, end);
    }
    private void evictMonth(YearMonth month) {
        try{
           cacheManager.getCache(CacheConfig.LOGS_BY_MONTH).evict(month);
        }catch(RuntimeException e){
            logger.warn("Could not evict {} from Redis: {}",month,e.getMessage());
        }
    }
}
