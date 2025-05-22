package com.toubson.modulith.user.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
public class User {

    @Id
    private UUID id = UUID.randomUUID();

    private String username;
    private String email;
    private Instant createdAt = Instant.now();

}
