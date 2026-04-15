package fr.univlorraine.separateproject.application;

import fr.univlorraine.separateproject.domain.ProjectProfile;

import java.time.LocalDate;
import java.util.Locale;

public final class ProjectProfileService {

    public ProjectProfile createProfile(String name, String description) {
        String normalizedName = requireText(name, "Le nom du projet est obligatoire.");
        String normalizedDescription = requireText(description, "La description du projet est obligatoire.");

        return new ProjectProfile(
                normalizedName,
                normalizedDescription,
                toSlug(normalizedName),
                LocalDate.now());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String toSlug(String projectName) {
        return projectName
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
