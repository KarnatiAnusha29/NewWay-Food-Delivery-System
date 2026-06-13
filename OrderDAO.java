package db;

import model.Order;
import model.Rider;
import engine.PredictionEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OrderDAO — Data Access Object for the orders table.
 *
 * Key responsibilities:
 *  • CRUD operations on orders
 *  • Rider assignment with conflict-check logic
 *  • Status transitions
 */
public class OrderDAO {

    private final Connection conn;

    public OrderDAO() {
        try {
            this.conn = DBConnection.getInstance().getConnection();
        } catch (DBConnection.DBException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Place Order  (called from CheckoutPanel after cart checkout)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inserts a new order and runs the PredictionEngine to set predicted_time_min.
     * Returns the generated order ID.
     */
    public int placeOrder(int userId, double totalPrice, String deliveryAddress,
                          double distanceKm, double trafficFactor, int weatherCode,
                          String specialNotes) throws SQLException {

        int predicted = PredictionEngine.getInstance()
                                        .predict(distanceKm, trafficFactor, weatherCode);

        String sql = """
            INSERT INTO orders
              (user_id, total_price, delivery_address, distance_km,
               traffic_factor, weather_code, predicted_time_min, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending')
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDouble(2, totalPrice);
            ps.setString(3, deliveryAddress);
            ps.setDouble(4, distanceKm);
            ps.setDouble(5, trafficFactor);
            ps.setInt(6, weatherCode);
            ps.setInt(7, predicted);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("placeOrder: no generated key returned.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Rider Assignment — with Conflict Check
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Assigns a rider to an order atomically.
     *
     * CONFLICT CHECK LOGIC:
     *  1. Re-read the order inside a transaction to verify it is still 'Pending'.
     *  2. Verify the rider's current status is 'Available' (not 'On Delivery').
     *  3. Check that the rider has no other 'Assigned' or 'Out for Delivery' order.
     *  4. If all checks pass → update orders.rider_id + orders.status = 'Assigned'
     *                        → update riders.status = 'On Delivery'.
     *  5. If any check fails → roll back and throw a descriptive exception.
     *
     * @throws RiderConflictException if rider is already busy or order taken
     * @throws SQLException           for any DB errors
     */
    public void assignRider(int orderId, int riderId)
            throws SQLException, RiderConflictException {

        conn.setAutoCommit(false);  // BEGIN TRANSACTION

        try {
            // ── Step 1: Lock & read the order row ────────────────────────────
            String orderSql = "SELECT status FROM orders WHERE id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                        throw new RiderConflictException("Order #" + orderId + " not found.");
                    String orderStatus = rs.getString("status");
                    if (!"Pending".equals(orderStatus))
                        throw new RiderConflictException(
                            "Order #" + orderId + " is no longer Pending (status: " + orderStatus + ").");
                }
            }

            // ── Step 2: Lock & read the rider row ────────────────────────────
            String riderSql = "SELECT status FROM riders WHERE id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(riderSql)) {
                ps.setInt(1, riderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                        throw new RiderConflictException("Rider #" + riderId + " not found.");
                    String riderStatus = rs.getString("status");
                    if (!"Available".equals(riderStatus))
                        throw new RiderConflictException(
                            "Rider #" + riderId + " is not available (status: " + riderStatus + ").");
                }
            }

            // ── Step 3: Double-check no active order already assigned ─────────
            String activeSql = """
                SELECT COUNT(*) FROM orders
                WHERE rider_id = ? AND status IN ('Assigned','Out for Delivery')
                """;
            try (PreparedStatement ps = conn.prepareStatement(activeSql)) {
                ps.setInt(1, riderId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0)
                        throw new RiderConflictException(
                            "Rider #" + riderId + " already has an active delivery.");
                }
            }

            // ── Step 4: Commit the assignment ────────────────────────────────
            String updateOrder = """
                UPDATE orders SET rider_id = ?, status = 'Assigned'
                WHERE id = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(updateOrder)) {
                ps.setInt(1, riderId);
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }

            String updateRider = "UPDATE riders SET status = 'On Delivery' WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateRider)) {
                ps.setInt(1, riderId);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.printf("[OrderDAO] Rider #%d assigned to Order #%d%n", riderId, orderId);

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Soft Delete: Cancel an order (status -> 'Cancelled', row preserved)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cancels an order by setting status to 'Cancelled' (soft delete).
     * Rider is freed if previously assigned.
     * @throws CancelException if the order cannot be cancelled
     */
    public void cancelOrder(int orderId) throws SQLException, CancelException {
        conn.setAutoCommit(false);
        try {
            int riderId = -1;
            String status;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, rider_id FROM orders WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new CancelException("Order #" + orderId + " not found.");
                    status  = rs.getString("status");
                    riderId = rs.getInt("rider_id");
                    if (rs.wasNull()) riderId = -1;
                }
            }
            if ("Delivered".equals(status))
                throw new CancelException("Order #" + orderId + " already delivered.");
            if ("Cancelled".equals(status))
                throw new CancelException("Order #" + orderId + " already cancelled.");
            if ("Out for Delivery".equals(status))
                throw new CancelException("Order #" + orderId + " is out for delivery.");

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET status='Cancelled', rider_id=NULL WHERE id=?")) {
                ps.setInt(1, orderId); ps.executeUpdate();
            }
            if (riderId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE riders SET status='Available' WHERE id=?")) {
                    ps.setInt(1, riderId); ps.executeUpdate();
                }
            }
            conn.commit();
            System.out.printf("[OrderDAO] Order #%d cancelled. Rider #%s freed.%n",
                orderId, riderId > 0 ? riderId : "none");
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
    }

    /** All orders incl. Cancelled for user history tab. */
    public List<Order> getOrdersByUserWithHistory(int userId) throws SQLException {
        String sql = """
            SELECT o.*, u.full_name AS user_name, r.full_name AS rider_name
            FROM orders o JOIN users u ON o.user_id=u.id
            LEFT JOIN riders r ON o.rider_id=r.id
            WHERE o.user_id=? ORDER BY o.placed_at DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); return mapOrders(ps);
        }
    }

    /** All orders incl. Cancelled for Admin delete view. */
    public List<Order> getAllOrders() throws SQLException {
        String sql = """
            SELECT o.*, u.full_name AS user_name, r.full_name AS rider_name
            FROM orders o JOIN users u ON o.user_id=u.id
            LEFT JOIN riders r ON o.rider_id=r.id
            ORDER BY o.placed_at DESC LIMIT 300
            """;
        return mapOrders(sql);
    }

    public static class CancelException extends Exception {
        public CancelException(String msg) { super(msg); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Status Transition: mark an order as Delivered
    // ─────────────────────────────────────────────────────────────────────────

    public void markDelivered(int orderId) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // 1. Fetch rider linked to this order
            String getRider = "SELECT rider_id FROM orders WHERE id = ?";
            int riderId = -1;
            try (PreparedStatement ps = conn.prepareStatement(getRider)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) riderId = rs.getInt("rider_id");
                }
            }

            // 2. Update order
            String updOrder = """
                UPDATE orders
                SET status = 'Delivered', delivered_at = NOW()
                WHERE id = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(updOrder)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            // 3. Free the rider
            if (riderId > 0) {
                String updRider = """
                    UPDATE riders
                    SET status = 'Available', total_deliveries = total_deliveries + 1
                    WHERE id = ?
                    """;
                try (PreparedStatement ps = conn.prepareStatement(updRider)) {
                    ps.setInt(1, riderId);
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Queries
    // ─────────────────────────────────────────────────────────────────────────

    /** All active orders (for Admin dashboard). */
    public List<Order> getActiveOrders() throws SQLException {
        String sql = """
            SELECT o.*, u.full_name AS user_name, r.full_name AS rider_name
            FROM orders o
            JOIN users u ON o.user_id = u.id
            LEFT JOIN riders r ON o.rider_id = r.id
            WHERE o.status IN ('Pending','Assigned','Out for Delivery')
            ORDER BY o.placed_at DESC
            """;
        return mapOrders(sql);
    }

    /** Order history for a specific user. */
    public List<Order> getOrdersByUser(int userId) throws SQLException {
        String sql = """
            SELECT o.*, u.full_name AS user_name, r.full_name AS rider_name
            FROM orders o
            JOIN users u ON o.user_id = u.id
            LEFT JOIN riders r ON o.rider_id = r.id
            WHERE o.user_id = ?
            ORDER BY o.placed_at DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return mapOrders(ps);
        }
    }

    /** Get a single order by ID (for tracking panel). */
    public Optional<Order> findById(int orderId) throws SQLException {
        String sql = """
            SELECT o.*, u.full_name AS user_name, r.full_name AS rider_name
            FROM orders o
            JOIN users u ON o.user_id = u.id
            LEFT JOIN riders r ON o.rider_id = r.id
            WHERE o.id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            List<Order> list = mapOrders(ps);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<Order> mapOrders(String sql) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapResultSet(rs);
        }
    }

    private List<Order> mapOrders(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return mapResultSet(rs);
        }
    }

    private List<Order> mapResultSet(ResultSet rs) throws SQLException {
        List<Order> list = new ArrayList<>();
        while (rs.next()) {
            Order o = new Order();
            o.setId(rs.getInt("id"));
            o.setUserId(rs.getInt("user_id"));
            o.setUserName(rs.getString("user_name"));
            int riderId = rs.getInt("rider_id");
            o.setRiderId(rs.wasNull() ? null : riderId);
            o.setRiderName(rs.getString("rider_name"));
            o.setTotalPrice(rs.getBigDecimal("total_price"));
            o.setDeliveryAddress(rs.getString("delivery_address"));
            o.setDistanceKm(rs.getDouble("distance_km"));
            o.setTrafficFactor(rs.getDouble("traffic_factor"));
            o.setWeatherCode(rs.getInt("weather_code"));
            int pt = rs.getInt("predicted_time_min");
            o.setPredictedTimeMin(rs.wasNull() ? null : pt);
            o.setStatus(parseOrderStatus(rs.getString("status")));
            Timestamp placed = rs.getTimestamp("placed_at");
            if (placed != null) o.setPlacedAt(placed.toLocalDateTime());
            list.add(o);
        }
        return list;
    }

    private Order.Status parseOrderStatus(String s) {
        if (s == null) return Order.Status.Pending;
        switch (s.trim()) {
            case "Out for Delivery": return Order.Status.OutForDelivery;
            default: return Order.Status.valueOf(s.replace(" ", ""));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Soft Delete — sets order status to 'Cancelled' (never deletes the row)
    //  Used by both User "Cancel Order" and Admin "Delete Order" actions.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Soft-deletes an order by setting its status to 'Cancelled'.
     * If a rider was assigned, that rider is freed back to 'Available'.
     *
     * Business rules:
     *  • Only orders in status Pending or Assigned may be cancelled.
     *  • Delivered orders cannot be cancelled.
     *  • The row is never removed from the DB (audit trail preserved).
     *
     * @param orderId     order to cancel
     * @param adminForce  if true, also allows cancelling Assigned orders (admin only)
     * @throws SQLException           on DB error
     * @throws IllegalStateException  if the order is in a non-cancellable state
     */
    public void softDeleteOrder(int orderId, boolean adminForce)
            throws SQLException, IllegalStateException {

        conn.setAutoCommit(false);
        try {
            // ── Read current state ────────────────────────────────────────────
            String readSql = "SELECT status, rider_id FROM orders WHERE id = ? FOR UPDATE";
            String currentStatus;
            int    riderId = -1;
            try (PreparedStatement ps = conn.prepareStatement(readSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                        throw new IllegalStateException("Order #" + orderId + " not found.");
                    currentStatus = rs.getString("status");
                    riderId       = rs.getInt("rider_id");
                    if (rs.wasNull()) riderId = -1;
                }
            }

            // ── Validate transition ───────────────────────────────────────────
            boolean cancellable = "Pending".equals(currentStatus)
                               || (adminForce && "Assigned".equals(currentStatus));
            if (!cancellable) {
                throw new IllegalStateException(
                    "Cannot cancel order #" + orderId
                    + " — current status: " + currentStatus + "."
                    + (adminForce ? "" : " Only Pending orders can be cancelled."));
            }

            // ── Soft-delete: mark Cancelled ───────────────────────────────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET status = 'Cancelled' WHERE id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            // ── Free the rider if one was assigned ────────────────────────────
            if (riderId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE riders SET status = 'Available' WHERE id = ?")) {
                    ps.setInt(1, riderId);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            System.out.printf("[OrderDAO] Order #%d soft-deleted (Cancelled). Rider freed: %s%n",
                orderId, riderId > 0 ? "#" + riderId : "none");

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Rider CRUD Operations
    // ─────────────────────────────────────────────────────────────────────────

    /** Insert a new rider. Returns the generated ID. */
    public int addRider(String fullName, String phone, String vehicleType)
            throws SQLException {
        String sql = """
            INSERT INTO riders (full_name, phone, vehicle_type, status,
                                total_deliveries, rating)
            VALUES (?, ?, ?, 'Available', 0, 5.00)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, phone.trim());
            ps.setString(3, vehicleType.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("addRider: no generated key returned.");
    }

    /** Update an existing rider's editable fields. */
    public void updateRider(int riderId, String fullName, String phone,
                            String vehicleType, String status) throws SQLException {
        String sql = """
            UPDATE riders
            SET full_name = ?, phone = ?, vehicle_type = ?, status = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, phone.trim());
            ps.setString(3, vehicleType.trim());
            ps.setString(4, status.trim());
            ps.setInt(5, riderId);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a rider permanently (hard delete — only safe if rider has no
     * active orders; FK ON DELETE SET NULL protects order history).
     */
    public void deleteRider(int riderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM riders WHERE id = ?")) {
            ps.setInt(1, riderId);
            ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Custom Exception
    // ─────────────────────────────────────────────────────────────────────────

    public static class RiderConflictException extends Exception {
        public RiderConflictException(String message) { super(message); }
    }
}
