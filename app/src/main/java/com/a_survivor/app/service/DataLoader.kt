package com.a_survivor.app.service

import android.content.Context
import android.util.Log

object DataLoader {

    private const val TAG = "DataLoader"

    fun loadCsv(context: Context, fileName: String): List<Map<String, String>> {
        // 내부 저장소 우선 (에디터에서 수정한 데이터), 없으면 assets 폴백
        val internalFile = java.io.File(context.filesDir, "data/$fileName")
        return try {
            val stream = if (internalFile.exists()) internalFile.inputStream()
                         else context.assets.open("data/$fileName")
            stream.bufferedReader(Charsets.UTF_8).use { reader ->
                val raw = reader.readLines()
                val lines = raw.mapIndexed { i, line ->
                    if (i == 0) line.trimStart('﻿').trim() else line.trim()
                }.filter { it.isNotBlank() && !it.startsWith("#") }
                if (lines.isEmpty()) return emptyList()
                val headers = parseLine(lines.first())
                lines.drop(1).mapNotNull { line ->
                    val values = parseLine(line)
                    if (values.size == headers.size) {
                        headers.zip(values).toMap()
                    } else {
                        Log.w(TAG, "컬럼 수 불일치, 스킵 [$fileName]: $line")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "$fileName 로드 실패: ${e.message}")
            emptyList()
        }
    }

    // 따옴표로 감싼 필드(쉼표 포함 가능) 지원하는 CSV 라인 파서
    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"'             -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else                 -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
