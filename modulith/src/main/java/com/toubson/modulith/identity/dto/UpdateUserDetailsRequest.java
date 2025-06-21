package com.toubson.modulith.identity.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDetailsRequest {
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;
}