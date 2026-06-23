package com.example.app;



  public class Client {

    private int id;
    private String username;
    private String password;
    private double balance;

    public Client(int id, String username, String password, double balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public double getBalance() {
        return balance;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", balance=" + balance +
                '}';
    }
}
  public static void main(Strings, args[]){
     public static void main(String[] args) {
        Client client = new Client(
                1,
                "john123",
                "password123",
                5000.0
        );

        System.out.println(client);
    }
    
  }
  

