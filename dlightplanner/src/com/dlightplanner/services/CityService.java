package com.dlightplanner.services;

import com.dlightplanner.models.City;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CityService {

    private List<City> cities;

    public CityService() {
        cities = new ArrayList<>();
    }

    /**
     * Load cities from a JSON file
     * @param jsonPath path to the cities.json file
     */
    public List<City> loadCitiesFromJson(String jsonPath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(jsonPath)) {
            Type cityListType = new TypeToken<List<City>>() {}.getType();
            cities = gson.fromJson(reader, cityListType);
            return cities;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to load cities from JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get all loaded cities
     * @return list of City objects
     */
    public List<City> getCities() {
        return cities;
    }

    /**
     * Get a city by its ID
     * @param cityId city ID
     * @return City object or null if not found
     */
    public City getCityById(int cityId) {
        for (City city : cities) {
            if (city.getId() == cityId) {
                return city;
            }
        }
        return null;
    }
}
