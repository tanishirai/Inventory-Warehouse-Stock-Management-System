package com.stockpilot;

import com.stockpilot.cli.WarehouseCLI;
import com.stockpilot.dao.DBConnectionManager;

/** Application entry point: wires up the database and hands off to the CLI. */
public class Main {
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(DBConnectionManager::close));
        try {
            DBConnectionManager.getConnection(); // eagerly create data/stockpilot.db + schema on startup
        } catch (Exception e) {
            System.err.println("Fatal: could not initialise the database. " + e.getMessage());
            System.exit(1);
        }
        new WarehouseCLI().start();
    }
}
