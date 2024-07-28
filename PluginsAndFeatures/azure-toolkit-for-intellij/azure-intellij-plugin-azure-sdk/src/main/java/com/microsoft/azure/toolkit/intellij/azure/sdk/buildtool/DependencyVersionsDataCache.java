package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A cache to store the dependency versions data for a certain period of time.
 * The cache is stored in memory and also saved to a file to persist the data across IDE restarts.
 * The cache is cleared at regular intervals to prevent it from growing indefinitely.
 *
 * @param <T> The type of the data to be stored in the cache.
 */
class DependencyVersionsDataCache<T> implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(DependencyVersionsDataCache.class.getName());

    // The serial version UID is used to verify that the class is compatible with the serialized object.
    @Serial
    private static final long serialVersionUID = 1L;

    // The cache is stored in a ConcurrentHashMap to ensure thread safety.
    private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();

    // The interval at which the cache is cleared. The cache is cleared every 30 days.
    private static final long CLEANUP_INTERVAL = TimeUnit.DAYS.toMillis(30); // 1 day

    // The file where the cache is saved.
    private final File cacheFile;

    /**
     * Creates a new instance of the DependencyVersionsDataCache class.
     *
     * @param cacheFileName The name of the file where the cache is saved.
     */
    DependencyVersionsDataCache(String cacheFileName) {
        this.cacheFile = new File(cacheFileName);
        loadCacheFromFile();

        // Schedule the cache cleanup task to run at regular intervals
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::clear, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Puts a value into the cache.
     *
     * @param key   The key to associate with the value.
     * @param value The value to store in the cache.
     */
    void put(String key, T value) {
        cache.put(key, value);
        saveCacheToFile();
    }

    /**
     * Gets a value from the cache.
     *
     * @param key The key to retrieve the value for.
     * @return The value associated with the key, or null if the key is not found.
     */
    T get(String key) {
        return cache.get(key);
    }

    /**
     * Clears the cache.
     * This method is called at regular intervals to prevent the cache from growing indefinitely.
     */
    void clear() {
        cache.clear();
        saveCacheToFile();
    }

    /**
     * Loads the cache from the file.
     * If the file exists, the cache is loaded from the file.
     * If the file does not exist, the cache is left empty.
     */
    private void loadCacheFromFile() {
        if (cacheFile.exists()) {
            // Load the cache from the file
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {

                // Use a temporary variable to hold the deserialized object
                Object loadedObject = ois.readObject();

                // Check if the deserialized object is an instance of ConcurrentHashMap
                if (loadedObject instanceof ConcurrentHashMap) {

                    // Cast the object to ConcurrentHashMap<String, T>
                    ConcurrentHashMap<String, T> loadedCache = (ConcurrentHashMap<String, T>) loadedObject;

                    // Put all the entries from the loaded cache into the current cache
                    cache.putAll(loadedCache);
                } else {
                    LOGGER.severe("Failed to load cache from file: Invalid cache format");
                }
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.severe("Failed to load cache from file: " + e);
            }
        }
    }

    /**
     * Saves the cache to the file.
     * The cache is saved to the file to persist the data across IDE restarts.
     */
    private void saveCacheToFile() {

        // Save the cache to the file using the ObjectOutputStream class.
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
            oos.writeObject(cache);
        } catch (IOException e) {
            LOGGER.severe("Failed to save cache to file: " + e.getMessage());
        }
    }
}
