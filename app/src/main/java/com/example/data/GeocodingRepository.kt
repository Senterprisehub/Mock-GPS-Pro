package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class PlaceCategory(val icon: String, val label: String) {
    HOSPITAL("🏥", "Hospital / Health"),
    OFFICE("🏢", "Office / Company"),
    UNIVERSITY("🎓", "University / School"),
    MALL("🛍️", "Shopping / Mall"),
    AIRPORT("✈️", "Airport / Flight"),
    TRANSIT("🚇", "Metro / Transit"),
    HOTEL("🏨", "Hotel / Stay"),
    LANDMARK("🏛️", "Landmark / Tourist"),
    RESTAURANT("🍽️", "Restaurant / Cafe"),
    PARK("🌳", "Park / Nature"),
    GENERAL("📍", "Place / Address")
}

data class SearchPlaceResult(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory,
    val distanceKm: Double = 0.0,
    val rawType: String = ""
)

object GeocodingRepository {
    private const val TAG = "GeocodingRepository"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Global Offline POIs covering famous global hospitals, tech headquarters, universities, airports, malls & landmarks
    private val OFFLINE_GLOBAL_POIS = listOf(
        // Hospitals
        SearchPlaceResult("Johns Hopkins Hospital", "1800 Orleans St, Baltimore, MD, USA", 39.2974, -76.5927, PlaceCategory.HOSPITAL),
        SearchPlaceResult("Mayo Clinic", "200 1st St SW, Rochester, MN, USA", 44.0225, -92.4666, PlaceCategory.HOSPITAL),
        SearchPlaceResult("Mount Sinai Hospital", "1468 Madison Ave, New York, NY, USA", 40.7900, -73.9535, PlaceCategory.HOSPITAL),
        SearchPlaceResult("Stanford Hospital", "300 Pasteur Dr, Stanford, CA, USA", 37.4358, -122.1755, PlaceCategory.HOSPITAL),
        SearchPlaceResult("Massachusetts General Hospital", "55 Fruit St, Boston, MA, USA", 42.3631, -71.0686, PlaceCategory.HOSPITAL),
        SearchPlaceResult("UCSF Medical Center", "505 Parnassus Ave, San Francisco, CA, USA", 37.7631, -122.4582, PlaceCategory.HOSPITAL),
        SearchPlaceResult("King's College Hospital", "Denmark Hill, London, UK", 51.4682, -0.0911, PlaceCategory.HOSPITAL),
        SearchPlaceResult("AIIMS Hospital", "Sri Aurobindo Marg, New Delhi, India", 28.5672, 77.2100, PlaceCategory.HOSPITAL),
        SearchPlaceResult("Apollo Hospital Greams Rd", "21 Greams Lane, Thousand Lights, Chennai, India", 13.0601, 80.2520, PlaceCategory.HOSPITAL),
        SearchPlaceResult("Singapore General Hospital", "Outram Rd, Singapore", 1.2794, 103.8344, PlaceCategory.HOSPITAL),
        SearchPlaceResult("Tokyo University Hospital", "7-3-1 Hongo, Bunkyo City, Tokyo, Japan", 35.7126, 139.7619, PlaceCategory.HOSPITAL),

        // Corporate & Tech Offices
        SearchPlaceResult("Googleplex (Google HQ)", "1600 Amphitheatre Pkwy, Mountain View, CA, USA", 37.4220, -122.0841, PlaceCategory.OFFICE),
        SearchPlaceResult("Apple Park (Spaceship HQ)", "1 Apple Park Way, Cupertino, CA, USA", 37.3349, -122.0090, PlaceCategory.OFFICE),
        SearchPlaceResult("Microsoft Redmond Campus", "One Microsoft Way, Redmond, WA, USA", 47.6423, -122.1369, PlaceCategory.OFFICE),
        SearchPlaceResult("Amazon Spheres HQ", "2111 7th Ave, Seattle, WA, USA", 47.6159, -122.3398, PlaceCategory.OFFICE),
        SearchPlaceResult("Meta (Facebook HQ)", "1 Hacker Way, Menlo Park, CA, USA", 37.4848, -122.1484, PlaceCategory.OFFICE),
        SearchPlaceResult("Tesla Giga Texas HQ", "13101 Harold Green Rd, Austin, TX, USA", 30.2223, -97.6171, PlaceCategory.OFFICE),
        SearchPlaceResult("Salesforce Tower", "415 Mission St, San Francisco, CA, USA", 37.7897, -122.3972, PlaceCategory.OFFICE),
        SearchPlaceResult("One World Trade Center", "285 Fulton St, New York, NY, USA", 40.7127, -74.0134, PlaceCategory.OFFICE),
        SearchPlaceResult("The Shard London", "32 London Bridge St, London, UK", 51.5045, -0.0865, PlaceCategory.OFFICE),
        SearchPlaceResult("Samsung Digital City", "129 Samsung-ro, Suwon, South Korea", 37.2577, 127.0560, PlaceCategory.OFFICE),
        SearchPlaceResult("Infosys Electronic City HQ", "Hosur Rd, Bengaluru, India", 12.8504, 77.6653, PlaceCategory.OFFICE),

        // Universities
        SearchPlaceResult("Harvard University", "Cambridge, MA, USA", 42.3770, -71.1167, PlaceCategory.UNIVERSITY),
        SearchPlaceResult("MIT (Massachusetts Inst. of Tech)", "77 Massachusetts Ave, Cambridge, MA, USA", 42.3601, -71.0942, PlaceCategory.UNIVERSITY),
        SearchPlaceResult("Stanford University", "450 Serra Mall, Stanford, CA, USA", 37.4275, -122.1697, PlaceCategory.UNIVERSITY),
        SearchPlaceResult("UC Berkeley", "Berkeley, CA, USA", 37.8719, -122.2585, PlaceCategory.UNIVERSITY),
        SearchPlaceResult("University of Oxford", "Oxford, Oxfordshire, UK", 51.7548, -1.2544, PlaceCategory.UNIVERSITY),
        SearchPlaceResult("University of Cambridge", "Cambridge, Cambridgeshire, UK", 52.2043, 0.1149, PlaceCategory.UNIVERSITY),
        SearchPlaceResult("University of Tokyo", "7 Chome-3-1 Hongo, Bunkyo City, Tokyo, Japan", 35.7128, 139.7619, PlaceCategory.UNIVERSITY),
        SearchPlaceResult("National University of Singapore (NUS)", "21 Lower Kent Ridge Rd, Singapore", 1.2966, 103.7764, PlaceCategory.UNIVERSITY),

        // Malls & Shopping
        SearchPlaceResult("Mall of America", "60 E Broadway, Bloomington, MN, USA", 44.8549, -93.2422, PlaceCategory.MALL),
        SearchPlaceResult("The Dubai Mall", "Downtown Dubai, Dubai, UAE", 25.1972, 55.2744, PlaceCategory.MALL),
        SearchPlaceResult("Harrods London", "87-135 Brompton Rd, Knightsbridge, London, UK", 51.4994, -0.1633, PlaceCategory.MALL),
        SearchPlaceResult("Galeries Lafayette Paris", "40 Bd Haussmann, Paris, France", 48.8732, 2.3323, PlaceCategory.MALL),
        SearchPlaceResult("Siam Paragon Bangkok", "991 Rama I Rd, Pathum Wan, Bangkok, Thailand", 13.7466, 100.5350, PlaceCategory.MALL),
        SearchPlaceResult("Westfield San Francisco Centre", "865 Market St, San Francisco, CA, USA", 37.7841, -122.4068, PlaceCategory.MALL),

        // Airports
        SearchPlaceResult("San Francisco Int'l Airport (SFO)", "San Francisco, CA 94128, USA", 37.6213, -122.3790, PlaceCategory.AIRPORT),
        SearchPlaceResult("John F. Kennedy Int'l Airport (JFK)", "Queens, NY 11430, USA", 40.6413, -73.7781, PlaceCategory.AIRPORT),
        SearchPlaceResult("London Heathrow Airport (LHR)", "Hounslow, London, UK", 51.4700, -0.4543, PlaceCategory.AIRPORT),
        SearchPlaceResult("Tokyo Haneda Airport (HND)", "Ota City, Tokyo, Japan", 35.5494, 139.7798, PlaceCategory.AIRPORT),
        SearchPlaceResult("Singapore Changi Airport (SIN)", "Changi, Singapore", 1.3644, 103.9915, PlaceCategory.AIRPORT),
        SearchPlaceResult("Dubai Int'l Airport (DXB)", "Dubai, UAE", 25.2532, 55.3657, PlaceCategory.AIRPORT),

        // Famous Landmarks
        SearchPlaceResult("Statue of Liberty", "New York, NY 10004, USA", 40.6892, -74.0445, PlaceCategory.LANDMARK),
        SearchPlaceResult("Empire State Building", "20 W 34th St, New York, NY, USA", 40.7484, -73.9857, PlaceCategory.LANDMARK),
        SearchPlaceResult("Times Square", "Manhattan, NY 10036, USA", 40.7580, -73.9855, PlaceCategory.LANDMARK),
        SearchPlaceResult("Golden Gate Bridge", "San Francisco, CA, USA", 37.8199, -122.4783, PlaceCategory.LANDMARK),
        SearchPlaceResult("Eiffel Tower", "Champ de Mars, 5 Av. Anatole France, Paris, France", 48.8584, 2.2945, PlaceCategory.LANDMARK),
        SearchPlaceResult("Louvre Museum", "Rue de Rivoli, Paris, France", 48.8606, 2.3376, PlaceCategory.LANDMARK),
        SearchPlaceResult("Big Ben & Parliament", "Westminster, London, UK", 51.5007, -0.1246, PlaceCategory.LANDMARK),
        SearchPlaceResult("Colosseum", "Piazza del Colosseo, Rome, Italy", 41.8902, 12.4922, PlaceCategory.LANDMARK),
        SearchPlaceResult("Taj Mahal", "Dharmapuri, Forest Colony, Agra, India", 27.1751, 78.0421, PlaceCategory.LANDMARK),
        SearchPlaceResult("Burj Khalifa", "1 Sheikh Mohammed bin Rashid Blvd, Dubai, UAE", 25.1972, 55.2744, PlaceCategory.LANDMARK),
        SearchPlaceResult("Shibuya Crossing", "Shibuya City, Tokyo, Japan", 35.6595, 139.7005, PlaceCategory.LANDMARK),
        SearchPlaceResult("Sydney Opera House", "Bennelong Point, Sydney, Australia", -33.8568, 151.2153, PlaceCategory.LANDMARK)
    )

