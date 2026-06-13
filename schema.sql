-- ============================================================
--  NewWay: Intelligent Food Delivery Time Prediction System
--  MySQL Schema — Indian Edition (Nandyal · Hyderabad · Kurnool)
--  Run this entire script in phpMyAdmin to reset/init the DB.
-- ============================================================

DROP DATABASE IF EXISTS newway_db;
CREATE DATABASE newway_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE newway_db;

CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(15),
    address       TEXT,
    location      VARCHAR(100) DEFAULT 'Hyderabad',
    role          ENUM('user','admin') NOT NULL DEFAULT 'user',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE restaurants (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    location     VARCHAR(100) NOT NULL,
    cuisine_type VARCHAR(80),
    rating       DECIMAL(3,2) DEFAULT 0.00,
    image_path   VARCHAR(300)
);

CREATE TABLE food_items (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id INT NOT NULL,
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    price         DECIMAL(10,2) NOT NULL,
    rating        DECIMAL(3,2) DEFAULT 0.00,
    category      VARCHAR(80),
    image_path    VARCHAR(300),
    is_available  TINYINT(1) DEFAULT 1,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);

CREATE TABLE riders (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    full_name        VARCHAR(100) NOT NULL,
    phone            VARCHAR(15)  NOT NULL UNIQUE,
    vehicle_type     VARCHAR(50)  DEFAULT 'Motorcycle',
    status           ENUM('Available','On Delivery','Offline') NOT NULL DEFAULT 'Available',
    total_deliveries INT DEFAULT 0,
    rating           DECIMAL(3,2) DEFAULT 5.00,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    user_id            INT NOT NULL,
    rider_id           INT,
    total_price        DECIMAL(10,2) NOT NULL,
    delivery_address   TEXT NOT NULL,
    distance_km        DECIMAL(6,2) DEFAULT 1.00,
    traffic_factor     DECIMAL(4,2) DEFAULT 1.00,
    weather_code       TINYINT DEFAULT 0,
    predicted_time_min INT,
    actual_time_min    INT,
    status             ENUM('Pending','Assigned','Out for Delivery','Delivered','Cancelled') NOT NULL DEFAULT 'Pending',
    placed_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at       TIMESTAMP NULL,
    special_notes      TEXT,
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (rider_id) REFERENCES riders(id) ON DELETE SET NULL
);

CREATE TABLE order_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT NOT NULL,
    food_item_id INT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    unit_price   DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id)     REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (food_item_id) REFERENCES food_items(id)
);

-- ── SEED: Users ────────────────────────────────────────────────────────────
INSERT INTO users (full_name, email, password_hash, role, location, phone) VALUES
('System Admin',   'admin@newway.com', 'admin123', 'admin', 'Hyderabad', '9000000001'),
('Ravi Kumar',     'user@newway.com',  'user123',  'user',  'Hyderabad', '9848012345');

-- ── SEED: Restaurants ──────────────────────────────────────────────────────
INSERT INTO restaurants (name, location, cuisine_type, rating) VALUES
('Paradise Biryani',         'Hyderabad', 'Biryani',      4.9),
('Shah Ghouse',              'Hyderabad', 'Hyderabadi',   4.8),
('Chutneys',                 'Hyderabad', 'South Indian', 4.7),
('Bawarchi Restaurant',      'Hyderabad', 'Biryani',      4.6),
('AB''s Absolute Barbecues', 'Hyderabad', 'Grill',        4.5),
('Minerva Coffee Shop',      'Hyderabad', 'Snacks',       4.4),
('Sri Sai Tiffins',          'Nandyal',   'South Indian', 4.6),
('Rayalaseema Ruchulu',      'Nandyal',   'Andhra',       4.8),
('Hotel Surya',              'Nandyal',   'Biryani',      4.4),
('Ganesh Bhavan',            'Nandyal',   'Veg',          4.5),
('Kurnool Biryani House',    'Kurnool',   'Biryani',      4.7),
('Andhra Spice Garden',      'Kurnool',   'Andhra',       4.8),
('Sri Venkateswara Hotel',   'Kurnool',   'South Indian', 4.5),
('Srinivas Fast Food',       'Kurnool',   'Snacks',       4.3);

