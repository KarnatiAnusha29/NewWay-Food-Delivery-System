package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {

    public enum Status {
        Pending,
        Assigned,
        OutForDelivery,
        Delivered,
        Cancelled
    }

    private int           id;
    private int           userId;
    private String        userName;
    private Integer       riderId;
    private String        riderName;
    private BigDecimal    totalPrice;
    private String        deliveryAddress;
    private double        distanceKm;
    private double        trafficFactor;
    private int           weatherCode;
    private Integer       predictedTimeMin;
    private Integer       actualTimeMin;
    private Status        status;
    private LocalDateTime placedAt;
    private LocalDateTime deliveredAt;
    private String        specialNotes;

    public Order() {}

    public int           getId()                    { return id; }
    public void          setId(int v)               { this.id = v; }
    public int           getUserId()                { return userId; }
    public void          setUserId(int v)           { this.userId = v; }
    public String        getUserName()              { return userName; }
    public void          setUserName(String v)      { this.userName = v; }
    public Integer       getRiderId()               { return riderId; }
    public void          setRiderId(Integer v)      { this.riderId = v; }
    public String        getRiderName()             { return riderName; }
    public void          setRiderName(String v)     { this.riderName = v; }
    public BigDecimal    getTotalPrice()            { return totalPrice; }
    public void          setTotalPrice(BigDecimal v){ this.totalPrice = v; }
    public String        getDeliveryAddress()       { return deliveryAddress; }
    public void          setDeliveryAddress(String v){ this.deliveryAddress = v; }
    public double        getDistanceKm()            { return distanceKm; }
    public void          setDistanceKm(double v)    { this.distanceKm = v; }
    public double        getTrafficFactor()         { return trafficFactor; }
    public void          setTrafficFactor(double v) { this.trafficFactor = v; }
    public int           getWeatherCode()           { return weatherCode; }
    public void          setWeatherCode(int v)      { this.weatherCode = v; }
    public Integer       getPredictedTimeMin()      { return predictedTimeMin; }
    public void          setPredictedTimeMin(Integer v){ this.predictedTimeMin = v; }
    public Integer       getActualTimeMin()         { return actualTimeMin; }
    public void          setActualTimeMin(Integer v){ this.actualTimeMin = v; }
    public Status        getStatus()                { return status; }
    public void          setStatus(Status v)        { this.status = v; }
    public LocalDateTime getPlacedAt()              { return placedAt; }
    public void          setPlacedAt(LocalDateTime v){ this.placedAt = v; }
    public LocalDateTime getDeliveredAt()           { return deliveredAt; }
    public void          setDeliveredAt(LocalDateTime v){ this.deliveredAt = v; }
    public String        getSpecialNotes()          { return specialNotes; }
    public void          setSpecialNotes(String v)  { this.specialNotes = v; }

    public boolean isActive() {
        return status == Status.Pending
            || status == Status.Assigned
            || status == Status.OutForDelivery;
    }
}
