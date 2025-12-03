package com.dlightplanner.services;

import com.dlightplanner.models.Hotel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hotel Service - loads hotels from JSON file and randomly assigns them to destinations
 */
public class HotelService {
    private List<Hotel> hotels;
    private AirportService airportService;
    private Random random;

    private static final Map<String, List<String>> CITY_LOCATIONS = new HashMap<>();
    
    static {
        CITY_LOCATIONS.put("Delhi", Arrays.asList("Connaught Place", "Old Delhi", "Aerocity", "Gurgaon", "Paharganj", "New Delhi", "Karol Bagh", "Dwarka", "Rohini", "Saket"));
        CITY_LOCATIONS.put("Mumbai", Arrays.asList("Marine Drive", "Bandra", "Andheri", "Colaba", "CST Area", "Juhu", "Powai", "Worli", "Lower Parel", "BKC"));
        CITY_LOCATIONS.put("Bengaluru", Arrays.asList("Residency Road", "MG Road", "Whitefield", "UB City", "Majestic", "Indiranagar", "Koramangala", "Electronic City", "Marathahalli", "HSR Layout"));
        CITY_LOCATIONS.put("Hyderabad", Arrays.asList("Banjara Hills", "Hitech City", "Old City", "Jubilee Hills", "Secunderabad", "Gachibowli", "Kondapur", "Madhapur", "Begumpet", "Abids"));
        CITY_LOCATIONS.put("Chennai", Arrays.asList("Marina Beach", "OMR", "T Nagar", "Anna Nagar", "Adyar", "Velachery", "Porur", "Guindy", "Nungambakkam", "Egmore"));
        CITY_LOCATIONS.put("Kolkata", Arrays.asList("Park Street", "Salt Lake", "Howrah", "New Town", "Ballygunge", "Alipore", "Dum Dum", "Rajarhat", "Behala", "Esplanade"));
        CITY_LOCATIONS.put("Jaipur", Arrays.asList("C-Scheme", "Old City", "Station Road", "Malviya Nagar", "Vaishali Nagar", "Bani Park", "Raja Park", "Sodala", "Mansarovar", "Pink City"));
        CITY_LOCATIONS.put("Goa", Arrays.asList("Baga Beach", "Calangute", "Anjuna", "Candolim", "Vagator", "Panaji", "Margao", "Mapusa", "Colva", "Palolem"));
        CITY_LOCATIONS.put("Kochi", Arrays.asList("Fort Kochi", "Alleppey", "MG Road", "Maradu", "Ernakulam", "Mattancherry", "Willingdon Island", "Kakkanad", "Edapally", "Vytilla"));
        CITY_LOCATIONS.put("Udaipur", Arrays.asList("Lake Pichola", "City Palace Road", "Station Area", "Fateh Sagar", "Saheliyon ki Bari", "Gulab Bagh", "Hathipole", "Shobhagpura", "Sukhadia Circle", "Bapu Bazaar"));
        CITY_LOCATIONS.put("Manali", Arrays.asList("Old Manali", "Mall Road", "Naggar Road", "Solang Valley", "Vashisht", "Hadimba", "Kullu", "Rohtang", "Manu Temple", "Club House"));
        CITY_LOCATIONS.put("Andaman", Arrays.asList("Havelock Island", "Neil Island", "Port Blair", "Radhanagar", "Elephant Beach", "Ross Island", "Cellular Jail", "Wandoor", "Baratang", "Diglipur"));
        CITY_LOCATIONS.put("Rishikesh", Arrays.asList("Laxman Jhula", "Tapovan", "Ram Jhula", "Triveni Ghat", "Neelkanth", "Beatles Ashram", "Parmarth Niketan", "Swarg Ashram", "Muni Ki Reti", "Kaudiyala"));
        CITY_LOCATIONS.put("Shimla", Arrays.asList("Mall Road", "Ridge", "Lower Bazaar", "Jakhoo", "Kufri", "Chadwick Falls", "Summer Hill", "Tara Devi", "Naldehra", "Chail"));
        CITY_LOCATIONS.put("Darjeeling", Arrays.asList("Mall Road", "Observatory Hill", "Station Road", "Tiger Hill", "Batasia Loop", "Happy Valley", "Ghoom", "Lebong", "Jorebungalow", "Singamari"));
        CITY_LOCATIONS.put("Pune", Arrays.asList("Koregaon Park", "Viman Nagar", "FC Road", "Hinjewadi", "Baner", "Kothrud", "Aundh", "Wakad", "Hadapsar", "Magarpatta"));
        CITY_LOCATIONS.put("Mysuru", Arrays.asList("Palace Road", "Jayalakshmipuram", "Railway Station", "Nazarbad", "Vijayanagar", "Gokulam", "Kuvempunagar", "Saraswathipuram", "Yadavagiri", "Devaraja Market"));
        CITY_LOCATIONS.put("Assam", Arrays.asList("Dispur", "Paltan Bazaar", "Jorhat", "Dibrugarh", "Tezpur", "Silchar", "Nagaon", "Tinsukia", "Bongaigaon", "Sivasagar"));
        CITY_LOCATIONS.put("Agra", Arrays.asList("Taj Ganj", "Fatehabad Road", "Station Road", "Sadar Bazaar", "Kamla Nagar", "Shahganj", "Rambagh", "Dayal Bagh", "Khandari", "Sanjay Place"));
        CITY_LOCATIONS.put("Varanasi", Arrays.asList("Assi Ghat", "Dashashwamedh Ghat", "Cantonment", "Godowlia", "Lanka", "Bhelupur", "Sigra", "Cantt", "Mahmoorganj", "Sarnath"));
        CITY_LOCATIONS.put("Amritsar", Arrays.asList("Golden Temple Road", "Ranjit Avenue", "Railway Station", "Hall Bazaar", "Lawrence Road", "Mall Road", "Cantonment", "Ranjit Avenue", "Guru Nanak Dev University", "Batala Road"));
        CITY_LOCATIONS.put("Lucknow", Arrays.asList("Hazratganj", "Gomti Nagar", "Charbagh", "Alambagh", "Aminabad", "Indira Nagar", "Mahanagar", "Vikas Nagar", "Sitapur Road", "Nirala Nagar"));
        CITY_LOCATIONS.put("Ahmedabad", Arrays.asList("SG Highway", "Prahlad Nagar", "Satellite", "Navrangpura", "Maninagar", "Vastrapur", "Bodakdev", "Thaltej", "Gurukul", "Paldi"));
    }

