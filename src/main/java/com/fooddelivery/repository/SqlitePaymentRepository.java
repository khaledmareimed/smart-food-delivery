package com.fooddelivery.repository;

import com.fooddelivery.domain.Payment;
import com.fooddelivery.persistence.Database;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlitePaymentRepository implements PaymentRepository {

    private Database db;

    public SqlitePaymentRepository(Database db) {
        this.db = db;
    }

    @Override
    public void save(Payment p) {
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO payments(id, order_id, method, amount, change_given, status) "
                            + "VALUES (?,?,?,?,?,?)");
            ps.setString(1, p.getId());
            ps.setString(2, p.getOrderId());
            ps.setString(3, p.getMethod());
            ps.setString(4, p.getAmount() == null ? null : p.getAmount().toPlainString());
            ps.setString(5, p.getChange() == null ? null : p.getChange().toPlainString());
            ps.setString(6, p.getStatus());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Payment> findByOrderId(String orderId) {
        List<Payment> result = new ArrayList<>();
        try {
            PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT * FROM payments WHERE order_id = ? ORDER BY rowid");
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String amount = rs.getString("amount");
                String change = rs.getString("change_given");
                result.add(new Payment(rs.getString("id"), rs.getString("order_id"),
                        rs.getString("method"), amount == null ? null : new BigDecimal(amount),
                        change == null ? null : new BigDecimal(change), rs.getString("status")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public int count() {
        try {
            ResultSet rs = db.getConnection().createStatement()
                    .executeQuery("SELECT count(*) FROM payments");
            rs.next();
            int n = rs.getInt(1);
            rs.close();
            return n;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
