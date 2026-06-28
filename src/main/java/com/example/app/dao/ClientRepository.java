package com.example.app.dao;
import com.example.app.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClientRepository extends JpaRepository<Client,String>{
    
}
