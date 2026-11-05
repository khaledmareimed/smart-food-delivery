package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests US1.2 and US1.3 using a mocked {@link RestaurantRepository}. */
class RestaurantServiceTest {

    private RestaurantRepository repository;
    private RestaurantService service;

    @BeforeEach
    void setUp() {
        repository = mock(RestaurantRepository.class);
        service = new RestaurantService(repository);
    }

    @Test
    void onlyOpenRestaurantsAreReturned() {
        Restaurant open1 = new Restaurant("r1", "Open 1", true);
        Restaurant closed = new Restaurant("r2", "Closed", false);
        Restaurant open2 = new Restaurant("r3", "Open 2", true);
        when(repository.findAll()).thenReturn(Arrays.asList(open1, closed, open2));

        List<Restaurant> result = service.getOpenRestaurants();

        assertEquals(Arrays.asList(open1, open2), result);
        assertFalse(result.contains(closed));
    }

    @Test
    void noRestaurantsWhenAllClosed() {
        when(repository.findAll()).thenReturn(
                Collections.singletonList(new Restaurant("r1", "Closed", false)));
        assertTrue(service.getOpenRestaurants().isEmpty());
    }

    @Test
    void menuShowsOnlyAvailableItemsWithCorrectPrices() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        r.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        r.addMenuItem(new MenuItem("m2", "Sold out", new BigDecimal("40.00"), false));
        r.addMenuItem(new MenuItem("m3", "Pepperoni", new BigDecimal("35.50"), true));
        when(repository.findById("r1")).thenReturn(Optional.of(r));

        List<MenuItem> menu = service.getAvailableMenu("r1");

        assertEquals(2, menu.size());
        assertEquals(new BigDecimal("30.00"), menu.get(0).getPrice());
        assertEquals(new BigDecimal("35.50"), menu.get(1).getPrice());
        assertTrue(menu.stream().noneMatch(i -> i.getId().equals("m2")));
    }

    @Test
    void unknownRestaurantThrows() {
        when(repository.findById("nope")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getAvailableMenu("nope"));
    }

    @Test
    void managerCanCloseRestaurantAndHideItem() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        r.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30"), true));
        when(repository.findById("r1")).thenReturn(Optional.of(r));

        service.setRestaurantOpen("r1", false);
        service.setMenuItemAvailable("r1", "m1", false);

        assertFalse(r.isOpen());
        assertFalse(r.getMenu().get(0).isAvailable());
        verify(repository, times(2)).save(r);
    }
}
