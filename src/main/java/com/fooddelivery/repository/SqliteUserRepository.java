package com.fooddelivery.repository;

import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.persistence.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {

    private Database db;

    public SqliteUserRepository(Database db) {
        this.db = db;
    }

    @Override
    public void save(User user) {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO users(id, username, password_hash, role) VALUES (?,?,?,?)");
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findOne("SELECT * FROM users WHERE username = ?", username);
    }

    @Override
    public Optional<User> findById(String id) {
        return findOne("SELECT * FROM users WHERE id = ?", id);
    }

    private Optional<User> findOne(String sql, String value) {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            User user = null;
            if (rs.next()) {
                user = new User(rs.getString("id"), rs.getString("username"),
                        rs.getString("password_hash"), Role.valueOf(rs.getString("role")));
            }
            rs.close();
            ps.close();
            return Optional.ofNullable(user);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
