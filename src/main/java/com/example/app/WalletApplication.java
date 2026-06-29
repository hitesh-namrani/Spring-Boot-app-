package com.example.app;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/*
Main entry point for the Wallet Spring Boot application.
This class bootstraps and launches the application.
*/

@SpringBootApplication
public class WalletApplication{
  /*
  Starts the Spring Boot application.
  @param args command-line arguments passed during application startup
  */
  
  public static void main(String[]  args){
    SpringApplication.run(WalletApplication.class,args);
  }
}


