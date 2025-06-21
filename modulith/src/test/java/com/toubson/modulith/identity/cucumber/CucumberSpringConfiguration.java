package com.toubson.modulith.identity.cucumber;

import com.toubson.modulith.notification.api.EmailService;
import com.toubson.modulith.shared.events.UserCreatedEvent;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EmailService emailService() {
            EmailService mockEmailService = mock(EmailService.class);
            doNothing().when(mockEmailService).sendVerificationEmail(anyString(), anyString());
            doNothing().when(mockEmailService).sendPasswordResetEmail(anyString(), anyString());
            return mockEmailService;
        }

        @Bean
        @Primary
        public ApplicationEventPublisher applicationEventPublisher() {
            ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
            doNothing().when(mockPublisher).publishEvent(any(UserCreatedEvent.class));
            return mockPublisher;
        }
    }
}