-- ── SEED: Food Items — Hyderabad ───────────────────────────────────────────
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(1,'Hyderabadi Dum Biryani','Slow-cooked aromatic basmati with tender mutton',280,4.9,'Biryani'),
(1,'Chicken Biryani','Juicy chicken pieces with saffron-infused basmati',220,4.8,'Biryani'),
(1,'Veg Dum Biryani','Fragrant basmati with fresh seasonal vegetables',160,4.6,'Biryani'),
(2,'Mutton Haleem','Slow-cooked mutton and lentil stew — city favourite',180,4.9,'Snacks'),
(2,'Nihari','Rich overnight-cooked beef stew with warm masala',200,4.7,'Curry'),
(2,'Sheer Khurma','Festival vermicelli pudding with dry fruits & rose water',80,4.6,'Dessert'),
(3,'Pesarattu','Green moong dal crispy crepe with ginger chutney',90,4.7,'Breakfast'),
(3,'Idli Sambar','Soft steamed rice cakes with piping-hot vegetable sambar',70,4.8,'Breakfast'),
(3,'Masala Dosa','Crispy crepe stuffed with spiced potato masala',110,4.7,'Breakfast'),
(3,'Upma','Roasted semolina with mixed vegetables and cashews',60,4.5,'Breakfast'),
(4,'Chicken 65','Spicy deep-fried chicken marinated Hyderabad style',180,4.8,'Starter'),
(4,'Paneer Butter Masala','Soft paneer cubes in silky tomato-cream gravy',160,4.7,'Curry'),
(5,'BBQ Chicken Platter','Flame-grilled whole chicken with mint chutney',350,4.6,'Grill'),
(5,'Seekh Kebab','Minced mutton pressed on iron skewers, char-grilled',280,4.7,'Starter'),
(6,'Osmania Biscuit','Iconic Hyderabadi melt-in-mouth butter biscuit',30,4.8,'Snacks'),
(6,'Double Ka Meetha','Hyderabadi bread pudding layered with rabdi & dry fruits',90,4.9,'Dessert'),
(6,'Irani Chai','Rich condensed-milk tea brewed the Irani way',25,4.9,'Beverages');

-- ── SEED: Food Items — Nandyal ─────────────────────────────────────────────
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(7,'Idli Vada Combo','Fluffy idlis + crispy medu vada with two chutneys',65,4.7,'Breakfast'),
(7,'Puri Kurma','Puffy puri served with spiced vegetable kurma',70,4.6,'Breakfast'),
(7,'Rava Dosa','Thin crispy semolina dosa with coconut chutney',80,4.5,'Breakfast'),
(8,'Natu Kodi Curry','Village-style country chicken with Rayalaseema spices',220,4.8,'Curry'),
(8,'Ragi Sankati','Finger millet ball served with spicy chicken curry',120,4.7,'Meals'),
(8,'Gongura Mutton','Tender mutton cooked with tangy sorrel leaves — Andhra special',240,4.9,'Curry'),
(8,'Pesalu Vada','Crispy moong dal vada — classic street snack',50,4.6,'Snacks'),
(9,'Nandyal Special Biryani','Local-style spicy biryani with native country chicken',200,4.5,'Biryani'),
(9,'Egg Biryani','Dum biryani with boiled eggs and aromatic masala',150,4.4,'Biryani'),
(10,'Full Meals Thali','Rice, sambar, rasam, 4 curries, curd, pickle & papad',100,4.6,'Meals'),
(10,'Gulab Jamun','Soft milk-solid dumplings soaked in rose syrup',40,4.7,'Dessert'),
(10,'Sweet Lassi','Chilled yogurt drink with cardamom & rose water',50,4.5,'Beverages');

