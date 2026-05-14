# Lunettes connectees

Projet de programmation avancee — MIAGE M1 2024.

Systeme distribue complet permettant de commander des lunettes connectees, de gerer leur fabrication en parallele, et de suivre leur livraison. La communication entre le frontend JavaFX et le backend de fabrication est entierement asynchrone via un bus MQTT.

## Modules Maven

Le projet est structure en quatre modules :

| Module | Role |
|---|---|
| `common` | Modele metier (`GlassType`, `OrderRequest`, `ProducedGlass`, `OrderStatus`), codec du protocole MQTT, chargement de configuration |
| `usine` | Pilotage du `Fabricateur`, mutualisation par lots, parallelisme de fabrication |
| `backend-server` | Serveur MQTT : validation des commandes, orchestration de l'usine, livraison |
| `frontend-app` | Application JavaFX : accueil, catalogue, suivi commande, verification de numero de serie |

## Prerequis

- Java 21 (JDK, pas seulement JRE)
- Maven 3.9+ (ou utiliser le Maven fourni dans `tools/`)
- Un broker MQTT sur `tcp://localhost:1883` (voir options ci-dessous)

## Lancement rapide

### Etape 1 — Compiler le projet

```powershell
mvn clean package
```

### Etape 2 — Demarrer un broker MQTT

**Option A — Mosquitto installe :**
```powershell
run-mosquitto.cmd
# ou directement :
"C:\Program Files\mosquitto\mosquitto.exe" -v
```

**Option B — Broker Python integre (aucune installation requise) :**
```powershell
python scripts\local-mqtt-broker.py
```
Ce broker minimal supporte CONNECT, PUBLISH (QoS 0 et 1), SUBSCRIBE, PING et DISCONNECT. Il suffit pour faire fonctionner le projet complet.

### Etape 3 — Demarrer le backend

```powershell
run-backend.cmd
# ou directement :
java -jar backend-server\target\backend-server-1.0.0-SNAPSHOT.jar
```

### Etape 4 — Demarrer le frontend

```powershell
run-frontend.cmd
# ou directement :
java -jar frontend-app\target\frontend-app-1.0.0-SNAPSHOT.jar
```

Plusieurs instances du frontend peuvent tourner en meme temps — chaque instance est un client independant.

## Configuration

Les valeurs par defaut sont embarquees dans les fichiers `.properties` de chaque module. Il est possible de les surcharger en passant un fichier externe en argument :

```powershell
java -jar backend-server\target\backend-server-1.0.0-SNAPSHOT.jar chemin\vers\backend.properties
java -jar frontend-app\target\frontend-app-1.0.0-SNAPSHOT.jar chemin\vers\frontend.properties
```

**`backend-server/src/main/resources/backend.properties`**
```properties
broker.url=tcp://localhost:1883
backend.clientId=lunettes-backend
backend.factoryCapacity=4
```

**`frontend-app/src/main/resources/frontend.properties`**
```properties
broker.url=tcp://localhost:1883
frontend.clientPrefix=lunettes-frontend
```

## Structure MQTT

Le protocole s'appuie sur des topics hierarchiques. Le frontend publie les commandes, le backend les traite et publie les reponses.

| Topic | Emetteur | Description |
|---|---|---|
| `orders/{id}` | Frontend | Publication d'une commande |
| `orders/{id}/validated` | Backend | Commande acceptee |
| `orders/{id}/cancelled` | Backend | Commande refusee (avec raison) |
| `orders/{id}/status` | Backend | Progression (`processing` puis `processed`) |
| `orders/{id}/delivery` | Backend | Liste des lunettes fabriquees avec numeros de serie |
| `orders/{id}/error` | Backend | Erreur de fabrication |
| `serials/{serial}/check` | Frontend | Demande de verification d'un numero de serie |
| `serials/{serial}` | Backend | Reponse : type de lunette ou `invalid` |

### Format du payload

Le payload utilise un format texte maison base sur des paires `TYPE=VALEUR` separees par `;`. Les caracteres speciaux (`;`, `=`, `,`, `\`) sont echappes avec `\`. Exemples :

```
# Commande : 2 BANANA et 1 CLAUDE
BANANA=2;CLAUDE=1

# Livraison
BANANA=BANANA-A1B2C3D4;CLAUDE=CLAUDE-E5F6G7H8
```

## Tests unitaires

```powershell
mvn test
```

Les tests couvrent :

- `PayloadCodecTest` : encodage/decodage round-trip des commandes et livraisons
- `OrderValidatorTest` : validation des quantites (commande vide, commande valide)
- `UsineServiceTest` : fabrication de plusieurs lunettes en parallele

## Publication de l'usine (CI/CD)

Le workflow `.github/workflows/publish-usine.yml` se declenche a chaque release GitHub. Il execute d'abord les tests, puis publie le module `usine` dans GitHub Packages.

Les secrets GitHub suivants doivent etre configures dans le depot :

- `PROF_MAVEN_USERNAME` : nom d'utilisateur GitHub pour acceder au depot Maven du professeur
- `PROF_MAVEN_TOKEN` : token d'acces personnel (lecture `read:packages`)

Pour compiler sur une machine sans credentials, un modele de configuration est fourni dans `maven-settings-template.xml`.

## Limitations connues

- **Persistance des numeros de serie** : les numeros de serie fabriques sont conserves en memoire dans le processus backend. Si le backend redémarre, les verifications de serie renverront `invalid` pour les commandes precedentes. Cette limite est acceptable pour un contexte de demonstration.

- **Verification de serie cross-session** : par conception, un numero de serie n'est valide que pour la session backend qui l'a produit.

## Architecture technique

- **Parallelisme** : l'usine utilise un thread de batching (`usine-batcher`) qui regroupe les demandes en lots optimaux, et un pool de virtual threads Java 21 pour la fabrication parallele. Le backend utilise egalement des virtual threads pour traiter plusieurs commandes simultanement.
- **Reconnexion MQTT** : le backend utilise `MqttCallbackExtended` pour se reabonner automatiquement aux topics apres une reconnexion.
- **Thread-safety JavaFX** : tous les callbacks MQTT du frontend passent par `Platform.runLater()` pour mettre a jour l'interface en toute securite.
