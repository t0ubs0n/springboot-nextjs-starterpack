package com.toubson.modulith.shared.events;

/**
 * Event published when a new user is created.
 * This event is used for communication between modules.
 */
public record UserCreatedEvent(String username, String email) {
    /**
     * Constructor with only username for backward compatibility
     */
    public UserCreatedEvent(String username) {
        this(username, null);
    }
}