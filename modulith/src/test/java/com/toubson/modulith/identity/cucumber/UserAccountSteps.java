package com.toubson.modulith.identity.cucumber;

import com.toubson.modulith.identity.application.UserService;
import com.toubson.modulith.identity.domain.User;
import com.toubson.modulith.identity.dto.RegistrationRequest;
import com.toubson.modulith.identity.dto.UpdateUserDetailsRequest;
import com.toubson.modulith.identity.infrastructure.UserRepository;
import com.toubson.modulith.notification.api.EmailService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class UserAccountSteps {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ApplicationEventPublisher publisher;
    private User registeredUser;
    private String verificationToken;
    private String resetToken;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Before
    public void setUp() {
        // Clear the repository
        userRepository.deleteAll();

        // Reset security context
        SecurityContextHolder.clearContext();

        // Reset mocks
        reset(emailService);
        reset(publisher);
    }

    @Given("the database is clean")
    public void theDatabaseIsClean() {
        userRepository.deleteAll();
    }

    @When("a user registers with username {string}, email {string}, and password {string}")
    public void aUserRegistersWithUsernameEmailAndPassword(String username, String email, String password) {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setPasswordConfirmation(password);
        request.setFirstName("Test");
        request.setLastName("User");

        registeredUser = userService.registerUser(request);
        verificationToken = registeredUser.getVerificationToken();
    }

    @Given("a user has registered with username {string}, email {string}, and password {string}")
    public void aUserHasRegisteredWithUsernameEmailAndPassword(String username, String email, String password) {
        aUserRegistersWithUsernameEmailAndPassword(username, email, password);
    }

    @Given("a user exists with username {string}, email {string}, and password {string}")
    public void aUserExistsWithUsernameEmailAndPassword(String username, String email, String password) {
        // Create a user that is already verified and enabled
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setPasswordConfirmation(password);
        request.setFirstName("Test");
        request.setLastName("User");

        registeredUser = userService.registerUser(request);
        verificationToken = registeredUser.getVerificationToken();

        // Verify the user
        registeredUser = userService.verifyEmail(verificationToken);
    }

    @Then("the user should be created with username {string}")
    public void theUserShouldBeCreatedWithUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        assertTrue(user.isPresent());
        assertEquals(username, user.get().getUsername());
    }

    @Then("the user should not be enabled")
    public void theUserShouldNotBeEnabled() {
        assertFalse(registeredUser.isEnabled());
    }

    @Then("a verification email should be sent to {string}")
    public void aVerificationEmailShouldBeSentTo(String email) {
        verify(emailService, times(1)).sendVerificationEmail(eq(email), anyString());
    }

    @When("the user verifies their email with the verification token")
    public void theUserVerifiesTheirEmailWithTheVerificationToken() {
        registeredUser = userService.verifyEmail(verificationToken);
    }

    @Then("the user should be enabled")
    public void theUserShouldBeEnabled() {
        assertTrue(registeredUser.isEnabled());
    }

    @Then("the user's email should be verified")
    public void theUserSEmailShouldBeVerified() {
        assertTrue(registeredUser.isEmailVerified());
    }

    @When("the user requests a password reset for email {string}")
    public void theUserRequestsAPasswordResetForEmail(String email) {
        userService.initiatePasswordReset(email);

        // Get the reset token
        Optional<User> user = userRepository.findByEmail(email);
        assertTrue(user.isPresent());
        resetToken = user.get().getResetPasswordToken();
    }

    @Then("a password reset email should be sent to {string}")
    public void aPasswordResetEmailShouldBeSentTo(String email) {
        verify(emailService, times(1)).sendPasswordResetEmail(eq(email), anyString());
    }

    @When("the user resets their password with the reset token and new password {string}")
    public void theUserResetsTheirPasswordWithTheResetTokenAndNewPassword(String newPassword) {
        registeredUser = userService.resetPassword(resetToken, newPassword, newPassword);
    }

    @Then("the user should be able to login with username {string} and password {string}")
    public void theUserShouldBeAbleToLoginWithUsernameAndPassword(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        assertTrue(user.isPresent());
        assertTrue(passwordEncoder.matches(password, user.get().getPassword()));

        // Create authentication
        Authentication auth = new UsernamePasswordAuthenticationToken(user.get(), null, user.get().getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Verify authentication
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
    }

    @When("the user updates their details with first name {string} and last name {string}")
    public void theUserUpdatesTheirDetailsWithFirstNameAndLastName(String firstName, String lastName) {
        UpdateUserDetailsRequest request = new UpdateUserDetailsRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);

        registeredUser = userService.updateUserDetails(registeredUser, request);
    }

    @Then("the user's first name should be {string} and last name should be {string}")
    public void theUserSFirstNameShouldBeAndLastNameShouldBe(String firstName, String lastName) {
        assertEquals(firstName, registeredUser.getFirstName());
        assertEquals(lastName, registeredUser.getLastName());

        // Verify in database
        Optional<User> user = userRepository.findByUsername(registeredUser.getUsername());
        assertTrue(user.isPresent());
        assertEquals(firstName, user.get().getFirstName());
        assertEquals(lastName, user.get().getLastName());
    }

    @When("the user updates their password from {string} to {string}")
    public void theUserUpdatesTheirPasswordFromTo(String oldPassword, String newPassword) {
        registeredUser = userService.updatePassword(registeredUser, oldPassword, newPassword, newPassword);
    }
}
