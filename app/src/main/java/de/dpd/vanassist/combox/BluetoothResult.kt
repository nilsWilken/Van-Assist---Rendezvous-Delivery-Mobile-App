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

    fun getNextStop(): DoubleArray {
        try {
            value?.let {
                val buffer = ByteBuffer.wrap(it)
                val array = DoubleArray(3)

                array[0] = buffer.getDouble()
                array[1] = buffer.getDouble()

                val fArray = ByteArray(8)
                buffer.get(fArray, 16, 2)
                array[2] = ByteBuffer.wrap(fArray).getDouble(0)

                return array
            }
        } catch(e: BufferUnderflowException) {
            Log.e(TAG, "buffer underflow exception")
        }

        return DoubleArray(0)
    }

    fun getPosition(): DoubleArray {
        try {
            value?.let {
                val buffer = ByteBuffer.wrap(it)
                val array = DoubleArray(4)
                array[0] = buffer.getDouble()
                array[1] = buffer.getDouble()

                val bArray = ByteArray(2)

                buffer.get(bArray, 16, 2)
                array[2] = ByteBuffer.wrap(bArray).getDouble(0)

                buffer.get(bArray, 18, 2)
                array[3] = ByteBuffer.wrap(bArray).getDouble(0)

                return array
            }

        } catch (e: BufferUnderflowException) {
            Log.e(TAG, "buffer underflow exception")
        }
        return DoubleArray(0)
    }

    fun getVehicleStatus(): ShortArray {
        try {
            value?.let{
                val buffer = ByteBuffer.wrap(it)
                val result = ShortArray(2)

                result[0] = buffer.getShort()
                //result[1] = buffer.getShort()
                result[1] = 0

                Log.i("VEHICLE_STATUS", result[0].toString() + " " + result[1].toString())

                return result
            }
        } catch(e: BufferUnderflowException) {
            Log.e(TAG, "buffer underflow exception")
        }

        return ShortArray(0)
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