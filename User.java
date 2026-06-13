package model;

import java.time.LocalDateTime;

public class User {
    private int    id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String phone;
    private String address;
    private String location;
    private String role;
    private LocalDateTime createdAt;

    public User() {}

    public User(int id, String fullName, String email, String role, String location) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.location = location;
    }

    public int    getId()                    { return id; }
    public void   setId(int id)              { this.id = id; }
    public String getFullName()              { return fullName; }
    public void   setFullName(String v)      { this.fullName = v; }
    public String getEmail()                 { return email; }
    public void   setEmail(String v)         { this.email = v; }
    public String getPasswordHash()          { return passwordHash; }
    public void   setPasswordHash(String v)  { this.passwordHash = v; }
    public String getPhone()                 { return phone; }
    public void   setPhone(String v)         { this.phone = v; }
    public String getAddress()               { return address; }
    public void   setAddress(String v)       { this.address = v; }
    public String getLocation()              { return location; }
    public void   setLocation(String v)      { this.location = v; }
    public String getRole()                  { return role; }
    public void   setRole(String v)          { this.role = v; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public void   setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + fullName + "', role='" + role + "'}";
    }
}
