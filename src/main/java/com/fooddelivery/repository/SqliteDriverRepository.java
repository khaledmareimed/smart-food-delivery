package com.fooddelivery.repository;

import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.Location;
import com.fooddelivery.persistence.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteDriverRepository implements DriverRepository {

    private Database db;

    public SqliteDriverRepository(Database db) {
        this.db = db;
    }

    @Override
    public void save(Driver d) {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO drivers(id, name, x, y, speed, status) VALUES (?,?,?,?,?,?)");
            ps.setString(1, d.getId());
            ps.setString(2, d.getName());
            ps.setDouble(3, d.getLocation().getX());
            ps.setDouble(4, d.getLocation().getY());
            ps.setDouble(5, d.getSpeedKmh());
            ps.setString(6, d.getStatus().name());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Driver> findById(String id) {
        List<Driver> list = query("SELECT * FROM drivers WHERE id = ?", id);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    @Override
    public List<Driver> findAll() {
        return query("SELECT * FROM drivers ORDER BY id", null);
    }

    private List<Driver> query(String sql, String param) {
        List<Driver> result = new ArrayList<>();
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            if (param != null) {
                ps.setString(1, param);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Driver d = new Driver(rs.getString("id"), rs.getString("name"),
                        new Location(rs.getDouble("x"), rs.getDouble("y")), rs.getDouble("speed"));
                d.setStatus(DriverStatus.valueOf(rs.getString("status")));
                result.add(d);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
