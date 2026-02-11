package com.miniorm.example;

import com.miniorm.annotations.Column;
import com.miniorm.annotations.Entity;
import com.miniorm.annotations.Id;
import com.miniorm.annotations.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    public User() {}
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
    
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}