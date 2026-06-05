package com.a_survivor.app.model

import androidx.compose.ui.geometry.Offset

data class CameraState(
    val x: Float = 0f,      // 카메라 중심의 월드 X 좌표
    val y: Float = 0f,      // 카메라 중심의 월드 Y 좌표
    val zoom: Float = 1f    // 확대 배율 (1f = 기본)
) {
    /** 월드 X → 스크린 X */
    fun toScreenX(worldX: Float, screenWidth: Float): Float =
        (worldX - x) * zoom + screenWidth / 2f

    /** 월드 Y → 스크린 Y */
    fun toScreenY(worldY: Float, screenHeight: Float): Float =
        (worldY - y) * zoom + screenHeight / 2f

    /** 월드 좌표 → 스크린 Offset (Canvas drawXxx 에 직접 전달) */
    fun toScreenOffset(
        worldX: Float,
        worldY: Float,
        screenWidth: Float,
        screenHeight: Float
    ): Offset = Offset(
        x = toScreenX(worldX, screenWidth),
        y = toScreenY(worldY, screenHeight)
    )

    /** 스크린 X → 월드 X (터치 좌표 역변환) */
    fun toWorldX(screenX: Float, screenWidth: Float): Float =
        (screenX - screenWidth / 2f) / zoom + x

    /** 스크린 Y → 월드 Y (터치 좌표 역변환) */
    fun toWorldY(screenY: Float, screenHeight: Float): Float =
        (screenY - screenHeight / 2f) / zoom + y

    /** 플레이어 위치로 카메라 중심 이동 */
    fun followPlayer(playerX: Float, playerY: Float): CameraState =
        copy(x = playerX, y = playerY)

    /**
     * 카메라를 월드 경계 안으로 제한.
     * 뷰포트 절반이 월드 밖을 넘어가지 않도록 clamp.
     */
    fun clampToWorld(
        world: GameWorld,
        screenWidth: Float,
        screenHeight: Float
    ): CameraState {
        val halfW = (screenWidth  / 2f) / zoom
        val halfH = (screenHeight / 2f) / zoom
        return copy(
            x = x.coerceIn(halfW, (world.width  - halfW).coerceAtLeast(halfW)),
            y = y.coerceIn(halfH, (world.height - halfH).coerceAtLeast(halfH))
        )
    }
}
