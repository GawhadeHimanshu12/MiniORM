package com.miniorm;

import java.sql.Connection; 
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.miniorm.example.Order;
import com.miniorm.example.User;
import com.miniorm.session.MiniSession;
import com.miniorm.session.SessionFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MiniOrmTest {

    private static SessionFactory factory;

    @BeforeAll
    static void setup() throws Exception {
        factory = new SessionFactory("jdbc:mysql://localhost:3306/miniorm_db", "root", "NewPassword123");

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/miniorm_db", "root", "NewPassword123");
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("DROP TABLE IF EXISTS users");
        }

        try (MiniSession session = factory.openSession()) {
            session.createTable(User.class);
            session.createTable(Order.class);
        }
    }

    @Test
    @org.junit.jupiter.api.Order(1) 
    void testInsertAndFind() throws Exception {
        System.out.println("TEST 1: Insert Piyush");
        Long piyushId;
        
        try (MiniSession session = factory.openSession()) {
            session.beginTransaction();
            User piyush = new User("Piyush", "piyush@test.com");
            session.save(piyush);
            session.commit();
            
            assertNotNull(piyush.getId(), "Piyush's ID should be generated");
            piyushId = piyush.getId();
        }

        try (MiniSession session = factory.openSession()) {
            User fetched = session.find(User.class, piyushId);
            assertNotNull(fetched);
            assertEquals("Piyush", fetched.getUsername());
            assertEquals("piyush@test.com", fetched.getEmail());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void testUpdate() throws Exception {
        System.out.println("TEST 2: Update Order Amount");
        Long orderId;

        try (MiniSession session = factory.openSession()) {
            session.beginTransaction();
            User piyush = session.find(User.class, 1L);
            
            Order order = new Order(100.0, piyush);
            session.save(order);
            session.commit();
            orderId = order.getId();
        }

        try (MiniSession session = factory.openSession()) {
            session.beginTransaction();
            Order order = session.find(Order.class, orderId);
            
            order.setAmount(500.0);
            session.update(order);
            session.commit();
        }

        try (MiniSession session = factory.openSession()) {
            Order updatedOrder = session.find(Order.class, orderId);
            assertTrue(updatedOrder.toString().contains("500.0"), "Order amount should be updated to 500.0");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3) 
    void testRelationshipMapping() throws Exception {
        System.out.println("TEST 3: Check User inside Order");
        try (MiniSession session = factory.openSession()) {
            Order order = session.find(Order.class, 1L);
            
            assertNotNull(order.getUser(), "User object should be loaded");
            assertEquals("Piyush", order.getUser().getUsername(), "The user inside order should be Piyush");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4) 
    void testFirstLevelCache() throws Exception {
        System.out.println("TEST 4: First Level Cache");
        try (MiniSession session = factory.openSession()) {
            User u1 = session.find(User.class, 1L);
            User u2 = session.find(User.class, 1L);
            
            assertSame(u1, u2, "Both references should point to the exact same memory object (Cache Hit)");
        }
    }
    
    @Test
    @org.junit.jupiter.api.Order(5) 
    void testDelete() throws Exception {
        System.out.println("TEST 5: Delete Operation");
        try (MiniSession session = factory.openSession()) {
            session.beginTransaction();
            User userToDelete = session.find(User.class, 1L); 
            Order orderToDelete = session.find(Order.class, 1L);
            if(orderToDelete != null) session.delete(orderToDelete);
            
            session.delete(userToDelete);
            session.commit();
        }
        
        try (MiniSession session = factory.openSession()) {
            User deletedUser = session.find(User.class, 1L);
            assertNull(deletedUser, "Piyush should be gone from the database");
        }
    }
}