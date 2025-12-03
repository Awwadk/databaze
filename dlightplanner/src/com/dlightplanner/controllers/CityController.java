package com.dlightplanner.controllers;

import com.dlightplanner.models.City;
import com.dlightplanner.services.CityService;
import com.dlightplanner.repository.CityRepository;
import java.util.List;

public class CityController {

    private CityService cityService;
    private CityRepository cityRepository;

    public CityController() {
        cityService = new CityService();
        cityRepository = new CityRepository();
    }

    public void loadCities(String jsonPath) {
        List<City> cityList = cityService.loadCitiesFromJson(jsonPath);

        if (cityList != null) {
            for (City c : cityList) {
                cityRepository.addCity(c);
            }
        }
    }

    public List<City> getAllCities() {
        return cityRepository.getAllCities();
    }

    public City getCityById(int id) {
        return cityRepository.getCityById(id);
    }
}
