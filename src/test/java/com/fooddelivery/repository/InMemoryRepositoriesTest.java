package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryRepositoriesTest {

    @Test
    void userCanBeSavedAndFoundByUsername() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        repo.save(new User("u1", "alice", "h", Role.CUSTOMER));
        assertTrue(repo.findByUsername("alice").isPresent());
        assertFalse(repo.findByUsername("nobody").isPresent());
    }

    @Test
    void restaurantCanBeSavedAndFoundById() {
        InMemoryRestaurantRepository repo = new InMemoryRestaurantRepository();
        repo.save(new Restaurant("r1", "A", true));
        assertTrue(repo.findById("r1").isPresent());
        assertFalse(repo.findById("zzz").isPresent());
    }

    @Test
    void findAllKeepsInsertionOrder() {
        InMemoryRestaurantRepository repo = new InMemoryRestaurantRepository();
        repo.save(new Restaurant("r1", "A", true));
        repo.save(new Restaurant("r2", "B", false));
        repo.save(new Restaurant("r3", "C", true));
        List<Restaurant> all = repo.findAll();
        assertEquals("r1", all.get(0).getId());
        assertEquals("r2", all.get(1).getId());
        assertEquals("r3", all.get(2).getId());
    }
}
