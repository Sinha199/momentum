package com.project.tracker.stats;

import java.time.LocalDate;
import java.util.Set;

public class StreakCalculator {
    public StreakCalculator() {
    }
    public static int currentStreak(Set<LocalDate> doneDays, LocalDate today){
        LocalDate day = doneDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while(doneDays.contains(day)){
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
