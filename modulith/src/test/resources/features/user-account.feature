Feature: User Account Management

  Background:
    Given the database is clean

  Scenario: User Registration
    When a user registers with username "testuser", email "test@example.com", and password "password"
    Then the user should be created with username "testuser"
    And the user should not be enabled
    And a verification email should be sent to "test@example.com"

  Scenario: Email Verification
    Given a user has registered with username "testuser", email "test@example.com", and password "password"
    When the user verifies their email with the verification token
    Then the user should be enabled
    And the user's email should be verified

  Scenario: Password Reset
    Given a user exists with username "testuser", email "test@example.com", and password "password"
    When the user requests a password reset for email "test@example.com"
    Then a password reset email should be sent to "test@example.com"
    When the user resets their password with the reset token and new password "newpassword"
    Then the user should be able to login with username "testuser" and password "newpassword"

  Scenario: Update User Details
    Given a user exists with username "testuser", email "test@example.com", and password "password"
    When the user updates their details with first name "John" and last name "Doe"
    Then the user's first name should be "John" and last name should be "Doe"

  Scenario: Update Password
    Given a user exists with username "testuser", email "test@example.com", and password "password"
    When the user updates their password from "password" to "newpassword"
    Then the user should be able to login with username "testuser" and password "newpassword"