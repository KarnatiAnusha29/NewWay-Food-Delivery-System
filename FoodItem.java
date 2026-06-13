package model;

import java.math.BigDecimal;

public class FoodItem {
    private int        id;
    private int        restaurantId;
    private String     restaurantName;
    private String     name;
    private String     description;
    private BigDecimal price;
    private double     rating;
    private String     category;
    private String     imagePath;
    private boolean    available;

    public FoodItem() {}

    public int        getId()                     { return id; }
    public void       setId(int v)                { this.id = v; }
    public int        getRestaurantId()           { return restaurantId; }
    public void       setRestaurantId(int v)      { this.restaurantId = v; }
    public String     getRestaurantName()         { return restaurantName; }
    public void       setRestaurantName(String v) { this.restaurantName = v; }
    public String     getName()                   { return name; }
    public void       setName(String v)           { this.name = v; }
    public String     getDescription()            { return description; }
    public void       setDescription(String v)    { this.description = v; }
    public BigDecimal getPrice()                  { return price; }
    public void       setPrice(BigDecimal v)      { this.price = v; }
    public double     getRating()                 { return rating; }
    public void       setRating(double v)         { this.rating = v; }
    public String     getCategory()               { return category; }
    public void       setCategory(String v)       { this.category = v; }
    public String     getImagePath()              { return imagePath; }
    public void       setImagePath(String v)      { this.imagePath = v; }
    public boolean    isAvailable()               { return available; }
    public void       setAvailable(boolean v)     { this.available = v; }
}