-- ── SEED: Food Items — Kurnool ─────────────────────────────────────────────
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(11,'Kurnool Mutton Biryani','Fiery Kurnool-style biryani with whole spices',260,4.8,'Biryani'),
(11,'Chicken Dum Biryani','Dum-cooked chicken biryani with mint & coriander',210,4.7,'Biryani'),
(11,'Egg Curry Rice','Spiced masala egg curry served with steamed rice',130,4.5,'Curry'),
(12,'Gongura Chicken','Tender chicken in tangy Andhra sorrel masala',200,4.8,'Curry'),
(12,'Royyala Iguru','Prawn stir-fry in spicy Andhra masala — coastal recipe',280,4.9,'Curry'),
(12,'Pesarattu Upma','Moong dal crepe stuffed with semolina upma',90,4.6,'Breakfast'),
(13,'Kurnool Meals Plate','Rice, sambar, rasam, 3 curries, curd & papad',90,4.6,'Meals'),
(13,'Pongal','Soft rice-lentil dish tempered with ghee, pepper & cashews',60,4.5,'Breakfast'),
(13,'Filter Coffee','Strong south Indian decoction in steel tumbler',25,4.8,'Beverages'),
(14,'Mirchi Bajji','Green chilli fritter with tamarind chutney filling',40,4.7,'Snacks'),
(14,'Samosa','Crispy fried samosa with spiced potato filling',20,4.6,'Snacks'),
(14,'Vada Pav','Mumbai-style spiced potato patty in a soft pav bun',35,4.5,'Snacks'),
(14,'Cold Coffee','Blended cold coffee with vanilla ice cream',70,4.6,'Beverages');

-- ── SEED: Additional Food Items — Hyderabad (extended) ───────────────────
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(1,'Veg Hyderabadi Biryani','Aromatic basmati with mixed vegetables and saffron',150,4.7,'Biryani'),
(2,'Dum Keema','Slow-cooked spiced minced meat — Hyderabadi street classic',160,4.8,'Curry'),
(3,'Uttapam','Thick rice pancake topped with onion, tomato & green chilli',95,4.6,'Breakfast'),
(4,'Lamb Curry','Slow-braised lamb in rich Hyderabadi masala',260,4.8,'Curry'),
(5,'Fish Tikka','Tandoor-marinated fish fillets with mint dip',320,4.7,'Grill'),
(6,'Luqaimat','Crispy honey-drizzled Hyderabadi dumplings',60,4.8,'Dessert'),
(6,'Qubani ka Meetha','Apricot dessert with cream — royal Hyderabadi sweet',70,4.9,'Dessert'),
(5,'Paneer Tikka','Marinated cottage cheese grilled in tandoor',180,4.7,'Starter');

-- ── SEED: Additional Food Items — Nandyal (extended) ──────────────────────
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(7,'Poha','Flattened rice tempered with mustard seeds & curry leaves',45,4.5,'Breakfast'),
(8,'Gongura Pachadi','Tangy sorrel leaf chutney — Rayalaseema staple',30,4.7,'Snacks'),
(9,'Chicken Curry Rice','Spicy village chicken curry with steam rice',170,4.6,'Meals'),
(10,'Tomato Rasam','Tangy South Indian lentil soup with pepper & cumin',35,4.6,'Beverages'),
(10,'Banana Leaf Meals','Traditional full South Indian banana-leaf meal',120,4.8,'Meals'),
(8,'Mutton Keema Pav','Spiced minced mutton served with soft bread rolls',180,4.7,'Snacks');

-- ── SEED: Additional Food Items — Kurnool (extended) ──────────────────────
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(11,'Prawn Biryani','Jumbo prawns layered in spiced basmati dum biryani',300,4.9,'Biryani'),
(12,'Crab Masala','Fresh crab in fiery Kurnool coastal masala',350,4.8,'Curry'),
(13,'Sabudana Khichdi','Tapioca pearls with peanuts & cumin — light breakfast',65,4.5,'Breakfast'),
(14,'Punugulu','Fermented rice batter fritters with coconut chutney',40,4.7,'Snacks'),
(14,'Bamboo Chicken','Marinated whole chicken slow-cooked inside bamboo',420,4.9,'Grill'),
(12,'Rayalaseema Ulavacharu','Horsegram rasam — the iconic Andhra sour soup',45,4.8,'Beverages');

