package com.fooddelivery.repository;

import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.persistence.Database;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteRestaurantRepository implements RestaurantRepository {

    private Database db;

    public SqliteRestaurantRepository(Database db) {
        this.db = db;
    }

    @Override
    public void save(Restaurant r) {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO restaurants(id, name, open, x, y, radius, min_order) "
                            + "VALUES (?,?,?,?,?,?,?)");
            ps.setString(1, r.getId());
            ps.setString(2, r.getName());
            ps.setInt(3, r.isOpen() ? 1 : 0);
            ps.setDouble(4, r.getLocation().getX());
            ps.setDouble(5, r.getLocation().getY());
            ps.setDouble(6, r.getDeliveryRadiusKm());
            ps.setString(7, r.getMinimumOrderValue().toPlainString());
            ps.executeUpdate();
            ps.close();

            // menu: delete and insert again, simplest way
            PreparedStatement del = db.getConnection().prepareStatement(
                    "DELETE FROM menu_items WHERE restaurant_id = ?");
            del.setString(1, r.getId());
            del.executeUpdate();
            del.close();
            for (MenuItem m : r.getMenu()) {
                PreparedStatement ins = db.getConnection().prepareStatement(
                        "INSERT INTO menu_items(id, restaurant_id, name, price, available) "
                                + "VALUES (?,?,?,?,?)");
                ins.setString(1, m.getId());
                ins.setString(2, r.getId());
                ins.setString(3, m.getName());
                ins.setString(4, m.getPrice().toPlainString());
                ins.setInt(5, m.isAvailable() ? 1 : 0);
                ins.executeUpdate();
                ins.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Restaurant> findById(String id) {
        List<Restaurant> list = query("SELECT * FROM restaurants WHERE id = ?", id);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    @Override
    public List<Restaurant> findAll() {
        return query("SELECT * FROM restaurants ORDER BY id", null);
    }

    private List<Restaurant> query(String sql, String param) {
        List<Restaurant> result = new ArrayList<>();
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            if (param != null) {
                ps.setString(1, param);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Restaurant r = new Restaurant(rs.getString("id"), rs.getString("name"),
                        rs.getInt("open") == 1, new Location(rs.getDouble("x"), rs.getDouble("y")),
                        rs.getDouble("radius"), new BigDecimal(rs.getString("min_order")));
                result.add(r);
            }
            rs.close();
            ps.close();
            for (Restaurant r : result) {
                PreparedStatement ms = db.getConnection().prepareStatement(
                        "SELECT * FROM menu_items WHERE restaurant_id = ? ORDER BY id");
                ms.setString(1, r.getId());
                ResultSet mrs = ms.executeQuery();
                while (mrs.next()) {
                    r.addMenuItem(new MenuItem(mrs.getString("id"), mrs.getString("name"),
                            new BigDecimal(mrs.getString("price")), mrs.getInt("available") == 1));
                }
                mrs.close();
                ms.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
