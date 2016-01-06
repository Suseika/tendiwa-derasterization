package org.tendiwa.derasterization

import org.junit.Test
import org.tendiwa.plane.geometry.rectangles.Rectangle
import org.tendiwa.plane.grid.constructors.GridRectangle
import org.tendiwa.plane.grid.dimensions.by
import org.tendiwa.plane.grid.tiles.Tile
import kotlin.test.assertEquals

class GridRectangleDerasterizationKtTest {
    @Test
    fun toRectangle() {
        GridRectangle(Tile(0, -4), 5 by 5)
            .toRectangle()
            .apply { assertEquals(Rectangle(0.0, -4.0, 5.0, 5.0), this) }
    }
}
