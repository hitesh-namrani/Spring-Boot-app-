package com.example.app.dao;

// Import the Client entity class

import com.example.app.entity.Client;

// Import JpaRepository to provide built-in CRUD operations
import org.springframework.data.jpa.repository.JpaRepository;

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
public interface ClientRepository extends JpaRepository<Client, String> {

}
