package com.carventure.authservice.controller;

import com.carventure.authservice.config.SecurityConfig;
import com.carventure.authservice.dto.AuthResponseDTO;
import com.carventure.authservice.dto.LoginRequestDTO;
import com.carventure.authservice.dto.RefreshTokenRequestDTO;
import com.carventure.authservice.dto.RegisterRequestDTO;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.exception.InvalidCredentialsException;
import com.carventure.authservice.exception.InvalidRefreshTokenException;
import com.carventure.authservice.exception.UserAlreadyExistsException;
import com.carventure.authservice.security.CustomUserDetailsService;
import com.carventure.authservice.security.JwtAuthenticationFilter;
import com.carventure.authservice.security.JwtUtil;
import com.carventure.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * These endpoints are all permitAll() in SecurityConfig, so @WithMockUser
 * is not strictly required, but the JWT filter/UserDetailsService are
 * still constructed as beans in the web layer context and must be mocked.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthResponseDTO sampleResponse() {
        return AuthResponseDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .username("jane.doe")
                .roles(Set.of("CUSTOMER"))
                .expiresInSeconds(900)
                .build();
    }

    @Test
    void register_returns201_withCustomerRole_regardlessOfRequestBody() throws Exception {
        when(authService.register(any())).thenReturn(sampleResponse());

        // No role field exists on RegisterRequestDTO at all - this is the point.
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .username("jane.doe")
                .password("S3curePass!23")
                .role(Role.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("jane.doe"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void register_ignoresUnknownRoleFieldInRawJson_ifSomeoneTriesToSmuggleOneIn() throws Exception {
        // Simulates a client sending {"username":..., "password":..., "role":"ADMIN"}
        // even though the DTO has no such field - Jackson silently drops
        // unmapped properties by default, so this must NOT grant ADMIN.
        when(authService.register(any())).thenReturn(sampleResponse());

        String rawJsonWithSmuggledRole = """
                {"username":"jane.doe","password":"S3curePass!23","role":"ADMIN"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJsonWithSmuggledRole))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    void register_returns400_whenInvalid() throws Exception {
        RegisterRequestDTO invalid = RegisterRequestDTO.builder()
                .username("ab") // too short
                .password("short")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void register_returns409_whenUsernameTaken() throws Exception {
        when(authService.register(any()))
                .thenThrow(new UserAlreadyExistsException("Username already taken: jane.doe"));

        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .username("jane.doe")
                .password("S3curePass!23")
                .role(Role.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_returns200_withUsersRoles() throws Exception {
        when(authService.login(any())).thenReturn(sampleResponse());

        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("jane.doe")
                .password("S3curePass!23")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    void login_returns401_whenCredentialsInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid username or password"));

        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("jane.doe")
                .password("wrong-password")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void refreshToken_returns200_whenTokenValid() throws Exception {
        when(authService.refreshToken(any())).thenReturn(sampleResponse());

        RefreshTokenRequestDTO request = RefreshTokenRequestDTO.builder()
                .refreshToken("valid-refresh-token")
                .build();

        mockMvc.perform(post("/api/auth/refreshToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void refreshToken_returns401_whenTokenInvalid() throws Exception {
        when(authService.refreshToken(any()))
                .thenThrow(new InvalidRefreshTokenException("Refresh token not recognized"));

        RefreshTokenRequestDTO request = RefreshTokenRequestDTO.builder()
                .refreshToken("bogus-token")
                .build();

        mockMvc.perform(post("/api/auth/refreshToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
