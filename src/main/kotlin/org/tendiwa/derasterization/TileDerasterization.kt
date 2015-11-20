package org.tendiwa.derasterization

import org.tendiwa.geometry.points.Point
import org.tendiwa.geometry.vectors.Vector
import org.tendiwa.grid.tiles.Tile

val Tile.point: Point
    get() = Point(x.toDouble(), y.toDouble())

val Tile.radiusVector: Vector
    get() = Vector(x.toDouble(), y.toDouble())

