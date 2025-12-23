package com.sidequest.identity.domain;

import java.util.Optional;

public interface UserRepository {
    void save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
}

