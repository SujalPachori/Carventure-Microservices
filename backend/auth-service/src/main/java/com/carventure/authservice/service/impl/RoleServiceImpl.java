package com.carventure.authservice.service.impl;

import com.carventure.authservice.dto.UserRolesResponseDTO;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.entity.User;
import com.carventure.authservice.exception.InvalidRoleAssignmentException;
import com.carventure.authservice.exception.ResourceNotFoundException;
import com.carventure.authservice.repository.UserRepository;
import com.carventure.authservice.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final UserRepository userRepository;

    @Override
    public UserRolesResponseDTO addRole(String username, Role roleToAdd) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (user.getRoles().contains(roleToAdd)) {
            throw new InvalidRoleAssignmentException(
                    "User " + username + " already has role " + roleToAdd);
        }

        if (roleToAdd == Role.ADMIN) {
            user.getRoles().remove(Role.CUSTOMER);
            user.getRoles().remove(Role.SELLER);
        } else if (roleToAdd == Role.CUSTOMER || roleToAdd == Role.SELLER) {
            user.getRoles().remove(Role.ADMIN);
        }

        user.getRoles().add(roleToAdd);
        userRepository.save(user);

        return toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserRolesResponseDTO getRoles(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUsersByRole(Role role) {
        return userRepository.findByRolesContaining(role).stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
    }

    private UserRolesResponseDTO toDTO(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return UserRolesResponseDTO.builder()
                .username(user.getUsername())
                .roles(roleNames)
                .build();
    }
}
