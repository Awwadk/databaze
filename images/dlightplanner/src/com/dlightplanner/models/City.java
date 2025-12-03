package com.dlightplanner.models;

import java.util.List;

public class City {

    private int id;
    private String name;
    private String imagePath;

    private String overview;
    private String attractions;
    private String activities;
    private String dining;
    private String shopping;

    public City() {}

    public City(int id, String name, String imagePath, String overview,
                String attractions, String activities, String dining, String shopping) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.overview = overview;
        this.attractions = attractions;
        this.activities = activities;
        this.dining = dining;
        this.shopping = shopping;
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public String getImagePath() { return imagePath; }


    public String getOverview() { return overview; }

    public String getAttractions() { return attractions; }

    public String getActivities() { return activities; }

    public String getDining() { return dining; }

    public String getShopping() { return shopping; }

    public void setId(int id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public void setOverview(String overview) { this.overview = overview; }

    public void setAttractions(String attractions) { this.attractions = attractions; }

    public void setActivities(String activities) { this.activities = activities; }

    public void setDining(String dining) { this.dining = dining; }

    public void setShopping(String shopping) { this.shopping = shopping; }
}
