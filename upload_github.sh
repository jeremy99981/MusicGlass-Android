#!/bin/bash
#
# Script pour initialiser Git dans MusicGlassAndroid et pousser vers
# https://github.com/jeremy99981/MusicGlass-Android
#
# IMPORTANT :
# - Avant d'exécuter ce script, créez le dépôt vide sur GitHub :
#   https://github.com/jeremy99981/MusicGlass-Android
#   (NE PAS cocher "Initialize this repository with a README")
#
# - Si vous avez activé l'authentification GitHub, assurez-vous que votre
#   clé SSH est configurée ou que vous utilisez un token d'accès personnel.

set -e

REMOTE_URL="git@github.com:jeremy99981/MusicGlass-Android.git"

echo "=== MusicGlass Android - Préparation de l'upload GitHub ==="
echo ""

cd "$(dirname "$0")"

# Vérifier si Git est déjà initialisé
if [ -d ".git" ]; then
    echo "[INFO] Dépôt Git déjà initialisé."
else
    echo "[1/4] Initialisation du dépôt Git..."
    git init
    echo "      ✓ Dépôt Git initialisé."
fi

# Vérifier si le remote existe déjà
if git remote get-url origin &>/dev/null; then
    echo "[INFO] Remote 'origin' déjà configuré : $(git remote get-url origin)"
    echo "       Mise à jour du remote..."
    git remote set-url origin "$REMOTE_URL"
else
    echo "[2/4] Ajout du remote origin..."
    git remote add origin "$REMOTE_URL"
fi
echo "      ✓ Remote configuré vers $REMOTE_URL"

echo "[3/4] Création du commit initial..."
git add .
git status

echo ""
echo "      Les fichiers ci-dessus seront commités."
echo "      Appuyez sur Entrée pour continuer (Ctrl+C pour annuler)..."
read -r

git commit -m "Initial commit - MusicGlass Android app

Structure du projet Android :
- app/src/main/java/com/musicglass/app/
  - core/        : Composants principaux
  - networking/  : Réseau et API
  - persistence/ : Stockage local
  - playback/    : Lecture audio
  - ui/          : Interface utilisateur
  - youtubemusic/: Intégration YouTube Music
- Gradle Kotlin DSL (build.gradle.kts)
- Kotlin 2.2.10 + Compose"

echo "      ✓ Commit créé."

echo "[4/4] Push vers GitHub..."
echo "      Branche principale : main"
git branch -M main
git push -u origin main

echo ""
echo "=== ✅ Upload terminé avec succès ! ==="
echo ""
echo "Votre projet est maintenant disponible sur :"
echo "  https://github.com/jeremy99981/MusicGlass-Android"
