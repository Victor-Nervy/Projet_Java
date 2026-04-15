# Separate Java Project

Ce dossier contient un nouveau projet Java autonome.

Il est volontairement separe du projet principal :

- il a son propre `pom.xml`
- il n'est pas declare dans le `pom.xml` racine
- il peut etre deplace ailleurs sans casser le projet des lunettes

## Structure

- `src/main/java` : code principal
- `src/test/java` : tests unitaires
- `application` : logique applicative
- `domain` : objets du domaine

## Commandes utiles

```powershell
mvn test
mvn package
java -jar target/separate-java-project-1.0.0-SNAPSHOT.jar
java -jar target/separate-java-project-1.0.0-SNAPSHOT.jar "Mon projet"
```

## Point de depart

Le code fourni sert de base propre :

- une classe `Main` pour demarrer
- un service `ProjectProfileService`
- un objet immutable `ProjectProfile`
- un test unitaire JUnit

Tu peux maintenant remplacer cet exemple par le vrai sujet de ton nouveau projet sans toucher aux modules existants.
