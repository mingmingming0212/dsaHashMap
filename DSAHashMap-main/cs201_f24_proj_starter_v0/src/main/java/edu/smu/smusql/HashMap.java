package edu.smu.smusql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HashMap<K, V> {
    private Entry<K, V>[] buckets;
    private int capacity;
    private int size;
    private static final int INITIAL_CAPACITY = 32;

    // Constructor to initialize HashMap
    public HashMap() {
        this.capacity = INITIAL_CAPACITY;
        this.size = 0;
        this.buckets = new Entry[capacity];
    }

    // Entry class to store key-value pairs
    static class Entry<K, V> {
        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }

    // Hash function to get the index for a key
    private int getBucketIndex(K key) {
        int hashCode = Objects.hashCode(key);
        return Math.abs(hashCode) % capacity;
    }

    // Resize and rehash only when the array is full
    private void resize() {
        int newCapacity = capacity * 2;
        Entry<K, V>[] newBuckets = new Entry[newCapacity];
        for (Entry<K, V> entry : buckets) {
            if (entry != null) {
                int newIndex = Math.abs(entry.key.hashCode()) % newCapacity;
                while (newBuckets[newIndex] != null) {
                    newIndex = (newIndex + 1) % newCapacity;  // Linear probing
                }
                newBuckets[newIndex] = entry;
            }
        }
        capacity = newCapacity;
        buckets = newBuckets;
    }

    // Put key-value pair into the hash map
    public void put(K key, V value) {
        int index = getBucketIndex(key);

        // Linear probing to find an empty slot or update existing key
        while (buckets[index] != null) {
            if (buckets[index].getKey().equals(key)) {
                buckets[index].setValue(value);  // Update value if key exists
                return;
            }
            index = (index + 1) % capacity;
        }

        // Insert new entry
        buckets[index] = new Entry<>(key, value);
        size++;

        // Resize if all buckets are full (array "full" condition)
        if (size == capacity) {
            resize();
        }
    }

    // Get value for a given key
    public V get(K key) {
        int index = getBucketIndex(key);

        // Linear probing to find the key
        while (buckets[index] != null) {
            if (buckets[index].getKey().equals(key)) {
                return buckets[index].getValue();
            }
            index = (index + 1) % capacity;
        }

        return null; // Key not found
    }

    // Remove key-value pair from the hash map
    public void remove(K key) {
        int index = getBucketIndex(key);

        // Linear probing to find the key
        while (buckets[index] != null) {
            if (buckets[index].getKey().equals(key)) {
                buckets[index] = null;  // Remove entry
                size--;
                // Rehash subsequent entries to fill gaps
                index = (index + 1) % capacity;
                while (buckets[index] != null) {
                    Entry<K, V> entryToRehash = buckets[index];
                    buckets[index] = null;
                    size--;
                    put(entryToRehash.getKey(), entryToRehash.getValue());
                    index = (index + 1) % capacity;
                }
                return;
            }
            index = (index + 1) % capacity;
        }
    }

    // Get all entries as an Iterable
    public Iterable<Entry<K, V>> entrySet() {
        List<Entry<K, V>> entries = new ArrayList<>(size);
        for (Entry<K, V> entry : buckets) {
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }



    // Get the size of the hash map
    public int size() {
        return size;
    }

}
