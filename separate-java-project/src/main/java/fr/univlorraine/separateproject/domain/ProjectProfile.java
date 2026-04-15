package fr.univlorraine.separateproject.domain;

import java.time.LocalDate;

public record ProjectProfile(
        String name,
        String description,
        String slug,
        LocalDate createdAt) {
}
