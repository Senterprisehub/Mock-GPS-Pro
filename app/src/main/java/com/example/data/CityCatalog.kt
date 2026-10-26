package com.example.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CityLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val flag: String = "📍",
    val keywords: List<String> = emptyList()
)

data class NearbyCity(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val flag: String
)

object CityCatalog {
    val ALL_CITIES = listOf(
        // North America
        CityLocation("San Francisco", "USA", 37.7749, -122.4194, "🇺🇸", listOf("SF", "Bay Area", "California", "Silicon Valley", "Golden Gate")),
        CityLocation("San Jose", "USA", 37.3382, -121.8863, "🇺🇸", listOf("Silicon Valley", "California")),
        CityLocation("Oakland", "USA", 37.8044, -122.2712, "🇺🇸", listOf("East Bay", "California")),
        CityLocation("Los Angeles", "USA", 34.0522, -118.2437, "🇺🇸", listOf("LA", "Hollywood", "California", "Santa Monica")),
        CityLocation("San Diego", "USA", 32.7157, -117.1611, "🇺🇸", listOf("California")),
        CityLocation("Seattle", "USA", 47.6062, -122.3321, "🇺🇸", listOf("Washington", "Space Needle")),
        CityLocation("Portland", "USA", 45.5152, -122.6784, "🇺🇸", listOf("Oregon")),
        CityLocation("Las Vegas", "USA", 36.1699, -115.1398, "🇺🇸", listOf("Nevada", "The Strip", "Vegas")),
        CityLocation("Phoenix", "USA", 33.4484, -112.0740, "🇺🇸", listOf("Arizona")),
        CityLocation("Denver", "USA", 39.7392, -104.9903, "🇺🇸", listOf("Colorado")),
        CityLocation("Austin", "USA", 30.2672, -97.7431, "🇺🇸", listOf("Texas")),
        CityLocation("Dallas", "USA", 32.7767, -96.7970, "🇺🇸", listOf("Texas")),
        CityLocation("Houston", "USA", 29.7604, -95.3698, "🇺🇸", listOf("Texas")),
        CityLocation("Chicago", "USA", 41.8781, -87.6298, "🇺🇸", listOf("Illinois", "Windy City")),
        CityLocation("New York", "USA", 40.7128, -74.0060, "🇺🇸", listOf("NYC", "Manhattan", "Brooklyn", "Times Square", "Central Park")),
        CityLocation("Boston", "USA", 42.3601, -71.0589, "🇺🇸", listOf("Massachusetts")),
        CityLocation("Washington D.C.", "USA", 38.9072, -77.0369, "🇺🇸", listOf("DC", "Capitol")),
        CityLocation("Miami", "USA", 25.7617, -80.1918, "🇺🇸", listOf("Florida", "South Beach")),
        CityLocation("Atlanta", "USA", 33.7490, -84.3880, "🇺🇸", listOf("Georgia")),
        CityLocation("Honolulu", "USA", 21.3069, -157.8583, "🇺🇸", listOf("Hawaii", "Waikiki")),
        CityLocation("Vancouver", "Canada", 49.2827, -123.1207, "🇨🇦", listOf("BC")),
        CityLocation("Toronto", "Canada", 43.6532, -79.3832, "🇨🇦", listOf("Ontario", "CN Tower")),
        CityLocation("Montreal", "Canada", 45.5017, -73.5673, "🇨🇦", listOf("Quebec")),
        CityLocation("Mexico City", "Mexico", 19.4326, -99.1332, "🇲🇽", listOf("CDMX")),
        CityLocation("Cancún", "Mexico", 21.1619, -86.8515, "🇲🇽"),

        // South America
        CityLocation("São Paulo", "Brazil", -23.5505, -46.6333, "🇧🇷"),
        CityLocation("Rio de Janeiro", "Brazil", -22.9068, -43.1729, "🇧🇷", listOf("Copacabana", "Christ the Redeemer")),
        CityLocation("Buenos Aires", "Argentina", -34.6037, -58.3816, "🇦🇷"),
        CityLocation("Santiago", "Chile", -33.4489, -70.6693, "🇨🇱"),
        CityLocation("Bogota", "Colombia", 4.7110, -74.0721, "🇨🇴"),
        CityLocation("Lima", "Peru", -12.0464, -77.0428, "🇵🇪"),

        // Europe
        CityLocation("London", "UK", 51.5074, -0.1278, "🇬🇧", listOf("England", "Big Ben", "Tower Bridge", "Soho")),
        CityLocation("Manchester", "UK", 53.4808, -2.2426, "🇬🇧"),
        CityLocation("Edinburgh", "UK", 55.9533, -3.1883, "🇬🇧", listOf("Scotland")),
        CityLocation("Paris", "France", 48.8566, 2.3522, "🇫🇷", listOf("Eiffel Tower", "Louvre", "Champs Elysees")),
        CityLocation("Nice", "France", 43.7102, 7.2620, "🇫🇷", listOf("Cote d'Azur")),
        CityLocation("Berlin", "Germany", 52.5200, 13.4050, "🇩🇪", listOf("Brandenburg Gate")),
        CityLocation("Munich", "Germany", 48.1351, 11.5820, "🇩🇪", listOf("Bavaria")),
        CityLocation("Frankfurt", "Germany", 50.1109, 8.6821, "🇩🇪"),
        CityLocation("Amsterdam", "Netherlands", 52.3676, 4.9041, "🇳🇱", listOf("Holland", "Canals")),
        CityLocation("Brussels", "Belgium", 50.8503, 4.3517, "🇧🇪"),
        CityLocation("Rome", "Italy", 41.9028, 12.4964, "🇮🇹", listOf("Colosseum", "Vatican")),
        CityLocation("Milan", "Italy", 45.4642, 9.1900, "🇮🇹", listOf("Duomo")),
        CityLocation("Venice", "Italy", 45.4408, 12.3155, "🇮🇹", listOf("Gondola", "Piazza San Marco")),
        CityLocation("Madrid", "Spain", 40.4168, -3.7038, "🇪🇸"),
        CityLocation("Barcelona", "Spain", 41.3879, 2.1699, "🇪🇸", listOf("Sagrada Familia")),
        CityLocation("Lisbon", "Portugal", 38.7223, -9.1393, "🇵🇹"),
        CityLocation("Vienna", "Austria", 48.2082, 16.3738, "🇦🇹"),
        CityLocation("Zurich", "Switzerland", 47.3769, 8.5417, "🇨🇭"),
        CityLocation("Geneva", "Switzerland", 46.2044, 6.1432, "🇨🇭"),
        CityLocation("Stockholm", "Sweden", 59.3293, 18.0686, "🇸🇪"),
        CityLocation("Oslo", "Norway", 59.9139, 10.7522, "🇳🇴"),
        CityLocation("Copenhagen", "Denmark", 55.6761, 12.5683, "🇩🇰"),
        CityLocation("Helsinki", "Finland", 60.1699, 24.9384, "🇫🇮"),
        CityLocation("Warsaw", "Poland", 52.2297, 21.0122, "🇵🇱"),
        CityLocation("Prague", "Czechia", 50.0755, 14.4378, "🇨🇿"),
        CityLocation("Budapest", "Hungary", 47.4979, 19.0402, "🇭🇺"),
        CityLocation("Athens", "Greece", 37.9838, 23.7275, "🇬🇷", listOf("Acropolis")),
        CityLocation("Istanbul", "Turkey", 41.0082, 28.9784, "🇹🇷", listOf("Bosphorus")),
        CityLocation("Kyiv", "Ukraine", 50.4501, 30.5234, "🇺🇦"),

        // Asia & Middle East
        CityLocation("Tokyo", "Japan", 35.6762, 139.6503, "🇯🇵", listOf("Shinjuku", "Shibuya", "Akihabara", "Ginza")),
        CityLocation("Osaka", "Japan", 34.6937, 135.5023, "🇯🇵", listOf("Dotonbori", "Kansai")),
        CityLocation("Kyoto", "Japan", 35.0116, 135.7681, "🇯🇵", listOf("Gion", "Arashiyama")),
        CityLocation("Seoul", "South Korea", 37.5665, 126.9780, "🇰🇷", listOf("Gangnam", "Myeongdong")),
        CityLocation("Busan", "South Korea", 35.1796, 129.0756, "🇰🇷", listOf("Haeundae")),
        CityLocation("Beijing", "China", 39.9042, 116.4074, "🇨🇳", listOf("Forbidden City")),
        CityLocation("Shanghai", "China", 31.2304, 121.4737, "🇨🇳", listOf("The Bund", "Pudong")),
        CityLocation("Shenzhen", "China", 22.5431, 114.0579, "🇨🇳"),
        CityLocation("Hong Kong", "HK", 22.3193, 114.1694, "🇭🇰", listOf("Victoria Harbour", "Kowloon")),
        CityLocation("Taipei", "Taiwan", 25.0330, 121.5654, "🇹🇼", listOf("Taipei 101")),
        CityLocation("Singapore", "Singapore", 1.3521, 103.8198, "🇸🇬", listOf("Marina Bay", "Changi")),
        CityLocation("Bangkok", "Thailand", 13.7563, 100.5018, "🇹🇭", listOf("Siam")),
        CityLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, "🇲🇾", listOf("Petronas")),
        CityLocation("Jakarta", "Indonesia", -6.2088, 106.8456, "🇮🇩"),
        CityLocation("Bali (Denpasar)", "Indonesia", -8.6705, 115.2126, "🇮🇩", listOf("Kuta", "Seminyak", "Ubud")),
        CityLocation("Manila", "Philippines", 14.5995, 120.9842, "🇵🇭"),
        CityLocation("Hanoi", "Vietnam", 21.0285, 105.8542, "🇻🇳"),
        CityLocation("Ho Chi Minh City", "Vietnam", 10.8231, 106.6297, "🇻🇳", listOf("Saigon")),
        CityLocation("Mumbai", "India", 19.0760, 72.8777, "🇮🇳", listOf("Bombay", "Marine Drive", "Bandra")),
        CityLocation("Delhi / New Delhi", "India", 28.6139, 77.2090, "🇮🇳", listOf("Connaught Place", "India Gate")),
        CityLocation("Bengaluru (Bangalore)", "India", 12.9716, 77.5946, "🇮🇳", listOf("Koramangala", "Indiranagar", "MG Road")),
        CityLocation("Hyderabad", "India", 17.3850, 78.4867, "🇮🇳", listOf("Hitec City", "Charminar")),
        CityLocation("Chennai", "India", 13.0827, 80.2707, "🇮🇳", listOf("Madras", "Marina Beach")),
        CityLocation("Kolkata", "India", 22.5726, 88.3639, "🇮🇳", listOf("Calcutta", "Howrah", "Park Street")),
        CityLocation("Pune", "India", 18.5204, 73.8567, "🇮🇳", listOf("Maharashtra")),
        CityLocation("Ahmedabad", "India", 23.0225, 72.5714, "🇮🇳", listOf("Gujarat")),
        CityLocation("Jaipur", "India", 26.9124, 75.7873, "🇮🇳", listOf("Pink City", "Rajasthan")),
        CityLocation("Goa (Panaji)", "India", 15.4909, 73.8278, "🇮🇳", listOf("Calangute", "Baga Beach")),
        CityLocation("Dubai", "UAE", 25.2048, 55.2708, "🇦🇪", listOf("Burj Khalifa", "Dubai Mall", "Palm Jumeirah")),
        CityLocation("Abu Dhabi", "UAE", 24.4539, 54.3773, "🇦🇪"),
        CityLocation("Doha", "Qatar", 25.2854, 51.5310, "🇶🇦"),
        CityLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753, "🇸🇦"),
        CityLocation("Tel Aviv", "Israel", 32.0853, 34.7818, "🇮🇱"),
        CityLocation("Jerusalem", "Israel", 31.7683, 35.2137, "🇮🇱"),

        // Oceania
        CityLocation("Sydney", "Australia", -33.8688, 151.2093, "🇦🇺", listOf("Opera House", "Bondi")),
        CityLocation("Melbourne", "Australia", -37.8136, 144.9631, "🇦🇺"),
        CityLocation("Brisbane", "Australia", -27.4698, 153.0251, "🇦🇺"),
        CityLocation("Perth", "Australia", -31.9505, 115.8605, "🇦🇺"),
        CityLocation("Auckland", "New Zealand", -36.8485, 174.7633, "🇳🇿"),

        // Africa
        CityLocation("Cairo", "Egypt", 30.0444, 31.2357, "🇪🇬", listOf("Pyramids", "Giza", "Nile")),
        CityLocation("Johannesburg", "South Africa", -26.2041, 28.0473, "🇿🇦"),
        CityLocation("Cape Town", "South Africa", -33.9249, 18.4241, "🇿🇦", listOf("Table Mountain")),
        CityLocation("Nairobi", "Kenya", -1.2921, 36.8219, "🇰🇪"),
        CityLocation("Lagos", "Nigeria", 6.5244, 3.3792, "🇳🇬"),
        CityLocation("Casablanca", "Morocco", 33.5731, -7.5898, "🇲🇦")
    )

    fun getNearestArea(userLat: Double, userLng: Double): NearbyCity {
        val nearest = ALL_CITIES.minByOrNull { calculateDistanceKm(userLat, userLng, it.latitude, it.longitude) }
            ?: ALL_CITIES[0]
        val dist = calculateDistanceKm(userLat, userLng, nearest.latitude, nearest.longitude)
        return NearbyCity(
            name = nearest.name,
            country = nearest.country,
            latitude = nearest.latitude,
            longitude = nearest.longitude,
            distanceKm = dist,
            flag = nearest.flag
        )
    }

    fun searchAreas(query: String, fromLat: Double, fromLng: Double, limit: Int = 8): List<NearbyCity> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return getNearbyCities(fromLat, fromLng, limit)

        return ALL_CITIES.filter { city ->
            city.name.lowercase().contains(q) ||
            city.country.lowercase().contains(q) ||
            city.keywords.any { it.lowercase().contains(q) }
        }
        .map { city ->
            val dist = calculateDistanceKm(fromLat, fromLng, city.latitude, city.longitude)
            NearbyCity(
                name = city.name,
                country = city.country,
                latitude = city.latitude,
                longitude = city.longitude,
                distanceKm = dist,
                flag = city.flag
            )
        }
        .sortedBy { it.distanceKm }
        .take(limit)
    }

    fun getNearbyCities(userLat: Double, userLng: Double, limit: Int = 16): List<NearbyCity> {
        return ALL_CITIES.map { city ->
            val dist = calculateDistanceKm(userLat, userLng, city.latitude, city.longitude)
            NearbyCity(
                name = city.name,
                country = city.country,
                latitude = city.latitude,
                longitude = city.longitude,
                distanceKm = dist,
                flag = city.flag
            )
        }
        .sortedBy { it.distanceKm }
        .take(limit)
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0.0
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)).coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        val res = r * c
        return if (res.isNaN()) 0.0 else res
    }
}
