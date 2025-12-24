package net.bi4vmr.study.kotlin_feature

class Car {

    fun getInfo() {
        println("This is a Car.")
    }
}

class Shop() {
    var car: Car? = null

    fun exchange() {
        car = Car()
    }
}
