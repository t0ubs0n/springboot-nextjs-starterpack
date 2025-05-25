package com.toubson.modulith.user.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
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
        String subject = "Email Verification";
        String verificationUrl = baseUrl + "/verify-email?token=" + token;
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

        sendHtmlEmail(to, subject, content);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Password Reset";
        String resetUrl = baseUrl + "/reset-password?token=" + token;
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

        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}