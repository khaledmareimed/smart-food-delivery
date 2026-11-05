package com.fooddelivery.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Holds the SQLite connection and creates the tables if they don't exist.
 */
public class Database {

    public static final String DEFAULT_URL = "jdbc:sqlite:food_delivery.db";

    private Connection connection;

    public Database(String url) {
        try {
            connection = DriverManager.getConnection(url);
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Could not open database: " + e.getMessage(), e);
        }
    }

    /** Opens a fresh in-memory database, used by the tests. */
    public static Database inMemory() {
        return new Database("jdbc:sqlite::memory:");
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTables() throws SQLException {
        Statement st = connection.createStatement();
        st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                + "id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, "
                + "password_hash TEXT NOT NULL, role TEXT NOT NULL)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS restaurants ("
                + "id TEXT PRIMARY KEY, name TEXT NOT NULL, open INTEGER NOT NULL, "
                + "x REAL, y REAL, radius REAL, min_order TEXT)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS menu_items ("
                + "id TEXT PRIMARY KEY, restaurant_id TEXT NOT NULL, name TEXT NOT NULL, "
                + "price TEXT NOT NULL, available INTEGER NOT NULL)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS orders ("
                + "id TEXT PRIMARY KEY, customer_id TEXT NOT NULL, restaurant_id TEXT NOT NULL, "
                + "street TEXT, x REAL, y REAL, status TEXT NOT NULL, driver_id TEXT, "
                + "pricing_type TEXT, delivery_fee TEXT, discount TEXT, total TEXT, payment_status TEXT)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS order_items ("
                + "order_id TEXT NOT NULL, menu_item_id TEXT NOT NULL, name TEXT, "
                + "unit_price TEXT, quantity INTEGER)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS drivers ("
                + "id TEXT PRIMARY KEY, name TEXT NOT NULL, x REAL, y REAL, speed REAL, status TEXT)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS notifications ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, message TEXT)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS payments ("
                + "id TEXT PRIMARY KEY, order_id TEXT, method TEXT, amount TEXT, change_given TEXT, "
                + "status TEXT)");
        st.close();
    }
}
