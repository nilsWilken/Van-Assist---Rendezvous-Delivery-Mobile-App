package de.dpd.vanassist

import androidx.test.platform.app.InstrumentationRegistry
import de.dpd.vanassist.database.repository.ParcelRepository
import org.junit.Before
import org.junit.Test

class ParcelRepositoryTest {
    private var parcelRepo : ParcelRepository? = null

    @Before
    fun setup() {
        parcelRepo = ParcelRepository(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testInsert() {
//        val parcel = Parcel(UUID.randomUUID(), 0, "bla", "a1", 50, 20, 30 , 10)
//        parcelRepo?.insert(parcel)
//
//        val parcelTest = parcelRepo?.find(parcel.id)
//        Assert.assertEquals(parcel.nameOfRecipient, parcelTest?.nameOfRecipient)
    }
}