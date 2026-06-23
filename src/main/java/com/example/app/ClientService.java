package com.example.app;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.services;
@Services
  public class ClientService{

  @Autowired private clientRepository;
  public Client Registerclient(String username,String rawPassword){
    if(repository.findByl
       d(username).isPresent())
      {
      throw new
        RuntimeException("Username is already taken!");
    }
    Client client = new client(username,rawPassword);
    return repository.save(client);
}
  public Client verifyLogin(String username, String password){
    Client client = repository.findBy(username).orElseThrow(() ->new Runtime Expection ("client not found!");
  }
return client;
}
publi Client
  processTransactoion(String username,string password,Double amount{

Client client = verifyLogin (username,password);
double newBalance = client.getBalance()+amount;
if(newBalance<0){
  throw new 
    RuntimeException("Insufficient funds!
                     ");
}
  client.setBalance(newBalance);
  return repository.save(client);
  }}
