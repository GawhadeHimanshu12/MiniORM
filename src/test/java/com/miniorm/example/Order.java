package com.miniorm.example;

import com.miniorm.annotations.Column;
import com.miniorm.annotations.Entity;
import com.miniorm.annotations.Id;
import com.miniorm.annotations.JoinColumn;
import com.miniorm.annotations.ManyToOne;
import com.miniorm.annotations.Table;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private Long id;

    @Column(name = "amount")
    private Double amount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Order() {}
    public Order(Double amount, User user) {
        this.amount = amount;
        this.user = user;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    @Override
    public String toString() {
        return "Order{id=" + id + ", amount=" + amount + ", user=" + user + "}";
    }
}