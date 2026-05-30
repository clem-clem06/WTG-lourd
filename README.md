# WorkTogether — Client lourd d'administration (WTG-lourd)

Application **desktop JavaFX** réservée au personnel de WorkTogether (administrateurs,
comptables, techniciens) pour piloter la plateforme de location de baies datacenter :
gestion des comptes, des baies/unités, des offres, des clients, des réservations
(validation des virements) et des interventions de maintenance.

> **Écosystème WorkTogether** — ce dépôt est le **client lourd** (administration interne).
> Il partage sa **base de données MySQL** avec le **client léger**
> [`WTG-leger`](https://github.com/clem-clem06/WTG-leger) (site web Symfony destiné aux clients).
> Les deux applications lisent la même table `user` et les mêmes rôles.

---

## Stack technique

| Couche       | Techno                                            |
|--------------|---------------------------------------------------|
| Langage      | Java 21                                            |
| UI           | JavaFX 21 (FXML + CSS, thème sombre)              |
| Framework    | Spring Boot 3.2.4 (Data JPA + Security, **sans serveur web**) |
| ORM          | Spring Data JPA / Hibernate                       |
| BDD          | MySQL 8 (partagée avec WTG-leger)                |
| Sécurité     | Spring Security — BCrypt + Argon2id (BouncyCastle) |
| Build        | Gradle (wrapper)                                  |
| Tests        | JUnit 5 + Mockito + AssertJ                       |

L'application n'expose **aucun port HTTP** : Spring est démarré en
`WebApplicationType.NONE` uniquement pour fournir l'injection de dépendances, JPA et la sécurité.

---

## Prérequis

- **JDK 21**
- Accès à la base MySQL de WorkTogether (la même que le site Symfony)
- Aucune installation de JavaFX nécessaire : le plugin Gradle `org.openjfx.javafxplugin`
  télécharge les modules automatiquement.

---

## Configuration de la base

La connexion se configure par **variables d'environnement** (valeurs par défaut entre `{}`) :

| Variable      | Défaut                                                    |
|---------------|-----------------------------------------------------------|
| `DB_URL`      | `jdbc:mysql://127.0.0.1:3306/worktogether?serverTimezone=Europe/Paris&useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USER`     | `root`                                                    |
| `DB_PASSWORD` | *(vide)*                                                  |

> `spring.jpa.hibernate.ddl-auto=none` : le schéma est créé par les migrations Symfony.
> Le client lourd ne modifie **jamais** la structure de la base.

---

## Lancer en développement

```powershell
# Depuis la racine du projet
.\gradlew.bat run
```

Pour viser une autre base :

```powershell
$env:DB_URL="jdbc:mysql://10.0.0.10:3306/appdb?useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USER="appuser"
$env:DB_PASSWORD="motdepasse"
.\gradlew.bat run
```

---

## Construire et distribuer le `.jar`

> ⚠️ **Gradle** : le plugin Spring Boot 3.2.4 n'est pas compatible **Gradle 9**
> (la tâche `bootJar` échoue). Utiliser **Gradle 8.x** — dans
> `gradle/wrapper/gradle-wrapper.properties` :
> `distributionUrl=...gradle-8.10.2-bin.zip`.

```powershell
.\gradlew.bat clean bootJar
java -jar build\libs\WTG-lourd-0.0.1-SNAPSHOT.jar
```

Le `bootJar` produit un **fat jar exécutable** (Spring + JavaFX + dépendances).
`java -jar` fonctionne car la classe de lancement `WtgApplication` n'étend pas
`Application` (elle appelle `Application.launch(JavaFxApp.class)`).

> Le jar embarque les natifs JavaFX de la **plateforme de compilation** : un jar
> construit sous Windows s'exécute sous Windows. Compiler sur l'OS cible pour Linux/macOS.

---

## Connexion à l'application

Seuls les comptes **du personnel** peuvent se connecter (les `ROLE_CLIENT` sont rejetés
avec « application réservée au personnel »).

| Email             | Mot de passe   | Rôle            |
|-------------------|----------------|-----------------|
| admin@wtg.fr      | Admin123!      | ROLE_ADMIN      |
| comptable@wtg.fr  | Comptable123!  | ROLE_COMPTABLE  |
| technicien@wtg.fr | Technicien123! | ROLE_TECHNICIEN |

*(Comptes créés par les fixtures du projet Symfony.)*

---

## Écrans selon le rôle

| Écran            | Rôle requis        | Fonction                                              |
|------------------|--------------------|-------------------------------------------------------|
| Utilisateurs     | ADMIN              | CRUD du personnel (admin / comptable / technicien)    |
| Baies            | ADMIN              | Gestion des baies et de leurs unités                  |
| Offres           | ADMIN              | Gestion des offres commerciales                       |
| Clients          | ADMIN + COMPTABLE  | Consultation des clients (recherche, stats)           |
| Réservations     | ADMIN + COMPTABLE  | Validation / annulation des virements bancaires       |
| Unités           | ADMIN + TECHNICIEN | État du parc (OK / Maintenance / Incident)            |
| Interventions    | ADMIN + TECHNICIEN | Planification et suivi des interventions              |

### Logiques métier notables

- **Validation d'un virement** (Réservations) → commande + paiement passent à `paid`,
  message « Virement reçu », et les **unités réservées sont activées** (`OK`).
  L'annulation libère les unités (`locataire = null`, `OK`).
- **Interventions** → l'**état de l'unité suit ses interventions** : une réparation
  en cours met l'unité en `Incident`, une autre intervention en `Maintenance`,
  et le retour à `Terminée` la repasse en `OK`.

---

## Architecture

```
src/main/java/org/example/wtg/
├── WtgApplication.java      # point d'entrée (main) → lance JavaFX
├── JavaFxApp.java           # Application JavaFX, démarre le contexte Spring
├── SceneManager.java        # navigation entre les vues FXML
├── entities/                # User, Baie, Unite, Offre, Order, OrderItem,
│                            # Payment, Card, Cart, CartItem, Intervention
├── repositories/            # Spring Data JPA
├── services/                # logique métier (Users, Baies, Offres, Clients,
│                            # Reservations, Unites, Interventions)
├── views/                   # contrôleurs JavaFX (un par écran)
├── security/                # CustomUserDetailsService, SecurityConfig
└── ui/                      # composants réutilisables (ConfirmDialog, PasswordDialog)

src/main/resources/fxml/     # vues FXML + style.css (thème sombre)
src/test/java/               # tests JUnit 5 (services)
```

---

## Tests

```powershell
.\gradlew.bat test
```

Rapport HTML : `build/reports/tests/test/index.html`.

---

## Sécurité

- Connexion via `CustomUserDetailsService` : lit le champ JSON `roles` de la table `user`
  et n'autorise que `ROLE_ADMIN`, `ROLE_COMPTABLE`, `ROLE_TECHNICIEN`.
- Mots de passe vérifiés en **BCrypt** et **Argon2id** (compatibilité avec le hachage Symfony).
- Les comptes `ROLE_CLIENT` ne sont jamais modifiables/supprimables depuis cette application.
