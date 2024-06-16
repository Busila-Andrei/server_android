package com.example.server_android.auth;

import com.example.server_android.config.JwtService;
import com.example.server_android.test.service.EmailService;
import com.example.server_android.token.Token;
import com.example.server_android.token.TokenRepository;
import com.example.server_android.token.TokenType;
import com.example.server_android.user.Role;
import com.example.server_android.user.User;
import com.example.server_android.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;


    public ApiResponse isUserEnabled(String email) {
        logger.debug("Checking if user is enabled for email: {}", email);
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            logger.warn("No user found with the provided email address: {}", email);
            return new ApiResponse(false, "No user found with the provided email address.");
        }

        User user = userOptional.get();
        if (!user.isEnabled()) {
            logger.info("User with email {} is not enabled", email);
            return new ApiResponse(false, "User is not enabled");
        }

        logger.info("User with email {} is enabled", email);
        return new ApiResponse(true, "User is enabled");
    }


    public ApiResponse register(RegisterRequest request) {
        logger.debug("Registering new user with email: {}", request.getEmail());
        try {
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            return existingUser.map(user -> handleExistingUser(user, request)).orElseGet(() -> handleNewUser(request));
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }


    private ApiResponse handleExistingUser(User user, RegisterRequest request) {
        if (!user.isEnabled()) {
            user.setFirst_name(request.getFirstname());
            user.setLast_name(request.getLastname());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            return resendToken(user);
        }
        return new ApiResponse(false, "Email already confirmed. Please log in.");
    }


    private ApiResponse handleNewUser(RegisterRequest request) {
        User newUser = User.builder()
                .first_name(request.getFirstname())
                .last_name(request.getLastname())
                .email(request.getEmail())
                .enabled(false)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .build();
        userRepository.save(newUser);
        return resendToken(newUser);
    }


    private ApiResponse resendToken(User user) {
        String newConfirmationToken = jwtService.generateToken(user);
        saveUserToken(user, newConfirmationToken);
        emailService.sendRegistrationMail(user.getEmail(), newConfirmationToken);
        return new ApiResponse(true, "User registered successfully. Please check your email for confirmation instructions.");
    }


    @Transactional
    public ApiResponse confirmAccount(String token) {
        logger.debug("Confirming account with token: {}", token);
        try {
            Optional<Token> tokenOpt = tokenRepository.findByToken(token);
            if (tokenOpt.isEmpty() || tokenOpt.get().isExpired() || tokenOpt.get().isDisable()) {
                logger.warn("Invalid or expired token used for confirmation: {}", token);
                return new ApiResponse(false, "Invalid or expired token.");
            }

            return confirmUser(tokenOpt.get());
        } catch (Exception e) {
            logger.error("Account confirmation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Account confirmation failed: " + e.getMessage());
        }
    }


    private ApiResponse confirmUser(Token token) {
        User user = token.getUser();
        if (user.isEnabled()) {
            logger.info("Account already confirmed for user: {}", user.getEmail());
            return new ApiResponse(false, "Account already confirmed.");
        }

        user.setEnabled(true);
        userRepository.save(user);
        revokeAllUserTokens(user);
        logger.info("User email confirmed and account enabled for user: {}", user.getEmail());
        return new ApiResponse(true, "Your email is confirmed. All existing tokens were revoked. Thank you for using our service!");
    }


    private void saveUserToken(User user, String token) {
        logger.debug("Saving token for user: {}", user.getEmail());
        try {
            var tokenRecord = Token.builder()
                    .user(user)
                    .token(token)
                    .tokenType(TokenType.BEARER)
                    .expired(false)
                    .disable(false)
                    .build();
            tokenRepository.save(tokenRecord);
            logger.info("Token saved successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to save token for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to save token: " + e.getMessage());
        }
    }


    private void revokeAllUserTokens(User user) {
        logger.debug("Revoking all tokens for user: {}", user.getEmail());
        try {
            var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
            if (validUserTokens.isEmpty()) {
                logger.info("No valid tokens found to revoke for user: {}", user.getEmail());
                return;
            }
            validUserTokens.forEach(token -> {
                token.setExpired(true);
                token.setDisable(true);
            });
            tokenRepository.saveAll(validUserTokens);
            logger.info("All tokens revoked for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to revoke tokens for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to revoke tokens: " + e.getMessage());
        }
    }


    public ApiResponse verifyToken(String token) {
        logger.debug("Verifying token: {}", token);
        try {
            if (jwtService.isTokenExpired(token)) {
                logger.warn("Token expired: {}", token);
                return new ApiResponse(false, "Token expired");
            }
            String username = jwtService.getUsernameFromToken(token);
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new IllegalStateException("User not found with email: " + username));
            boolean isValid = jwtService.isTokenValid(token, user);
            logger.info("Token verification result for user {}: {}", username, isValid);
            return new ApiResponse(isValid, "Token verification");
        } catch (Exception e) {
            logger.error("Token verification failed: {}", e.getMessage());
            return new ApiResponse(false, "Token verification failed");
        }
    }


    public ApiResponse resendConfirmationEmail(String request) {
        logger.debug("Resending confirmation email to: {}", request);
        try {
            Optional<User> userOptional = userRepository.findByEmail(request);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                System.out.println(user.getEmail());
                if (!user.isEnabled()) {
                    revokeAllUserTokens(user);
                    String newConfirmationToken = jwtService.generateToken(user);
                    saveUserToken(user, newConfirmationToken);
                    emailService.sendRegistrationMail(user.getEmail(), newConfirmationToken);
                    logger.info("Confirmation email resent to: {}", request);
                    return new ApiResponse(true, "Confirmation email resent. Please check your email.");
                } else {
                    logger.info("Email already confirmed for: {}", request);
                    return new ApiResponse(false, "This email is already confirmed. Please log in.");
                }
            } else {
                logger.warn("No user found with email: {}", request);
                throw new RuntimeException("No user found with the provided email address.");
            }
        } catch (Exception e) {
            logger.error("Failed to resend confirmation email to: {}", request, e);
            throw new RuntimeException("Failed to resend confirmation email: " + e.getMessage());
        }
    }


    public ApiResponse login(LoginRequest request) {
        logger.debug("Attempting to login user with email: {}", request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            logger.info("Authentication successful for user: {}", request.getEmail());
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.error("User not found after authentication for email: {}", request.getEmail());
                    return new RuntimeException("User not found");
                });

        var jwtToken = jwtService.generateToken(user);

        revokeAllUserTokens(user);

        saveUserToken(user, jwtToken);

        logger.info("Login successful, token issued for user: {}", request.getEmail());
        return new ApiResponse(true, "Login success", jwtToken);
    }

    public ApiResponse logout(String tokenRequest) {
        logger.debug("Attempting to logout user with token: {}", tokenRequest);
        try {
            Optional<Token> tokenOpt = tokenRepository.findByToken(tokenRequest);
            if (tokenOpt.isEmpty()) {
                logger.warn("Logout attempt with invalid token: {}", tokenRequest);
                return new ApiResponse(false, "Invalid token.");
            }

            // Setăm tokenul ca expirat și dezactivat
            Token token = tokenOpt.get();
            token.setExpired(true);
            token.setDisable(true);
            tokenRepository.save(token);

            // De asemenea, dezactivăm utilizatorul asociat
            User user = token.getUser();
            user.setEnabled(false);
            userRepository.save(user);

            logger.info("Logout successful for user: {}", user.getEmail());
            return new ApiResponse(true, "Logout successful");
        } catch (Exception e) {
            // Logăm orice excepție care ar putea apărea în timpul procesului de logout
            logger.error("Logout failed for token: {}: {}", tokenRequest, e.getMessage(), e);
            throw new RuntimeException("Logout process failed: " + e.getMessage());
        }
    }




}
