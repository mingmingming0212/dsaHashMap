package edu.smu.smusql;

import java.util.ArrayList;
import java.util.List;

public class Table {
    private int primaryKeyCounter = 0;
    public String tableName;
    public List<String> columnNames;  // Table schema (column names)
    public HashMap<Integer, Row> rows; // Stores the rows (assuming primary key is an Integer)

    // Constructor to initialize the table
    public Table(String tableName, List<String> columnNames) {
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.rows = new HashMap<>(); // Initial capacity for the HashMap
    }

    // Row class to store the data for each row
    public static class Row {
        public ArrayList<Object> values; // Column values for a single row

        public Row(ArrayList<Object> values) {
            this.values = values;
        }

        public ArrayList<Object> getValues() {
            return values;
        }

        public Object getValue(int index) {
            return values.get(index);
        }

        public void setValue(int index, Object value) {
            values.set(index, value);
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }

    // Insert a new row into the table
    public void insertRow(ArrayList<Object> values) {
        if (values.size() != columnNames.size()) {
            throw new IllegalArgumentException("Number of values doesn't match the schema");
        }
        primaryKeyCounter++;
        Row row = new Row(values);
        rows.put(primaryKeyCounter, row); // Store row in the hash map by primary key
    }

    // Select a row by primary key
    public Row selectRow(int primaryKey) {
        return rows.get(primaryKey);
    }

    // Update a row by primary key
    public void updateRow(int primaryKey, ArrayList<Object> newValues) {
        if (newValues.size() != columnNames.size()) {
            throw new IllegalArgumentException("Number of values doesn't match the schema");
        }
        Row row = rows.get(primaryKey);
        if (row != null) {
            row = new Row(newValues);  // Replace with new values
            rows.put(primaryKey, row); // Update the row in the map
        }
    }

    // Delete a row by primary key
    public void deleteRow(int primaryKey) {
        rows.remove(primaryKey);  // Remove row from the hash map
    }

    // Get the column names (schema)
    public List<String> getColumnNames() {
        return columnNames;
    }

}
