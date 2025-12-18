package net.bi4vmr.study.mockk.base

class Car {

    fun getInfo() {
        println("This is a Car.")
    }
}

class Shop(){
    var car :Car? = null

    fun exchange(){
        car = Car()
    }
}
