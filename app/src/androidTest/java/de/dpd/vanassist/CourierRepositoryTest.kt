package de.dpd.vanassist

import androidx.test.platform.app.InstrumentationRegistry
import de.dpd.vanassist.database.repository.CourierRepository
import org.junit.Before
import org.junit.Test

class CourierRepositoryTest {
    private var courierRepo : CourierRepository? = null

    @Before
    fun setup() {
        //courierRepo = CourierRepository(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testInsert() {
        //val courier = Courier("yolo1", "elon", "musk")
        //courierRepo?.insert(courier)

        //val courierTest = courierRepo?.find("yolo")
        //Assert.assertEquals(courier.firstName, courierTest?.firstName)
    }
}