-- ── SQL ALTER statements (run only if upgrading existing DB, not fresh install) ──
-- ALTER TABLE riders ADD COLUMN IF NOT EXISTS active_status ENUM('Active','Inactive','On-Delivery') DEFAULT 'Active';
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS is_deleted TINYINT(1) DEFAULT 0;
-- (In this schema, riders.status and orders.status='Cancelled' handle both requirements natively)
INSERT INTO riders (full_name, phone, vehicle_type, status, total_deliveries, rating) VALUES
('Ravi Shankar',    '9848012345', 'Motorcycle', 'Available',   142, 4.8),
('Suresh Reddy',    '9000123456', 'Motorcycle', 'Available',    98, 4.7),
('Venkat Naidu',    '8125043210', 'Bicycle',    'Available',    76, 4.6),
('Kiran Kumar',     '9177654321', 'Motorcycle', 'On Delivery', 220, 4.9),
('Arjun Rao',       '7995541230', 'Motorcycle', 'Available',   189, 4.8),
('Praveen Goud',    '9440312456', 'Motorcycle', 'Available',    54, 4.5),
('Mahesh Varma',    '8555012789', 'Car',        'Available',   310, 4.9),
('Srinivas Chary',  '9346012678', 'Bicycle',    'Offline',      33, 4.4),
('Dinesh Kumar',    '9885501234', 'Motorcycle', 'Available',   167, 4.7),
('Lokesh Naidu',    '8498012345', 'Motorcycle', 'On Delivery', 201, 4.8);

-- ══════════════════════════════════════════════════════════════════════════════
--  TASK 1 — ALTER TABLE STATEMENTS
--  Run these ONLY if upgrading an EXISTING database (not fresh install).
--  If running fresh: the CREATE TABLE statements above already include
--  all required columns natively.
-- ══════════════════════════════════════════════════════════════════════════════

-- TASK 1A: Riders — add status (Active/Inactive/On-Delivery) + phone_number
--   NOTE: In this schema both columns already exist.
--   Use these ALTER statements only against an OLD schema that lacks them:
-- ALTER TABLE riders
--     ADD COLUMN IF NOT EXISTS status
--         ENUM('Available','On Delivery','Offline') NOT NULL DEFAULT 'Available';
-- ALTER TABLE riders
--     ADD COLUMN IF NOT EXISTS phone VARCHAR(15) UNIQUE;

-- TASK 1B: Orders — add is_deleted flag + extend status enum
--   The fresh schema uses status='Cancelled' as the soft-delete mechanism.
--   For backward-compat with older schemas that lack the Cancelled value:
-- ALTER TABLE orders
--     MODIFY COLUMN status
--         ENUM('Pending','Assigned','Out for Delivery','Delivered','Cancelled')
--         NOT NULL DEFAULT 'Pending';
-- ALTER TABLE orders
--     ADD COLUMN IF NOT EXISTS is_deleted TINYINT(1) NOT NULL DEFAULT 0;

-- TASK 1C: PredictionEngine — weather_code extended to support 5 codes (0-4)
--   0=Clear, 1=Light Rain, 2=Heavy Storm, 3=Fog, 4=Extreme Heat
--   No DB change needed; weatherCode is just an INT column.

-- ══════════════════════════════════════════════════════════════════════════════

-- ══════════════════════════════════════════════════════════════════════════════
--  ADDITIONAL FOOD ITEMS (expanded menu — 30+ new items)
-- ══════════════════════════════════════════════════════════════════════════════

-- Hyderabad — Paradise Biryani (id=1)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(1,'Prawn Biryani',     'Fresh prawns dum-cooked with basmati and coastal spices', 320, 4.7, 'Biryani'),
(1,'Veg Dum Biryani',   'Garden vegetables slow-cooked in saffron basmati',        160, 4.6, 'Biryani'),
(1,'Chicken Shorba',    'Thin aromatic chicken broth — served with biryani',         60, 4.5, 'Beverages');

-- Hyderabad — Shah Ghouse (id=2)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(2,'Paya Soup',         'Slow-simmered trotters in rich bone broth with spices',   140, 4.6, 'Curry'),
(2,'Kheema Samosa',     'Crispy samosa stuffed with spiced minced mutton',           50, 4.7, 'Snacks'),
(2,'Qubani Ka Meetha',  'Apricot dessert with almond cream — Hyderabadi classic',   80, 4.8, 'Dessert');

-- Hyderabad — Chutneys (id=3)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(3,'Set Dosa',          'Soft spongy dosas with coconut chutney and sambar',        75, 4.6, 'Breakfast'),
(3,'Punugulu',          'Deep-fried idli-batter balls with peanut chutney',          55, 4.5, 'Snacks'),
(3,'Mango Lassi',       'Chilled thick lassi blended with Alphonso mango pulp',      70, 4.7, 'Beverages');

