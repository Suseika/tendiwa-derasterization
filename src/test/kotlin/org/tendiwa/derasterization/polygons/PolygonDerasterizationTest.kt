package org.tendiwa.derasterization.polygons

import org.junit.Test
import org.tendiwa.plane.geometry.segments.Segment
import org.tendiwa.plane.grid.constructors.GridRectangle
import org.tendiwa.plane.grid.dimensions.by
import org.tendiwa.plane.grid.masks.*
import org.tendiwa.plane.grid.rectangles.hulls.GridRectangularHull
import kotlin.test.assertEquals

class PolygonDerasterizationTest {
    @Test fun collapsesContinuousEdgeChains() {
        GridRectangle(10 by 10)
            .derasterized()
            .first()
            .enclosing
            .apply { assertEquals(4, segments.size) }
    }

    @Test fun derasterizesGridMaskWithMultipleConnectivityComponents() {
        val component1 = GridRectangle(10 by 10)
        val component2 = component1.move(20, 20)
        component1
            .union(component2)
            .boundedBy(GridRectangularHull(component1, component2))
            .derasterized()
            .apply { assertEquals(2, size) }
    }

    @Test fun rejectsExtraTiles() {
        val complex =
            StringGridMask(
                ".....###..",
                ".....###..",
                ".....###.#",
                ".....##..#",
                "..##..####",
                "##########",
                "#####.####",
                ".####.####",
                ".####.#.##",
                "......####"
            )
        val simplified =
            StringGridMask(
                ".....###..",
                ".....###..",
                ".....###..",
                "..........",
                "......####",
                ".####.####",
                ".####.####",
                ".####.####",
                ".####.....",
                ".........."
            )
        val edgesOf: (StringGridMask) -> Set<Segment> = {
            it.derasterized()
                .map { it.enclosing }
                .flatMap { it.segments }
                .toSet()
        }
        assertEquals(
            edgesOf(simplified),
            edgesOf(complex)
        )
    }

    @Test
    fun derasterizesPolygonWithHoles() {
        StringGridMask(
            "..#######.",
            ".########.",
            ".########.",
            ".####.###.",
            ".###..####",
            ".#########",
            ".#########",
            ".#########",
            ".####.....",
            ".........."
        )
            .derasterized()
            .apply { assertEquals(1, size) }
            .first()
            .apply { assertEquals(1, holes.size) }
    }

    @Test
    fun derasterizesMultiplePolygonsWithHoles() {
        StringGridMask(
            "#######..##############...",
            "#######..##############...",
            "#######..##############...",
            "###.###..###.######.###...",
            "#######..##############...",
            "#######..##############...",
            "#######..##############..."
        )
            .derasterized()
            .apply { assertEquals(2, size) }
            .apply { assertEquals(3, flatMap { it.holes }.size) }
    }

    @Test fun derasterizesEmptyMask() {
        EmptyGridMask()
            .boundedBy(GridRectangle(3 by 4))
            .derasterized()
            .apply { assertEquals(0, size) }
    }
}

