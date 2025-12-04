package com.dlightplanner.repository;

import com.dlightplanner.models.City;
import java.util.ArrayList;
import java.util.List;

public class CityRepository {
    private List<City> cities = new ArrayList<>();

    public void addCity(City city) {
        cities.add(city);
    }

    public List<City> getAllCities() {
        return cities;
    }

    public City getCityById(int id) {
        for (City city : cities) {
            if (city.getId() == id) {
                return city;
            }
        }
        return null;
    }
}
