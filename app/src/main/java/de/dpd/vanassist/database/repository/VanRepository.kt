package de.dpd.vanassist.database.repository

import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.daos.VanDao
import de.dpd.vanassist.database.entity.VanEntity

class VanRepository {

    companion object {

        private var instance: VanRepository? = null
        private var vanDao : VanDao? = null

        /* Access variable for VanRepository */
        val shared: VanRepository
            get() {
                if (instance == null) {
                    vanDao = AppDatabase.shared.vanDao()
                    instance = VanRepository()
                }
                return instance!!
            }
    }

    /* Insert van record */
    fun insert(van:VanEntity) {
        vanDao!!.insertVan(van)
    }

    /* Get van by id */
    fun getVanById(vanId:String): VanEntity {
        return vanDao!!.getVan(vanId)
    }
}