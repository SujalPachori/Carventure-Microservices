package com.carventure.authservice.controller;

import com.carventure.authservice.config.SecurityConfig;
import com.carventure.authservice.dto.RoleAssignmentRequestDTO;
import com.carventure.authservice.dto.UserRolesResponseDTO;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.exception.InvalidRoleAssignmentException;
import com.carventure.authservice.exception.ResourceNotFoundException;
import com.carventure.authservice.security.CustomUserDetailsService;
import com.carventure.authservice.security.JwtAuthenticationFilter;
import com.carventure.authservice.security.JwtUtil;
import com.carventure.authservice.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unlike AuthControllerTest, this imports the REAL SecurityConfig
 * (including @EnableMethodSecurity) rather than only mocking its
 * collaborators - the whole point of these tests is to prove the
 * admin-only enforcement actually works at the HTTP layer, not just that
 * the controller method contains a @PreAuthorize annotation.
 */
@WebMvcTest(RoleController.class)
@Import(SecurityConfig.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(username = "admin.smith", roles = "ADMIN")
    void addRole_returns200_forAdminCaller() throws Exception {
        when(roleService.addRole(eq("jane.doe"), eq(Role.SELLER))).thenReturn(
                UserRolesResponseDTO.builder().username("jane.doe").roles(Set.of("CUSTOMER", "SELLER")).build());

        mockMvc.perform(post("/api/auth/admin/users/{username}/roles", "jane.doe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RoleAssignmentRequestDTO.builder().role(Role.SELLER).build()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2));
    }

    @Test
    @WithMockUser(username = "jane.doe", roles = "CUSTOMER")
    void addRole_returns403_forNonAdminCaller() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users/{username}/roles", "jane.doe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RoleAssignmentRequestDTO.builder().role(Role.SELLER).build()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void addRole_returns401_forUnauthenticatedCaller() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users/{username}/roles", "jane.doe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RoleAssignmentRequestDTO.builder().role(Role.SELLER).build())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin.smith", roles = "ADMIN")
    void addRole_returns409_whenViolatesExclusivityRule() throws Exception {
        when(roleService.addRole(eq("jane.doe"), eq(Role.ADMIN)))
                .thenThrow(new InvalidRoleAssignmentException(
                        "Cannot grant ADMIN to jane.doe: user already has CUSTOMER and/or SELLER."));

        mockMvc.perform(post("/api/auth/admin/users/{username}/roles", "jane.doe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RoleAssignmentRequestDTO.builder().role(Role.ADMIN).build()))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin.smith", roles = "ADMIN")
    void addRole_returns404_whenUserMissing() throws Exception {
        when(roleService.addRole(eq("nonexistent"), any()))
                .thenThrow(new ResourceNotFoundException("User not found with username: nonexistent"));

        mockMvc.perform(post("/api/auth/admin/users/{username}/roles", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RoleAssignmentRequestDTO.builder().role(Role.SELLER).build()))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin.smith", roles = "ADMIN")
    void addRole_returns400_whenRoleMissing() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users/{username}/roles", "jane.doe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin.smith", roles = "ADMIN")
    void getRoles_returns200_forAdminCaller() throws Exception {
        when(roleService.getRoles("jane.doe")).thenReturn(
                UserRolesResponseDTO.builder().username("jane.doe").roles(Set.of("CUSTOMER")).build());

        mockMvc.perform(get("/api/auth/admin/users/{username}/roles", "jane.doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jane.doe"));
    }

    @Test
    @WithMockUser(username = "jane.doe", roles = "SELLER")
    void getRoles_returns403_forNonAdminCaller() throws Exception {
        mockMvc.perform(get("/api/auth/admin/users/{username}/roles", "jane.doe"))
                .andExpect(status().isForbidden());
    }
}
