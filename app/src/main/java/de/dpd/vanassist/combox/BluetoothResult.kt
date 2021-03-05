package de.dpd.vanassist.combox

import android.util.Log
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

data class BluetoothResult(val uuid: UUID, val value: ByteArray?, val status: Int) {

    private val TAG = BluetoothResult::class.java.simpleName

    fun readByte(): Byte? {
        if (value?.size == 1)
            return value[0]
        else return 5
    }

    fun getString() =
        value?.toString(Charset.defaultCharset()) ?: ""

    fun getPosition(): DoubleArray {
        try {
            value?.let {
                val buffer = ByteBuffer.wrap(it)
                val array = DoubleArray(3)
                array[0] = buffer.getDouble()
                array[1] = buffer.getDouble()
                array[2] = buffer.getFloat().toDouble()
                return array
            }

        } catch (e: BufferUnderflowException) {
            Log.e(TAG, "buffer underflow exception")
        }
        return DoubleArray(0)
    }

    fun getShort(): Short {
        value?.let {
            return ByteBuffer.wrap(it).short
        }
        return 0
    }

    fun getByteArray(bitCount: Int): ByteArray {
        value?.let {
            val buffer = ByteBuffer.wrap(it)
            val bitSet = BitSet.valueOf(buffer)
            val elementCount = bitSet.size() / bitCount
            val array = ByteArray(elementCount)
            for (position in (0 until elementCount)) {
                val result = bitSet.get(position, position + bitCount).toByteArray()
                array[position] = if (result.size == 1) result.get(0) else 0
            }
            return array
        }
        return ByteArray(0)

    }

}