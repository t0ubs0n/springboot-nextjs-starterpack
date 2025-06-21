package com.toubson.modulith.identity.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private final String testEmail = "test@example.com";
    private final String testToken = "test-token-123";
    @Mock
    private JavaMailSender mailSender;
    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Set up properties
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:3000");

        // Mock mail sender to return a real MimeMessage
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_Success() {
        // Act
        emailService.sendVerificationEmail(testEmail, testToken);

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_Success() {
        // Act
        emailService.sendPasswordResetEmail(testEmail, testToken);

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlEmail_MessagingException_ThrowsRuntimeException() throws MessagingException {
        // Arrange
        doThrow(new RuntimeException("Failed to send email")).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                emailService.sendVerificationEmail(testEmail, testToken)
        );

        assertEquals("Failed to send email", exception.getMessage());
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }
}
