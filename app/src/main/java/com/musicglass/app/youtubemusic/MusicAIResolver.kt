package com.musicglass.app.youtubemusic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves parsed MusicAIIntent objects into playable music via the InnerTube API.
 * Mirrors the iOS MusicAIResolver behavior.
 */
class MusicAIResolver(
    private val client: InnerTubeClient,
    private val mapper: InnerTubeJSONMapper
) {
    suspend fun resolve(intent: MusicAIIntent): MusicAIResolution = withContext(Dispatchers.IO) {
        if (intent.confidence < 0.5) {
            return@withContext MusicAIResolution.NeedsClarification(
                intent.clarificationQuestion ?: "Précisez votre demande."
            )
        }

        when (intent.type) {
            "playTrack" -> resolveTrack(intent)
            "playPopularTrack" -> resolvePopularTrack(intent)
            "playLatestTrack" -> resolveLatestTrack(intent)
            "playAlbum" -> resolveAlbum(intent)
            "playLatestAlbum" -> resolveLatestAlbum(intent)
            "openAlbum" -> resolveAlbum(intent)
            "listAlbums" -> resolveListAlbums(intent)
            "playPlaylist" -> resolvePlaylist(intent)
            "playLikedSongs" -> resolveLikedSongs()
            "playArtistMix" -> resolveArtistMix(intent)
            else -> MusicAIResolution.OpenSearch(
                listOfNotNull(intent.artistName, intent.trackTitle).joinToString(" ")
            )
        }
    }

    private suspend fun resolveTrack(intent: MusicAIIntent): MusicAIResolution {
        val q = listOfNotNull(intent.trackTitle, intent.artistName).joinToString(" ")
        val json = client.search(q, null)
        val results = mapper.mapSearchResults(json).filter { it.type == ItemType.SONG }
        return if (results.isEmpty()) MusicAIResolution.Failure("Titre non trouvé")
        else MusicAIResolution.PlayableTrack(results[0], results)
    }

    private suspend fun resolvePopularTrack(intent: MusicAIIntent): MusicAIResolution {
        val q = "${intent.artistName ?: ""} meilleurs titres"
        val json = client.search(q, null)
        val results = mapper.mapSearchResults(json).filter { it.type == ItemType.SONG }
        return if (results.isEmpty()) MusicAIResolution.Failure("Titres populaires non trouvés")
        else MusicAIResolution.PlayableTrack(results[0], results.take(10))
    }

    private suspend fun resolveLatestTrack(intent: MusicAIIntent): MusicAIResolution {
        val q = "${intent.artistName ?: ""} nouveau single"
        val json = client.search(q, null)
        val results = mapper.mapSearchResults(json).filter { it.type == ItemType.SONG }
        return if (results.isEmpty()) MusicAIResolution.Failure("Nouveau titre non trouvé")
        else MusicAIResolution.PlayableTrack(results[0], results)
    }

    private suspend fun resolveAlbum(intent: MusicAIIntent): MusicAIResolution {
        val q = listOfNotNull(intent.albumTitle, intent.artistName).joinToString(" ")
        val json = client.search(q, null)
        val albums = mapper.mapSearchResults(json).filter { it.type == ItemType.ALBUM }
        val browseId = albums.firstOrNull()?.browseId
            ?: return MusicAIResolution.Failure("Album non trouvé")
        return try {
            val playlistJson = client.getPlaylistQueue(browseId)
            val queueTracks = mapper.mapSearchResults(playlistJson)
            if (queueTracks.isEmpty()) MusicAIResolution.Failure("Album vide")
            else {
                val albumInfo = AlbumInfo(
                    name = albums.firstOrNull()?.title ?: "Album",
                    browseId = browseId
                )
                MusicAIResolution.PlayableAlbum(albumInfo, queueTracks)
            }
        } catch (_: Exception) {
            MusicAIResolution.Failure("Impossible de charger l'album")
        }
    }

    private suspend fun resolveLatestAlbum(intent: MusicAIIntent): MusicAIResolution {
        val q = intent.artistName ?: return MusicAIResolution.Failure("Artiste non spécifié")
        val json = client.search(q, null)
        val artists = mapper.mapSearchResults(json).filter { it.type == ItemType.ARTIST }
        val artistId = artists.firstOrNull()?.browseId
            ?: return MusicAIResolution.Failure("Artiste non trouvé")
        val artistJson = client.getBrowse(artistId)
        val results = mapper.mapSearchResults(artistJson)
        val albums = results.filter { it.type == ItemType.ALBUM && it.browseId != null }
        if (albums.isEmpty()) return MusicAIResolution.Failure("Aucun album trouvé")
        val latest = albums.first()
        return resolveAlbumListPlayback(latest)
    }

    private suspend fun resolveListAlbums(intent: MusicAIIntent): MusicAIResolution {
        val q = intent.artistName ?: return MusicAIResolution.Failure("Artiste non spécifié")
        val json = client.search(q, null)
        val artists = mapper.mapSearchResults(json).filter { it.type == ItemType.ARTIST }
        val artistId = artists.firstOrNull()?.browseId
            ?: return MusicAIResolution.Failure("Artiste non trouvé")
        val artistJson = client.getBrowse(artistId)
        val results = mapper.mapSearchResults(artistJson)
        val albums = results.filter { it.type == ItemType.ALBUM }
            .distinctBy { it.browseId ?: it.id }
        return if (albums.isEmpty()) MusicAIResolution.Failure("Aucun album trouvé")
        else MusicAIResolution.AlbumList(albums)
    }

    private suspend fun resolvePlaylist(intent: MusicAIIntent): MusicAIResolution {
        val q = intent.playlistName ?: return MusicAIResolution.Failure("Playlist non spécifiée")
        val json = client.search(q, null)
        val playlists = mapper.mapSearchResults(json).filter { it.type == ItemType.PLAYLIST }
        val browseId = playlists.firstOrNull()?.browseId
            ?: return MusicAIResolution.Failure("Playlist non trouvée")
        return try {
            val queueJson = client.getPlaylistQueue(browseId)
            val tracks = mapper.mapSearchResults(queueJson)
            if (tracks.isEmpty()) MusicAIResolution.Failure("Playlist vide")
            else MusicAIResolution.PlayablePlaylist(playlists.first(), tracks)
        } catch (_: Exception) {
            MusicAIResolution.Failure("Impossible de charger la playlist")
        }
    }

    private suspend fun resolveLikedSongs(): MusicAIResolution {
        return try {
            val json = client.getLikedSongs()
            val tracks = mapper.mapSearchResults(json)
            if (tracks.isEmpty()) MusicAIResolution.Failure("Favoris vides")
            else MusicAIResolution.PlayableTrack(tracks[0], tracks)
        } catch (_: Exception) {
            MusicAIResolution.Failure("Impossible de charger les favoris")
        }
    }

    private suspend fun resolveArtistMix(intent: MusicAIIntent): MusicAIResolution {
        val q = intent.artistName ?: return MusicAIResolution.Failure("Artiste non spécifié")
        val json = client.search(q, null)
        val artists = mapper.mapSearchResults(json).filter { it.type == ItemType.ARTIST }
        val artistId = artists.firstOrNull()?.browseId
            ?: return MusicAIResolution.Failure("Artiste non trouvé")
        val artistJson = client.getBrowse(artistId)
        val results = mapper.mapSearchResults(artistJson)
        val firstTrack = results.firstOrNull { it.type == ItemType.SONG }
            ?: return MusicAIResolution.Failure("Mix artiste impossible")
        return MusicAIResolution.PlayableRadio(firstTrack)
    }

    private suspend fun resolveAlbumListPlayback(album: SongItem): MusicAIResolution {
        val browseId = album.browseId ?: return MusicAIResolution.Failure("Album sans identifiant")
        return try {
            val json = client.getPlaylistQueue(browseId)
            val tracks = mapper.mapSearchResults(json)
            if (tracks.isEmpty()) MusicAIResolution.Failure("Album vide")
            else MusicAIResolution.PlayableAlbum(
                AlbumInfo(name = album.title, browseId = browseId),
                tracks
            )
        } catch (_: Exception) {
            MusicAIResolution.Failure("Impossible de charger l'album")
        }
    }
}
