package edu.smu.smusql;

import java.util.*;

public class Engine {
    // stores the contents of database tables in-memory using a HashMap for quick lookup
    private HashMap<String, Table> tables = new HashMap<>();

    public String executeSQL(String query) {
        String[] tokens = query.trim().split("\\s+");
        String command = tokens[0].toUpperCase();

        switch (command) {
            case "CREATE":
                return create(tokens);
            case "INSERT":
                return insert(tokens);
            case "SELECT":
                return select(tokens);
            case "UPDATE":
                return update(tokens);
            case "DELETE":
                return delete(tokens);
            default:
                return "ERROR: Unknown command";
        }
    }

    // INSERT command implementation
    public String insert(String[] tokens) {
        // "INSERT INTO users VALUES (%d, '%s', %d, '%s')" | INTO token in idx = 1
        final int INTO_CLAUSE = 1;
        if (!tokens[INTO_CLAUSE].toUpperCase().equals("INTO")) {
            return "ERROR: Invalid INSERT INTO syntax";
        }

        // "INSERT INTO users | users (tableName) token in idx = 2
        String tableName = tokens[2];
        Table table = tables.get(tableName);

        if (table == null) {
            return "Error: no such table: " + tableName;
        }

        String valueList = queryBetweenParentheses(tokens, 4); // Get values list between parentheses
        List<String> values = Arrays.asList(valueList.split(","));
        values.replaceAll(String::trim);

        List<String> columns = table.getColumnNames();
        if (values.size() != columns.size()) {
            return "ERROR: Column count doesn't match value count";
        }

        ArrayList<Object> rowValues = new ArrayList<>(values);
        table.insertRow(rowValues);

        return "Row inserted into " + tableName;
    }

    // DELETE command implementation
    public String delete(String[] tokens) {
        if (!tokens[1].toUpperCase().equals("FROM")) {
            return "ERROR: Invalid DELETE syntax";
        }

        // "DELETE FROM users WHERE id = " | users idx = 2 (tableName)
        String tableName = tokens[2];
        Table table = tables.get(tableName);

        if (table == null) {
            return "Error: no such table: " + tableName;
        }

        // Parse WHERE clause conditions
        List<String[]> whereClauseConditions = new ArrayList<>();
        // delete from users WHERE | where idx = 3
        final int WHERE_CLAUSE_DELETE = 3;
        if (tokens.length > WHERE_CLAUSE_DELETE && tokens[WHERE_CLAUSE_DELETE].toUpperCase().equals("WHERE")) {
            for (int i = WHERE_CLAUSE_DELETE + 1; i < tokens.length; i++) { // read after where clause
                if (isOperator(tokens[i])) {
                    String column = tokens[i - 1];
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[]{column, operator, value});
                    i++; // Skip the value token
                }
            }
        }

        int rowsAffected = 0;
        Iterator<edu.smu.smusql.HashMap.Entry<Integer, Table.Row>> iterator = table.rows.entrySet().iterator();
        while (iterator.hasNext()) {
            edu.smu.smusql.HashMap.Entry<Integer, Table.Row> entry = iterator.next();
            int primaryKey = entry.getKey();
            if (evaluateWhereConditions(entry.getValue(), whereClauseConditions, table.getColumnNames())) {
                table.deleteRow(primaryKey);  // Use deleteRow method
                rowsAffected++;
            }
        }

