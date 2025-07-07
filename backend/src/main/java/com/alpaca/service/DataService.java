package com.alpaca.service;

/**
 * Service interface for initializing essential application data.
 *
 * <p>This interface defines a method for setting up initial data, such as default roles,
 * permissions, users, and profiles. The implementation is expected to be invoked during application
 * startup to ensure the system has the necessary base configuration.
 */
public interface DataService {

    /**
     * Initializes the application's default data.
     *
     * <p>This method is responsible for creating and saving initial roles, permissions, users, and
     * profiles required for the proper functioning of the system. It should be executed only once
     * upon application startup.
     */
    void initializeData();
}
