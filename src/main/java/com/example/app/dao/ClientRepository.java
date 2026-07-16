package com.example.app.dao;

// Import the Client entity class

import com.example.app.entity.Client;

// Import JpaRepository to provide built-in CRUD operations
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/*
Repository interface for Client entity.

Extends JpaRepository to inherit standard database operations such as:
- save()
- findById()
- findAll()
- deleteById()
- count()

No implementation is required because Spring Data JPA
automatically generates it at runtime.

Client  -> Entity class
String  -> Data type of the primary key (ID)
*/
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByUsername(String username);

    // Used only inside processTransaction — acquires a row-level write lock
    // to prevent concurrent transactions from racing on the same client's balance.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Client c WHERE c.username = :username")
    Optional<Client> findByUsernameForUpdate(String username);
}
