# Configuration GitHub Actions

Ce document explique la configuration GitHub Actions mise en place pour le projet springboot-nextjs-starterpack.

## Structure des Workflows

### 1. Backend CI (`.github/workflows/backend-ci.yml`)
- **Déclencheurs** : Push et PR sur `main` et `develop` avec changements dans `modulith/`
- **Services** : PostgreSQL 15 pour les tests
- **Actions** :
  - Setup JDK 21
  - Cache Maven
  - Tests avec PostgreSQL
  - Build du JAR
  - Build et push des images Docker (sur main)

### 2. Frontend CI (`.github/workflows/frontend-ci.yml`)
- **Déclencheurs** : Push et PR sur `main` et `develop` avec changements dans `frontend-next/`
- **Actions** :
  - Setup Node.js 20
  - Installation des dépendances
  - Linting
  - Build
  - Tests Lighthouse (sur PR)

### 3. CI/CD Pipeline (`.github/workflows/ci-cd.yml`)
- **Déclencheurs** : Push et PR sur `main` et `develop`
- **Fonctionnalités** :
  - Détection intelligente des changements
  - Exécution conditionnelle des jobs
  - Déploiement automatique vers staging (develop) et production (main)

### 4. Security Scanning (`.github/workflows/security.yml`)
- **Déclencheurs** : Push, PR, et planification hebdomadaire
- **Outils** :
  - OWASP Dependency Check
  - CodeQL Analysis (Java et JavaScript)
  - Trivy vulnerability scanner

### 5. Release (`.github/workflows/release.yml`)
- **Déclencheurs** : Tags `v*` et déclenchement manuel
- **Actions** :
  - Build des artéfacts
  - Création des releases GitHub
  - Push des images Docker taguées

## Configuration Requise

### Secrets GitHub
Configurez les secrets suivants dans votre dépôt GitHub :

```bash
# Docker Hub (optionnel)
DOCKER_USERNAME=your-dockerhub-username
DOCKER_PASSWORD=your-dockerhub-password-or-token

# SonarCloud (optionnel)
SONAR_TOKEN=your-sonarcloud-token

# Lighthouse CI (optionnel)
LHCI_GITHUB_APP_TOKEN=your-lighthouse-ci-token
```

### Environnements GitHub
Créez les environnements suivants dans GitHub :
- `staging` : pour les déploiements de développement
- `production` : pour les déploiements en production

## Scripts Utilitaires

### Test Local (`scripts/test-local.sh`)
```bash
# Tous les tests
./scripts/test-local.sh all

# Backend uniquement
./scripts/test-local.sh backend

# Frontend uniquement
./scripts/test-local.sh frontend

# Sécurité uniquement
./scripts/test-local.sh security
```

### Configuration (`scripts/setup-github-actions.sh`)
Script d'aide pour configurer les secrets et environnements GitHub.

## Déploiement

### Staging
- **Déclencheur** : Push sur `develop`
- **Condition** : Tests backend et frontend réussis
- **Environnement** : `staging`

### Production
- **Déclencheur** : Push sur `main`
- **Condition** : Tests backend et frontend réussis
- **Environnement** : `production`

## Optimisations

### Cache
- **Maven** : Cache des dépendances Maven (`.m2`)
- **Node.js** : Cache npm basé sur `package-lock.json`

### Détection des Changements
Le workflow CI/CD utilise `dorny/paths-filter` pour exécuter uniquement les jobs nécessaires :
- Backend CI si changements dans `modulith/`
- Frontend CI si changements dans `frontend-next/`

### Parallélisation
- Les tests backend et frontend s'exécutent en parallèle
- CodeQL analyse Java et JavaScript en parallèle

## Monitoring et Sécurité

### Rapports
- **Tests** : Rapports Surefire uploadés comme artéfacts
- **Sécurité** : Rapports OWASP et Trivy dans l'onglet Security
- **Code Quality** : Intégration CodeQL pour l'analyse statique

### Notifications
Les workflows envoient des notifications en cas d'échec et créent des issues de sécurité automatiquement.

## Personnalisation

### Ajout de Tests
Pour ajouter des tests spécifiques :
1. Modifiez les workflows dans `.github/workflows/`
2. Ajoutez les scripts dans `scripts/`
3. Mettez à jour cette documentation

### Déploiement Custom
Remplacez les étapes de déploiement fictives dans `ci-cd.yml` par vos propres scripts :
```yaml
- name: Deploy to staging
  run: |
    echo "Deploying to staging environment..."
    # Vos commandes de déploiement ici
```

## Dépannage

### Erreurs Communes
1. **Tests Maven échouent** : Vérifiez la configuration PostgreSQL
2. **Build Docker échoue** : Vérifiez les secrets Docker Hub
3. **Lighthouse timeout** : Augmentez le délai d'attente dans `lighthouserc.json`

### Logs
Consultez les logs des Actions dans l'onglet "Actions" de votre dépôt GitHub.

## Support

Pour toute question sur cette configuration, consultez :
- [Documentation GitHub Actions](https://docs.github.com/en/actions)
- [Spring Boot CI/CD Best Practices](https://spring.io/guides/gs/continuous-integration/)
- [Next.js Deployment](https://nextjs.org/docs/deployment)
