package org.tendiwa.derasterization.polygons

import org.tendiwa.derasterization.point
import org.tendiwa.geometry.points.move
import org.tendiwa.geometry.polygons.Polygon
import org.tendiwa.grid.algorithms.buffers.inwardBuffer
import org.tendiwa.grid.constructors.centeredGridRectangle
import org.tendiwa.grid.dimensions.by
import org.tendiwa.grid.masks.*
import org.tendiwa.grid.metrics.GridMetric
import org.tendiwa.grid.tiles.Tile
import org.tendiwa.grid.tiles.isNear
import org.tendiwa.grid.tiles.move
import org.tendiwa.grid.tiles.neighbors
import org.tendiwa.plane.directions.CardinalDirection
import org.tendiwa.plane.directions.CardinalDirection.*
import org.tendiwa.plane.directions.OrdinalDirection.*
import org.tendiwa.plane.geometry.graphs.MutableGraph2D
import org.tendiwa.plane.geometry.graphs.algorithms.minimumCycleBasis.MinimumCycleBasis
import org.tendiwa.plane.geometry.graphs.algorithms.minimumCycleBasis.minimumCycleBasis
import java.awt.Color
import java.util.*

fun BoundedGridMask.derasterized(seed: Tile = findFloodStart()): Set<Polygon> {
    val graph : MutableGraph2D = PolygonBorderGraph(this.polygonsBorders)
    return graph.minimumCycleBasis.minimalCycles
}

fun PolygonBorderGraph(polygonsBorders: BoundedGridMask): MutableGraph2D {
    val graph = MutableGraph2D()
    val tilesToPoints = polygonsBorders
        .tiles
        .map { Pair(it, it.point)}
        .toMap()
    tilesToPoints.values.forEach { graph.addVertex(it) }
    tilesToPoints.keys.forEach {
        if (tilesToPoints.containsKey(it.move(E))) {
            graph.addEdge(tilesToPoints[it], tilesToPoints[it.move (E)])
        }
        if (tilesToPoints.containsKey(it.move(S))) {
            graph.addEdge(tilesToPoints[it], tilesToPoints[it.move(S)])
        }
    }
    return graph
}

private val BoundedGridMask.polygonsBorders: BoundedGridMask
    get() {
        val border = this.inwardBuffer(1).boundedBy(this.hull)
        val innards = this.difference(this.inwardBuffer(1))
        val excludedTilesMask = ExcludedTilesMask(border, innards)
        val difference = border.difference(excludedTilesMask)
        return difference
    }

/**
 * Mask of tiles that should be excluded form `original` mask in order for the
 * rest of the `original` mask to contain only tiles with 2 [GridMetric.TAXICAB]
 * neighbors.
 */
private fun ExcludedTilesMask(
    border: BoundedGridMask,
    innards: BoundedGridMask
): FiniteGridMask {
    return FiniteGridMask(
        LinkedHashSet<Tile>().apply {
            val excluded3 = tilesExcludedBecauseOf3Knots(border, innards)
            addAll(excluded3)
            addAll(tilesExcludedBecauseOf4Knots(border, innards))
        }
    )
}

private fun tilesExcludedBecauseOf4Knots(
    border: BoundedGridMask,
    innards: BoundedGridMask
): Collection<Tile> =
    border.find4Knots().run {
        ArrayList<Tile>().apply {
            addAll(
                this.filter {
                    innards.run {
                        contains(it.move(NW)) && contains(it.move(SE))
                    }
                }
                    .flatMap { listOf(it.move(NW), it.move(SE)) }
            )
            addAll(
                this.filter {
                    innards.run {
                        contains(it.move(NE)) && contains(it.move(SW))
                    }
                }
                    .flatMap { listOf(it.move(NE), it.move(SW)) }
            )
        }
    }

private fun tilesExcludedBecauseOf3Knots(
    border: BoundedGridMask,
    innards: BoundedGridMask
): List<Tile> {
    return border.find3Knots().run {
        this.map { knot -> Pair(knot, this.neighborOf(knot)) }
            .filter { it.second != null }
            .map { Adjacent3KnotsPair(it.first, it.second!!) }
            .map { it.surroundingTiles }
            .map { it.difference(innards) }
            .flatMap { it.tiles }
    }
}

private data class Adjacent3KnotsPair(val knot: Tile, val neighbor: Tile) {
    init {
        assert(knot.isNear(neighbor, GridMetric.KING))
    }
}

private val Adjacent3KnotsPair.surroundingTiles: BoundedGridMask
    get() = GridMask {
        x, y ->
        Tile(x, y).run {
            this.isNear(knot, GridMetric.KING)
                || this.isNear(neighbor, GridMetric.KING)
        }
    }
        .boundedBy(centeredGridRectangle(knot, 3 by 3))

private fun Set<Tile>.neighborOf(knot: Tile): Tile? =
    if (contains(knot.move (1, 0))) {
        knot.move(1, 0)
    } else if (contains(knot.move(0, 1))) {
        knot.move(0, 1)
    } else {
        null
    }


private fun BoundedGridMask.find3Knots(): Set<Tile> =
    this.tiles.filter { this.numberOfNeighbors(it) > 2 }.toHashSet()

private fun BoundedGridMask.find4Knots(): Set<Tile> =
    this.tiles.filter { this.numberOfNeighbors(it) == 4 }.toHashSet()

private fun GridMask.numberOfNeighbors(it: Tile): Int {
    var number = 0
    it.neighbors(GridMetric.TAXICAB)
        .forEachTile { x, y ->
            if (this.contains (x, y)) {
                number++
            }
        }
    assert(number <= 4)
    return number
}

private fun findFloodStart(): Tile {
    throw UnsupportedOperationException();
}
