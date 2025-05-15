package com.ufomap.ufosightingmap.utils

import kotlin.math.*
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

/**
 * Utility class for geographic calculations and operations.
 * Contains methods for distance calculations, coordinate validation,
 * bounding box operations, and other geo-related utilities.
 */
object GeoUtils {
    // Earth radius in kilometers
    private const val EARTH_RADIUS_KM = 6371.0

    // Earth radius in miles
    private const val EARTH_RADIUS_MILES = 3958.8

    // Minimum and maximum coordinate bounds
    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0

    /**
     * Calculate the distance between two points using the Haversine formula.
     * This is the "great-circle" distance - the shortest distance over the earth's surface.
     *
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @param inKilometers If true, return distance in kilometers; if false, in miles
     * @return Distance between the points in kilometers or miles
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        inKilometers: Boolean = true
    ): Double {
        // Convert to radians
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = sin(dLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Calculate distance
        return if (inKilometers) {
            EARTH_RADIUS_KM * c
        } else {
            EARTH_RADIUS_MILES * c
        }
    }

    /**
     * Checks if a pair of coordinates is valid (within standard lat/long ranges)
     *
     * @param latitude Latitude to validate
     * @param longitude Longitude to validate
     * @return True if coordinates are valid, false otherwise
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        return latitude.isFinite() && longitude.isFinite() &&
                latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE &&
                longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE
    }

    /**
     * Creates a bounding box around a central point with a given radius.
     *
     * @param centerLat Center latitude in degrees
     * @param centerLon Center longitude in degrees
     * @param radiusKm Radius in kilometers
     * @return BoundingBox object that encompasses the circular region
     */
    fun createBoundingBoxFromRadius(centerLat: Double, centerLon: Double, radiusKm: Double): BoundingBox {
        // Rough approximation: 1 degree of latitude is about 111 km
        val latDelta = radiusKm / 111.0

        // Longitude degrees per km varies by latitude
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(centerLat)))

        // Create bounding box
        return BoundingBox(
            centerLat + latDelta, // North
            centerLon + lonDelta, // East
            centerLat - latDelta, // South
            centerLon - lonDelta  // West
        )
    }

    /**
     * Formats coordinates into a readable string.
     *
     * @param latitude Latitude to format
     * @param longitude Longitude to format
     * @param decimalPlaces Number of decimal places to include
     * @return Formatted coordinate string
     */
    fun formatCoordinates(latitude: Double, longitude: Double, decimalPlaces: Int = 6): String {
        return String.format(
            "%.${decimalPlaces}f° %s, %.${decimalPlaces}f° %s",
            abs(latitude),
            if (latitude >= 0) "N" else "S",
            abs(longitude),
            if (longitude >= 0) "E" else "W"
        )
    }

    /**
     * Calculates the center point of multiple coordinates.
     *
     * @param coordinates List of Pair<Double, Double> representing lat/long points
     * @return Pair with the center latitude and longitude, or null if the list is empty
     */
    fun calculateCenterPoint(coordinates: List<Pair<Double, Double>>): Pair<Double, Double>? {
        if (coordinates.isEmpty()) return null

        // Filter out invalid coordinates
        val validCoordinates = coordinates.filter { (lat, lon) ->
            isValidCoordinate(lat, lon)
        }

        if (validCoordinates.isEmpty()) return null

        // Calculate sum of coordinates
        var sumLat = 0.0
        var sumLon = 0.0

        validCoordinates.forEach { (lat, lon) ->
            sumLat += lat
            sumLon += lon
        }

        // Return average
        return Pair(
            sumLat / validCoordinates.size,
            sumLon / validCoordinates.size
        )
    }

    /**
     * Determines if a point is inside a bounding box.
     *
     * @param lat Latitude of the point
     * @param lon Longitude of the point
     * @param boundingBox BoundingBox to check against
     * @return True if the point is inside the bounding box, false otherwise
     */
    fun isPointInBoundingBox(lat: Double, lon: Double, boundingBox: BoundingBox): Boolean {
        return lat >= boundingBox.latSouth &&
                lat <= boundingBox.latNorth &&
                lon >= boundingBox.lonWest &&
                lon <= boundingBox.lonEast
    }

    /**
     * Converts coordinates from degrees to a standard format.
     * Example: 40.7128° N, 74.0060° W becomes "40°42'46.1"N 74°00'21.6"W"
     *
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return Coordinates in degrees-minutes-seconds format
     */
    fun convertToDMS(latitude: Double, longitude: Double): String {
        // Format latitude
        val latDegrees = abs(latitude.toInt())
        val latMinutes = ((abs(latitude) - latDegrees) * 60).toInt()
        val latSeconds = ((abs(latitude) - latDegrees - latMinutes / 60.0) * 3600)
        val latDirection = if (latitude >= 0) "N" else "S"

        // Format longitude
        val lonDegrees = abs(longitude.toInt())
        val lonMinutes = ((abs(longitude) - lonDegrees) * 60).toInt()
        val lonSeconds = ((abs(longitude) - lonDegrees - lonMinutes / 60.0) * 3600)
        val lonDirection = if (longitude >= 0) "E" else "W"

        return String.format(
            "%d°%02d'%05.2f\"%s %d°%02d'%05.2f\"%s",
            latDegrees, latMinutes, latSeconds, latDirection,
            lonDegrees, lonMinutes, lonSeconds, lonDirection
        )
    }

    /**
     * Groups coordinates into clusters based on proximity.
     *
     * @param coordinates List of coordinate pairs (latitude, longitude)
     * @param radiusKm Maximum distance between points in a cluster (in kilometers)
     * @return List of clusters, where each cluster is a list of coordinate pairs
     */
    fun clusterCoordinates(
        coordinates: List<Pair<Double, Double>>,
        radiusKm: Double
    ): List<List<Pair<Double, Double>>> {
        if (coordinates.isEmpty()) return emptyList()

        // Filter valid coordinates
        val validCoordinates = coordinates.filter { (lat, lon) ->
            isValidCoordinate(lat, lon)
        }

        val clusters = mutableListOf<MutableList<Pair<Double, Double>>>()
        val processed = mutableSetOf<Pair<Double, Double>>()

        for (coordinate in validCoordinates) {
            if (coordinate in processed) continue

            // Create a new cluster with this coordinate
            val cluster = mutableListOf(coordinate)
            processed.add(coordinate)

            // Find nearby coordinates
            for (other in validCoordinates) {
                if (other in processed) continue

                val distance = calculateDistance(
                    coordinate.first, coordinate.second,
                    other.first, other.second
                )

                if (distance <= radiusKm) {
                    cluster.add(other)
                    processed.add(other)
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }

    /**
     * Extends a BoundingBox to include a specific point.
     *
     * @param bbox Existing BoundingBox
     * @param lat Latitude of the point to include
     * @param lon Longitude of the point to include
     * @return New BoundingBox that includes the original area plus the new point
     */
    fun extendBoundingBox(bbox: BoundingBox, lat: Double, lon: Double): BoundingBox {
        return BoundingBox(
            maxOf(bbox.latNorth, lat),
            maxOf(bbox.lonEast, lon),
            minOf(bbox.latSouth, lat),
            minOf(bbox.lonWest, lon)
        )
    }

    /**
     * Calculates a bounding box that encompasses all provided points.
     *
     * @param points List of coordinate pairs (latitude, longitude)
     * @return BoundingBox containing all points, or null if no valid points provided
     */
    fun createBoundingBoxFromPoints(points: List<Pair<Double, Double>>): BoundingBox? {
        val validPoints = points.filter { (lat, lon) -> isValidCoordinate(lat, lon) }
        if (validPoints.isEmpty()) return null

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        validPoints.forEach { (lat, lon) ->
            minLat = minOf(minLat, lat)
            maxLat = maxOf(maxLat, lat)
            minLon = minOf(minLon, lon)
            maxLon = maxOf(maxLon, lon)
        }

        return BoundingBox(maxLat, maxLon, minLat, minLon)
    }

    /**
     * Converts GeoPoint list to list of coordinate pairs.
     */
    fun geoPointsToCoordinatePairs(geoPoints: List<GeoPoint>): List<Pair<Double, Double>> {
        return geoPoints.map { Pair(it.latitude, it.longitude) }
    }

    /**
     * Converts coordinate pairs to GeoPoint list.
     */
    fun coordinatePairsToGeoPoints(coordinates: List<Pair<Double, Double>>): List<GeoPoint> {
        return coordinates.map { GeoPoint(it.first, it.second) }
    }
}