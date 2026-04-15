package fr.univlorraine.separateproject.application;

import fr.univlorraine.separateproject.domain.ProjectProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectProfileServiceTest {

    private final ProjectProfileService projectProfileService = new ProjectProfileService();

    @Test
    void shouldCreateNormalizedProfile() {
        ProjectProfile profile = projectProfileService.createProfile(
                "  Projet Java Propre  ",
                "  Structure de depart  ");

        assertEquals("Projet Java Propre", profile.name());
        assertEquals("Structure de depart", profile.description());
        assertEquals("projet-java-propre", profile.slug());
    }

    @Test
    void shouldRejectBlankProjectName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> projectProfileService.createProfile("   ", "Description"));
    }
}