    public HotelService() {
        hotels = new ArrayList<>();
        random = new Random();
    }
    
    public void setAirportService(AirportService airportService) {
        this.airportService = airportService;
    }

    /**
     * Load hotels from JSON file
     * @param jsonPath path to the hotels.json file
     * @return List of hotels
     */
    public List<Hotel> loadHotelsFromJson(String jsonPath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(jsonPath)) {
            Type hotelListType = new TypeToken<List<Hotel>>() {}.getType();
            hotels = gson.fromJson(reader, hotelListType);
            if (hotels == null) {
                hotels = new ArrayList<>();
            }
            return hotels;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Hotel> getHotels() {
        return hotels;
    }

    /**
     * Get city name from destination code
     */
    private String getCityNameFromCode(String destinationCode) {
        if (airportService == null) {
            return destinationCode;
        }
        return airportService.getAirports().stream()
                .filter(airport -> airport.getIata().equalsIgnoreCase(destinationCode))
                .map(airport -> airport.getCity())
                .findFirst()
                .orElse(destinationCode);
    }

    /**
     * Customize hotel for a specific city
     */
    private Hotel customizeHotelForCity(Hotel hotel, String cityName, String destinationCode) {
        Hotel customized = new Hotel();
        customized.setId(hotel.getId());


        boolean addCityPrefix = random.nextDouble() < 0.3; // 30% chance
        
        if (addCityPrefix) {

            customized.setName(cityName + " " + hotel.getName());
        } else {

            customized.setName(hotel.getName());
        }

        List<String> cityLocations = CITY_LOCATIONS.getOrDefault(cityName, 
            Arrays.asList("City Center", "Downtown", "Business District", "Residential Area", "Commercial Area"));
        String location = cityLocations.get(random.nextInt(cityLocations.size()));
        customized.setLocation(location);

        customized.setStarRating(hotel.getStarRating());
        customized.setImagePath(hotel.getImagePath());
        customized.setPricePerNight(hotel.getPricePerNight());
        customized.setDistanceFromCityCenter(hotel.getDistanceFromCityCenter());
        customized.setAmenities(new ArrayList<>(hotel.getAmenities()));
        customized.setDestinationCode(destinationCode); // Set to actual destination code
        
        return customized;
    }

    /**
     * Get hotels filtered by destination code (airport code)
     * Randomly selects 10-15 hotels from the pool and customizes them for the destination
     */
    public List<Hotel> getHotelsByDestination(String destinationCode) {
        return getHotelsByDestination(destinationCode, null, null);
    }
    
    /**
     * Get hotels filtered by destination code with specific dates (dates ignored for JSON)
     * Randomly selects 10-15 hotels from the pool and customizes them for the destination
     */
    public List<Hotel> getHotelsByDestination(String destinationCode, LocalDate checkIn, LocalDate checkOut) {
        if (hotels == null || hotels.isEmpty()) {
            return new ArrayList<>();
        }

        String cityName = getCityNameFromCode(destinationCode);

        int numHotels = 10 + random.nextInt(6); // 10 to 15 hotels
        numHotels = Math.min(numHotels, hotels.size());

        List<Hotel> shuffledHotels = new ArrayList<>(hotels);
        Collections.shuffle(shuffledHotels, random);

        List<Hotel> selectedHotels = shuffledHotels.stream()
                .limit(numHotels)
                .map(hotel -> customizeHotelForCity(hotel, cityName, destinationCode))
                .collect(Collectors.toList());

        selectedHotels.sort((a, b) -> Integer.compare(b.getStarRating(), a.getStarRating()));
        
        return selectedHotels;
    }

    /**
     * Get hotel by ID
     */
    public Hotel getHotelById(int id) {
        return hotels.stream()
                .filter(hotel -> hotel.getId() == id)
                .findFirst()
                .orElse(null);
    }
}

