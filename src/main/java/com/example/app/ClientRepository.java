package com.example.app;
import org.springframework.data.jpa.respository.JpaRespositry;
public class ClientRepository {
  public interface ClientRespository extends JpaRespositry<Client,String>{
    
  }
}
