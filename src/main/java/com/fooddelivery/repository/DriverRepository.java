package com.fooddelivery.repository;

import com.fooddelivery.domain.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverRepository {

    void save(Driver driver);

    Optional<Driver> findById(String id);

    List<Driver> findAll();
}
