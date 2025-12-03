package com.dlightplanner.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to load local food and travel costs per city from JSON
 */
public class LocalCostService {
    private Map<String, Double> localFoodCosts;
    private Map<String, Double> localTravelCosts;
    
    public LocalCostService() {
        localFoodCosts = new HashMap<>();
        localTravelCosts = new HashMap<>();
    }
    
    /**
     * Load local costs from JSON file
     * @param jsonPath path to the local_costs.json file
     */
    public void loadLocalCostsFromJson(String jsonPath) {
        try (FileReader reader = new FileReader(jsonPath)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            if (jsonObject.has("localFoodCosts")) {
                JsonObject foodCosts = jsonObject.getAsJsonObject("localFoodCosts");
                for (String key : foodCosts.keySet()) {
                    localFoodCosts.put(key, foodCosts.get(key).getAsDouble());
                }
            }

            if (jsonObject.has("localTravelCosts")) {
                JsonObject travelCosts = jsonObject.getAsJsonObject("localTravelCosts");
                for (String key : travelCosts.keySet()) {
                    localTravelCosts.put(key, travelCosts.get(key).getAsDouble());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load local costs from JSON: " + e.getMessage());
        }
    }
    
    /**
     * Get local food cost per person for a city (airport code)
     * @param cityCode airport code (e.g., "DEL", "BOM")
     * @return food cost per person, or 0 if not found
     */
    public double getLocalFoodCost(String cityCode) {
        return localFoodCosts.getOrDefault(cityCode.toUpperCase(), 0.0);
    }
    
    /**
     * Get local travel cost for a city (airport code)
     * @param cityCode airport code (e.g., "DEL", "BOM")
     * @return travel cost, or 0 if not found
     */
    public double getLocalTravelCost(String cityCode) {
        return localTravelCosts.getOrDefault(cityCode.toUpperCase(), 0.0);
    }
}

