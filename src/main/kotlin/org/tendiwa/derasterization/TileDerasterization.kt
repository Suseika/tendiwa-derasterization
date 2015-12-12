package org.tendiwa.derasterization

import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.vectors.Vector
import org.tendiwa.plane.grid.tiles.Tile

val Tile.point: Point
    get() = Point(x.toDouble(), y.toDouble())

val Tile.radiusVector: Vector
    get() = Vector(x.toDouble(), y.toDouble())

