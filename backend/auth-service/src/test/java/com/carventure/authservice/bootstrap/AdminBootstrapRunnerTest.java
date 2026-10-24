package com.carventure.authservice.bootstrap;

import com.carventure.authservice.entity.Role;
import com.carventure.authservice.entity.User;
import com.carventure.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrapRunner bootstrapRunner;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapUsername", "");
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapPassword", "");
    }

    @Test
    void run_doesNothing_whenEnvVarsNotSet() {
        bootstrapRunner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_doesNothing_whenOnlyUsernameSet() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapUsername", "bootstrap.admin");
        // password left blank

        bootstrapRunner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_createsAdmin_whenBothEnvVarsSetAndNoAdminExists() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapUsername", "bootstrap.admin");
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapPassword", "S3curePass!23");

        when(userRepository.findAll()).thenReturn(List.of()); // no existing users at all
        when(userRepository.existsByUsername("bootstrap.admin")).thenReturn(false);
        when(passwordEncoder.encode("S3curePass!23")).thenReturn("hashed-password");

        bootstrapRunner.run();

        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("bootstrap.admin")
                        && user.getRoles().equals(Set.of(Role.ADMIN))
                        && user.getPassword().equals("hashed-password")));
    }

    @Test
    void run_skipsCreation_whenAnAdminAlreadyExists() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapUsername", "bootstrap.admin");
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapPassword", "S3curePass!23");

        User existingAdmin = User.builder()
                .username("existing.admin")
                .password("already-hashed")
                .roles(Set.of(Role.ADMIN))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(existingAdmin));

        bootstrapRunner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_skipsCreation_whenUsernameAlreadyTakenByNonAdmin() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapUsername", "bootstrap.admin");
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapPassword", "S3curePass!23");

        when(userRepository.findAll()).thenReturn(List.of()); // no admins exist yet
        when(userRepository.existsByUsername("bootstrap.admin")).thenReturn(true); // but username taken

        bootstrapRunner.run();

        verify(userRepository, never()).save(any());
    }
}
