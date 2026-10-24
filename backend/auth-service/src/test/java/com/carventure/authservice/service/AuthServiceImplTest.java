package com.carventure.authservice.service;

import com.carventure.authservice.dto.AuthResponseDTO;
import com.carventure.authservice.dto.LoginRequestDTO;
import com.carventure.authservice.dto.RefreshTokenRequestDTO;
import com.carventure.authservice.dto.RegisterRequestDTO;
import com.carventure.authservice.entity.RefreshToken;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.entity.User;
import com.carventure.authservice.exception.InvalidCredentialsException;
import com.carventure.authservice.exception.InvalidRefreshTokenException;
import com.carventure.authservice.exception.UserAlreadyExistsException;
import com.carventure.authservice.repository.RefreshTokenRepository;
import com.carventure.authservice.repository.UserRepository;
import com.carventure.authservice.security.JwtUtil;
import com.carventure.authservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequestDTO.builder()
                .username("jane.doe")
                .password("S3curePass!23")
                .role(Role.CUSTOMER)
                .build();

        loginRequest = LoginRequestDTO.builder()
                .username("jane.doe")
                .password("S3curePass!23")
                .build();
    }

    @Test
    void register_createsUserWithRequestedCustomerRole() {
        when(userRepository.existsByUsername("jane.doe")).thenReturn(false);
        when(passwordEncoder.encode("S3curePass!23")).thenReturn("hashed-password");
        when(jwtUtil.generateAccessToken(eq("jane.doe"), any())).thenReturn("access-token");
        when(jwtUtil.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponseDTO result = authService.register(registerRequest);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getUsername()).isEqualTo("jane.doe");
        assertThat(result.getRoles()).containsExactly("CUSTOMER");
        assertThat(result.getRefreshToken()).isNotBlank();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("jane.doe");
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRoles()).containsExactly(Role.CUSTOMER);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_throwsUserAlreadyExists_whenUsernameTaken() {
        when(userRepository.existsByUsername("jane.doe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("jane.doe");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsAllOfAUsersRoles_whenCredentialsValid() {
        User storedUser = User.builder()
                .username("jane.doe")
                .password("hashed-password")
                .roles(Set.of(Role.CUSTOMER, Role.SELLER))
                .build();

        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("S3curePass!23", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(eq("jane.doe"), any())).thenReturn("access-token");
        when(jwtUtil.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponseDTO result = authService.login(loginRequest);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRoles()).containsExactlyInAnyOrder("CUSTOMER", "SELLER");
    }

    @Test
    void login_throwsInvalidCredentials_whenUserNotFound() {
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordWrong() {
        User storedUser = User.builder()
                .username("jane.doe")
                .password("hashed-password")
                .roles(Set.of(Role.CUSTOMER))
                .build();

        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("S3curePass!23", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshToken_issuesNewTokens_whenTokenValidAndNotExpired() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .username("jane.doe")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        User storedUser = User.builder()
                .username("jane.doe")
                .password("hashed-password")
                .roles(Set.of(Role.CUSTOMER))
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(storedToken));
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(storedUser));
        when(jwtUtil.generateAccessToken(eq("jane.doe"), any())).thenReturn("new-access-token");
        when(jwtUtil.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponseDTO result = authService.refreshToken(
                RefreshTokenRequestDTO.builder().refreshToken("valid-refresh-token").build());

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository).delete(storedToken);
    }

    @Test
    void refreshToken_throwsInvalidRefreshToken_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("bogus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(
                RefreshTokenRequestDTO.builder().refreshToken("bogus").build()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshToken_throwsInvalidRefreshToken_whenTokenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(2L)
                .token("expired-token")
                .username("jane.doe")
                .expiryDate(Instant.now().minusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshToken(
                RefreshTokenRequestDTO.builder().refreshToken("expired-token").build()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expiredToken);
        verify(userRepository, never()).findByUsername(anyString());
    }
}
