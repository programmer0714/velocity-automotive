package com.rhsoft.velocityautomotive.model;

import com.google.gson.annotations.SerializedName;

public class Car {

    @SerializedName("id")
    private int id;

    @SerializedName("brand")
    private String brand;

    @SerializedName("model")
    private String model;

    @SerializedName("location")
    private String location;

    @SerializedName("engine")
    private String engine;

    @SerializedName("price")
    private String price;

    @SerializedName("horsepower")
    private String horsepower;

    @SerializedName("acceleration")
    private String acceleration;

    @SerializedName("top_speed")
    private String topSpeed;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("category")
    private String category;

    @SerializedName("year")
    private int year;

    // =====================
    // GETTERS
    // =====================
    public int    getId()           { return id; }
    public String getBrand()        { return brand; }
    public String getModel()        { return model; }
    public String getLocation()     { return location; }
    public String getEngine()       { return engine; }
    public String getPrice()        { return price; }
    public String getHorsepower()   { return horsepower; }
    public String getAcceleration() { return acceleration; }
    public String getTopSpeed()     { return topSpeed; }
    public String getImageUrl()     { return imageUrl; }
    public String getCategory()     { return category; }
    public int    getYear()         { return year; }

    // =====================
    // SETTERS
    // =====================
    public void setId(int id)                   { this.id = id; }
    public void setBrand(String brand)          { this.brand = brand; }
    public void setModel(String model)          { this.model = model; }
    public void setLocation(String location)    { this.location = location; }
    public void setEngine(String engine)        { this.engine = engine; }
    public void setPrice(String price)          { this.price = price; }
    public void setHorsepower(String hp)        { this.horsepower = hp; }
    public void setAcceleration(String acc)     { this.acceleration = acc; }
    public void setTopSpeed(String topSpeed)    { this.topSpeed = topSpeed; }
    public void setImageUrl(String imageUrl)    { this.imageUrl = imageUrl; }
    public void setCategory(String category)    { this.category = category; }
    public void setYear(int year)               { this.year = year; }
}