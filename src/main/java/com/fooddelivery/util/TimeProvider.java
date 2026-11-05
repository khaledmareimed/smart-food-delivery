package com.fooddelivery.util;

import java.time.LocalDateTime;

/**
 * Gives the current time. Mocked in tests.
 */
public interface TimeProvider {

    LocalDateTime now();
}
