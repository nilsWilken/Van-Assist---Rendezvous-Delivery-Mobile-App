package de.dpd.vanassist.util

/**
 * Created by Jasmin Weimüller
 * Parsing the Vehicle Object
 */
class Vehicle {

var id : String
var lat_location : String
var long_location : String

   constructor(lat_locaction: String, long_location: String) {
        this.id = "some id"
        this.lat_location = lat_locaction
        this.long_location = long_location
    }

   constructor(id: String, lat_locaction: String, long_location: String){
        this.id  = id
        this.lat_location = lat_locaction
        this.long_location = long_location
    }



}