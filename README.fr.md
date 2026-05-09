<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="Icône MusicGlass" style="border-radius: 24px;">
</p>

<h1 align="center">MusicGlass</h1>

<p align="center">
  <a href="README.md">🇬🇧 English</a> | <strong>🇫🇷 Français</strong>
</p>

<p align="center">
  <strong>Un lecteur de musique Android natif haut de gamme, doté d'une interface Material Design 3 sophistiquée et propulsé par YouTube Music.</strong>
</p>

<p align="center">
  <a href="#-aper%C3%A7u">Aperçu</a> •
  <a href="#-fonctionnalit%C3%A9s">Fonctionnalités</a> •
  <a href="#-stack-technique">Stack Technique</a> •
  <a href="#%EF%B8%8F-architecture">Architecture</a> •
  <a href="#-d%C3%A9marrage">Démarrage</a> •
  <a href="#-compilation">Compilation</a> •
  <a href="#%EF%B8%8F-roadmap-feuille-de-route">Roadmap</a>
</p>

---

## ✨ Aperçu

**MusicGlass** est un lecteur de musique moderne et haute-fidélité pour **Android**, conçu avec une interface méticuleusement soignée qui fait le pont entre le vaste catalogue de YouTube Music et une expérience Android native premium.

Propulsé par un client **InnerTube** personnalisé — la même API interne utilisée par YouTube Music — MusicGlass offre une expérience d'écoute riche, rapide et visuellement époustouflante. Chaque interaction, de la navigation dans le flux d'accueil à la lecture immersive en plein écran, est conçue avec des animations fluides et Material Design 3 au cœur de l'expérience.

> **Avertissement :** Ceci est un prototype tiers. Il n'est ni affilié, ni approuvé, ni associé à YouTube, Google ou leurs affiliés. Aucun contournement de DRM n'est inclus.

---

## 🚀 Fonctionnalités

### 🎨 Interface Utilisateur Premium
- **Material Design 3** avec couleurs dynamiques (thème Monet sur Android 12+).
- **Effets inspirés du Liquid Glass** avec flou et transparences personnalisés.
- **Rendu bord-à-bord** avec transparence de la barre de statut et de la barre de navigation.
- **Micro-animations fluides** — transitions d'écran, expansion/réduction du lecteur, barre supérieure défilante.
- **Dispositions adaptatives** — lecteur plein écran responsive, mini-lecteur élégant, file d'attente interactive par glisser-déposer.
- **Modes de thème** — Clair / Sombre / Système avec préférences persistantes.

### 🎧 Lecture Audio Avancée
- **Moteur ExoPlayer / Media3** pour un streaming audio fiable et performant.
- **Notification média native** avec pochette, titre, artiste et contrôles de transport complets.
- **Service de premier plan** (`mediaPlayback`) pour une lecture audio en arrière-plan fiable.
- **Contrôles média système** — écran de verrouillage, panneau de notifications, Bluetooth, compatible Android Auto.
- **Gestion intelligente de la file d'attente** — aléatoire, répétition (une/toutes), insertion dynamique de pistes, transitions quasi sans blanc.
- **Cache audio LRU** (256 Mo) avec éviction automatique et préchargement de la piste suivante.
- **Résilience réseau** — survit aux pertes de connexion, reconnexions et URL de flux expirées de manière transparente.

### 🔍 Découverte & Bibliothèque (API InnerTube)
- **Flux d'accueil personnalisé** propulsé par le point de terminaison `FEmusic_home` de YouTube Music.
- **Recherche ultra-rapide** avec debounce de 350 ms et résultats groupés : Chansons, Albums, Artistes, Playlists, Vidéos.
- **Suggestions de recherche** avec autocomplétion via le point de terminaison `get_search_suggestions`.
- **Sections de bibliothèque** — Titres likés, Playlists utilisateur, Historique d'écoute (authentifié).
- **Enrichissement des pochettes** — correspondance intelligente et fallback via l'API de recherche YouTube Music, avec récupération concurrente (6 permis parallèles).

