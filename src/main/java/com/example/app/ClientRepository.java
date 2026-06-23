package com.example.app;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClientRepository extends JpaRepository<Client,String>{
    
}
