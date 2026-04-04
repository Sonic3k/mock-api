package com.mockapi.seeder;

import com.mockapi.entity.*;
import com.mockapi.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final PaymentRepository paymentRepo;

    public DataSeeder(UserRepository userRepo, OrderRepository orderRepo,
                      ProductRepository productRepo, PaymentRepository paymentRepo) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.paymentRepo = paymentRepo;
    }

    @Override
    public void run(String... args) {
        resetAll();
    }

    /** Clears all tables and re-seeds with original data. Callable from /test/reset. */
    public void resetAll() {
        paymentRepo.deleteAll();
        orderRepo.deleteAll();
        productRepo.deleteAll();
        userRepo.deleteAll();
        seedUsers();
        seedProducts();
        seedOrders();
        seedPayments();
        System.out.println("✅ Database seeded successfully");
    }

    private void seedUsers() {
        String[][] data = {
            {"John Doe", "john.doe@example.com", "user", "active", "Engineering", "+1-555-0101", "123 Main St", "New York", "US", "0"},
            {"Jane Smith", "jane.smith@example.com", "admin", "active", "Management", "+1-555-0102", "456 Oak Ave", "Boston", "US", "10"},
            {"Bob Johnson", "bob.johnson@example.com", "user", "active", "Sales", "+1-555-0103", "789 Pine Rd", "Chicago", "US", "5"},
            {"Alice Brown", "alice.brown@example.com", "user", "inactive", "HR", "+1-555-0104", "321 Elm St", "Houston", "US", "0"},
            {"Charlie Wilson", "charlie.wilson@example.com", "user", "active", "Marketing", "+1-555-0105", "654 Maple Dr", "Phoenix", "US", "15"},
            {"Diana Taylor", "diana.taylor@example.com", "manager", "active", "Engineering", "+1-555-0106", "987 Cedar Ln", "Philadelphia", "US", "20"},
            {"Edward Martinez", "edward.martinez@example.com", "user", "active", "Finance", "+1-555-0107", "147 Birch Blvd", "San Antonio", "US", "0"},
            {"Fiona Davis", "fiona.davis@example.com", "user", "suspended", "Support", "+1-555-0108", "258 Walnut Ave", "San Diego", "US", "0"},
            {"George Anderson", "george.anderson@example.com", "user", "active", "Engineering", "+1-555-0109", "369 Spruce St", "Dallas", "US", "5"},
            {"Hannah Thomas", "hannah.thomas@example.com", "admin", "active", "Management", "+1-555-0110", "741 Ash Rd", "San Jose", "US", "25"},
            {"Ivan White", "ivan.white@example.com", "user", "active", "Sales", "+1-555-0111", "852 Oak St", "Austin", "US", "0"},
            {"Julia Harris", "julia.harris@example.com", "user", "active", "Marketing", "+1-555-0112", "963 Pine Ave", "Jacksonville", "US", "10"},
            {"Kevin Clark", "kevin.clark@example.com", "user", "inactive", "Finance", "+1-555-0113", "174 Elm Blvd", "Fort Worth", "US", "0"},
            {"Laura Lewis", "laura.lewis@example.com", "manager", "active", "HR", "+1-555-0114", "285 Maple St", "Columbus", "US", "5"},
            {"Michael Lee", "michael.lee@example.com", "user", "active", "Engineering", "+1-555-0115", "396 Cedar Dr", "Charlotte", "US", "0"},
            {"Nancy Walker", "nancy.walker@example.com", "user", "active", "Support", "+1-555-0116", "407 Birch Rd", "Indianapolis", "US", "8"},
            {"Oscar Hall", "oscar.hall@example.com", "user", "active", "Sales", "+1-555-0117", "518 Walnut Ln", "San Francisco", "US", "3"},
            {"Patricia Allen", "patricia.allen@example.com", "user", "suspended", "Marketing", "+1-555-0118", "629 Spruce Ave", "Seattle", "US", "0"},
            {"Quinn Young", "quinn.young@example.com", "user", "active", "Finance", "+1-555-0119", "730 Ash Blvd", "Denver", "US", "0"},
            // Special user: discount=100 triggers payment error
            {"Special Discount", "special.discount@example.com", "user", "active", "VIP", "+1-555-0120", "999 VIP Lane", "Las Vegas", "US", "100"},
        };

        long baseEpoch = 1700000000L;
        for (int i = 0; i < data.length; i++) {
            User u = new User();
            u.setName(data[i][0]);
            u.setEmail(data[i][1]);
            u.setRole(data[i][2]);
            u.setStatus(data[i][3]);
            u.setDepartment(data[i][4]);
            u.setPhone(data[i][5]);
            u.setStreet(data[i][6]);
            u.setCity(data[i][7]);
            u.setCountry(data[i][8]);
            u.setDiscount(Double.parseDouble(data[i][9]));
            long epoch = baseEpoch + (i * 86400L);
            u.setCreatedAtEpoch(epoch);
            u.setCreatedAtIso("2023-" + String.format("%02d", (i / 28) + 11) + "-" + String.format("%02d", (i % 28) + 1) + "T10:00:00Z");
            u.setUpdatedAtEpoch(epoch + 3600L);
            userRepo.save(u);
        }
    }

    private void seedProducts() {
        Object[][] data = {
            {"PROD-001", "Dell XPS 15 Laptop", "electronics", "Dell", 1299.99, 45, "High-performance laptop", "laptop,dell,windows", 4.5, 234},
            {"PROD-002", "Apple MacBook Pro 14", "electronics", "Apple", 1999.99, 30, "Professional laptop", "laptop,apple,macos", 4.8, 567},
            {"PROD-003", "Samsung 4K Monitor 27\"", "electronics", "Samsung", 449.99, 78, "4K UHD display", "monitor,samsung,4k", 4.3, 189},
            {"PROD-004", "Logitech MX Master 3 Mouse", "accessories", "Logitech", 99.99, 156, "Wireless ergonomic mouse", "mouse,logitech,wireless", 4.7, 892},
            {"PROD-005", "Mechanical Keyboard K95", "accessories", "Corsair", 149.99, 67, "RGB mechanical keyboard", "keyboard,corsair,mechanical", 4.4, 445},
            {"PROD-006", "Sony WH-1000XM5 Headphones", "audio", "Sony", 349.99, 92, "Noise cancelling headphones", "headphones,sony,wireless", 4.6, 1203},
            {"PROD-007", "iPad Pro 12.9\"", "electronics", "Apple", 1099.99, 25, "Professional tablet", "tablet,apple,ipad", 4.7, 678},
            {"PROD-008", "Samsung Galaxy S24", "mobile", "Samsung", 899.99, 120, "Flagship Android phone", "phone,samsung,android", 4.4, 934},
            {"PROD-009", "USB-C Hub 10-in-1", "accessories", "Anker", 49.99, 234, "Multi-port USB hub", "hub,usb,anker", 4.2, 1567},
            {"PROD-010", "Standing Desk Converter", "furniture", "FlexiSpot", 199.99, 34, "Adjustable desk converter", "desk,ergonomic,standing", 4.3, 267},
            {"PROD-011", "Webcam HD 1080p", "accessories", "Logitech", 79.99, 189, "Full HD webcam", "webcam,logitech,hd", 4.1, 445},
            {"PROD-012", "External SSD 2TB", "storage", "Samsung", 179.99, 67, "Portable SSD drive", "ssd,samsung,storage", 4.6, 789},
            {"PROD-013", "Wireless Charger 15W", "accessories", "Belkin", 39.99, 345, "Fast wireless charger", "charger,wireless,belkin", 4.0, 2341},
            {"PROD-014", "Gaming Chair Pro", "furniture", "DXRacer", 399.99, 23, "Ergonomic gaming chair", "chair,gaming,dxracer", 4.5, 567},
            {"PROD-015", "Network Switch 24-Port", "networking", "Cisco", 289.99, 15, "Managed network switch", "switch,cisco,networking", 4.8, 123},
        };

        long baseEpoch = 1690000000L;
        for (int i = 0; i < data.length; i++) {
            Product p = new Product();
            p.setProductId((String) data[i][0]);
            p.setName((String) data[i][1]);
            p.setCategory((String) data[i][2]);
            p.setBrand((String) data[i][3]);
            p.setPrice((Double) data[i][4]);
            p.setPriceStr(String.valueOf(data[i][4]));
            p.setStock((Integer) data[i][5]);
            p.setDescription((String) data[i][6]);
            p.setTags((String) data[i][7]);
            p.setRating((Double) data[i][8]);
            p.setReviewCount((Integer) data[i][9]);
            p.setCreatedAtIso("2023-" + String.format("%02d", (i % 12) + 1) + "-15T08:00:00Z");
            p.setCreatedAtEpoch(baseEpoch + (i * 100000L));
            productRepo.save(p);
        }
    }

    private void seedOrders() {
        String[][] data = {
            {"ORD-10001", "1", "confirmed", "299.98", "USD", "Laptop Stand, Mouse", "2", "123 Main St", "New York", "US", "standard", "2024-01-10T09:00:00Z", null},
            {"ORD-10002", "2", "shipped", "1999.99", "USD", "MacBook Pro 14", "1", "456 Oak Ave", "Boston", "US", "express", "2024-01-11T10:30:00Z", null},
            {"ORD-10003", "3", "delivered", "549.98", "USD", "Monitor, Keyboard", "2", "789 Pine Rd", "Chicago", "US", "standard", "2024-01-12T11:00:00Z", null},
            {"ORD-10004", "1", "pending", "99.99", "USD", "MX Master 3", "1", "123 Main St", "New York", "US", "economy", "2024-01-13T12:00:00Z", null},
            {"ORD-10005", "5", "confirmed", "1449.98", "USD", "Dell Laptop, Headphones", "2", "654 Maple Dr", "Phoenix", "US", "express", "2024-01-14T13:00:00Z", null},
            {"ORD-10006", "6", "cancelled", "449.99", "USD", "Samsung Monitor", "1", "987 Cedar Ln", "Philadelphia", "US", "standard", "2024-01-15T14:00:00Z", "customer_request"},
            {"ORD-10007", "7", "delivered", "249.98", "USD", "Webcam, USB Hub", "2", "147 Birch Blvd", "San Antonio", "US", "economy", "2024-01-16T15:00:00Z", null},
            {"ORD-10008", "9", "shipped", "1099.99", "USD", "iPad Pro", "1", "369 Spruce St", "Dallas", "US", "express", "2024-01-17T16:00:00Z", null},
            {"ORD-10009", "10", "confirmed", "899.99", "USD", "Samsung Galaxy S24", "1", "741 Ash Rd", "San Jose", "US", "standard", "2024-01-18T17:00:00Z", null},
            {"ORD-10010", "11", "pending", "179.99", "USD", "External SSD 2TB", "1", "852 Oak St", "Austin", "US", "economy", "2024-01-19T18:00:00Z", null},
            {"ORD-10011", "12", "delivered", "599.97", "USD", "Headphones, Mouse, Hub", "3", "963 Pine Ave", "Jacksonville", "US", "standard", "2024-01-20T09:30:00Z", null},
            {"ORD-10012", "14", "confirmed", "399.99", "USD", "Gaming Chair Pro", "1", "285 Maple St", "Columbus", "US", "standard", "2024-01-21T10:00:00Z", null},
            {"ORD-10013", "15", "shipped", "289.99", "USD", "Network Switch", "1", "396 Cedar Dr", "Charlotte", "US", "standard", "2024-01-22T11:00:00Z", null},
            {"ORD-10014", "16", "cancelled", "149.99", "USD", "Corsair Keyboard", "1", "407 Birch Rd", "Indianapolis", "US", "economy", "2024-01-23T12:00:00Z", "price_change"},
            {"ORD-10015", "17", "delivered", "79.99", "USD", "Webcam HD", "1", "518 Walnut Ln", "San Francisco", "US", "standard", "2024-01-24T13:00:00Z", null},
            {"ORD-10016", "1", "confirmed", "289.98", "USD", "SSD, Charger", "2", "123 Main St", "New York", "US", "standard", "2024-01-25T14:00:00Z", null},
            {"ORD-10017", "3", "pending", "1299.99", "USD", "Dell XPS 15", "1", "789 Pine Rd", "Chicago", "US", "express", "2024-01-26T15:00:00Z", null},
            {"ORD-10018", "5", "shipped", "199.99", "USD", "Standing Desk", "1", "654 Maple Dr", "Phoenix", "US", "standard", "2024-01-27T16:00:00Z", null},
            {"ORD-10019", "9", "delivered", "49.99", "USD", "USB Hub", "1", "369 Spruce St", "Dallas", "US", "economy", "2024-01-28T17:00:00Z", null},
            {"ORD-10020", "20", "confirmed", "1349.98", "USD", "Laptop, Monitor", "2", "999 VIP Lane", "Las Vegas", "US", "express", "2024-01-29T18:00:00Z", null},
        };

        for (String[] row : data) {
            Order o = new Order();
            o.setOrderId(row[0]);
            o.setUserId(Long.parseLong(row[1]));
            o.setStatus(row[2]);
            o.setTotalAmount(Double.parseDouble(row[3]));
            o.setCurrency(row[4]);
            o.setItemsSummary(row[5]);
            o.setItemCount(Integer.parseInt(row[6]));
            o.setShippingStreet(row[7]);
            o.setShippingCity(row[8]);
            o.setShippingCountry(row[9]);
            o.setShippingMethod(row[10]);
            o.setCreatedAtIso(row[11]);
            o.setCreatedAtEpoch(1704844800L + (orderRepo.count() * 86400L));
            o.setCancelReason(row[12]);
            orderRepo.save(o);
        }
    }

    private void seedPayments() {
        String[][] data = {
            {"PAY-20001", "ORD-10001", "1", "299.98", "USD", "completed", "credit_card", "4242"},
            {"PAY-20002", "ORD-10002", "2", "1999.99", "USD", "completed", "credit_card", "1111"},
            {"PAY-20003", "ORD-10003", "3", "549.98", "USD", "completed", "paypal", null},
            {"PAY-20004", "ORD-10005", "5", "1449.98", "USD", "completed", "credit_card", "5555"},
            {"PAY-20005", "ORD-10007", "7", "249.98", "USD", "completed", "debit_card", "6789"},
            {"PAY-20006", "ORD-10008", "9", "1099.99", "USD", "completed", "credit_card", "4444"},
            {"PAY-20007", "ORD-10009", "10", "899.99", "USD", "pending", "bank_transfer", null},
            {"PAY-20008", "ORD-10011", "12", "599.97", "USD", "completed", "credit_card", "9999"},
            {"PAY-20009", "ORD-10012", "14", "399.99", "USD", "completed", "credit_card", "3333"},
            {"PAY-20010", "ORD-10013", "15", "289.99", "USD", "completed", "paypal", null},
            {"PAY-20011", "ORD-10015", "17", "79.99", "USD", "completed", "credit_card", "7777"},
            {"PAY-20012", "ORD-10016", "1", "289.98", "USD", "pending", "credit_card", "4242"},
            {"PAY-20013", "ORD-10019", "9", "49.99", "USD", "completed", "debit_card", "2222"},
            {"PAY-20014", "ORD-10020", "20", "1349.98", "USD", "completed", "credit_card", "8888"},
            {"PAY-20015", "ORD-10004", "1", "99.99", "USD", "failed", "credit_card", "0000"},
        };

        for (String[] row : data) {
            Payment p = new Payment();
            p.setPaymentId(row[0]);
            p.setOrderId(row[1]);
            p.setUserId(Long.parseLong(row[2]));
            p.setAmount(Double.parseDouble(row[3]));
            p.setCurrency(row[4]);
            p.setStatus(row[5]);
            p.setMethod(row[6]);
            p.setCardLast4(row[7]);
            p.setCreatedAtIso("2024-01-" + String.format("%02d", (paymentRepo.count() % 28) + 10) + "T10:00:00Z");
            p.setCreatedAtEpoch(1705000000L + (paymentRepo.count() * 3600L));
            paymentRepo.save(p);
        }
    }
}
