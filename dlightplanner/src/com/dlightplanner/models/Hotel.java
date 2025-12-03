package com.dlightplanner.models;

import java.util.List;

public class Hotel {
    private int id;
    private String name;
    private String destinationCode; // Airport code (DEL, BOM, etc.)
    private int starRating; // 3, 4, or 5
    private String location;
    private String imagePath;
    private double pricePerNight; // base price for 2-bed room
    private double distanceFromCityCenter; // in km
    private List<String> amenities; // list of amenities

    public Hotel() {}

    public Hotel(int id, String name, String destinationCode, int starRating, String location, 
                 String imagePath, double pricePerNight, double distanceFromCityCenter, List<String> amenities) {
        this.id = id;
        this.name = name;
        this.destinationCode = destinationCode;
        this.starRating = starRating;
        this.location = location;
        this.imagePath = imagePath;
        this.pricePerNight = pricePerNight;
        this.distanceFromCityCenter = distanceFromCityCenter;
        this.amenities = amenities;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDestinationCode() { return destinationCode; }
    public int getStarRating() { return starRating; }
    public String getLocation() { return location; }
    public String getImagePath() { return imagePath; }
    public double getPricePerNight() { return pricePerNight; }
    public double getDistanceFromCityCenter() { return distanceFromCityCenter; }
    public List<String> getAmenities() { return amenities; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }
    public void setStarRating(int starRating) { this.starRating = starRating; }
    public void setLocation(String location) { this.location = location; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
    public void setDistanceFromCityCenter(double distanceFromCityCenter) { this.distanceFromCityCenter = distanceFromCityCenter; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    /**
     * Calculate price for different bed configurations
     * @param beds number of beds (2, 3, or 4)
     * @return price per night for that configuration
     */
    public double getPriceForBeds(int beds) {
        double multiplier = 1.0;
        switch (beds) {
            case 2: multiplier = 1.0; break;
            case 3: multiplier = 1.4; break; // 40% more for 3 beds
            case 4: multiplier = 1.8; break; // 80% more for 4 beds
        }
        return pricePerNight * multiplier;
    }
}

