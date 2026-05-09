# MusicGlass 0.0.4

Cette version 0.0.4 apporte des améliorations majeures sur iOS et Android, avec un gros focus sur la fluidité, la stabilité visuelle, l’expérience Bibliothèque et la cohérence entre les deux plateformes.

## iOS

- Optimisation ProMotion 120Hz : isolation des états de lecture pour supprimer les micro-saccades.
- Pipeline d’images : downsampling automatique et traitement en arrière-plan pour un scroll fluide.
- Swipe Artwork : nouvelle animation interactive avec physique de ressort et retours haptiques.
- Correctif Mini-Player : rendu stable en mode Light.
- Timeline Ultra-Fluide : refonte du scrubbing pour un suivi du doigt à 120Hz sans lag.
- Fix Snapback Timeline : suppression du saut visuel lors du relâchement du curseur.
- Layout Home : header “Bon après-midi” rapproché de la Dynamic Island pour un look plus premium.
- Compatibilité iOS 26+ : correction des accès asynchrones aux propriétés système.

## Android

- Ajout d’un profil Material Design 3 accessible depuis l’avatar du header.
- Ajout d’un bottom sheet Profil avec ouverture partielle puis expansion par swipe.
- Ajout des sections Compte, YouTube Music, Application, Données et À propos dans le profil.
- Ajout de la page Connexion / Inscription MusicGlass en Material Design 3.
- Refonte complète de la page Bibliothèque pour se rapprocher de l’expérience iOS tout en respectant Material Design 3.
- Ajout de cartes statistiques Titres aimés / Playlists.
- Ajout des raccourcis Artistes, Albums, Téléchargements et Historique.
- Ajout d’un affichage visuel horizontal des playlists YouTube Music avec artworks et badges YT Music.
- Ajout de la page Artistes Android.
- Classement automatique des artistes les plus écoutés avec Top 3.
- Liste des autres artistes triée par nombre d’écoutes.
- Ajout de la recherche d’artistes.
- Amélioration du cache Bibliothèque pour éviter les rechargements inutiles lors du retour depuis une playlist ou un autre onglet.
- Correction des chargements visibles et des compteurs qui repassaient temporairement à 0.
- Améliorations UI Material Design 3 : surfaces tonales, cartes arrondies, alignements, typographies et compatibilité thème clair/sombre.
- Mise à jour version Android : `versionName 0.0.4`, `versionCode 4`.
- Ajout de l’APK Android signé dans la release GitHub.

## Builds inclus

- `MusicGlass-0.0.4.apk` : APK Android signé.
- `MusicGlass-0.0.4-unsigned.ipa` : IPA iOS non signée.
