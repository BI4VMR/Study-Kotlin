package net.bi4vmr.study.mockk.base

import io.mockk.every
import io.mockk.mockkObject
import org.junit.Test


class UtilKotlinX {
    companion object {
        @JvmStatic
        fun method(): String {
            return "UtilKotlinX.ok()"
        }
    }
}

class ObjectTest {

    @Test
    fun testMethod() {
        mockkObject(UtilKotlinX.Companion)

        every { UtilKotlinX.method() } returns "Test"

        println("method:[${UtilKotlinX.method()}]")
    }
}

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