-- Hyderabad — Bawarchi (id=4)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(4,'Egg Fried Rice',    'Wok-tossed basmati with scrambled egg and spring onion',  120, 4.5, 'Meals'),
(4,'Mutton Curry',      'Slow-cooked mutton in Bawarchi special gravy',            220, 4.7, 'Curry'),
(4,'Garlic Naan',       'Soft tandoor naan brushed with garlic butter',             45, 4.6, 'Snacks');

-- Hyderabad — AB's Barbecues (id=5)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(5,'Chicken Wings',     'Smoky marinated wings flame-grilled over charcoal',       220, 4.7, 'Grill'),
(5,'Fish Tikka',        'Surmai fish marinated in ajwain-ginger tikka masala',     280, 4.6, 'Grill'),
(5,'Paneer Tikka',      'Charred cottage cheese cubes with capsicum and onion',    200, 4.7, 'Grill');

-- Hyderabad — Minerva Coffee Shop (id=6)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(6,'Bun Maska',         'Buttered soft bun with Irani chai — iconic Minerva combo', 35, 4.7, 'Snacks'),
(6,'Lukhmi',            'Flaky pastry squares stuffed with spiced minced mutton',   45, 4.8, 'Snacks'),
(6,'Falooda',           'Rose milk falooda with basil seeds and vanilla ice cream',  90, 4.6, 'Dessert');

-- Nandyal — Sri Sai Tiffins (id=7)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(7,'Pesarattu Upma',    'Moong crepe stuffed with upma — Nandyal breakfast special', 75, 4.6, 'Breakfast'),
(7,'Medu Vada',         'Crispy lentil doughnut with sambar and chutneys',            55, 4.5, 'Breakfast'),
(7,'Buttermilk',        'Chilled salted chaas with green chilli and curry leaves',    25, 4.6, 'Beverages');

-- Nandyal — Rayalaseema Ruchulu (id=8)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(8,'Ulavacharu',        'Horsegram rasam — Rayalaseema''s signature comfort dish',   80, 4.9, 'Curry'),
(8,'Boti Curry',        'Tender lamb offal curry in fiery Rayalaseema masala',       180, 4.7, 'Curry'),
(8,'Tamarind Rice',     'Tangy puli sadam tempered with mustard and groundnuts',      90, 4.6, 'Meals');

-- Nandyal — Ganesh Bhavan (id=10)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(10,'Curd Rice',        'Soft rice mixed with fresh curd, pomegranate and carrot',   60, 4.6, 'Meals'),
(10,'Lemon Rice',       'Tangy lemon-turmeric rice with roasted groundnuts',          65, 4.5, 'Meals'),
(10,'Badam Milk',       'Warm almond milk spiced with cardamom and saffron',          55, 4.7, 'Beverages');

-- Kurnool — Kurnool Biryani House (id=11)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(11,'Fish Biryani',     'Kurnool-style biryani with fresh river fish and masala',   240, 4.7, 'Biryani'),
(11,'Chicken Curry',    'Spicy Kurnool chicken curry with coconut masala base',     180, 4.6, 'Curry'),
(11,'Raita',            'Chilled yogurt with cucumber, carrot and roasted cumin',    30, 4.4, 'Snacks');

-- Kurnool — Andhra Spice Garden (id=12)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(12,'Natu Kodi Pulusu', 'Country chicken in tangy Andhra tamarind pulusu',          200, 4.9, 'Curry'),
(12,'Bendakaya Fry',    'Crispy okra stir-fry with Kurnool chilli powder',            80, 4.5, 'Meals'),
(12,'Avakai Pickle',    'Traditional Kurnool raw mango pickle with mustard oil',      40, 4.7, 'Snacks');

-- Kurnool — Srinivas Fast Food (id=14)
INSERT INTO food_items (restaurant_id, name, description, price, rating, category) VALUES
(14,'Egg Puff',         'Flaky pastry stuffed with spiced egg and onion masala',     25, 4.5, 'Snacks'),
(14,'Chicken Roll',     'Soft paratha roll stuffed with tandoori chicken strips',     90, 4.6, 'Snacks'),
(14,'Fresh Lime Soda',  'Chilled nimbu soda — salted or sweet as you like',           30, 4.5, 'Beverages');
