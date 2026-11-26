package com.tatkalx.dao;

import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.exception.DatabaseOperationException;
import com.tatkalx.model.Admin;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for Users, Passengers, and Admins using PreparedStatement and ResultSet mapping.
 */
public class UserPassengerDao implements GenericDao<Passenger, Integer> {

    private final DatabaseConnectionManager connectionManager;

    public UserPassengerDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Integer insert(Passenger passenger) {
        return connectionManager.executeWriteTransaction(conn -> {
            String insertUserSql = "INSERT INTO Users (username, password_hash, full_name, email, phone, role, is_active) "
                    + "VALUES (?, ?, ?, ?, ?, 'PASSENGER', ?)";
            int userId;
            try (PreparedStatement psUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, passenger.getUsername());
                psUser.setString(2, passenger.getPasswordHash() == null ? "pass123" : passenger.getPasswordHash());
                psUser.setString(3, passenger.getFullName());
                psUser.setString(4, passenger.getEmail());
                psUser.setString(5, passenger.getPhone());
                psUser.setInt(6, passenger.isActive() ? 1 : 0);
                psUser.executeUpdate();
                try (ResultSet keys = psUser.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to obtain generated user_id.");
                    }
                    userId = keys.getInt(1);
                    passenger.setUserId(userId);
                }
            }

            String insertPaxSql = "INSERT INTO Passengers (user_id, age, gender, id_proof_type, id_proof_number, "
                    + "is_verified, priority_category, berth_preference, wallet_balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psPax = conn.prepareStatement(insertPaxSql, Statement.RETURN_GENERATED_KEYS)) {
                psPax.setInt(1, userId);
                psPax.setInt(2, passenger.getAge());
                psPax.setString(3, passenger.getGender());
                psPax.setString(4, passenger.getIdProofType());
                psPax.setString(5, passenger.getIdProofNumber());
                psPax.setInt(6, passenger.isVerified() ? 1 : 0);
                psPax.setString(7, passenger.getPriorityCategory());
                psPax.setString(8, passenger.getBerthPreference());
                psPax.setDouble(9, passenger.getWalletBalance());
                psPax.executeUpdate();
                try (ResultSet keys = psPax.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to obtain generated passenger_id.");
                    }
                    int paxId = keys.getInt(1);
                    passenger.setPassengerId(paxId);
                    return paxId;
                }
            }
        });
    }

    @Override
    public Passenger findById(Integer passengerId) {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.is_active, u.created_at, "
                + "p.passenger_id, p.age, p.gender, p.id_proof_type, p.id_proof_number, p.is_verified, "
                + "p.priority_category, p.berth_preference, p.wallet_balance "
                + "FROM Passengers p JOIN Users u ON p.user_id = u.user_id WHERE p.passenger_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPassenger(rs) : null;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error finding passenger id=" + passengerId, ex);
        }
    }

    public Passenger findByUsername(String username) {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.is_active, u.created_at, "
                + "p.passenger_id, p.age, p.gender, p.id_proof_type, p.id_proof_number, p.is_verified, "
                + "p.priority_category, p.berth_preference, p.wallet_balance "
                + "FROM Passengers p JOIN Users u ON p.user_id = u.user_id WHERE u.username = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPassenger(rs) : null;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error finding passenger username=" + username, ex);
        }
    }

    public User authenticate(String username, String password) {
        String sql = "SELECT user_id, username, password_hash, full_name, email, phone, role, is_active, created_at "
                + "FROM Users WHERE username = ? AND password_hash = ? AND is_active = 1";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");
                if ("PASSENGER".equalsIgnoreCase(role)) {
                    return findPassengerByUserId(conn, userId);
                }
                return findAdminByUserId(conn, userId);
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Authentication query failed.", ex);
        }
    }

    @Override
    public List<Passenger> findAll() {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.is_active, u.created_at, "
                + "p.passenger_id, p.age, p.gender, p.id_proof_type, p.id_proof_number, p.is_verified, "
                + "p.priority_category, p.berth_preference, p.wallet_balance "
                + "FROM Passengers p JOIN Users u ON p.user_id = u.user_id ORDER BY p.passenger_id";
        List<Passenger> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapPassenger(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error loading all passengers.", ex);
        }
    }

    @Override
    public boolean update(Passenger passenger) {
        return connectionManager.executeWriteTransaction(conn -> {
            String userSql = "UPDATE Users SET full_name = ?, email = ?, phone = ? WHERE user_id = ?";
            try (PreparedStatement psU = conn.prepareStatement(userSql)) {
                psU.setString(1, passenger.getFullName());
                psU.setString(2, passenger.getEmail());
                psU.setString(3, passenger.getPhone());
                psU.setInt(4, passenger.getUserId());
                psU.executeUpdate();
            }

            String paxSql = "UPDATE Passengers SET age = ?, gender = ?, is_verified = ?, "
                    + "priority_category = ?, berth_preference = ?, wallet_balance = ? WHERE passenger_id = ?";
            try (PreparedStatement psP = conn.prepareStatement(paxSql)) {
                psP.setInt(1, passenger.getAge());
                psP.setString(2, passenger.getGender());
                psP.setInt(3, passenger.isVerified() ? 1 : 0);
                psP.setString(4, passenger.getPriorityCategory());
                psP.setString(5, passenger.getBerthPreference());
                psP.setDouble(6, passenger.getWalletBalance());
                psP.setInt(7, passenger.getPassengerId());
                return psP.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean deleteById(Integer passengerId) {
        Passenger existing = findById(passengerId);
        if (existing == null) {
            return false;
        }
        return connectionManager.executeWriteTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Users WHERE user_id = ?")) {
                ps.setInt(1, existing.getUserId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    private Passenger findPassengerByUserId(Connection conn, int userId) throws SQLException {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.is_active, u.created_at, "
                + "p.passenger_id, p.age, p.gender, p.id_proof_type, p.id_proof_number, p.is_verified, "
                + "p.priority_category, p.berth_preference, p.wallet_balance "
                + "FROM Passengers p JOIN Users u ON p.user_id = u.user_id WHERE u.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPassenger(rs) : null;
            }
        }
    }

    private Admin findAdminByUserId(Connection conn, int userId) throws SQLException {
        String sql = "SELECT u.user_id, u.username, u.full_name, u.email, u.phone, "
                + "a.admin_id, a.employee_code, a.department, a.clearance_level "
                + "FROM Admins a JOIN Users u ON a.user_id = u.user_id WHERE u.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Admin(
                        rs.getInt("user_id"),
                        rs.getInt("admin_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("employee_code"),
                        rs.getString("department"),
                        rs.getInt("clearance_level")
                );
            }
        }
    }

    private Passenger mapPassenger(ResultSet rs) throws SQLException {
        Passenger p = new Passenger(
                rs.getInt("user_id"),
                rs.getInt("passenger_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("id_proof_type"),
                rs.getString("id_proof_number"),
                rs.getInt("is_verified") == 1,
                rs.getString("priority_category"),
                rs.getString("berth_preference"),
                rs.getDouble("wallet_balance")
        );
        p.setPasswordHash(rs.getString("password_hash"));
        p.setActive(rs.getInt("is_active") == 1);
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    }
}