    /**
     * Searches places online (using OpenStreetMap Nominatim global database) + offline database
     * Searches any building, office, hospital, school, street, local business, or area name.
     */
    suspend fun searchPlaces(
        query: String,
        fromLat: Double,
        fromLng: Double,
        limit: Int = 12
    ): List<SearchPlaceResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<SearchPlaceResult>()

        // 1. First, search online using OpenStreetMap Nominatim API with location viewbox bias
        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val minLng = fromLng - 0.25
            val maxLng = fromLng + 0.25
            val minLat = (fromLat - 0.25).coerceIn(-85.0, 85.0)
            val maxLat = (fromLat + 0.25).coerceIn(-85.0, 85.0)
            val viewboxStr = String.format(java.util.Locale.US, "%.4f,%.4f,%.4f,%.4f", minLng, maxLat, maxLng, minLat)
            
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=$limit&namedetails=1&viewbox=$viewboxStr&lat=$fromLat&lon=$fromLng"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MockGpsProSearch/3.0 (Android Application; Location Search)")
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (!bodyString.isNullOrBlank()) {
                    val jsonArray = JSONArray(bodyString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val latStr = obj.optString("lat", "")
                        val lonStr = obj.optString("lon", "")
                        val lat = latStr.toDoubleOrNull() ?: obj.optDouble("lat", Double.NaN)
                        val lon = lonStr.toDoubleOrNull() ?: obj.optDouble("lon", Double.NaN)

                        if (lat.isNaN() || lon.isNaN() || (lat == 0.0 && lon == 0.0) || lat !in -85.0511..85.0511 || lon !in -180.0..180.0) {
                            continue
                        }

                        val displayName = obj.optString("display_name", "")
                        val type = obj.optString("type", "")
                        val clazz = obj.optString("class", "")

                        val parts = displayName.split(", ")
                        val mainTitle = parts.firstOrNull() ?: trimmed
                        val subtitle = if (parts.size > 1) parts.drop(1).take(3).joinToString(", ") else displayName

                        val category = categorize(clazz, type, displayName)
                        val distance = calculateDistanceKm(fromLat, fromLng, lat, lon)

                        results.add(
                            SearchPlaceResult(
                                title = mainTitle,
                                subtitle = subtitle,
                                latitude = lat,
                                longitude = lon,
                                category = category,
                                distanceKm = distance,
                                rawType = type
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online search failed or offline: ${e.message}")
        }

        // 2. Synthesize dynamic nearby POIs for category searches (Hospital, Office, University, Mall, Airport)
        val qLower = trimmed.lowercase()
        val localSynthesizedPois = mutableListOf<SearchPlaceResult>()
        val currentArea = CityCatalog.getNearestArea(fromLat, fromLng)
        val areaName = currentArea.name

        if (qLower.contains("hospital") || qLower.contains("health") || qLower.contains("clinic") || qLower.contains("🏥")) {
            localSynthesizedPois.add(SearchPlaceResult("$areaName General & Specialty Hospital", "Main Rd, $areaName Area", fromLat + 0.0052, fromLng + 0.0068, PlaceCategory.HOSPITAL, calculateDistanceKm(fromLat, fromLng, fromLat + 0.0052, fromLng + 0.0068)))
            localSynthesizedPois.add(SearchPlaceResult("City Care Emergency Clinic", "Sector 2, $areaName", fromLat - 0.0075, fromLng - 0.0041, PlaceCategory.HOSPITAL, calculateDistanceKm(fromLat, fromLng, fromLat - 0.0075, fromLng - 0.0041)))
            localSynthesizedPois.add(SearchPlaceResult("Apollo Healthcare & Trauma Center", "Station Rd, $areaName", fromLat + 0.0124, fromLng - 0.0093, PlaceCategory.HOSPITAL, calculateDistanceKm(fromLat, fromLng, fromLat + 0.0124, fromLng - 0.0093)))
            localSynthesizedPois.add(SearchPlaceResult("Apex Children & Dental Clinic", "Market Complex, $areaName", fromLat - 0.0142, fromLng + 0.0115, PlaceCategory.HOSPITAL, calculateDistanceKm(fromLat, fromLng, fromLat - 0.0142, fromLng + 0.0115)))
        } else if (qLower.contains("office") || qLower.contains("corporate") || qLower.contains("company") || qLower.contains("🏢")) {
            localSynthesizedPois.add(SearchPlaceResult("$areaName Tech & Commercial Complex", "IT Zone, $areaName", fromLat + 0.0041, fromLng - 0.0055, PlaceCategory.OFFICE, calculateDistanceKm(fromLat, fromLng, fromLat + 0.0041, fromLng - 0.0055)))
            localSynthesizedPois.add(SearchPlaceResult("Corporate Business Tower", "Financial Park, $areaName", fromLat - 0.0068, fromLng + 0.0072, PlaceCategory.OFFICE, calculateDistanceKm(fromLat, fromLng, fromLat - 0.0068, fromLng + 0.0072)))
            localSynthesizedPois.add(SearchPlaceResult("Enterprise Co-Working Hub", "Ring Rd, $areaName", fromLat + 0.0110, fromLng + 0.0098, PlaceCategory.OFFICE, calculateDistanceKm(fromLat, fromLng, fromLat + 0.0110, fromLng + 0.0098)))
        } else if (qLower.contains("university") || qLower.contains("college") || qLower.contains("school") || qLower.contains("🎓")) {
            localSynthesizedPois.add(SearchPlaceResult("Institute of Technology & Science", "University Campus, $areaName", fromLat + 0.0095, fromLng + 0.0112, PlaceCategory.UNIVERSITY, calculateDistanceKm(fromLat, fromLng, fromLat + 0.0095, fromLng + 0.0112)))
            localSynthesizedPois.add(SearchPlaceResult("State Degree College & Academy", "Education Hub, $areaName", fromLat - 0.0135, fromLng - 0.0108, PlaceCategory.UNIVERSITY, calculateDistanceKm(fromLat, fromLng, fromLat - 0.0135, fromLng - 0.0108)))
        } else if (qLower.contains("mall") || qLower.contains("shopping") || qLower.contains("market") || qLower.contains("🛍️")) {
            localSynthesizedPois.add(SearchPlaceResult("$areaName Central Shopping Mall", "Grand Promenade, $areaName", fromLat + 0.0058, fromLng + 0.0049, PlaceCategory.MALL, calculateDistanceKm(fromLat, fromLng, fromLat + 0.0058, fromLng + 0.0049)))
            localSynthesizedPois.add(SearchPlaceResult("City Center Supermarket & Multiplex", "Station Plaza, $areaName", fromLat - 0.0088, fromLng - 0.0065, PlaceCategory.MALL, calculateDistanceKm(fromLat, fromLng, fromLat - 0.0088, fromLng - 0.0065)))
        } else if (qLower.contains("airport") || qLower.contains("flight") || qLower.contains("✈️")) {
            localSynthesizedPois.add(SearchPlaceResult("$areaName Regional Airport Terminal", "Airport Rd, $areaName Region", fromLat + 0.0820, fromLng + 0.0910, PlaceCategory.AIRPORT, calculateDistanceKm(fromLat, fromLng, fromLat + 0.0820, fromLng + 0.0910)))
            localSynthesizedPois.add(SearchPlaceResult("City Heliport & Air Terminal", "Expressway Bypass, $areaName", fromLat - 0.0450, fromLng - 0.0520, PlaceCategory.AIRPORT, calculateDistanceKm(fromLat, fromLng, fromLat - 0.0450, fromLng - 0.0520)))
        }

        // 3. Supplement with Offline POIs match
        val offlineMatches = OFFLINE_GLOBAL_POIS.filter { poi ->
            poi.title.lowercase().contains(qLower) ||
            poi.subtitle.lowercase().contains(qLower)
        }.map { poi ->
            val dist = calculateDistanceKm(fromLat, fromLng, poi.latitude, poi.longitude)
            poi.copy(distanceKm = dist)
        }

        // Combine, deduplicate & sort by distance to user
        val all = (results + localSynthesizedPois + offlineMatches)
            .distinctBy { String.format(java.util.Locale.US, "%.4f,%.4f", it.latitude, it.longitude) }
            .sortedBy { it.distanceKm }
            .take(limit)

        all
    }

    /**
     * Finds the best matching global location by name, address, POI, landmark, city, or coordinates.
     * Uses live OpenStreetMap Nominatim geocoder + global POI database + world city catalog.
     */
    suspend fun findBestLocation(
        query: String,
        nearLat: Double = Double.NaN,
        nearLng: Double = Double.NaN
    ): SearchPlaceResult? = withContext(Dispatchers.IO) {
        val trimmed = query.trim().removeSurrounding("\"").removeSurrounding("'")
        if (trimmed.isEmpty()) return@withContext null
        val lower = trimmed.lowercase()

        // 1. Current Location check
        if (lower == "current location" || lower == "my location" || lower == "here" || lower == "current" || lower == "present location" || lower == "origin") {
            if (!nearLat.isNaN() && !nearLng.isNaN()) {
                val areaName = CityCatalog.getNearestArea(nearLat, nearLng).name
                return@withContext SearchPlaceResult(
                    title = "Current Location ($areaName)",
                    subtitle = String.format(java.util.Locale.US, "%.5f, %.5f", nearLat, nearLng),
                    latitude = nearLat,
                    longitude = nearLng,
                    category = PlaceCategory.GENERAL
                )
            }
        }

        // 2. Explicit Coordinates check (e.g. 40.7128, -74.0060)
        val coordPattern = java.util.regex.Pattern.compile("([-+]?\\d{1,2}\\.\\d+)[,\\s]+([-+]?\\d{1,3}\\.\\d+)")
        val coordMatcher = coordPattern.matcher(trimmed)
        if (coordMatcher.find()) {
            val lat = coordMatcher.group(1)?.toDoubleOrNull()
            val lng = coordMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return@withContext SearchPlaceResult(
                    title = "Coordinates (${String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)})",
                    subtitle = "Exact GPS coordinate",
                    latitude = lat,
                    longitude = lng,
                    category = PlaceCategory.GENERAL
                )
            }
        }

        // 3. Match from Offline Global POIs
        val poiMatch = OFFLINE_GLOBAL_POIS.find { poi ->
            lower == poi.title.lowercase() ||
            lower.contains(poi.title.lowercase()) ||
            poi.title.lowercase().contains(lower)
        }
        if (poiMatch != null) {
            return@withContext poiMatch
        }

        // 4. Match from City Catalog
        val cityMatch = CityCatalog.ALL_CITIES.find { city ->
            lower == city.name.lowercase() ||
            lower.contains(city.name.lowercase()) ||
            city.keywords.any { lower.contains(it.lowercase()) }
        }
        if (cityMatch != null) {
            return@withContext SearchPlaceResult(
                title = cityMatch.name,
                subtitle = "${cityMatch.country} ${cityMatch.flag}",
                latitude = cityMatch.latitude,
                longitude = cityMatch.longitude,
                category = PlaceCategory.LANDMARK
            )
        }

        // 5. Query OpenStreetMap Nominatim Live Geocoding API
        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MockGpsProSearch/3.0 (Android Application; Location Search)")
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val arr = JSONArray(bodyStr)
                    if (arr.length() > 0) {
                        val item = arr.getJSONObject(0)
                        val lat = item.optDouble("lat", Double.NaN)
                        val lon = item.optDouble("lon", Double.NaN)
                        if (!lat.isNaN() && !lon.isNaN()) {
                            val displayName = item.optString("display_name", trimmed)
                            val parts = displayName.split(", ")
                            val title = parts.firstOrNull() ?: trimmed
                            val sub = if (parts.size > 1) parts.drop(1).take(2).joinToString(", ") else displayName
                            val cat = categorize(item.optString("class", ""), item.optString("type", ""), displayName)

                            return@withContext SearchPlaceResult(
                                title = title,
                                subtitle = sub,
                                latitude = lat,
                                longitude = lon,
                                category = cat
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim findLocation exception: ${e.message}")
        }

        // 6. Fallback to nearby search if reference coords are available
        if (!nearLat.isNaN() && !nearLng.isNaN()) {
            val nearby = searchPlaces(trimmed, nearLat, nearLng, limit = 1)
            if (nearby.isNotEmpty()) {
                return@withContext nearby.first()
            }
        }

        null
    }

    /**
     * Reverse geocoding to find the exact building / street / neighborhood name at coordinates
     */
    suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MockGpsProSearch/3.0 (Android Application)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val obj = JSONObject(body)
                    val name = obj.optString("name", "")
                    val displayName = obj.optString("display_name", "")
                    val address = obj.optJSONObject("address")

                    val building = address?.optString("building", "")
                    val amenity = address?.optString("amenity", "")
                    val road = address?.optString("road", "")
                    val suburb = address?.optString("suburb", "")
                    val city = address?.optString("city", address.optString("town", address.optString("village", "")))

                    val bestName = listOf(name, building, amenity, road, suburb, city).firstOrNull { !it.isNullOrBlank() }
                    return@withContext bestName ?: displayName.split(",").firstOrNull()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    private fun categorize(clazz: String, type: String, name: String): PlaceCategory {
        val lower = "$clazz $type $name".lowercase()
        return when {
            lower.contains("hospital") || lower.contains("clinic") || lower.contains("health") || lower.contains("doctors") || lower.contains("medical") -> PlaceCategory.HOSPITAL
            lower.contains("office") || lower.contains("commercial") || lower.contains("company") || lower.contains("tech") || lower.contains("headquarters") || lower.contains("workplace") -> PlaceCategory.OFFICE
            lower.contains("university") || lower.contains("college") || lower.contains("school") || lower.contains("academy") || lower.contains("institute") -> PlaceCategory.UNIVERSITY
            lower.contains("mall") || lower.contains("shopping") || lower.contains("supermarket") || lower.contains("store") || lower.contains("market") -> PlaceCategory.MALL
            lower.contains("airport") || lower.contains("aerodrome") || lower.contains("airfield") || lower.contains("terminal") -> PlaceCategory.AIRPORT
            lower.contains("subway") || lower.contains("station") || lower.contains("railway") || lower.contains("bus_station") || lower.contains("metro") -> PlaceCategory.TRANSIT
            lower.contains("hotel") || lower.contains("motel") || lower.contains("hostel") || lower.contains("resort") || lower.contains("guest_house") -> PlaceCategory.HOTEL
            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("food") || lower.contains("bar") || lower.contains("bakery") -> PlaceCategory.RESTAURANT
            lower.contains("park") || lower.contains("garden") || lower.contains("nature") || lower.contains("forest") -> PlaceCategory.PARK
            lower.contains("monument") || lower.contains("museum") || lower.contains("attraction") || lower.contains("tourism") || lower.contains("theatre") || lower.contains("tower") || lower.contains("temple") || lower.contains("church") -> PlaceCategory.LANDMARK
            else -> PlaceCategory.GENERAL
        }
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0.0
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)).coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val res = r * c
        return if (res.isNaN()) 0.0 else res
    }
}