        return "Deleted " + rowsAffected + " rows from " + tableName;
    }

    // SELECT command implementation
    public String select(String[] tokens) {
        if (!tokens[1].equals("*") || !tokens[2].toUpperCase().equals("FROM")) {
            return "ERROR: Invalid SELECT syntax";
        }

        // SELECT * FROM tableName | tableName idx = 3
        String tableName = tokens[3];
        Table table = tables.get(tableName);

        if (table == null) {
            return "Error: no such table: " + tableName;
        }

        StringBuilder result = new StringBuilder();
        result.append(String.join("\t", table.getColumnNames())).append("\n");

        // Parse WHERE clause conditions
        List<String[]> whereClauseConditions = new ArrayList<>();
        // Select * from tableName where | where idx = 4
        final int WHERE_CLAUSE_SELECT = 4;
        if (tokens.length > WHERE_CLAUSE_SELECT && tokens[WHERE_CLAUSE_SELECT].toUpperCase().equals("WHERE")) {
            for (int i = WHERE_CLAUSE_SELECT + 1; i < tokens.length; i++) {
                if (isOperator(tokens[i])) {
                    String column = tokens[i - 1];
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[]{column, operator, value});
                    i++;
                }
            }
        }

        // Iterate over rows in the custom HashMap and filter based on WHERE conditions
        for (edu.smu.smusql.HashMap.Entry<Integer, Table.Row> entry : table.rows.entrySet()) {
            Table.Row row = entry.getValue();
            if (evaluateWhereConditions(row, whereClauseConditions, table.getColumnNames())) {
                for (Object value : row.getValues()) {
                    result.append(value.toString()).append("\t");
                }
                result.append("\n");
            }
        }
        return result.toString();
    }

    // UPDATE command implementation
    public String update(String[] tokens) {
        String tableName = tokens[1];
        Table table = tables.get(tableName);

        if (table == null) {
            return "Error: no such table: " + tableName;
        }

        String columnName = tokens[3];
        String newValue = tokens[5];  // This is the new value to set for the specified column
        int WHERE_CLAUSE_UPDATE = 6;

        // Parse WHERE clause conditions
        List<String[]> whereClauseConditions = new ArrayList<>();
        if (tokens.length > WHERE_CLAUSE_UPDATE && tokens[WHERE_CLAUSE_UPDATE].toUpperCase().equals("WHERE")) {
            for (int i = WHERE_CLAUSE_UPDATE + 1; i < tokens.length; i++) {
                if (isOperator(tokens[i])) {
                    String column = tokens[i - 1];
                    String operator = tokens[i];
                    String value = tokens[i + 1];
                    whereClauseConditions.add(new String[]{column, operator, value});
                    i++;  // Skip the value token
                }
            }
        }

        int rowsAffected = 0;
        int columnIndex = table.getColumnNames().indexOf(columnName);
        if (columnIndex == -1) {
            return "Error: no such column: " + columnName;
        }

        // Iterate over rows in the custom HashMap and update matching rows
        for (edu.smu.smusql.HashMap.Entry<Integer, Table.Row> entry : table.rows.entrySet()) {
            Table.Row row = entry.getValue();
            if (evaluateWhereConditions(row, whereClauseConditions, table.getColumnNames())) {
                row.setValue(columnIndex, newValue);  // Set the new value in the specified column
                rowsAffected++;
            }
        }

        return "Updated " + rowsAffected + " rows in " + tableName;
    }


    // CREATE TABLE command implementation
    public String create(String[] tokens) {
        if (!tokens[1].toUpperCase().equals("TABLE")) {
            return "ERROR: Invalid CREATE TABLE syntax";
        }

        String tableName = tokens[2];
        String columnList = queryBetweenParentheses(tokens, 3); // Get columns between parentheses
        List<String> columns = Arrays.asList(columnList.split(","));
        columns.replaceAll(String::trim);

        Table table = new Table(tableName, columns);
        tables.put(tableName, table);

        return "Table " + tableName + " created.";
    }

    // HELPER METHODS
    private String queryBetweenParentheses(String[] tokens, int startIndex) {
        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < tokens.length; i++) {
            result.append(tokens[i]).append(" ");
        }
        return result.toString().trim().replaceAll("\\(", "").replaceAll("\\)", "");
    }

    private boolean isOperator(String token) {
        return token.equals("=") || token.equals(">") || token.equals("<") || token.equals(">=") || token.equals("<=");
    }

    private boolean evaluateWhereConditions(Table.Row row, List<String[]> conditions, List<String> columns) {
        boolean match = true;
        for (String[] condition : conditions) {
            String column = condition[0];
            String operator = condition[1];
            String value = condition[2];
            int columnIndex = columns.indexOf(column);
            Object columnValue = row.getValue(columnIndex);

            if (!evaluateCondition(columnValue.toString(), operator, value)) {
                match = false;
                break;
            }
        }
        return match;
    }

    private boolean evaluateCondition(String columnValue, String operator, String value) {
        if (columnValue == null) return false;

        Comparator<String> comparator = isNumeric(columnValue) && isNumeric(value)
                ? Comparator.comparingDouble(Double::parseDouble)
                : String::compareTo;

        int comparison = comparator.compare(columnValue, value);

        switch (operator) {
            case "=": return comparison == 0;
            case ">": return comparison > 0;
            case "<": return comparison < 0;
            case ">=": return comparison >= 0;
            case "<=": return comparison <= 0;
            default: return false;
        }
    }

    // Helper method to determine if a string is numeric
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
