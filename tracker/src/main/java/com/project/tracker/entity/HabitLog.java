package com.project.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
public class HabitLog {
    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Getter
    @Setter
    private LocalDate date;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private Status status;

}
