# NewWay — Intelligent Food Delivery Time Prediction System
## Architectural Blueprint & Developer Guide

---

## 1. Project Overview

NewWay is a desktop Java Swing application that provides an end-to-end food-delivery
management system with an embedded Linear Regression engine for predicting delivery
times. It targets two actor types — customers and admins — separated by a role-based
login flow.

---

## 2. Technology Stack

| Layer        | Technology                                |
|--------------|-------------------------------------------|
| GUI          | Java Swing (JFrame, JPanel, CardLayout)   |
| Language     | Java 17+ (records, text blocks, var)      |
| Database     | MySQL 8.x via XAMPP / phpMyAdmin          |
| Connectivity | JDBC — mysql-connector-j-8.x.jar          |
| ML Engine    | Pure Java Linear Regression               |
| Build        | (optional) Maven / Gradle                 |

---

## 3. Package Structure

```
src/
├── db/
│   ├── DBConnection.java       ← Singleton JDBC connection
│   ├── OrderDAO.java           ← Orders + rider assignment logic
│   ├── FoodItemDAO.java        ← Marketplace queries
│   └── RiderDAO.java           ← Rider CRUD
│
├── model/
│   ├── User.java
│   ├── FoodItem.java
│   ├── Order.java
│   └── Rider.java
│
├── engine/
│   └── PredictionEngine.java   ← Linear Regression + SGD update
│
├── ui/
│   ├── MainFrame.java          ← CardLayout shell
│   ├── auth/
│   │   └── LoginPanel.java
│   ├── user/
│   │   └── UserDashboard.java  ← Marketplace, Cart, Orders, Tracking
│   └── admin/
│       └── AdminDashboard.java ← Orders table, Rider management, Stats
│
└── util/
    ├── ImageLoader.java        ← Scaled ImageIcon helper
    └── PasswordUtils.java      ← BCrypt wrapper
```

---

## 4. CardLayout Navigation Flow

```
MainFrame (JFrame)
│
└── cardPanel (CardLayout)
    ├── "LOGIN"  →  LoginPanel
    │                  │
    │               onLoginSuccess(user)
    │                  │
    │          ┌───────┴────────┐
    │          │                │
    │      role=admin        role=user
    │          │                │
    ├── "ADMIN" →  AdminDashboard
    │               ├── Tab: Active Orders
    │               ├── Tab: Rider Management
    │               └── Tab: Statistics
    │
    └── "USER"  →  UserDashboard (inner CardLayout)
                    ├── "MARKET"   →  MarketplacePanel
                    ├── "CART"     →  CartPanel
                    ├── "ORDERS"   →  OrdersPanel
                    └── "TRACKING" →  TrackingPanel
```

---

## 5. Database Schema (Tables & Relationships)

```
users ──────────────── orders ──────────────── riders
  id (PK)                id (PK)                 id (PK)
  email                  user_id (FK→users)       full_name
  password_hash          rider_id (FK→riders)     phone
  role                   status                   vehicle_type
  location               distance_km              current_zone
                         traffic_factor           status
                         weather_code             total_deliveries
                         predicted_time_min
                         actual_time_min

orders ──────────── order_items ──────────── food_items
  id (PK)               order_id (FK)           id (PK)
                         food_item_id (FK)        restaurant_id (FK)
                         quantity                 name, price, rating
                         unit_price               image_path

food_items ─── restaurants
  restaurant_id (FK)    id (PK)
                         name, location
                         cuisine_type, rating

users ──────────── cart
  id (FK)               user_id (FK)
                         food_item_id (FK)
                         quantity
```

---

## 6. Prediction Engine

### Formula
```
T = β₀ + β₁·d + β₂·(τ−1) + β₃·w

Where:
  T   = predicted delivery time (minutes)
  d   = distance in km
  τ   = traffic factor  (1.0 = clear → 3.0 = gridlock)
  w   = weather code    (0 = clear, 1 = rain, 2 = storm)
  β₀  = 8.0   (base: prep + handoff time)
  β₁  = 4.5   (minutes per km)
  β₂  = 6.0   (penalty per unit of extra traffic)
  β₃  = 5.0   (penalty per weather severity level)
```

