# Order Management System (Java JDBC + MySQL)

A menu-driven Java 17 Maven project for the given Order Management System practical.

## Features

1. Insert three sample orders with `PreparedStatement`. Existing/duplicate primary keys are reported and skipped; the remaining rows are still attempted.
2. Search by order status where `quantity > 2`, sorted by `unit_price DESC`.
3. Increase the quantity of an order by a positive additional quantity. Reports if the order ID is not found.
4. Update status only to `Pending`, `Shipped`, or `Delivered`, after checking that the order exists. Displays the updated record.
5. Display the total order value using `COALESCE(SUM(quantity * unit_price), 0)`.
6. Delete an order only when its current status is `Cancelled`.

All SQL values supplied by the user are passed through `PreparedStatement` parameters.

## Requirements

- JDK 17 or later
- Maven 3.8+ (Maven 3.9+ recommended)
- MySQL Server 8.4 or later
- Eclipse IDE with Maven support (optional)

## 1. Create the database and table

Open MySQL Workbench, paste the contents of `schema.sql`, and execute it. This creates `ecommerce_db` and the `Orders` table if they do not already exist.

The application inserts the three sample records itself through menu option 1; `schema.sql` does not pre-insert data.

## 2. Configure the MySQL connection

Open:

`src/main/resources/db.properties`

Update the username and password for your MySQL installation. For example:

```properties
db.url=jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=YOUR_MYSQL_PASSWORD
```

Alternatively, set these environment variables (they take priority over `db.properties`):

- `ECOMMERCE_DB_URL`
- `ECOMMERCE_DB_USER`
- `ECOMMERCE_DB_PASSWORD`

## 3. Run from a terminal

Open a terminal in the folder containing `pom.xml`, then run:

```bash
mvn clean compile exec:java
```

To build a JAR:

```bash
mvn clean package
```

Maven downloads MySQL Connector/J from Maven Central automatically on the first build. The dependency and version are declared in `pom.xml`; no manual JAR copying is required.

## 4. Import into Eclipse

1. Extract the ZIP file.
2. Open Eclipse.
3. Select **File > Import > Maven > Existing Maven Projects**.
4. Browse to the extracted `OrderManagementSystem` folder (the folder containing `pom.xml`).
5. Select the project and click **Finish**.
6. Right-click the project and select **Maven > Update Project** if dependencies do not appear immediately.
7. Edit `db.properties`, then open `OrderManagementApp.java` and choose **Run As > Java Application**.

## Sample records inserted by option 1

| Order ID | Customer | Product | Quantity | Unit Price | Status |
|---:|---|---|---:|---:|---|
| 1001 | Rahul Sharma | Laptop | 1 | 65000.00 | Pending |
| 1002 | Priya Verma | Wireless Mouse | 5 | 799.00 | Pending |
| 1003 | Aman Gupta | Monitor | 3 | 12000.00 | Cancelled |

You can run option 1 again safely: duplicate IDs are detected and skipped instead of terminating the insertion process.

## Menu mapping to the question

- **(a)** Option 1 — database connection + three `PreparedStatement` inserts + duplicate handling.
- **(b)** Option 2 — status filter + quantity greater than 2 + descending unit price.
- **(c)** Option 3 — increase quantity and handle missing order ID.
- **(d)** Option 4 — validate status, check order, update, and display details.
- **(e)** Option 5 — aggregate total value; Option 6 — delete only a `Cancelled` order.
