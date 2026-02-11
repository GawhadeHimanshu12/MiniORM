package com.miniorm.example;

import com.miniorm.session.MiniSession;
import com.miniorm.session.SessionFactory;

public class DemoApp {
    public static void main(String[] args) {
        SessionFactory factory = new SessionFactory(
            "jdbc:mysql://localhost:3306/miniorm_db", "root", "NewPassword123"
        );

        System.out.println("1. INITIALIZING DATABASE");
        try (MiniSession session = factory.openSession()) {
            session.createTable(User.class);
            session.createTable(Order.class);
        } catch (Exception e) { e.printStackTrace(); }

        Long himanshuId = null;
        Long orderId = null;

        System.out.println("\n2. CREATING DATA");
        try (MiniSession session = factory.openSession()) {
            session.beginTransaction();

            User himanshu = new User("Himanshu", "himanshu@example.com");
            session.save(himanshu);
            himanshuId = himanshu.getId(); 
            System.out.println("Saved: " + himanshu);

            User rohit = new User("Rohit", "rohit@example.com");
            session.save(rohit);
            System.out.println("Saved: " + rohit);

            Order order1 = new Order(1500.00, himanshu);
            session.save(order1);
            orderId = order1.getId();
            System.out.println("Saved Order for Himanshu: " + order1);

            Order order2 = new Order(2500.00, rohit);
            session.save(order2);
            System.out.println("Saved Order for Rohit: " + order2);

            session.commit();
        } catch (Exception e) { e.printStackTrace(); }

        System.out.println("\n3. READING & CACHING");
        try (MiniSession session = factory.openSession()) {
            System.out.println("Fetching Himanshu (DB Hit)...");
            User user1 = session.find(User.class, himanshuId);
            
            System.out.println("Fetching Himanshu Again (Cache Hit)...");
            User user2 = session.find(User.class, himanshuId);
            
            System.out.println("Are objects exactly the same in memory? " + (user1 == user2));
        } catch (Exception e) { e.printStackTrace(); }


        System.out.println("\n4. UPDATING DATA");
        try (MiniSession session = factory.openSession()) {
            session.beginTransaction();

            Order order = session.find(Order.class, orderId);
            System.out.println("Original Order Amount: " + order);

            order.setAmount(9999.99); 
            session.update(order);
            
            session.commit();
            System.out.println("Updated Order Amount in DB.");
        } catch (Exception e) { e.printStackTrace(); }

        System.out.println("\n5. DELETING DATA ");
        try (MiniSession session = factory.openSession()) {
            session.beginTransaction();
            
            Order orderToDelete = session.find(Order.class, 2L); 
            if (orderToDelete != null) {
                session.delete(orderToDelete);
                System.out.println("Deleted Order ID: " + orderToDelete.getId());
            }

            session.commit();
        } catch (Exception e) { e.printStackTrace(); }

        System.out.println("\n6. VERIFYING RELATIONSHIPS");
        try (MiniSession session = factory.openSession()) {
            Order loadedOrder = session.find(Order.class, orderId);
            
            System.out.println("Fetched Order Amount: " + loadedOrder);
            System.out.println("Who owns this order? " + loadedOrder.getUser().getUsername());
        } catch (Exception e) { e.printStackTrace(); }
    }
}