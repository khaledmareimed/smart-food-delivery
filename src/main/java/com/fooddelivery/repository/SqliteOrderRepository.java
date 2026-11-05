package com.fooddelivery.repository;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.persistence.Database;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteOrderRepository implements OrderRepository {

    private Database db;
    private SqliteUserRepository users;
    private SqliteRestaurantRepository restaurants;

    public SqliteOrderRepository(Database db) {
        this.db = db;
        this.users = new SqliteUserRepository(db);
        this.restaurants = new SqliteRestaurantRepository(db);
    }

    @Override
    public void save(Order o) {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO orders(id, customer_id, restaurant_id, street, x, y, status) "
                            + "VALUES (?,?,?,?,?,?,?)");
            ps.setString(1, o.getId());
            ps.setString(2, o.getCustomer().getId());
            ps.setString(3, o.getRestaurant().getId());
            ps.setString(4, o.getDeliveryAddress().getStreet());
            ps.setDouble(5, o.getDeliveryAddress().getLocation().getX());
            ps.setDouble(6, o.getDeliveryAddress().getLocation().getY());
            ps.setString(7, o.getStatus().name());
            ps.executeUpdate();
            ps.close();

            PreparedStatement del = db.getConnection().prepareStatement(
                    "DELETE FROM order_items WHERE order_id = ?");
            del.setString(1, o.getId());
            del.executeUpdate();
            del.close();
            for (OrderItem item : o.getItems()) {
                PreparedStatement ins = db.getConnection().prepareStatement(
                        "INSERT INTO order_items(order_id, menu_item_id, name, unit_price, quantity) "
                                + "VALUES (?,?,?,?,?)");
                ins.setString(1, o.getId());
                ins.setString(2, item.getMenuItemId());
                ins.setString(3, item.getName());
                ins.setString(4, item.getUnitPrice().toPlainString());
                ins.setInt(5, item.getQuantity());
                ins.executeUpdate();
                ins.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Order> findById(String id) {
        List<Order> list = query("SELECT * FROM orders WHERE id = ?", id);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return query("SELECT * FROM orders WHERE customer_id = ? ORDER BY rowid", customerId);
    }

    @Override
    public List<Order> findAll() {
        return query("SELECT * FROM orders ORDER BY rowid", null);
    }

    private List<Order> query(String sql, String param) {
        List<Order> result = new ArrayList<>();
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(sql);
            if (param != null) {
                ps.setString(1, param);
            }
            ResultSet rs = ps.executeQuery();
            List<String[]> rows = new ArrayList<>();
            List<double[]> coords = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[] {rs.getString("id"), rs.getString("customer_id"),
                        rs.getString("restaurant_id"), rs.getString("street"), rs.getString("status")});
                coords.add(new double[] {rs.getDouble("x"), rs.getDouble("y")});
            }
            rs.close();
            ps.close();
            for (int i = 0; i < rows.size(); i++) {
                String[] row = rows.get(i);
                User customer = users.findById(row[1]).orElse(null);
                Restaurant restaurant = restaurants.findById(row[2]).orElse(null);
                DeliveryAddress address = new DeliveryAddress(row[3],
                        new Location(coords.get(i)[0], coords.get(i)[1]));
                Order order = new Order(row[0], customer, restaurant, loadItems(row[0]), address);
                order.setStatus(OrderStatus.valueOf(row[4]));
                result.add(order);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private List<OrderItem> loadItems(String orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT * FROM order_items WHERE order_id = ? ORDER BY rowid");
        ps.setString(1, orderId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            MenuItem m = new MenuItem(rs.getString("menu_item_id"), rs.getString("name"),
                    new BigDecimal(rs.getString("unit_price")), true);
            items.add(new OrderItem(m, rs.getInt("quantity")));
        }
        rs.close();
        ps.close();
        return items;
    }
}
