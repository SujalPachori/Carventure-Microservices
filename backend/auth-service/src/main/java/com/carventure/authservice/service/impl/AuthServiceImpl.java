package com.carventure.authservice.service.impl;

import com.carventure.authservice.client.UserManagementClient;
import com.carventure.authservice.dto.*;
import com.carventure.authservice.entity.RefreshToken;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.entity.User;
import com.carventure.authservice.exception.InvalidCredentialsException;
import com.carventure.authservice.exception.InvalidRefreshTokenException;
import com.carventure.authservice.exception.InvalidRoleAssignmentException;
import com.carventure.authservice.exception.UserAlreadyExistsException;
import com.carventure.authservice.repository.RefreshTokenRepository;
import com.carventure.authservice.repository.UserRepository;
import com.carventure.authservice.security.JwtUtil;
import com.carventure.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final long REFRESH_TOKEN_VALIDITY_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserManagementClient userManagementClient;

    @Override
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        }
        if (request.getRole() != Role.CUSTOMER && request.getRole() != Role.SELLER) {
            throw new InvalidRoleAssignmentException(
                    "Self-registration only allows CUSTOMER or SELLER, got: " + request.getRole());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(request.getRole()))
                .build();

        userRepository.save(user);
        UserManagementUserDTO profile = new UserManagementUserDTO();

        profile.setUsername(request.getUsername());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmail(request.getEmail());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setAddress(request.getAddress());
        profile.setPincode(request.getPincode());
        profile.setDateOfBirth(request.getDateOfBirth());

        if (request.getRole() == Role.CUSTOMER) {
            userManagementClient.registerCustomer(profile);
        } else if (request.getRole() == Role.SELLER) {
            userManagementClient.registerSeller(profile);
        }

        return issueTokens(user.getUsername(), user.getRoles());
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return issueTokens(user.getUsername(), user.getRoles());
    }

    @Override
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidRefreshTokenException("Refresh token has expired, please log in again");
        }

        User user = userRepository.findByUsername(storedToken.getUsername())
                .orElseThrow(() -> new InvalidRefreshTokenException("User associated with token no longer exists"));

        // Rotate: invalidate old refresh token, issue a fresh pair
        refreshTokenRepository.delete(storedToken);

        return issueTokens(user.getUsername(), user.getRoles());
    }

    private AuthResponseDTO issueTokens(String username, Set<Role> roles) {
        Set<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toSet());
        String accessToken = jwtUtil.generateAccessToken(username, roleNames);

        // One active refresh token per user
        refreshTokenRepository.deleteByUsername(username);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .username(username)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_VALIDITY_MS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .username(username)
                .roles(roleNames)
                .expiresInSeconds(jwtUtil.getAccessTokenExpirationMs() / 1000)
                .build();
    }
}
