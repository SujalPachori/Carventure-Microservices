package com.carventure.usermanagement.repository;

import com.carventure.usermanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CHANGED: the second type argument was String, but User's @Id is
 * Integer userId. That mismatch is what broke every findById /
 * existsById / deleteById call in UserServiceImpl.
 *
 * userId stays the primary key. Lookups happen by username, because
 * that is what the JWT subject carries and what the other services
 * pass around — hence the derived ...ByUsername methods below.
 */
@Repository
public interface UserManagementRepository extends JpaRepository<User, Integer> {

    List<User> findAllByUsernameIn(List<String> usernames);

    // ADDED: username lookups, replacing the misused findById family.
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByUsername(String username);
}
