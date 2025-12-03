package com.dlightplanner.services;

import com.dlightplanner.models.Airport;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;

public class AirportService {
    private List<Airport> airports;

    public void loadAirportsFromJson(String path) {
        try {
            Gson gson = new Gson();
            Reader reader = new FileReader(path);
            airports = gson.fromJson(reader, new TypeToken<List<Airport>>(){}.getType());
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Airport> getAirports() {
        return airports;
    }
}
