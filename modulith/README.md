# 🎯 Spring Modulith - Projet monolithique modulaire

Ce projet est une coquille prête à l'emploi pour des applications Spring Boot modulaires (basées sur [Spring Modulith](https://moduliths.org/)).

---

## 🧱 Structure

- `user/` : Gestion des utilisateurs
- `catalog/` : Gestion des produits (albums, goodies, etc.)
- `shared/` : Composants réutilisables
- `db/changelog/` : Scripts de migration Liquibase au format YAML
- `test/` : Tests unitaires et d'intégration modulaires

### Sous-dossiers des modules
- `./api/` : Controllers
- `./application/` : Services métiers
- `./domain/` : Entités + interfaces
- `./infrastructure/` : Repos JPA, adaptateurs

---

## 🚀 Fonctionnalités disponibles

### Authentification

- Authentification et Gesttion des autorisations
- Connexion avec JWT
- Connexion web et mobile
- Gestion des rôles et permissions
- Mot de passe oublié
- Changement de mot de passe

### Users

- Inscription des utilisateurs
  - Vérification de l'email
- Mise à jour du profil utilisateur

---

## 🧩 Librairies utilisées

| Librairie                                     | Rôle                                    |
|-----------------------------------------------|-----------------------------------------|
| `spring-boot-starter-web`                     | API REST                                |
| `spring-boot-starter-data-jpa`                | Persistance JPA                         |
| `spring-boot-starter-security`                | Sécurité (si besoin d'authentification) |
| `spring-boot-starter-actuator`                | Monitoring / Healthcheck                |
| `spring-modulith-starter-core`                | Base de Spring Modulith                 |
| `spring-modulith-starter-jpa`                 | Intégration JPA avec Modulith           |
| `spring-modulith-actuator`                    | Introspection des modules               |
| `spring-modulith-observability`               | Traces entre modules                    |
| `liquibase-core`                              | Migration de base de données            |
| `spring-boot-devtools`                        | Reload à chaud                          |
| `postgresql`                                  | Driver PostgreSQL                       |
| `testcontainers + spring-boot-testcontainers` | Tests intégrés avec PostgreSQL          |
| `spring-modulith-starter-test`                | Tests unitaires modulith                |
| `spring-security-test`                        | Tests sécurité                          |
| `mockito-core`                                | Tests unitaires                         |
| `assertj-core`                                | Assertions                              |
| `lombok`                                      | Réduction du code boilerplate           |

---

## 🐳 Containerisation

- `Dockerfile` : build de l'application
- `docker-compose.yml` : service app + base de données PostgreSQL

---

## ▶️ Démarrer l'application

```bash
./mvnw clean package
docker-compose up --build
```