### 🎤 Intégration de Paroles Immersive
- **Base de données LRCLib** — intégration avec la base de paroles open-source LRCLib.
- **Paroles synchronisées en temps réel** avec surlignage ligne par ligne.
- **Texte brut en fallback** lorsque les paroles synchronisées ne sont pas disponibles.
- **Détection de la langue** et affichage.

### 🔄 Mises à Jour Over-the-Air
- **Intégration GitHub Releases** — vérification automatique des mises à jour au démarrage.
- **Suivi de la progression du téléchargement** avec indicateur visuel.
- **Installation d'APK** déclenchée directement via `FileProvider`.
- **Affichage du changelog** lors de l'installation d'une nouvelle version.
- **Suggestions de mise à jour désactivables** avec suivi par version.

---

## 🛠️ Stack Technique

| Catégorie | Technologie | Version |
|---|---|---|
| **Langage** | Kotlin | 2.2.10 |
| **Framework UI** | Jetpack Compose | BOM 2023.10.01 |
| **Design System** | Material Design 3 | Compose BOM |
| **Moteur Audio** | Media3 / ExoPlayer | 1.2.0 |
| **Session Média** | Media3 Session | 1.2.0 |
| **Client HTTP** | OkHttp | 4.12.0 |
| **Parsing JSON** | kotlinx.serialization + org.json | 1.6.2 |
| **Chargement d'images** | Coil | 2.5.0 |
| **Concurrence** | Kotlin Coroutines | 1.7.3 |
| **Gestion d'état** | StateFlow + ViewModel | 2.6.2 |
| **Navigation** | Navigation Compose | 2.7.5 |
| **Système de build** | Gradle (Kotlin DSL) | 9.1.1 |
| **SDK Minimum** | Android 8.0 (API 26) | — |
| **SDK Cible / Compilation** | Android 14 (API 34) | — |

---

## 🏗️ Architecture

MusicGlass suit une **architecture MVVM modulaire mono-module** avec une séparation claire des responsabilités :

