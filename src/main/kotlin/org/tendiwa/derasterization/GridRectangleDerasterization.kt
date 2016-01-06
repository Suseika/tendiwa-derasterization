package org.tendiwa.derasterization

import org.tendiwa.plane.geometry.rectangles.Rectangle
import org.tendiwa.plane.grid.rectangles.GridRectangle

fun GridRectangle.toRectangle(): Rectangle =
    Rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

