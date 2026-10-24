package com.carventure.authservice.service;

import com.carventure.authservice.dto.UserRolesResponseDTO;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.entity.User;
import com.carventure.authservice.exception.InvalidRoleAssignmentException;
import com.carventure.authservice.exception.ResourceNotFoundException;
import com.carventure.authservice.repository.UserRepository;
import com.carventure.authservice.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private User customerUser() {
        return User.builder()
                .username("jane.doe")
                .password("hashed")
                .roles(new HashSet<>(Set.of(Role.CUSTOMER)))
                .build();
    }

    @Test
    void addRole_grantsSeller_toExistingCustomer_bothAllowedTogether() {
        User user = customerUser();
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRolesResponseDTO result = roleService.addRole("jane.doe", Role.SELLER);

        assertThat(result.getRoles()).containsExactlyInAnyOrder("CUSTOMER", "SELLER");
    }

    @Test
    void addRole_grantsAdmin_toUserWithNoOtherRoles() {
        User user = User.builder()
                .username("new.admin")
                .password("hashed")
                .roles(new HashSet<>())
                .build();

        when(userRepository.findByUsername("new.admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRolesResponseDTO result = roleService.addRole("new.admin", Role.ADMIN);

        assertThat(result.getRoles()).containsExactly("ADMIN");
    }

    @Test
    void addRole_grantingAdmin_removesCustomerRole() {
        User user = customerUser(); // has CUSTOMER
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRolesResponseDTO result = roleService.addRole("jane.doe", Role.ADMIN);

        assertThat(result.getRoles()).containsExactly("ADMIN");
    }

    @Test
    void addRole_grantingAdmin_removesSellerRole() {
        User user = User.builder()
                .username("seller.sam")
                .password("hashed")
                .roles(new HashSet<>(Set.of(Role.SELLER)))
                .build();
        when(userRepository.findByUsername("seller.sam")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRolesResponseDTO result = roleService.addRole("seller.sam", Role.ADMIN);

        assertThat(result.getRoles()).containsExactly("ADMIN");
    }

    @Test
    void addRole_grantingCustomer_removesAdminRole() {
        User admin = User.builder()
                .username("admin.smith")
                .password("hashed")
                .roles(new HashSet<>(Set.of(Role.ADMIN)))
                .build();
        when(userRepository.findByUsername("admin.smith")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRolesResponseDTO result = roleService.addRole("admin.smith", Role.CUSTOMER);

        assertThat(result.getRoles()).containsExactly("CUSTOMER");
    }

    @Test
    void addRole_grantingSeller_removesAdminRole() {
        User admin = User.builder()
                .username("admin.smith")
                .password("hashed")
                .roles(new HashSet<>(Set.of(Role.ADMIN)))
                .build();
        when(userRepository.findByUsername("admin.smith")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRolesResponseDTO result = roleService.addRole("admin.smith", Role.SELLER);

        assertThat(result.getRoles()).containsExactly("SELLER");
    }

    @Test
    void addRole_rejectsDuplicateRoleGrant() {
        User user = customerUser(); // already has CUSTOMER
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> roleService.addRole("jane.doe", Role.CUSTOMER))
                .isInstanceOf(InvalidRoleAssignmentException.class)
                .hasMessageContaining("already has");

        verify(userRepository, never()).save(any());
    }

    @Test
    void addRole_throwsNotFound_whenUserMissing() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.addRole("nonexistent", Role.SELLER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRoles_returnsCurrentRoles() {
        User user = customerUser();
        when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(user));

        UserRolesResponseDTO result = roleService.getRoles("jane.doe");

        assertThat(result.getRoles()).containsExactly("CUSTOMER");
    }

    @Test
    void getRoles_throwsNotFound_whenUserMissing() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoles("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