```
app/src/main/java/com/musicglass/app/
├── App.kt                         # Classe Application + configuration Coil ImageLoader
├── MainActivity.kt                # Point d'entrée Single-Activity
│
├── core/
│   └── update/                    # Système d'auto-update
│       ├── UpdateModels.kt        # DTOs GitHub Release
│       ├── UpdateRepository.kt    # Vérification + téléchargement + installation APK
│       ├── UpdateViewModel.kt     # Logique changelog & dialogue de mise à jour
│       └── UpdateDialogs.kt       # Dialogues Compose
│
├── ui/
│   ├── MainScreen.kt              # Navigation racine (Accueil, Recherche, Bibliothèque, Paramètres)
│   ├── theme/
│   │   └── Theme.kt               # Thème Material 3 + Couleurs Dynamiques + Clair/Sombre
│   ├── features/
│   │   ├── HomeScreen.kt          # Flux d'accueil + en-tête de salutation
│   │   ├── HomeViewModel.kt       # État du flux d'accueil
│   │   ├── SearchScreen.kt        # Recherche avec suggestions + résultats
│   │   ├── LibraryScreen.kt       # Onglets : Titres likés, playlists, historique
│   │   ├── PlaylistScreen.kt      # Vue détaillée d'une playlist
│   │   ├── PlaylistViewModel.kt   # État playlist + gestion file d'attente
│   │   ├── MediaDetailScreens.kt  # Pages détail Album/Artiste
│   │   ├── SettingsScreen.kt      # Thème, qualité audio, debug, mises à jour
│   │   ├── LoginWebViewScreen.kt  # Connexion OAuth-style via WebView
│   │   ├── TrackActionsMenu.kt    # Menu contextuel appui long
│   │   ├── auth/
│   │   │   └── AccountAuthScreen.kt
│   │   ├── library/
│   │   │   └── artists/
│   │   │       └── ArtistsScreen.kt
│   │   └── profile/
│   │       └── ProfileBottomSheet.kt
│   └── player/
│       ├── FullPlayerScreen.kt    # Lecteur plein écran immersif
│       ├── LyricsScreen.kt        # Affichage des paroles synchronisées
│       └── QueueScreen.kt         # File d'attente réorganisable par glisser-déposer
│
├── playback/
│   ├── MusicGlassPlaybackController.kt   # Wrapper ExoPlayer + gestion file d'attente
│   ├── MusicGlassMediaSessionService.kt  # Service de premier plan Media3
│   ├── MusicGlassPlaybackCache.kt        # Cache audio LRU (SimpleCache)
│   ├── MusicGlassBitmapLoader.kt         # Chargeur bitmap personnalisé pour Media3
│   ├── NetworkConnectivityObserver.kt    # Surveillance réseau en temps réel
│   └── PlayerViewModel.kt               # État du lecteur
│
├── youtubemusic/
│   ├── InnerTubeClient.kt         # API InnerTube multi-client (WEB_REMIX, ANDROID_VR, TV_EMBEDDED)
│   ├── InnerTubeDTO.kt            # DTOs requête/réponse sérialisables
│   ├── InnerTubeJSONMapper.kt     # Mapping défensif JSON → modèles domaine
│   ├── MusicMetadataSanitizer.kt  # Normalisation + déduplication des métadonnées
│   ├── ArtworkEnricher.kt         # Moteur concurrent de recherche + correspondance de pochettes
│   ├── AuthService.kt             # Authentification SAPISID + persistance des cookies
│   └── LyricsService.kt           # Intégration des paroles LRCLib
│
└── persistence/
    ├── AppSettingsRepository.kt   # Préférences : thème, qualité audio, debug
    ├── LibraryRepository.kt       # Cache bibliothèque en mémoire
    └── PlaybackHistoryRepository.kt # Historique de lecture local avec recherche
```

### Décisions de Conception Clés

#### Stratégie InnerTube Multi-Client
L'innovation centrale de MusicGlass est son client **InnerTube** personnalisé, qui émule trois clients YouTube différents pour maximiser la compatibilité :

| Client | Utilisé pour | Bénéfice clé |
|---|---|---|
| `WEB_REMIX` (client 67) | Browse, Search, Home, Next, Lyrics | Accès complet au catalogue, recommandations |
| `ANDROID_VR` (client 28) | Player / URL du flux audio | URLs audio directes **sans** déchiffrement de signature |
| `TV_EMBEDDED` (client 85) | Lecteur de secours | Fonctionne pour le contenu nécessitant une authentification |

Le client `ANDROID_VR` est particulièrement critique — il retourne les URLs audio déchiffrées directement, évitant le complexe chiffrement de signature à n-paramètres qui affecte les autres clients YouTube.

#### Authentification (basée sur SAPISID)
- Authentification par cookies via `SAPISID` / `__Secure-3PAPISID`.
- Génération d'en-tête `Authorization` personnalisé utilisant SHA-1 HMAC.
- Stockage persistant via `SharedPreferences` avec flux de connexion WebView optionnel.

#### Pipeline d'Enrichissement des Pochettes
De nombreuses réponses de l'API YouTube Music manquent de pochettes de haute qualité. MusicGlass résout ce problème avec un **pipeline d'enrichissement concurrent** :
1. Pour chaque piste sans pochette, effectuer une recherche filtrée sur YouTube Music.
2. Faire correspondre les résultats en utilisant le stemming flou des titres + la correspondance des tokens d'artiste.
3. Replier sur des correspondances plus faibles (artiste → racine du titre).
4. Mettre en cache les correspondances réussies dans une `ConcurrentHashMap` pour réutilisation instantanée.

