package org.tendiwa.derasterization.polygons

import org.tendiwa.collections.untilDepleted
import org.tendiwa.derasterization.toPoint
import org.tendiwa.graphs.trails.trail
import org.tendiwa.graphs.vertices
import org.tendiwa.math.doubles.isInteger
import org.tendiwa.plane.directions.CardinalDirection.E
import org.tendiwa.plane.directions.CardinalDirection.S
import org.tendiwa.plane.geometry.graphs.Graph2D
import org.tendiwa.plane.geometry.graphs.constructors.Graph2D
import org.tendiwa.plane.geometry.graphs.cycles.Cycle2D
import org.tendiwa.plane.geometry.graphs.cycles.toPolygon
import org.tendiwa.plane.geometry.graphs.toDisconnectedCycles
import org.tendiwa.plane.geometry.holeygons.Holeygon
import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.polygons.Polygon
import org.tendiwa.plane.geometry.polygons.contains
import org.tendiwa.plane.geometry.segments.Segment
import org.tendiwa.plane.geometry.segments.slope
import org.tendiwa.plane.grid.algorithms.buffers.inwardBuffer
import org.tendiwa.plane.grid.algorithms.buffers.kingBuffer
import org.tendiwa.plane.grid.algorithms.connectivity.connectivityComponents
import org.tendiwa.plane.grid.masks.BoundedGridMask
import org.tendiwa.plane.grid.masks.boundedBy
import org.tendiwa.plane.grid.masks.difference
import org.tendiwa.plane.grid.metrics.GridMetric.TAXICAB
import org.tendiwa.plane.grid.tiles.Tile
import org.tendiwa.plane.grid.tiles.neighbors
import java.util.*

fun BoundedGridMask.derasterized(): Collection<Holeygon> =
    connectivityComponents(metric = TAXICAB)
        .map { component ->
            component
                .difference(component.inwardBuffer(1))
                .kingBuffer(1)
                .boundedBy(component.hull)
        }
        .flatMap { it.tiles }
        .run(::segmentsBetweenNeighbors)
        .run(::Graph2D)
        .collapseConsecutiveEdges()
        .toDisconnectedCycles()
        .map(Cycle2D::toPolygon)
        .let { polygons ->
            object {
                val holeygons =
                    polygons.map {
                        holeygon(enclosing = it, candidates = polygons)
                    }

                val notHoles =
                    holeygons
                        .flatMap { it.holes }
                        .toSet()
                        .let { holes ->
                            holeygons.filter { !holes.contains(it.enclosing) }
                        }
            }
        }
        .notHoles

private fun holeygon(
    enclosing: Polygon,
    candidates: Collection<Polygon>
): Holeygon =
    Holeygon(
        enclosing,
        candidates
            .filter { enclosing != it && enclosing.contains(it.points.first()) }
    )

private fun segmentsBetweenNeighbors(tiles: Iterable<Tile>): List<Segment> =
    tiles
        .map { Pair(it, it.toPoint()) }
        .apply { assert(all { it.second.x.isInteger || it.second.y.isInteger }) }
        .toMap()
        .let { tilesToPoints ->
            tilesToPoints.keys
                .flatMap { tile ->
                    tile.neighbors(S, E)
                        .tiles
                        .filter { tilesToPoints.containsKey(it) }
                        .map { neighbor -> Pair(tile, neighbor) }
                }
                .map {
                    Segment(
                        tilesToPoints[it.first]!!,
                        tilesToPoints[it.second]!!
                    )
                }
        }

private fun Graph2D.collapseConsecutiveEdges(): Graph2D {
    val remainingVertices = vertices
        .filter { !hasCollapseableEdges(it) }
        .toSet()
    val connectivityComponents = LinkedHashSet<Polygon>()
    remainingVertices.untilDepleted {
        trail(start = it)
            .map { it.payload }
            .filter { it in remainingVertices }
            .apply { assert(true) }
            .apply { forEach { markUsed(it) } }
            .apply { connectivityComponents.add(Polygon(this)) }
    }
    return Graph2D(connectivityComponents)
}

private fun Graph2D.hasCollapseableEdges(vertex: Point): Boolean =
    edgesOf(vertex).map { it.slope }.distinct().size == 1
