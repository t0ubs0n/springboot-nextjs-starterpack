package com.toubson.modulith.catalog.application;

import com.toubson.modulith.user.application.UserCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class CatalogListener {

    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("New user registered: " + event.username());
    }
}
