# Lunettes connectees

Projet de programmation avancee construit en Maven autour de 4 modules :

- `common` : modele metier, topics MQTT, codec du protocole
- `usine` : pilotage du `fabricateur` et mutualisation des lots
- `backend-server` : serveur MQTT qui valide, fabrique et livre
- `frontend-app` : application JavaFX avec catalogue, suivi et verification de numero de serie

## Fonctionnalites implementees

- protocole MQTT asynchrone sur `orders/*` et `serials/*`
- validation des commandes
- bonus `/status` avec `processing` puis `processed`
- verification d'un numero de serie
- catalogue charge depuis les assets fournis
- backend multi-commandes avec mutualisation par lots via l'usine
- tests unitaires du codec, de la validation et de l'usine

## Structure MQTT

- `orders/{id}` : publication de la commande
- `orders/{id}/validated` : commande acceptee
- `orders/{id}/cancelled` : commande invalide
- `orders/{id}/status` : progression
- `orders/{id}/delivery` : liste des lunettes fabriquees
- `orders/{id}/error` : erreur de traitement
- `serials/{serial}/check` : verification
- `serials/{serial}` : reponse avec type ou `invalid`

Le payload utilise un format texte maison base sur des paires `TYPE=VALEUR` separees par `;`, avec echappement via `\`.

## Prerequis

- Java 21
- Maven
- Mosquitto sur `tcp://localhost:1883`
- acces Maven a la dependance privee `fabricateur`

## Compilation

```powershell
mvn test
mvn package
```

## Lancement rapide

### 1. Demarrer Mosquitto

```powershell
"C:\Program Files\mosquitto\mosquitto.exe" -v
```

### 2. Demarrer le backend

```powershell
java -jar backend-server/target/backend-server-1.0.0-SNAPSHOT.jar
```

### 3. Demarrer le frontend

```powershell
java -jar frontend-app/target/frontend-app-1.0.0-SNAPSHOT.jar
```

## Configuration

Les valeurs par defaut sont embarquees dans :

- `backend-server/src/main/resources/backend.properties`
- `frontend-app/src/main/resources/frontend.properties`

Vous pouvez passer un chemin vers un fichier `properties` externe :

```powershell
java -jar backend-server/target/backend-server-1.0.0-SNAPSHOT.jar path\\to\\backend.properties
java -jar frontend-app/target/frontend-app-1.0.0-SNAPSHOT.jar path\\to\\frontend.properties
```

## Publication de l'usine

Le workflow GitHub `.github/workflows/publish-usine.yml` publie le module `usine` lors d'une release.

Pour compiler sur une machine propre, il faut fournir des credentials Maven pour le depot du professeur.
Un modele est fourni dans `maven-settings-template.xml`.
