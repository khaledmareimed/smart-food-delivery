package com.fooddelivery.domain;

/**
 * Roles a {@link User} can have in the system.
 */
public enum Role {
    /** A customer who browses restaurants and places orders. */
    CUSTOMER,
    /** A restaurant manager. */
    MANAGER,
    /** A delivery driver; the user id is the same as the driver id. */
    DRIVER
}
