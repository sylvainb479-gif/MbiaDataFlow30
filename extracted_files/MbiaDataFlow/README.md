# MbiaDataFlow

Application Android native de surveillance globale de l'utilisation des données mobiles.

## Fonctions incluses
- Compteur global mobile RX + TX via TrafficStats.
- Compteur de période et compteur depuis l'installation.
- Surveillance activable/désactivable.
- Application activable/désactivable.
- Protection par limite activable/désactivable.
- Alertes à 80 %, 90 % et 100 % de la limite.
- Limite configurable en GB.
- Réinitialisation : aucune, manuelle, quotidienne, hebdomadaire, mensuelle ou date personnalisée.
- Historique des réinitialisations conservé.
- Surveillance en arrière-plan via service au premier plan.
- Relance après redémarrage de l'appareil lorsque les contrôles sont actifs.
- Workflow GitHub Actions qui compile l'APK Debug et le publie comme artefact.

## Compilation GitHub
1. Décompresser le projet.
2. Pousser le dossier `MbiaDataFlow` dans un dépôt GitHub.
3. Ouvrir **Actions** et lancer **Build MbiaDataFlow APK** ou pousser sur `main`/`master`.
4. Télécharger l'artefact `MbiaDataFlow-debug-apk`.

Le workflow utilise JDK 17 et Gradle 9.6.0. Le wrapper Gradle n'est pas nécessaire pour GitHub Actions : l'action `gradle/actions/setup-gradle` installe Gradle avant la compilation.

## Limitation Android importante
L'application surveille les compteurs globaux de données mobiles exposés par Android. Elle peut alerter à la limite, mais ne prétend pas couper elle-même les données mobiles système sur un téléphone Android standard sans privilèges spéciaux.
