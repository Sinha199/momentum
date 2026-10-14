package com.project.tracker.repository;

import com.project.tracker.entity.Category;
import com.project.tracker.entity.CategoryType;
import com.project.tracker.entity.HabitLog;
import com.project.tracker.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    Optional<HabitLog> findByUserAndCategoryAndDate(User user, Category category, LocalDate date);

    List<HabitLog> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    @Query("select l.date from HabitLog l "
            + "where l.category.id = :categoryId "
            +"and l.status = com.project.tracker.entity.Status.DONE and l.date>= :from")
    List<LocalDate>findDoneDates(@Param("categoryId")Long categoryId, @Param("from") LocalDate from);
}
