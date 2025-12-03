package com.dlightplanner.services;

import com.dlightplanner.models.TouristSpot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tourist Spot Service - loads tourist spots from JSON file
 */
public class TouristSpotService {
    private List<TouristSpot> touristSpots;
    
    public TouristSpotService() {
        touristSpots = new ArrayList<>();
    }
    
    /**
     * Load tourist spots from JSON file
     * @param jsonPath path to the tourist_spots.json file
     * @return List of tourist spots
     */
    public List<TouristSpot> loadTouristSpotsFromJson(String jsonPath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(jsonPath)) {
            Type spotListType = new TypeToken<List<TouristSpot>>() {}.getType();
            touristSpots = gson.fromJson(reader, spotListType);
            if (touristSpots == null) {
                touristSpots = new ArrayList<>();
            }
            return touristSpots;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<TouristSpot> getTouristSpots() {
        return touristSpots;
    }
    
    /**
     * Get tourist spots filtered by destination code (airport code)
     */
    public List<TouristSpot> getTouristSpotsByDestination(String destinationCode) {
        return touristSpots.stream()
                .filter(spot -> spot.getDestinationCode() != null && 
                               spot.getDestinationCode().equalsIgnoreCase(destinationCode))
                .collect(Collectors.toList());
    }
    
    /**
     * Get tourist spot by ID
     */
    public TouristSpot getTouristSpotById(int id) {
        return touristSpots.stream()
                .filter(spot -> spot.getId() == id)
                .findFirst()
                .orElse(null);
    }
}

