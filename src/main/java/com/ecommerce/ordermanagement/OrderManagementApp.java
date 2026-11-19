package com.ecommerce.ordermanagement;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Properties;
import java.util.Scanner;

/**
 * Menu-driven Order Management System using Java JDBC and MySQL.
 */
public class OrderManagementApp {

    private static final String[] VALID_STATUSES = {"Pending", "Shipped", "Delivered"};

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in);
             Connection connection = getConnection()) {

            System.out.println("Connected to ecommerce_db successfully.");
            boolean running = true;

            while (running) {
                printMenu();
                int choice = readInt(scanner, "Enter your choice: ");

                try {
                    switch (choice) {
                        case 1 -> insertThreeSampleOrders(connection);
                        case 2 -> displayOrdersByStatus(connection, scanner);
                        case 3 -> increaseOrderQuantity(connection, scanner);
                        case 4 -> updateOrderStatus(connection, scanner);
                        case 5 -> displayTotalOrderValue(connection);
                        case 6 -> deleteOrderOnlyIfCancelled(connection, scanner);
                        case 0 -> {
                            running = false;
                            System.out.println("Exiting Order Management System. Goodbye!");
                        }
                        default -> System.out.println("Invalid choice. Please select 0 to 6.");
                    }
                } catch (SQLException e) {
                    System.out.println("Database operation failed: " + e.getMessage());
                }

                if (running) {
                    System.out.println();
                }
            }
        } catch (SQLException e) {
            System.out.println("Unable to connect to MySQL.");
            System.out.println("Check that MySQL is running, ecommerce_db exists, and src/main/resources/db.properties has the correct credentials.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    /** Loads connection settings and opens the JDBC connection. */
    private static Connection getConnection() throws SQLException {
        Properties properties = new Properties();

        try (InputStream input = OrderManagementApp.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new SQLException("Could not read db.properties", e);
        }

        String url = setting("ECOMMERCE_DB_URL", properties.getProperty("db.url"),
                "jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String username = setting("ECOMMERCE_DB_USER", properties.getProperty("db.username"), "root");
        String password = setting("ECOMMERCE_DB_PASSWORD", properties.getProperty("db.password"), "");

        // Connector/J supports automatic registration, but loading it explicitly is useful in JDBC lab work.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Check the Maven dependency.", e);
        }

        return DriverManager.getConnection(url, username, password);
    }

    /** Environment variables take priority over db.properties. */
    private static String setting(String envName, String propertyValue, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return defaultValue;
    }

    private static void printMenu() {
        System.out.println("========== ORDER MANAGEMENT SYSTEM ==========");
        System.out.println("1. Insert at least three sample orders");
        System.out.println("2. Display orders by status (quantity > 2, price descending)");
        System.out.println("3. Increase quantity of an existing order");
        System.out.println("4. Update order status (Pending / Shipped / Delivered)");
        System.out.println("5. Display total order value");
        System.out.println("6. Delete an order only if its status is Cancelled");
        System.out.println("0. Exit");
        System.out.println("==============================================");
    }

    /**
     * Inserts three predefined records using PreparedStatement.
     * Existing primary keys are skipped and reported without stopping other inserts.
     */
    private static void insertThreeSampleOrders(Connection connection) throws SQLException {
        String sql = "INSERT INTO Orders "
                + "(order_id, customer_name, product_name, quantity, unit_price, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        Object[][] sampleOrders = {
                {1001, "Rahul Sharma", "Laptop", 1, 65000.00, "Pending"},
                {1002, "Priya Verma", "Wireless Mouse", 5, 799.00, "Pending"},
                {1003, "Aman Gupta", "Monitor", 3, 12000.00, "Cancelled"}
        };

        int inserted = 0;
        int duplicates = 0;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Object[] order : sampleOrders) {
                ps.setInt(1, (Integer) order[0]);
                ps.setString(2, (String) order[1]);
                ps.setString(3, (String) order[2]);
                ps.setInt(4, (Integer) order[3]);
                ps.setDouble(5, (Double) order[4]);
                ps.setString(6, (String) order[5]);

                try {
                    ps.executeUpdate();
                    inserted++;
                    System.out.println("Inserted order ID: " + order[0]);
                } catch (SQLException e) {
                    if (isDuplicateOrderId(e)) {
                        duplicates++;
                        System.out.println("Order ID " + order[0]
                                + " already exists. Duplicate record skipped.");
                    } else {
                        throw e;
                    }
                }
            }
        }

        System.out.println("Insertion completed. New records: " + inserted
                + ", duplicate IDs skipped: " + duplicates + ".");
    }

    private static boolean isDuplicateOrderId(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            if (current instanceof SQLIntegrityConstraintViolationException
                    || current.getErrorCode() == 1062
                    || "23000".equals(current.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /** Displays matching orders, with quantity > 2 and unit_price in descending order. */
    private static void displayOrdersByStatus(Connection connection, Scanner scanner) throws SQLException {
        String status = readNonEmptyLine(scanner, "Enter order status to search: ");
        String sql = "SELECT order_id, customer_name, product_name, quantity, unit_price, status "
                + "FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\nOrders with status '" + status + "' and quantity greater than 2:");
                printTableHeader();
                boolean found = false;
                while (rs.next()) {
                    printOrderRow(rs);
                    found = true;
                }
                if (!found) {
                    System.out.println("No matching orders found.");
                }
            }
        }
    }

    /** Adds a positive quantity to an existing order using one atomic UPDATE query. */
    private static void increaseOrderQuantity(Connection connection, Scanner scanner) throws SQLException {
        int orderId = readInt(scanner, "Enter order ID: ");
        int additionalQuantity = readPositiveInt(scanner, "Enter additional quantity to add: ");

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, additionalQuantity);
            ps.setInt(2, orderId);
            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated == 0) {
                System.out.println("Order ID " + orderId + " does not exist. Quantity was not changed.");
            } else {
                System.out.println("Order quantity increased successfully.");
                displayOrderById(connection, orderId);
            }
        }
    }

    /** Validates the new status, checks that the order exists, updates it, and displays it. */
    private static void updateOrderStatus(Connection connection, Scanner scanner) throws SQLException {
        int orderId = readInt(scanner, "Enter order ID: ");
        String enteredStatus = readNonEmptyLine(scanner,
                "Enter new status (Pending / Shipped / Delivered): ");
        String newStatus = normalizeValidStatus(enteredStatus);

        if (newStatus == null) {
            System.out.println("Invalid status. Allowed values are Pending, Shipped, and Delivered.");
            return;
        }

        if (!displayOrderById(connection, orderId)) {
            System.out.println("Order ID " + orderId + " does not exist. Status was not changed.");
            return;
        }

        String sql = "UPDATE Orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated == 0) {
                System.out.println("The order could not be updated. It may have been changed or removed by another process.");
            } else {
                System.out.println("Order status updated successfully.");
                System.out.println("Updated order details:");
                displayOrderById(connection, orderId);
            }
        }
    }

    private static String normalizeValidStatus(String enteredStatus) {
        for (String validStatus : VALID_STATUSES) {
            if (validStatus.equalsIgnoreCase(enteredStatus.trim())) {
                return validStatus;
            }
        }
        return null;
    }

    /** Displays SUM(quantity * unit_price) for all orders. */
    private static void displayTotalOrderValue(Connection connection) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) AS total_value FROM Orders";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.printf("Total order value: Rs. %.2f%n", rs.getDouble("total_value"));
            }
        }
    }

    /** Deletes a record only when its current status is Cancelled. */
    private static void deleteOrderOnlyIfCancelled(Connection connection, Scanner scanner) throws SQLException {
        int orderId = readInt(scanner, "Enter order ID to delete: ");
        String selectSql = "SELECT status FROM Orders WHERE order_id = ?";
        String currentStatus;

        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order ID " + orderId + " does not exist. Nothing was deleted.");
                    return;
                }
                currentStatus = rs.getString("status");
            }
        }

        if (currentStatus == null || !"Cancelled".equalsIgnoreCase(currentStatus.trim())) {
            System.out.println("Order cannot be deleted because its status is '" + currentStatus
                    + "'. Only Cancelled orders can be deleted.");
            return;
        }

        String deleteSql = "DELETE FROM Orders WHERE order_id = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setInt(1, orderId);
            ps.setString(2, currentStatus);
            int rowsDeleted = ps.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Cancelled order " + orderId + " deleted successfully.");
            } else {
                System.out.println("Order was not deleted. Its status may have changed; check the order again.");
            }
        }
    }

    /** Finds and displays one order. Returns false if it does not exist. */
    private static boolean displayOrderById(Connection connection, int orderId) throws SQLException {
        String sql = "SELECT order_id, customer_name, product_name, quantity, unit_price, status "
                + "FROM Orders WHERE order_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    printTableHeader();
                    printOrderRow(rs);
                    return true;
                }
            }
        }
        return false;
    }

    private static void printTableHeader() {
        System.out.printf("%-9s %-20s %-20s %-10s %-14s %-12s%n",
                "Order ID", "Customer", "Product", "Quantity", "Unit Price", "Status");
        System.out.println("-----------------------------------------------------------------------------------------------");
    }

    private static void printOrderRow(ResultSet rs) throws SQLException {
        System.out.printf("%-9d %-20s %-20s %-10d Rs. %-10.2f %-12s%n",
                rs.getInt("order_id"),
                rs.getString("customer_name"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getDouble("unit_price"),
                rs.getString("status"));
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static int readPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            int value = readInt(scanner, prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("Value must be greater than zero.");
        }
    }

    private static String readNonEmptyLine(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Input cannot be empty.");
        }
    }
}
