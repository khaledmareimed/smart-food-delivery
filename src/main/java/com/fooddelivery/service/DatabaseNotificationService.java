package com.fooddelivery.service;

import com.fooddelivery.persistence.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores notifications in the notifications table and prints them to the console.
 */
public class DatabaseNotificationService implements NotificationService {

    private Database db;

    public DatabaseNotificationService(Database db) {
        this.db = db;
    }

    @Override
    public void send(String userId, String message) {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT INTO notifications(user_id, message) VALUES (?, ?)");
            ps.setString(1, userId);
            ps.setString(2, message);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[notification to " + userId + "] " + message);
    }

    @Override
    public List<String> getNotifications(String userId) {
        List<String> result = new ArrayList<>();
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT message FROM notifications WHERE user_id = ? ORDER BY id");
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
