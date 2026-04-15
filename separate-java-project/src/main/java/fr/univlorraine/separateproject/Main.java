package fr.univlorraine.separateproject;

import fr.univlorraine.separateproject.application.ProjectProfileService;
import fr.univlorraine.separateproject.domain.ProjectProfile;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ProjectProfileService projectProfileService = new ProjectProfileService();
        String requestedName = args.length > 0 ? args[0] : "Mon nouveau projet Java";

        ProjectProfile profile = projectProfileService.createProfile(
                requestedName,
                "Base propre pour commencer un nouveau projet sans melanger les modules existants.");

        System.out.println("Projet : " + profile.name());
        System.out.println("Slug   : " + profile.slug());
        System.out.println("Cree le: " + profile.createdAt());
        System.out.println("Info   : " + profile.description());
    }
}
