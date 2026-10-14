package com.rideak.tools.regionpack

import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.exploration.RegionPackCodec
import com.rideak.exploration.TrackPoint
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Sanity checks on the pack shipped in the app, replaying the debug simulated ride. */
class CergyPackTest {
    private val pack = RegionPackCodec.decode(
        File("../../shared/src/commonMain/composeResources/files/regions/cergy.region").readText(),
    )

    /** Same 600 m loop around Cergy-Préfecture as SimulatedLocationSource, one fix per second at 6 m/s. */
    private val loop = (0 until 630).map { s ->
        val angle = 6.0 * s / (2 * PI * 600) * 2 * PI
        TrackPoint(
            GeoPoint(49.0364 + 600 * sin(angle) / 111_195.0, 2.0761 + 600 * cos(angle) / (111_195.0 * cos(49.0364 * PI / 180))),
            s * 1_000L,
            horizontalAccuracyMeters = 5f,
        )
    }

    @Test
    fun packIsCergy() {
        assertEquals("Cergy", pack.region.name)
        assertTrue(pack.region.rideableCells.size in 300..600)
        assertTrue(pack.places.isNotEmpty())
        assertEquals(pack.places.size, pack.places.map { it.id }.toSet().size, "place ids are unique")
    }

    @Test
    fun simulatedRideDiscoversPlacesAndProgressesInCergy() {
        val engine = ExplorationEngine.standard()
        val paths = engine.pathsFor(listOf(loop))
        val found = PlaceDiscoverer().discovered(pack.places, paths)
        val names = pack.places.filter { it.id in found }.map { it.name }
        println("Simulated loop discovers: $names")
        assertTrue("Parc François Mitterrand" in names, "found $names")
        val progress = pack.region.progress(engine.cellsForSegments(listOf(loop)))
        assertTrue(progress.exploredRideableCells > 10, "explored ${progress.exploredRideableCells}")
    }
}
