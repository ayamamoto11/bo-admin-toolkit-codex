package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.reports.ReportInventoryModule;

import java.util.Scanner;

public class MainMenu {
    private final Scanner scanner;
    private final ReportInventoryModule reportInventoryModule;

    public MainMenu(Scanner scanner, ReportInventoryModule reportInventoryModule) {
        this.scanner = scanner;
        this.reportInventoryModule = reportInventoryModule;
    }

    public static void main(String[] args) {
        try (ConnectionManager connectionManager = new ConnectionManager();
             Scanner scanner = new Scanner(System.in)) {
            ReportInventoryModule reportInventoryModule = new ReportInventoryModule(connectionManager);
            new MainMenu(scanner, reportInventoryModule).run();
        } catch (IllegalStateException exception) {
            System.err.println("Configuration error: " + exception.getMessage());
        }
    }

    public void run() {
        boolean running = true;

        while (running) {
            printMenu();
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    runReportInventory();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Unknown option. Please try again.");
                    break;
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("SAP BusinessObjects Administration Utilities");
        System.out.println("1. Report inventory");
        System.out.println("0. Exit");
        System.out.print("Select an option: ");
    }

    private void runReportInventory() {
        try {
            reportInventoryModule.printWebIntelligenceReports();
        } catch (SDKException exception) {
            System.err.println("Unable to query report inventory: " + exception.getMessage());
        }
    }
}