#### Résilience de la Lecture
- `NetworkConnectivityObserver` surveille l'état réseau en temps réel via `ConnectivityManager`.
- Nouvelle tentative transparente en cas de perte réseau avec `retryOnConnectionFailure`.
- Cache LRU (`SimpleCache`) avec plafond de 256 Mo, éviction du moins récemment utilisé.
- Préchargement de la piste suivante pendant la lecture.
- Support des redirections inter-protocoles pour une résolution résiliente des URLs de flux.

---

## 📦 Démarrage

### Prérequis
- **Android Studio** Hedgehog (2023.1.1) ou plus récent
- **JDK 17**
- **Android SDK 34** avec les outils de build
- Un appareil ou émulateur Android sous **Android 8.0 (API 26)** ou supérieur

### Cloner & Compiler

```bash
# Cloner le dépôt
git clone https://github.com/jeremy99981/MusicGlass-Android.git
cd MusicGlass-Android

# Compiler l'APK debug
./gradlew assembleDebug

# Ou installer directement sur un appareil connecté
./gradlew installDebug
```

### Signature

Les builds release utilisent une configuration de signature flexible :

1. Créez `keystore.properties` à la racine du projet :
   ```properties
   storeFile=/chemin/vers/votre.keystore
   storePassword=votreStorePassword
   keyAlias=votreKeyAlias
   keyPassword=votreKeyPassword
   ```

2. Ou définissez les variables d'environnement : `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

3. Sans l'un ni l'autre, le keystore de debug est utilisé comme fallback.

```bash
./gradlew assembleRelease
```

---

## 🔧 Compilation pour la Production

```bash
# Compiler l'APK release
./gradlew assembleRelease

# L'APK se trouvera dans :
# app/build/outputs/apk/release/MusicGlass-{versionName}.apk
```

> **Note :** Le nom de l'APK est automatiquement défini sur `MusicGlass-{versionName}.apk` selon `defaultConfig.versionName` dans `build.gradle.kts`.

---

## 🛣️ Roadmap (Feuille de route)

- [ ] **Authentification OAuth Complète** — Connexion sécurisée YouTube/Google avec rafraîchissement de token.
- [ ] **Synchronisation de la Bibliothèque** — Synchronisation distante des titres likés, albums, artistes et playlists.
- [ ] **Mode Hors-Ligne** — Mise en cache des fichiers audio pour une véritable lecture hors ligne avec gestion des téléchargements.
- [ ] **Paroles Avancées** — Surlignage mot par mot façon karaoké et superposition de traduction.
- [ ] **Égaliseur** — Intégration de l'égaliseur système via `AudioEffect`.
- [ ] **Android Auto** — Support complet de navigation et lecture média Android Auto.
- [ ] **Wear OS** — Application compagnon pour contrôles depuis une montre connectée.
- [ ] **Widgets** — Widgets d'écran d'accueil (lecture en cours, actions rapides, favoris).
- [ ] **Fonctionnalités Sociales** — Scrobbling Last.fm.
- [ ] **Multi-langue** — Localisation complète (FR, ES, DE, JA, etc.).

---

## 🤝 Contribution

Les contributions, signalements de bugs et demandes de fonctionnalités sont les bienvenus !

1. Forkez le dépôt
2. Créez votre branche de fonctionnalité (`git checkout -b feature/FonctionnaliteGeniale`)
3. Commitez vos changements (`git commit -m 'Ajout de la FonctionnaliteGeniale'`)
4. Poussez vers la branche (`git push origin feature/FonctionnaliteGeniale`)
5. Ouvrez une Pull Request

---

## 📄 Licence

Ce projet est un prototype tiers. Consultez le [dépôt principal MusicGlass](https://github.com/jeremy99981/MusicGlass) pour les informations de licence.

---

<p align="center">
  <i>Conçu avec ❤️ pour les passionnés de musique et les développeurs Android.</i>
</p>
