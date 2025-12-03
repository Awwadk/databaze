package com.dlightplanner.models;

public class Airport {
    private int id;
    private String city;
    private String airportName;
    private String iata;

    public Airport() {}

    public Airport(int id, String city, String airportName, String iata) {
        this.id = id;
        this.city = city;
        this.airportName = airportName;
        this.iata = iata;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAirportName() { return airportName; }
    public void setAirportName(String airportName) { this.airportName = airportName; }
    public String getIata() { return iata; }
    public void setIata(String iata) { this.iata = iata; }
}
