package com.dlightplanner.models;

public class TouristSpot {
    private int id;
    private String name;
    private String destinationCode; // Airport code (DEL, BOM, etc.)
    private String description;
    private String category; // e.g., "Historical", "Nature", "Adventure", "Cultural", "Religious"
    private double price; // Entry fee or activity price per person
    private String imagePath;
    private double distanceFromCityCenter; // in km
    private double estimatedDuration; // in hours
    private String location; // Specific location/address

    private String openingHours; // e.g., "9:30 AM - 4:30 PM"
    private String bestVisitingTime; // e.g., "Early Morning", "Evening", "Anytime"
    private String peakHours; // e.g., "11:00 AM - 2:00 PM"
    private String crowdLevel; // "Low", "Medium", "High", "Very High"
    private String closedDays; // e.g., "Monday" or null
    private double latitude; // For distance calculation
    private double longitude; // For distance calculation
    
    public TouristSpot() {}
    
    public TouristSpot(int id, String name, String destinationCode, String description, 
                      String category, double price, String imagePath, 
                      double distanceFromCityCenter, double estimatedDuration, String location,
                      String openingHours, String bestVisitingTime, String peakHours,
                      String crowdLevel, String closedDays, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.destinationCode = destinationCode;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imagePath = imagePath;
        this.distanceFromCityCenter = distanceFromCityCenter;
        this.estimatedDuration = estimatedDuration;
        this.location = location;
        this.openingHours = openingHours;
        this.bestVisitingTime = bestVisitingTime;
        this.peakHours = peakHours;
        this.crowdLevel = crowdLevel;
        this.closedDays = closedDays;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDestinationCode() { return destinationCode; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getImagePath() { return imagePath; }
    public double getDistanceFromCityCenter() { return distanceFromCityCenter; }
    public double getEstimatedDuration() { return estimatedDuration; }
    public String getLocation() { return location; }
    public String getOpeningHours() { return openingHours; }
    public String getBestVisitingTime() { return bestVisitingTime; }
    public String getPeakHours() { return peakHours; }
    public String getCrowdLevel() { return crowdLevel; }
    public String getClosedDays() { return closedDays; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setDistanceFromCityCenter(double distanceFromCityCenter) { this.distanceFromCityCenter = distanceFromCityCenter; }
    public void setEstimatedDuration(double estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    public void setLocation(String location) { this.location = location; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public void setBestVisitingTime(String bestVisitingTime) { this.bestVisitingTime = bestVisitingTime; }
    public void setPeakHours(String peakHours) { this.peakHours = peakHours; }
    public void setCrowdLevel(String crowdLevel) { this.crowdLevel = crowdLevel; }
    public void setClosedDays(String closedDays) { this.closedDays = closedDays; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}