### Example Predictions
| Scenario              | d    | τ   | w | T     |
|-----------------------|------|-----|---|-------|
| Nearby, clear         | 1.0  | 1.0 | 0 | 12.5  |
| 5km, light rain       | 5.0  | 1.5 | 1 | 39.5  |
| 10km, gridlock, storm | 10.0 | 3.0 | 2 | 70.0  |

### Online Learning (Optional)
After every delivered order, call:
```java
PredictionEngine.getInstance().updateModel(input, actualTime, 0.001);
```
This runs one SGD step to nudge the coefficients toward real observations.

---

## 7. Rider Assignment — Conflict Check Logic

The `OrderDAO.assignRider(orderId, riderId)` method uses a **serializable transaction**:

```
BEGIN TRANSACTION
  1. SELECT ... FOR UPDATE on orders WHERE id=orderId
     → Fail if status ≠ 'Pending'
  2. SELECT ... FOR UPDATE on riders WHERE id=riderId
     → Fail if status ≠ 'Available'
  3. COUNT active orders for this rider
     → Fail if count > 0
  4. UPDATE orders SET rider_id=?, status='Assigned'
  5. UPDATE riders SET status='On Delivery'
COMMIT
  → On any failure: ROLLBACK + throw RiderConflictException
```

The `FOR UPDATE` row-level locks prevent two admins from simultaneously
assigning the same rider or the same order.

---

## 8. Setup & Run Instructions

### Step 1 — Prerequisites
- JDK 17+
- XAMPP running (Apache + MySQL)
- `mysql-connector-j-8.x.jar` on classpath

### Step 2 — Database
1. Open phpMyAdmin → http://localhost/phpmyadmin
2. Execute `docs/schema.sql` in full
3. Verify tables exist in `newway_db`

### Step 3 — Image Assets
Store food images locally, e.g.:
```
project-root/
└── assets/
    └── images/
        ├── burger.jpg
        ├── sushi.jpg
        └── ...
```
In the DB:  `image_path = 'assets/images/burger.jpg'`

### Step 4 — Compile & Run
```bash
javac -cp ".;mysql-connector-j-8.x.jar" -d out src/**/*.java
java  -cp ".;out;mysql-connector-j-8.x.jar" ui.MainFrame
```

### Step 5 — Default Login
| Role  | Email              | Password  |
|-------|--------------------|-----------|
| Admin | admin@newway.com   | admin123  |
*(Update the password_hash in the DB after first login)*

---

## 9. Key Implementation Notes

### Image Handling
```java
// In FoodItemCard builder:
ImageIcon raw    = new ImageIcon(imagePath);
Image    scaled  = raw.getImage().getScaledInstance(200, 140, Image.SCALE_SMOOTH);
imageLabel.setIcon(new ImageIcon(scaled));
```
Do NOT store images as BLOBs — store the path string in VARCHAR(300).

### Password Security (Production)
Replace plain-text comparison in LoginPanel with BCrypt:
```java
// Add dependency: org.mindrot:jbcrypt:0.4
boolean valid = BCrypt.checkpw(rawPassword, storedHash);
// On register:
String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
```

### SwingWorker Pattern (DB calls off EDT)
All DAO calls are wrapped in SwingWorker to keep the UI responsive:
```java
new SwingWorker<List<Order>, Void>() {
    @Override protected List<Order> doInBackground() { return dao.query(); }
    @Override protected void done() { /* update UI here */ }
}.execute();
```

### Auto-Refresh (Admin Dashboard)
Add a `javax.swing.Timer` in AdminDashboard to auto-refresh every 30 seconds:
```java
new Timer(30_000, e -> refresh()).start();
```

---

## 10. Recommended Enhancements (Phase 2)

1. **FlatLaf UI Library** — Add `com.formdev:flatlaf:3.x` for a modern flat Look & Feel
2. **Map Integration** — Embed a JxBrowser/WebView to show delivery route on OpenStreetMap
3. **Real Traffic API** — Query TomTom or Google Maps Distance Matrix for live distance + traffic
4. **Notification System** — Use Java Desktop API toasts or an embedded notification bar
5. **Report Export** — Generate PDF delivery reports using Apache PDFBox
6. **Model Persistence** — Serialise PredictionEngine coefficients to a JSON file between sessions

---

*Generated for NewWay v1.0 | Java 17 | MySQL 8 | Swing*
