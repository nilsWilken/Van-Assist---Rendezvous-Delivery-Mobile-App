package de.dpd.vanassist.combox

import android.util.Log
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

                val fArray = ByteArray(2)
                buffer.get(fArray, 0, 2)
                array[2] = ByteBuffer.wrap(fArray).getShort(0).toDouble()

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

                //array[2] = buffer.getDouble()
                //array[3] = buffer.getDouble()

                //val bArray = ByteArray(8)
                val bArray = ByteArray(2)

                buffer.get(bArray, 0, 2)
                array[2] = ByteBuffer.wrap(bArray).getShort(0).toDouble()

                buffer.get(bArray, 0, 2)
                array[3] = ((ByteBuffer.wrap(bArray).getShort(0)).toInt() shr 4).toDouble()

                Log.i(TAG, "Received position: [" + array[0] + ", " + array[1] + ", " + array[2] + ", " + array[3] + "]")

                return array
            }

        } catch (e: BufferUnderflowException) {
            Log.e(TAG, "buffer underflow exception")
        }
        return DoubleArray(0)
    }

    fun getTargetPosition(): DoubleArray {
        try {
            value?.let {
                val buffer = ByteBuffer.wrap(it)
                val array = DoubleArray(5)
                array[0] = buffer.getDouble()
                array[1] = buffer.getDouble()

                //array[2] = buffer.getDouble()
                //array[3] = buffer.getDouble()

                //val bArray = ByteArray(8)
                var bArray = ByteArray(2)

                bArray[0] = buffer.get()
                bArray[1] = buffer.get()
                array[2] = ByteBuffer.wrap(bArray).getShort(0).toDouble()

                bArray = ByteArray(2)
                bArray[0] = buffer.get()
                bArray[1] = buffer.get()
                //buffer.get(bArray, 0, 2)
                //ByteBuffer.wrap(bArray).getShort(0).toDouble()

                val orientation = ByteArray(2)
                val posReached = ByteArray(1)

                //1111 0000
                //val mask1: UByte = 240u
                //val mask1 = BitSet(8)
                //mask1.set(0, 3, true)
                //mask1.set(4, 7, false)
                val mask1 = 240

                //0000 1111
                //val mask2: UByte = 15u
                //val mask2 = BitSet(8)
                //mask2.set(0, 3, false)
                //mask2.set(4, 7, true)
                val mask2 = 15

                orientation[0] = bArray[0]

                val bA = ByteArray(1)
                bA[0] = bArray[1]

               /* val bSet = BitSet.valueOf(bA)

                var tBSet = BitSet(8)

                for(i in 0 until (bSet.length()-1)) {
                    tBSet.set(i, bSet.get(i))
                }

                for(i in bSet.length() until bA[0].countLeadingZeroBits()-1) {
                    tBSet.set(i, false)
                }

                Log.i(TAG, tBSet.size().toString())
                Log.i(TAG, bA[0].countLeadingZeroBits().toString())
                for(i in 0..7) {
                    Log.i(TAG, i.toString() + ": " + tBSet.get(i).toString())
                }*/

                val comp1 = bA[0].toInt().and(mask1)

                val comp2 = bA[0].toInt().and(mask2)

                orientation[1] = comp1.toByte()
                Log.i(TAG, bArray[0].toByte().toString() + " " + orientation[1].toString())
                Log.i(TAG, "Received: " + ((ByteBuffer.wrap(orientation).getShort(0)).toInt() shr 4))

                array[3] = ((ByteBuffer.wrap(orientation).getShort(0)).toInt() shr 4).toDouble()

                //array[4] = ByteBuffer.wrap(posReached).get().toDouble()
                array[4] = comp2.toDouble()

                Log.i(TAG, "Received target position: [" + array[0] + ", " + array[1] + ", " + array[2] + ", " + array[3] + ", " + array[4] + "]")

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

                result[0] = buffer.get().toShort()
                //result[1] = buffer.getShort()
                result[1] = buffer.get().toShort()

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