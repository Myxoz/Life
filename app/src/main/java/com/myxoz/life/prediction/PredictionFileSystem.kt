package com.myxoz.life.prediction

import android.content.Context
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

object PredictionFileSystem {
    fun getPathOfModel(context: Context, name: String): File {
        val dir = File(context.filesDir, "models")
        dir.mkdir()
        val modelPath = File(dir, name)
        return modelPath
    }
    fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    fun DataInputStream.readString(): String {
        val length = readInt()
        val bytes = ByteArray(length)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
    fun DataOutputStream.writeStringList(value: List<String>) {
        writeInt(value.size)
        value.forEach {
            writeString(it)
        }
    }

    fun DataInputStream.readStringList(): List<String> {
        return List(readInt()) { readString() }
    }

    fun DataOutputStream.writeLongList(value: List<Long>) {
        writeInt(value.size)
        value.forEach {
            writeLong(it)
        }
    }

    fun DataInputStream.readLongList(): List<Long> {
        return List(readInt()) { readLong() }
    }
}