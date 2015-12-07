package org.tendiwa.derasterization.polygons

import org.tendiwa.derasterization.point
import org.tendiwa.geometry.points.Point
import org.tendiwa.geometry.polygons.Polygon
import org.tendiwa.geometry.segments.Segment
import org.tendiwa.geometry.segments.slope
import org.tendiwa.graphs.neighbors.neighborsOf
import org.tendiwa.graphs.trails.trail
import org.tendiwa.graphs.vertices
import org.tendiwa.grid.algorithms.buffers.inwardBuffer
import org.tendiwa.grid.algorithms.buffers.kingBuffer
import org.tendiwa.grid.algorithms.connectivity.connectivityComponents
import org.tendiwa.grid.masks.BoundedGridMask
import org.tendiwa.grid.masks.boundedBy
import org.tendiwa.grid.masks.difference
import org.tendiwa.grid.metrics.GridMetric.TAXICAB
import org.tendiwa.grid.tiles.Tile
import org.tendiwa.grid.tiles.neighbors
import org.tendiwa.math.doubles.isInteger
import org.tendiwa.plane.directions.CardinalDirection.E
import org.tendiwa.plane.directions.CardinalDirection.S
import org.tendiwa.plane.geometry.graphs.Graph2D
import org.tendiwa.plane.geometry.graphs.algorithms.minimumCycleBasis.minimumCycleBasis
import org.tendiwa.plane.geometry.graphs.constructors.Graph2D

val BoundedGridMask.derasterized: Set<Polygon>
    get() =
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
        .withConsecutiveEdgesCollapsed
        .minimumCycleBasis
        .minimalCycles

private fun segmentsBetweenNeighbors(tiles: Iterable<Tile>): List<Segment> =
    tiles
        .map { Pair(it, it.point) }
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

private val Graph2D.withConsecutiveEdgesCollapsed: Graph2D
    get() {
        val remainingVertices = vertices
            .filter { !hasCollapseableEdges(it) }
            .toSet()
        return vertices
            .first()
            .let { anyVertex ->
                trail(
                    start = anyVertex,
                    past = neighborsOf(anyVertex).first()
                )
            }
            .map { it.payload }
            .filter { it in remainingVertices }
            .run { Graph2D(Polygon(this)) }
    }

private fun Graph2D.hasCollapseableEdges(vertex: Point): Boolean =
    edgesOf(vertex).map { it.slope }.distinct().size == 1

