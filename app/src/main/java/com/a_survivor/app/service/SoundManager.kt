package com.a_survivor.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.a_survivor.app.model.MapType
import java.io.File

/**
 * assets/sounds/ 폴더에 파일을 배치하면 자동 인식.
 * 확장자: ogg·mp3·wav·flac·m4a 모두 허용.
 *
 * FLAC 등 APK에 압축 저장된 파일은 캐시 디렉토리에 복사해서 재생.
 */
object SoundManager {

    private const val TAG = "SoundManager"
    private const val DIR = "sounds"
    private val EXTENSIONS = listOf("ogg", "mp3", "wav", "flac", "m4a")

    enum class Bgm(val baseName: String) {
        NONE(""),
        TOWN("bgm_town"),
        BATTLE("bgm_battle")
    }

    enum class Sfx(val baseName: String) {
        ATTACK("sfx_attack"),
        MONSTER_HIT("sfx_monster_hit"),
        MONSTER_DIE("sfx_monster_die"),
        PLAYER_HIT("sfx_player_hit"),
        LEVEL_UP("sfx_level_up"),
        ITEM_PICKUP("sfx_item_pickup"),
        SCROLL_SUCCESS("sfx_scroll_success"),
        SCROLL_FAIL("sfx_scroll_fail"),
        PORTAL("sfx_portal")
    }

    var bgmMuted: Boolean = false
        set(value) {
            field = value
            if (value) bgmPlayer?.pause()
            else if (currentBgm != Bgm.NONE) bgmPlayer?.start()
        }

    var sfxMuted: Boolean = false

    private var bgmPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    private val sampleIdToSfx = mutableMapOf<Int, Sfx>()
    private val readyIds       = mutableMapOf<Sfx, Int>()
    private val fallbackPlayers = mutableMapOf<Sfx, MediaPlayer>()

    private var currentBgm = Bgm.NONE
    private var appContext: Context? = null

    /**
     * asset 파일을 캐시 디렉토리로 복사해 절대 경로를 반환.
     * APK 압축 여부와 무관하게 동작한다.
     */
    private fun cacheAsset(context: Context, baseName: String): Pair<File, String>? {
        for (ext in EXTENSIONS) {
            val assetPath = "$DIR/$baseName.$ext"
            try {
                // 파일 존재 확인
                val size = context.assets.open(assetPath).use { it.available() }
                if (size == 0) continue

                val cacheFile = File(context.cacheDir, "snd_${baseName}.$ext")
                if (!cacheFile.exists() || cacheFile.length() == 0L) {
                    context.assets.open(assetPath).use { input ->
                        cacheFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    Log.d(TAG, "$assetPath → 캐시 복사 완료 (${cacheFile.length()}B)")
                }
                return cacheFile to ext
            } catch (_: Exception) { }
        }
        return null
    }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        val sp = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        soundPool = sp

        sp.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                val sfx = sampleIdToSfx[sampleId] ?: return@setOnLoadCompleteListener
                readyIds[sfx] = sampleId
                Log.d(TAG, "SoundPool 준비: ${sfx.baseName}")
            }
        }

        for (sfx in Sfx.entries) {
            val (cacheFile, ext) = cacheAsset(context, sfx.baseName) ?: run {
                Log.w(TAG, "${sfx.baseName}.* 없음 — 스킵")
                continue
            }

            // ① MediaPlayer 폴백 즉시 준비 (SoundPool 완료 전에도 재생 보장)
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(cacheFile.absolutePath)
                    prepare()
                }
                fallbackPlayers[sfx] = mp
                Log.d(TAG, "${sfx.baseName}.$ext MediaPlayer 준비 완료")
            } catch (e: Exception) {
                Log.e(TAG, "${sfx.baseName}.$ext MediaPlayer 실패: ${e.message}")
            }

            // ② SoundPool 비동기 로드 (완료 후 저지연 경로로 자동 전환)
            val sampleId = sp.load(cacheFile.absolutePath, 1)
            if (sampleId > 0) {
                sampleIdToSfx[sampleId] = sfx
                Log.d(TAG, "${sfx.baseName}.$ext SoundPool 로드 시작")
            }
        }
    }

    fun switchBgm(bgm: Bgm) {
        if (bgm == currentBgm) return
        currentBgm = bgm

        bgmPlayer?.stop()
        bgmPlayer?.release()
        bgmPlayer = null

        if (bgm == Bgm.NONE || bgmMuted) return

        val ctx = appContext ?: return
        val (cacheFile, ext) = cacheAsset(ctx, bgm.baseName) ?: run {
            Log.w(TAG, "${bgm.baseName}.* 없음 — BGM 스킵")
            return
        }
        try {
            bgmPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(cacheFile.absolutePath)
                isLooping = true
                setVolume(0.6f, 0.6f)
                prepare()
                start()
                Log.d(TAG, "${bgm.baseName}.$ext BGM 시작")
            }
        } catch (e: Exception) {
            Log.e(TAG, "${bgm.baseName}.$ext BGM 실패: ${e.message}")
        }
    }

    fun bgmForMap(mapType: MapType): Bgm = when (mapType) {
        MapType.TOWN -> Bgm.TOWN
        else -> Bgm.BATTLE
    }

    fun playSfx(sfx: Sfx) {
        if (sfxMuted) return

        // SoundPool이 준비됐으면 우선 사용 (저지연)
        val id = readyIds[sfx]
        if (id != null) {
            val stream = soundPool?.play(id, 1f, 1f, 1, 0, 1f) ?: 0
            if (stream > 0) return
        }

        // MediaPlayer 폴백 (init()에서 항상 준비됨)
        val fb = fallbackPlayers[sfx] ?: return
        try {
            fb.seekTo(0)
            if (!fb.isPlaying) fb.start()
        } catch (e: Exception) {
            Log.e(TAG, "playSfx(${sfx.baseName}) 실패: ${e.message}")
        }
    }

    fun onPause() {
        bgmPlayer?.pause()
    }

    fun onResume() {
        if (!bgmMuted && currentBgm != Bgm.NONE) bgmPlayer?.start()
    }

    fun release() {
        bgmPlayer?.stop()
        bgmPlayer?.release()
        bgmPlayer = null
        soundPool?.release()
        soundPool = null
        fallbackPlayers.values.forEach { it.release() }
        fallbackPlayers.clear()
        sampleIdToSfx.clear()
        readyIds.clear()
        currentBgm = Bgm.NONE
        appContext = null
    }
}
