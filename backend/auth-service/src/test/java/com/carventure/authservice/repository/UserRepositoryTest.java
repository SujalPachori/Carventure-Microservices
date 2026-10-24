package com.carventure.authservice.repository;

import com.carventure.authservice.entity.RefreshToken;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void findByUsername_returnsUser_withCorrectRoles() {
        userRepository.save(User.builder()
                .username("jane.doe")
                .password("hashed")
                .roles(Set.of(Role.CUSTOMER))
                .build());

        Optional<User> result = userRepository.findByUsername("jane.doe");

        assertThat(result).isPresent();
        assertThat(result.get().getRoles()).containsExactly(Role.CUSTOMER);
    }

    @Test
    void findByUsername_persistsMultipleRoles_customerAndSellerTogether() {
        userRepository.save(User.builder()
                .username("dual.role")
                .password("hashed")
                .roles(Set.of(Role.CUSTOMER, Role.SELLER))
                .build());

        Optional<User> result = userRepository.findByUsername("dual.role");

        assertThat(result).isPresent();
        assertThat(result.get().getRoles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.SELLER);
    }

    @Test
    void findByUsername_returnsEmpty_whenNotExists() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void existsByUsername_reflectsPresence() {
        userRepository.save(User.builder()
                .username("jane.doe")
                .password("hashed")
                .roles(Set.of(Role.CUSTOMER))
                .build());

        assertThat(userRepository.existsByUsername("jane.doe")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    void refreshToken_findByToken_andDeleteByUsername_work() {
        refreshTokenRepository.save(RefreshToken.builder()
                .token("token-1")
                .username("jane.doe")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .token("token-2")
                .username("jane.doe")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build());

        assertThat(refreshTokenRepository.findByToken("token-1")).isPresent();

        refreshTokenRepository.deleteByUsername("jane.doe");

        assertThat(refreshTokenRepository.findByToken("token-1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("token-2")).isEmpty();
    }
}
