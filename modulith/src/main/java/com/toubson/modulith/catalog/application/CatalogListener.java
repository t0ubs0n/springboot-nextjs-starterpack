package com.toubson.modulith.catalog.application;

import com.toubson.modulith.shared.events.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CatalogListener {

    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.debug("New user registered: {}", event.username());
    }
}
