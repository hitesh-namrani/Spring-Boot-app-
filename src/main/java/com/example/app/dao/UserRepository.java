package com.example.app.dao;

// Import the User entity class

import com.example.app.entity.User;

// Import JpaRepository to provide built-in CRUD operations
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/*
Repository interface for User entity.

Extends JpaRepository to inherit standard database operations such as:
- save()
- findById()
- findAll()
- deleteById()
- count()

No implementation is required because Spring Data JPA
automatically generates it at runtime.

User  -> Entity class
Long  -> Data type of the primary key (ID)
*/
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // Used only inside processTransaction — acquires a row-level write lock
    // to prevent concurrent transactions from racing on the same user's balance.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM User c WHERE c.username = :username")
    Optional<User> findByUsernameForUpdate(String username);
}
