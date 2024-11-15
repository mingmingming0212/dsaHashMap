package edu.smu.smusql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceEvaluator {
    // Main method to run the simple database engine
    static Engine dbEngine = new Engine();

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("smuSQL Performance Evaluator");
        System.out.println("Different benchmarks for Engine");
        System.out.println();
        System.out.println("Enter any SQL Statement");
        System.out.println();
        System.out.println("Other options (Enter option number or word):");
        System.out.println("1. AverageRuntime:       Test average time taken for 100,000 queries");
        System.out.println("2. DataSizeImpact:       Test time taken for 1000, 10000, 100000 and 1000000 size dataset");
        System.out.println("3. DataDistributionTest: Test time taken for uniform, skewed and clustered distributions");
        System.out.println("4. TestSelect%:          Test time taken for 100,000 queries with 20%, 50% and 80% Selects");
        System.out.println("5. TestInsert%:          Test time taken for 100,000 queries with 20%, 50% and 80% Inserts");
        System.out.println("6. clearDB:              Clear the database");
        System.out.println("7. exit:                 Exit the program");

        while (true) {
            System.out.print("smusql> ");
            String query = scanner.nextLine();
            if (query.equalsIgnoreCase("exit") || query.equalsIgnoreCase("7")) {
                break;
            } else if (query.equalsIgnoreCase("AverageRuntime")|| query.equalsIgnoreCase("1")) {
                AverageRuntime(100000);
            } else if (query.equalsIgnoreCase("DataSizeImpact")|| query.equalsIgnoreCase("2")) {
                dataSizeImpactTest();
            } else if (query.equalsIgnoreCase("DataDistributionTest")|| query.equalsIgnoreCase("3")) {
                dataDistributionTest();
            } else if (query.equalsIgnoreCase("TestSelect%")|| query.equalsIgnoreCase("4")) {
                simulateReadHeavyWorkload(dbEngine, 100000, 0.2);
                simulateReadHeavyWorkload(dbEngine, 100000, 0.5);
                simulateReadHeavyWorkload(dbEngine, 100000, 0.8);
            } else if (query.equalsIgnoreCase("TestInsert%")|| query.equalsIgnoreCase("5")) {
                simulateWriteHeavyWorkload(dbEngine, 100000, 0.2);
                simulateWriteHeavyWorkload(dbEngine, 100000, 0.5);
                simulateWriteHeavyWorkload(dbEngine, 100000, 0.8);
            } else if (query.equalsIgnoreCase("clearDB")|| query.equalsIgnoreCase("6")) {
                clearDatabase();
            } else {
                System.out.println(dbEngine.executeSQL(query));
            }

        }
        scanner.close();
    }

    public static void AverageRuntime(int num) {

        // Set the number of queries to execute
        int numberOfQueries = num;

        // Create tables
        dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)");
        dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)");
        dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");

        // Random data generator
        Random random = new Random(201);

        prepopulateTables(random);

        // Initialize counters and timers for each query type
        long insertTime = 0, selectTime = 0, updateTime = 0, deleteTime = 0;
        long complexSelectTime = 0, complexUpdateTime = 0;
        int insertCount = 0, selectCount = 0, updateCount = 0, deleteCount = 0;
        int complexSelectCount = 0, complexUpdateCount = 0;

        // Loop to simulate millions of queries
        for (int i = 0; i < numberOfQueries; i++) {
            int queryType = random.nextInt(6); // Randomly choose the type of query to execute

            long startTime = System.nanoTime(); // Start timing

            // Execute the query based on its type
            switch (queryType) {
                case 0: // INSERT query
                    insertRandomData(random);
                    insertTime += (System.nanoTime() - startTime);
                    insertCount++;
                    break;
                case 1: // SELECT query (simple)
                    selectRandomData(random);
                    selectTime += (System.nanoTime() - startTime);
                    selectCount++;
                    break;
                case 2: // UPDATE query
                    updateRandomData(random);
                    updateTime += (System.nanoTime() - startTime);
                    updateCount++;
                    break;
                case 3: // DELETE query
                    deleteRandomData(random);
                    deleteTime += (System.nanoTime() - startTime);
                    deleteCount++;
                    break;
                case 4: // Complex SELECT query with WHERE, AND, OR, >, <, LIKE
                    complexSelectQuery(random);
                    complexSelectTime += (System.nanoTime() - startTime);
                    complexSelectCount++;
                    break;
                case 5: // Complex UPDATE query with WHERE
                    complexUpdateQuery(random);
                    complexUpdateTime += (System.nanoTime() - startTime);
                    complexUpdateCount++;
                    break;
            }

            // Print progress every 10,000 queries
            if (i % 10000 == 0) {
                System.out.println("Processed " + i + " queries...");
            }
        }

        // Print average time for each query type
        System.out.println("Average Time per Query Type:");
        System.out.println("INSERT: " + (insertCount == 0 ? 0 : insertTime / insertCount) + " ns");
        System.out.println("SELECT: " + (selectCount == 0 ? 0 : selectTime / selectCount) + " ns");
        System.out.println("UPDATE: " + (updateCount == 0 ? 0 : updateTime / updateCount) + " ns");
        System.out.println("DELETE: " + (deleteCount == 0 ? 0 : deleteTime / deleteCount) + " ns");
        System.out.println(
                "Complex SELECT: " + (complexSelectCount == 0 ? 0 : complexSelectTime / complexSelectCount) + " ns");
        System.out.println(
                "Complex UPDATE: " + (complexUpdateCount == 0 ? 0 : complexUpdateTime / complexUpdateCount) + " ns");

        System.out.println("Finished processing " + numberOfQueries + " queries.");
    }

    public static void dataSizeImpactTest() {
        int[] dataSizes = { 1000, 10000, 100000, 1000000 };

        for (int size : dataSizes) {
            clearDatabase();
            prePopulateLargeDataset(dbEngine, new Random(201), size);

            long totaltime = 0;
            for (int index = 0; index < 5; index++) {
                long startTime = System.nanoTime();
                String query = "SELECT * FROM users WHERE age > 25";
                dbEngine.executeSQL(query);
                totaltime += (System.nanoTime() - startTime) / 1_000_000.0;
            }
            long queryTime = totaltime / 5;

            System.out.println("Query time for " + size + " records: " + queryTime + "ms");
        }
    }

    public static void dataDistributionTest() {
        String[] distributions = { "Uniform", "Skewed", "Clustered" };

        // Define query patterns to test different aspects of the distribution
        String[][] queryPatterns = {
                // {query description, SQL query}
                { "Single Cluster", "SELECT * FROM users WHERE age > %d and age < %d" },
                { "Multi Cluster", "SELECT * FROM users WHERE age > %d and age < %d" },
                { "Cluster Boundary", "SELECT * FROM users WHERE age > %d and age < %d" },
                { "High Range", "SELECT * FROM users WHERE age > %d" },
                { "Low Range", "SELECT * FROM users WHERE age < %d" }
        };

        for (String distribution : distributions) {
            System.out.println("\nTesting " + distribution + " distribution:");
            clearDatabase();

            // Create tables
            dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)");
            dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)");
            dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");

            // Populate with different distributions
            
            switch (distribution) {
                case "Uniform":
                    populateUniformData(10000);
                    break;
                case "Skewed":
                    populateSkewedData(10000);
                    break;
                case "Clustered":
                    populateClusteredData(10000);
                    break;
            }

            // Test each query pattern
            for (String[] queryPattern : queryPatterns) {
                String description = queryPattern[0];
                String queryTemplate = queryPattern[1];

                System.out.println("\nTesting " + description + ":");

                // Run different variations of each query pattern
                switch (description) {
                    case "Single Cluster":
                        // Test different single clusters
                        testQuery(String.format(queryTemplate, 40, 44)); // middle cluster
                        testQuery(String.format(queryTemplate, 20, 24)); // first cluster
                        testQuery(String.format(queryTemplate, 110, 114)); // last cluster
                        break;

                    case "Multi Cluster":
                        // Test spans of different sizes
                        testQuery(String.format(queryTemplate, 35, 75)); // middle span
                        testQuery(String.format(queryTemplate, 20, 54)); // lower span
                        testQuery(String.format(queryTemplate, 70, 114)); // upper span
                        break;

                    case "Cluster Boundary":
                        // Test different boundary conditions
                        testQuery(String.format(queryTemplate, 29, 31)); // lower boundary
                        testQuery(String.format(queryTemplate, 49, 51)); // middle boundary
                        testQuery(String.format(queryTemplate, 89, 91)); // upper boundary
                        break;

                    case "High Range":
                        // Test different high range conditions
                        testQuery(String.format(queryTemplate, 100));
                        testQuery(String.format(queryTemplate, 80));
                        testQuery(String.format(queryTemplate, 60));
                        break;

                    case "Low Range":
                        // Test different low range conditions
                        testQuery(String.format(queryTemplate, 35));
                        testQuery(String.format(queryTemplate, 55));
                        testQuery(String.format(queryTemplate, 75));
                        break;
                }
            }
        }
    }

    private static void testQuery(String query) {
        List<Double> queryTimes = new ArrayList<>();

        // Run each query multiple times to get reliable statistics
        for (int i = 0; i < 10; i++) { // 10 iterations per query is usually sufficient
            long startTime = System.nanoTime();
            dbEngine.executeSQL(query);
            double queryTime = (System.nanoTime() - startTime) / 1_000_000.0;
            queryTimes.add(queryTime);
        }

        // Calculate statistics
        double avgTime = queryTimes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(queryTimes.stream()
                .mapToDouble(time -> Math.pow(time - avgTime, 2))
                .average()
                .orElse(0.0));

        System.out.printf("Query: %s%n", query);
        System.out.printf("Average time: %.2f ms%n", avgTime);
        System.out.printf("Std Dev: %.2f ms%n", stdDev);
        System.out.printf("Min time: %.2f ms%n", Collections.min(queryTimes));
        System.out.printf("Max time: %.2f ms%n%n", Collections.max(queryTimes));
    }

    private static void populateUniformData(int size) {
        Random rand = new Random(201);
        for (int i = 0; i < size; i++) {
            int age = rand.nextInt(60) + 20; // Uniform distribution
            dbEngine.executeSQL("INSERT INTO users VALUES (" + i +
                    ", 'User" + i + "', " + age + ", " + getRandomCity(rand) + ")");
        }
    }

    private static void populateSkewedData(int size) {
        Random rand = new Random(201);
        for (int i = 0; i < size; i++) {
            int age = (int) Math.pow(rand.nextDouble() * 10, 2); // Skewed towards lower values
            dbEngine.executeSQL("INSERT INTO users VALUES (" + i +
                    ", 'User" + i + "', " + age + ", " + getRandomCity(rand) + ")");
        }
    }

    private static void populateClusteredData(int size) {
        Random rand = new Random(201);
        for (int i = 0; i < size; i++) {
            int cluster = i / (size / 10); // Creates 10 clusters
            int age = 10 + cluster * 10 + rand.nextInt(5);
            dbEngine.executeSQL("INSERT INTO users VALUES (" + i +
                    ", 'User" + i + "', " + age + ", " + getRandomCity(rand) + ")");
        }
    }

    public static void clearDatabase() {
        try {
            // List of table names, assuming you have a method to get this or know the table
            // names in advance
            String[] tables = { "users", "products", "orders" };

            for (String table : tables) {
                String clearTableSQL = "DELETE FROM " + table + " WHERE id > -1";
                dbEngine.executeSQL(clearTableSQL); // Replace with your SQL execution method
                System.out.println("Cleared table: " + table);
            }

            System.out.println("All tables cleared.");
        } catch (Exception e) {
            System.err.println("Error clearing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // small datasets
    private static void prepopulateTables(Random random) {
        System.out.println("Prepopulating users");
        // Insert initial users
        for (int i = 0; i < 50; i++) {
            String name = "User" + i;
            int age = 20 + (i % 41); // Ages between 20 and 60
            String city = getRandomCity(random);
            String insertCommand = String.format("INSERT INTO users VALUES (%d, '%s', %d, '%s')", i, name, age, city);
            dbEngine.executeSQL(insertCommand);
        }
        System.out.println("Prepopulating products");
        // Insert initial products
        for (int i = 0; i < 50; i++) {
            String productName = "Product" + i;
            double price = 10 + (i % 990); // Prices between $10 and $1000
            String category = getRandomCategory(random);
            String insertCommand = String.format("INSERT INTO products VALUES (%d, '%s', %.2f, '%s')", i, productName,
                    price, category);
            dbEngine.executeSQL(insertCommand);
        }
        System.out.println("Prepopulating orders");
        // Insert initial orders
        for (int i = 0; i < 50; i++) {
            int user_id = random.nextInt(9999);
            int product_id = random.nextInt(9999);
            int quantity = random.nextInt(1, 100);
            String category = getRandomCategory(random);
            String insertCommand = String.format("INSERT INTO orders VALUES (%d, %d, %d, %d)", i, user_id, product_id,
                    quantity);
            dbEngine.executeSQL(insertCommand);
        }
    }

    // large datasets
    public static void prePopulateLargeDataset(Engine dbEngine, Random random, int size) {
        // First ensure tables exist
        dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)");
        dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)");
        dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");

        System.out.println("Prepopulating users");
        // Insert initial users
        for (int i = 0; i < size; i++) {
            String name = "User" + i;
            int age = 20 + (i % 41); // Ages between 20 and 60
            String city = getRandomCity(random);
            String insertCommand = String.format("INSERT INTO users VALUES (%d, '%s', %d, '%s')", i, name, age, city);
            dbEngine.executeSQL(insertCommand);
        }
        System.out.println("Prepopulating products");
        // Insert initial products
        for (int i = 0; i < size; i++) {
            String productName = "Product" + i;
            double price = 10 + (i % 990); // Prices between $10 and $1000
            String category = getRandomCategory(random);
            String insertCommand = String.format("INSERT INTO products VALUES (%d, '%s', %.2f, '%s')", i, productName,
                    price, category);
            dbEngine.executeSQL(insertCommand);
        }
        System.out.println("Prepopulating orders");
        // Insert initial orders
        for (int i = 0; i < size; i++) {
            int user_id = random.nextInt(9999);
            int product_id = random.nextInt(9999);
            int quantity = random.nextInt(1, 100);
            String insertCommand = String.format("INSERT INTO orders VALUES (%d, %d, %d, %d)", i, user_id, product_id,
                    quantity);
            dbEngine.executeSQL(insertCommand);
        }
    }

    // Greater proportion of write
    public static void simulateReadHeavyWorkload(Engine dbEngine, int numQueries, double percent) {
        Random random = new Random(201);

        clearDatabase();

        // First ensure tables exist
        dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)");
        dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)");
        dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");

        // Prepopulate with some initial data
        prepopulateTables(random);

        System.out.println("Starting execution of " + numQueries + " queries with "
                + (percent * 100) + "% selects and "
                + ((1 - percent) * 100) + "% other operations");

        long startTime = System.nanoTime(); // Start timing

        for (int i = 0; i < numQueries; i++) {
            if (random.nextDouble() < percent) {
                // Perform SELECT operation
                selectRandomData(random);
            } else {
                // Randomly choose between INSERT, UPDATE, and DELETE
                int operation = random.nextInt(3);
                switch (operation) {
                    case 0:
                        insertRandomData(random);
                        break;
                    case 1:
                        updateRandomData(random);
                        break;
                    case 2:
                        deleteRandomData(random);
                        break;
                }
            }

            // Print progress every 50000 queries
            // if (i % 50000 == 0) {
            // System.out.println("Processed " + i + " queries...");
            // }
        }

        // End timing
        long endTime = System.nanoTime();
        long totalTimeNanos = endTime - startTime;

        // Calculate statistics
        double totalTimeSeconds = totalTimeNanos / 1_000_000_000.0; // Convert to seconds
        double averageTimePerQuery = totalTimeNanos / (double) numQueries; // Average time in nanoseconds
        double averageTimePerQueryMs = averageTimePerQuery / 1_000_000.0; // Convert to milliseconds

        // Print performance statistics
        System.out.println("\nPerformance Statistics:");
        System.out.println("------------------------");
        System.out.printf("Total Execution Time: %.2f seconds%n", totalTimeSeconds);
        System.out.printf("Total Queries Executed: %d%n", numQueries);
        System.out.printf("Average Time per Query: %.4f ms%n", averageTimePerQueryMs);
        System.out.printf("Queries per Second: %.2f%n", numQueries / totalTimeSeconds);
        clearDatabase();
    }

    // Greater proportion of write
    public static void simulateWriteHeavyWorkload(Engine dbEngine, int numQueries, double percent) {
        Random random = new Random(201);

        clearDatabase();

        // First ensure tables exist
        dbEngine.executeSQL("CREATE TABLE users (id, name, age, city)");
        dbEngine.executeSQL("CREATE TABLE products (id, name, price, category)");
        dbEngine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");

        // Prepopulate with some initial data
        prepopulateTables(random);

        System.out.println("Starting execution of " + numQueries + " queries with "
                + (percent * 100) + "% inserts and "
                + ((1 - percent) * 100) + "% other operations");

        long startTime = System.nanoTime();

        for (int i = 0; i < numQueries; i++) {
            if (random.nextDouble() < percent) {
                // Perform INSERT operation
                insertRandomData(random);
            } else {
                // Randomly choose between SELECT, UPDATE, and DELETE
                int operation = random.nextInt(3);
                switch (operation) {
                    case 0:
                        selectRandomData(random);
                        break;
                    case 1:
                        updateRandomData(random);
                        break;
                    case 2:
                        deleteRandomData(random);
                        break;
                }
            }

            // Print progress every 100 queries
            if (i % 50000 == 0) {
                System.out.println("Processed " + i + " queries...");
            }
        }

        // End timing
        long endTime = System.nanoTime();
        long totalTimeNanos = endTime - startTime;

        // Calculate statistics
        double totalTimeSeconds = totalTimeNanos / 1_000_000_000.0; // Convert to seconds
        double averageTimePerQuery = totalTimeNanos / (double) numQueries; // Average time in nanoseconds
        double averageTimePerQueryMs = averageTimePerQuery / 1_000_000.0; // Convert to milliseconds

        // Print performance statistics
        System.out.println("\nPerformance Statistics:");
        System.out.println("------------------------");
        System.out.printf("Total Execution Time: %.2f seconds%n", totalTimeSeconds);
        System.out.printf("Total Queries Executed: %d%n", numQueries);
        System.out.printf("Average Time per Query: %.4f ms%n", averageTimePerQueryMs);
        System.out.printf("Queries per Second: %.2f%n", numQueries / totalTimeSeconds);
        clearDatabase();
    }

    // Helper method to insert random data into users, products, or orders table
    private static void insertRandomData(Random random) {
        int tableChoice = random.nextInt(3);
        switch (tableChoice) {
            case 0: // Insert into users table
                int id = random.nextInt(10000) + 10000;
                String name = "User" + id;
                int age = random.nextInt(60) + 20;
                String city = getRandomCity(random);
                String insertUserQuery = "INSERT INTO users VALUES (" + id + ", '" + name + "', " + age + ", '" + city
                        + "')";
                dbEngine.executeSQL(insertUserQuery);
                break;
            case 1: // Insert into products table
                int productId = random.nextInt(1000) + 10000;
                String productName = "Product" + productId;
                double price = 50 + (random.nextDouble() * 1000);
                String category = getRandomCategory(random);
                String insertProductQuery = "INSERT INTO products VALUES (" + productId + ", '" + productName + "', "
                        + price + ", '" + category + "')";
                dbEngine.executeSQL(insertProductQuery);
                break;
            case 2: // Insert into orders table
                int orderId = random.nextInt(10000) + 1;
                int userId = random.nextInt(10000) + 1;
                int productIdRef = random.nextInt(1000) + 1;
                int quantity = random.nextInt(10) + 1;
                String insertOrderQuery = "INSERT INTO orders VALUES (" + orderId + ", " + userId + ", " + productIdRef
                        + ", " + quantity + ")";
                dbEngine.executeSQL(insertOrderQuery);
                break;
        }
    }

    // Helper method to randomly select data from tables
    private static void selectRandomData(Random random) {
        int tableChoice = random.nextInt(3);
        String selectQuery;
        switch (tableChoice) {
            case 0:
                selectQuery = "SELECT * FROM users";
                break;
            case 1:
                selectQuery = "SELECT * FROM products";
                break;
            case 2:
                selectQuery = "SELECT * FROM orders";
                break;
            default:
                selectQuery = "SELECT * FROM users";
        }
        dbEngine.executeSQL(selectQuery);
    }

    // Helper method to update random data in the tables
    private static void updateRandomData(Random random) {
        int tableChoice = random.nextInt(3);
        switch (tableChoice) {
            case 0: // Update users table
                int id = random.nextInt(10000) + 1;
                int newAge = random.nextInt(60) + 20;
                String updateUserQuery = "UPDATE users SET age = " + newAge + " WHERE id = " + id;
                dbEngine.executeSQL(updateUserQuery);
                break;
            case 1: // Update products table
                int productId = random.nextInt(1000) + 1;
                double newPrice = 50 + (random.nextDouble() * 1000);
                String updateProductQuery = "UPDATE products SET price = " + newPrice + " WHERE id = " + productId;
                dbEngine.executeSQL(updateProductQuery);
                break;
            case 2: // Update orders table
                int orderId = random.nextInt(10000) + 1;
                int newQuantity = random.nextInt(10) + 1;
                String updateOrderQuery = "UPDATE orders SET quantity = " + newQuantity + " WHERE id = " + orderId;
                dbEngine.executeSQL(updateOrderQuery);
                break;
        }
    }

    // Helper method to delete random data from tables
    private static void deleteRandomData(Random random) {
        int tableChoice = random.nextInt(3);
        switch (tableChoice) {
            case 0: // Delete from users table
                int userId = random.nextInt(10000) + 1;
                String deleteUserQuery = "DELETE FROM users WHERE id = " + userId;
                dbEngine.executeSQL(deleteUserQuery);
                break;
            case 1: // Delete from products table
                int productId = random.nextInt(1000) + 1;
                String deleteProductQuery = "DELETE FROM products WHERE id = " + productId;
                dbEngine.executeSQL(deleteProductQuery);
                break;
            case 2: // Delete from orders table
                int orderId = random.nextInt(10000) + 1;
                String deleteOrderQuery = "DELETE FROM orders WHERE id = " + orderId;
                dbEngine.executeSQL(deleteOrderQuery);
                break;
        }
    }

    // Helper method to execute a complex SELECT query with WHERE, AND, OR, >, <,
    // LIKE
    private static void complexSelectQuery(Random random) {
        int tableChoice = random.nextInt(2); // Complex queries only on users and products for now
        String complexSelectQuery;
        switch (tableChoice) {
            case 0: // Complex SELECT on users
                int minAge = random.nextInt(20) + 20;
                int maxAge = minAge + random.nextInt(30);
                String city = getRandomCity(random);
                complexSelectQuery = "SELECT * FROM users WHERE age > " + minAge + " AND age < " + maxAge;
                break;
            case 1: // Complex SELECT on products
                double minPrice = 50 + (random.nextDouble() * 200);
                double maxPrice = minPrice + random.nextDouble() * 500;
                complexSelectQuery = "SELECT * FROM products WHERE price > " + minPrice + " AND price < " + maxPrice;
                break;
            case 2: // Complex SELECT on products
                double minPrice2 = 50 + (random.nextDouble() * 200);
                String category = getRandomCategory(random);
                complexSelectQuery = "SELECT * FROM products WHERE price > " + minPrice2 + " AND category = "
                        + category;
                break;
            default:
                complexSelectQuery = "SELECT * FROM users";
        }
        dbEngine.executeSQL(complexSelectQuery);
    }

    // Helper method to execute a complex UPDATE query with WHERE
    private static void complexUpdateQuery(Random random) {
        int tableChoice = random.nextInt(2); // Complex updates only on users and products for now
        switch (tableChoice) {
            case 0: // Complex UPDATE on users
                int newAge = random.nextInt(60) + 20;
                String city = getRandomCity(random);
                String updateUserQuery = "UPDATE users SET age = " + newAge + " WHERE city = '" + city + "'";
                dbEngine.executeSQL(updateUserQuery);
                break;
            case 1: // Complex UPDATE on products
                double newPrice = 50 + (random.nextDouble() * 1000);
                String category = getRandomCategory(random);
                String updateProductQuery = "UPDATE products SET price = " + newPrice + " WHERE category = '" + category
                        + "'";
                dbEngine.executeSQL(updateProductQuery);
                break;
        }
    }

    // Helper method to return a random city
    private static String getRandomCity(Random random) {
        String[] cities = { "New York", "Los Angeles", "Chicago", "Boston", "Miami", "Seattle", "Austin", "Dallas",
                "Atlanta", "Denver" };
        return cities[random.nextInt(cities.length)];
    }

    // Helper method to return a random category for products
    private static String getRandomCategory(Random random) {
        String[] categories = { "Electronics", "Appliances", "Clothing", "Furniture", "Toys", "Sports", "Books",
                "Beauty", "Garden" };
        return categories[random.nextInt(categories.length)];
    }
}