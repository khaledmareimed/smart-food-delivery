package com.fooddelivery.repository;

import com.fooddelivery.domain.BicycleDriver;
import com.fooddelivery.domain.CarDriver;
import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.ElectricDriver;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MotorcycleDriver;
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
                    "INSERT OR REPLACE INTO drivers(id, name, x, y, speed, status, type, battery_range) "
                            + "VALUES (?,?,?,?,?,?,?,?)");
            ps.setString(1, d.getId());
            ps.setString(2, d.getName());
            ps.setDouble(3, d.getLocation().getX());
            ps.setDouble(4, d.getLocation().getY());
            ps.setDouble(5, d.getSpeedKmh());
            ps.setString(6, d.getStatus().name());
            ps.setString(7, d.getType());
            if (d instanceof ElectricDriver) {
                ps.setDouble(8, ((ElectricDriver) d).getBatteryRangeKm());
            } else {
                ps.setDouble(8, 0);
            }
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
                String type = rs.getString("type");
                String id = rs.getString("id");
                String name = rs.getString("name");
                Location loc = new Location(rs.getDouble("x"), rs.getDouble("y"));
                double speed = rs.getDouble("speed");
                Driver d;
                if ("BICYCLE".equals(type)) {
                    d = new BicycleDriver(id, name, loc, speed);
                } else if ("MOTORCYCLE".equals(type)) {
                    d = new MotorcycleDriver(id, name, loc, speed);
                } else if ("CAR".equals(type)) {
                    d = new CarDriver(id, name, loc, speed);
                } else if ("ELECTRIC".equals(type)) {
                    d = new ElectricDriver(id, name, loc, speed, rs.getDouble("battery_range"));
                } else {
                    d = new Driver(id, name, loc, speed);
                }
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
