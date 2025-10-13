package net.bi4vmr.study.mockk.base

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.Test

/**
 * TODO 添加描述。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ConsTest {

    @Test
    fun aaa() {

        // val mockCar:Car = mockk()
        // println("mockd car : $mockCar")

        val car = Car()
        println("car1 : $car")
        mockkConstructor(Car::class)
        val car2 = Car()
        println("car2 : $car2")
        // every { constructedWith<Car>() } returns mockk()
        // val s = Shop()
        // s.exchange()
        // println("${s.car}")
        // verify { Car() }
        // println("${Car()}")

        // val car = Car()
        // println("new car : $car")
    }
}
