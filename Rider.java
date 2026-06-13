package model;

public class Rider {

    public enum Status {
        Available,
        OnDelivery,
        Offline
    }

    private int    id;
    private String fullName;
    private String phone;
    private String vehicleType;
    private String currentZone;
    private Status status;
    private int    totalDeliveries;
    private double rating;

    public Rider() {}

    public int    getId()                   { return id; }
    public void   setId(int v)              { this.id = v; }
    public String getFullName()             { return fullName; }
    public void   setFullName(String v)     { this.fullName = v; }
    public String getPhone()                { return phone; }
    public void   setPhone(String v)        { this.phone = v; }
    public String getVehicleType()          { return vehicleType; }
    public void   setVehicleType(String v)  { this.vehicleType = v; }
    public String getCurrentZone()          { return currentZone; }
    public void   setCurrentZone(String v)  { this.currentZone = v; }
    public Status getStatus()               { return status; }
    public void   setStatus(Status v)       { this.status = v; }
    public int    getTotalDeliveries()      { return totalDeliveries; }
    public void   setTotalDeliveries(int v) { this.totalDeliveries = v; }
    public double getRating()               { return rating; }
    public void   setRating(double v)       { this.rating = v; }

    public boolean isAvailable() { return status == Status.Available; }
}
