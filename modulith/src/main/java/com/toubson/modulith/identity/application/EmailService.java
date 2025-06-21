package com.toubson.modulith.identity.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

//@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    //TODO Voir s'il n'existe pas de meilleure solution
    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@example.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String token) {
        log.debug("Preparing verification email for: {}", to);

        String subject = "Email Verification";
        String verificationUrl = baseUrl + "/verify-email?token=" + token;
        log.debug("Generated verification URL: {}", verificationUrl);

        String content = """
                <html>
                <body>
                <h2>Email Verification</h2>
                <p>Please click the link below to verify your email address:</p>
                <p><a href="%s">Verify Email</a></p>
                <p>If you did not create an account, please ignore this email.</p>
                </body>
                </html>
                """.formatted(verificationUrl);

        log.debug("Sending verification email to: {}", to);
        sendHtmlEmail(to, subject, content);
    }

    public void sendPasswordResetEmail(String to, String token) {
        log.debug("Preparing password reset email for: {}", to);

        String subject = "Password Reset";
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        log.debug("Generated password reset URL: {}", resetUrl);

        String content = """
                <html>
                <body>
                <h2>Password Reset</h2>
                <p>Please click the link below to reset your password:</p>
                <p><a href="%s">Reset Password</a></p>
                <p>If you did not request a password reset, please ignore this email.</p>
                </body>
                </html>
                """.formatted(resetUrl);

        log.debug("Sending password reset email to: {}", to);
        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        log.debug("Creating email message with subject: '{}' for recipient: {}", subject, to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            log.debug("Setting email parameters: from={}, to={}", fromEmail, to);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.debug("Attempting to send email to: {}", to);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {} - Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
