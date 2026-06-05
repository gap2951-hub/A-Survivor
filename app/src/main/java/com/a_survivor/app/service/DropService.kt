package com.a_survivor.app.service

import com.a_survivor.app.model.DropEntry
import com.a_survivor.app.model.DropItem
import kotlin.random.Random

class DropService {

    /**
     * 드랍 테이블의 각 항목을 독립적으로 확률 판정합니다.
     * 한 처치에서 여러 아이템이 동시에 드랍될 수 있습니다.
     */
    fun roll(
        entries: List<DropEntry>,
        random: Random = Random.Default
    ): List<DropItem> = entries
        .filter { random.nextFloat() < it.probability }
        .map { it.item }
}
