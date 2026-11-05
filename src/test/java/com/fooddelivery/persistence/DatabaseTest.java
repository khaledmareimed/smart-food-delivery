package com.fooddelivery.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class DatabaseTest {

    @Test
    void tablesAreCreated() throws SQLException {
        Database db = Database.inMemory();
        ResultSet rs = db.getConnection().createStatement()
                .executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table'");
        rs.next();
        assertTrue(rs.getInt(1) >= 5);
        db.close();
        assertTrue(db.getConnection().isClosed());
    }

    @Test
    void badUrlThrows() {
        assertThrows(RuntimeException.class, () -> new Database("jdbc:nothing:xyz"));
    }
}
