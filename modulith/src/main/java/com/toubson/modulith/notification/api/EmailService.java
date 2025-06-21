package com.toubson.modulith.notification.api;

/**
 * Interface for email services.
 * This interface is exposed for use by other modules.
 */
public interface EmailService {

    /**
     * Send a verification email to a user
     *
     * @param to    the email address to send to
     * @param token the verification token
     */
    void sendVerificationEmail(String to, String token);

    /**
     * Send a password reset email to a user
     *
     * @param to    the email address to send to
     * @param token the password reset token
     */
    void sendPasswordResetEmail(String to, String token);
}