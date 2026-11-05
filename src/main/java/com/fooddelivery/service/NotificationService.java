package com.fooddelivery.service;

import java.util.List;

/**
 * Sends notifications to users. Mocked in the tests.
 */
public interface NotificationService {

    void send(String userId, String message);

    List<String> getNotifications(String userId);
}
