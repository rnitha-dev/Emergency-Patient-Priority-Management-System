import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Main is the application entry point.
 * 
 * Default Execution:
 * Launches the graphical browser-based Web Application (EmergencyWebServer)
 * and automatically opens Google Chrome at http://localhost:8080.
 * 
 * Optional Flag:
 * Passing "--console" runs the classic console-driven text interface.
 */
public class Main {

    private static final EmergencyManagementSystem system = new EmergencyManagementSystem();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        // If --console flag is provided, run console mode
        if (args.length > 0 && args[0].equalsIgnoreCase("--console")) {
            runConsoleMenu();
            return;
        }

        // Default: Start Full Web Application and launch Google Chrome
        System.out.println("=========================================================");
        System.out.println("      EMERGENCY PATIENT PRIORITY MANAGEMENT SYSTEM       ");
        System.out.println("               Hospital Triage Web Portal                ");
        System.out.println("=========================================================");
        System.out.println("Starting web application server on http://localhost:8080 ...");
        
        EmergencyWebServer.main(args);
    }

    private static void runConsoleMenu() {
        int loadedHistory = system.loadTreatmentHistoryFromFile();
        if (loadedHistory > 0) {
            System.out.printf("[System] Successfully loaded %d past treatment records from disk.%n", loadedHistory);
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice: ", -1, 12);

            switch (choice) {
                case 1: handleRegisterPatient(); break;
                case 2: handleDisplayWaitingPatients(); break;
                case 3: handleSearchPatient(); break;
                case 4: handleUpdateMedicalCondition(); break;
                case 5: handleUpdatePriority(); break;
                case 6: handleTreatNextPatient(); break;
                case 7: handleRemovePatient(); break;
                case 8: handleDisplayTreatmentHistory(); break;
                case 9: handleMultiplePatientRegistration(); break;
                case 10: handleConcurrentRegistrationDemo(); break;
                case 11: handleConcurrentUpdateDemo(); break;
                case 12: handleFilePersistenceMenu(); break;
                case 0:
                    System.out.println("\nSaving session and shutting down Emergency System. Goodbye!");
                    try { system.saveTreatmentHistoryToFile(); } catch (Exception ignored) {}
                    running = false;
                    break;
                default:
                    System.out.println("[Error] Invalid selection. Please choose an option from the menu.");
            }

            if (running) {
                promptEnterKey();
            }
        }
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println("        EMERGENCY PATIENT PRIORITY MANAGEMENT SYSTEM     ");
        System.out.println("=========================================================");
        System.out.println(" 1. Register Patient");
        System.out.println(" 2. Display Waiting Patients (Treatment Order)");
        System.out.println(" 3. Search Patient by ID");
        System.out.println(" 4. Update Medical Condition (Recalculate Priority)");
        System.out.println(" 5. Update Patient Priority Manually (1 - 5)");
        System.out.println(" 6. Treat Next Patient");
        System.out.println(" 7. Remove Patient from Waiting Queue");
        System.out.println(" 8. Display Treatment History");
        System.out.println(" 9. Multiple Patient Registration (Sequential)");
        System.out.println("10. Concurrent Patient Registration (Multithreading Demo)");
        System.out.println("11. Concurrent Priority Update (Multithreading Demo)");
        System.out.println("12. Save / Reload Treatment History from File");
        System.out.println(" 0. Exit System");
        System.out.println("=========================================================");
    }

    private static void handleRegisterPatient() {
        System.out.println("\n----------------- REGISTER NEW PATIENT ------------------");
        try {
            int id = readPositiveInt("Enter Patient ID (e.g. 101): ");
            String name = readNonEmptyString("Enter Patient Full Name: ");
            int age = readInt("Enter Age (1 - 120): ", 1, 120);
            String gender = readNonEmptyString("Enter Gender (Male/Female/Other): ");
            String phone = readNonEmptyString("Enter Phone Number: ");

            System.out.println("\nSupported Medical Conditions:");
            List<String> conditions = system.getPriorityCalculator().getSupportedConditions();
            for (int i = 0; i < conditions.size(); i++) {
                String cond = conditions.get(i);
                int prio = system.getPriorityCalculator().calculatePriority(cond);
                String prioName = system.getPriorityCalculator().getPriorityLevelName(prio);
                System.out.printf("  %d. %-18s (Priority %d - %s)%n", i + 1, cond, prio, prioName);
            }

            String conditionInput = readNonEmptyString("Enter Medical Condition: ");
            try {
                int condIdx = Integer.parseInt(conditionInput);
                if (condIdx >= 1 && condIdx <= conditions.size()) {
                    conditionInput = conditions.get(condIdx - 1);
                }
            } catch (NumberFormatException ignored) {}

            int priority = system.getPriorityCalculator().calculatePriority(conditionInput);
            String priorityName = system.getPriorityCalculator().getPriorityLevelName(priority);

            System.out.printf("-> Calculated Emergency Priority: %d (%s)%n", priority, priorityName);

            boolean isTrauma = readYesNo("Is this a severe Trauma Case? (Y/N): ");
            String notes = readString("Enter Triage Nurse Notes (optional): ");
            if (notes.isEmpty()) notes = "Initial intake assessment completed.";

            EmergencyPatient patient = new EmergencyPatient(id, name, age, gender, phone, conditionInput, priority, notes, isTrauma);
            system.registerPatient(patient);

            System.out.println("\n[Success] Patient registered successfully in Triage Queue!");
            patient.displayDetails();

        } catch (InvalidPatientException e) {
            System.err.println("\n[Validation Error] " + e.getMessage());
        }
    }

    private static void handleDisplayWaitingPatients() {
        System.out.println("\n=========================================================================================");
        System.out.println("                        CURRENT WAITING EMERGENCY PATIENTS                               ");
        System.out.println("=========================================================================================");

        List<EmergencyPatient> queue = system.getWaitingPatients();
        if (queue.isEmpty()) {
            System.out.println("No patients currently waiting in the emergency queue.");
            return;
        }

        System.out.printf("%-4s | %-6s | %-16s | %-4s | %-16s | %-14s | %-8s | %-19s%n",
                "Pos", "ID", "Name", "Age", "Condition", "Priority", "Trauma", "Registration Time");
        System.out.println("-----+--------+------------------+------+------------------+----------------+----------+--------------------");

        int position = 1;
        for (EmergencyPatient p : queue) {
            String prioStr = String.format("%d - %s", p.getPriority(), p.getPriorityLevelName());
            System.out.printf("%-4d | %-6d | %-16s | %-4d | %-16s | %-14s | %-8s | %-19s%n",
                    position++,
                    p.getPatientId(),
                    truncate(p.getName(), 16),
                    p.getAge(),
                    truncate(p.getMedicalCondition(), 16),
                    truncate(prioStr, 14),
                    p.isTraumaCase() ? "YES" : "No",
                    p.getFormattedRegistrationTime()
            );
        }
        System.out.println("=========================================================================================");
        System.out.printf("Total Patients Waiting: %d%n", queue.size());
    }

    private static void handleSearchPatient() {
        System.out.println("\n------------------ SEARCH PATIENT -------------------");
        int id = readPositiveInt("Enter Patient ID to search: ");
        try {
            EmergencyPatient patient = system.searchPatient(id);
            System.out.println("\n[Patient Record Found]:");
            patient.displayDetails();
        } catch (InvalidPatientException e) {
            System.err.println("\n[Not Found] " + e.getMessage());
        }
    }

    private static void handleUpdateMedicalCondition() {
        System.out.println("\n------------- UPDATE MEDICAL CONDITION --------------");
        int id = readPositiveInt("Enter Patient ID to update: ");
        try {
            EmergencyPatient patient = system.searchPatient(id);
            System.out.printf("Current Status: Name: %s | Condition: %s | Priority: %d (%s)%n",
                    patient.getName(), patient.getMedicalCondition(), patient.getPriority(), patient.getPriorityLevelName());

            System.out.println("\nSupported Medical Conditions:");
            List<String> conditions = system.getPriorityCalculator().getSupportedConditions();
            for (int i = 0; i < conditions.size(); i++) {
                String cond = conditions.get(i);
                int prio = system.getPriorityCalculator().calculatePriority(cond);
                System.out.printf("  %d. %-18s (Priority %d - %s)%n", i + 1, cond, prio, system.getPriorityCalculator().getPriorityLevelName(prio));
            }

            String newCondition = readNonEmptyString("Enter New Medical Condition: ");
            try {
                int condIdx = Integer.parseInt(newCondition);
                if (condIdx >= 1 && condIdx <= conditions.size()) {
                    newCondition = conditions.get(condIdx - 1);
                }
            } catch (NumberFormatException ignored) {}

            system.updateMedicalCondition(id, newCondition);
            System.out.println("\n[Success] Medical condition updated and queue re-ordered successfully!");
            patient.displayDetails();

        } catch (InvalidPatientException e) {
            System.err.println("\n[Error] " + e.getMessage());
        }
    }

    private static void handleUpdatePriority() {
        System.out.println("\n-------------- UPDATE PATIENT PRIORITY --------------");
        int id = readPositiveInt("Enter Patient ID to override priority: ");
        try {
            EmergencyPatient patient = system.searchPatient(id);
            System.out.printf("Current Status: Name: %s | Condition: %s | Priority: %d (%s)%n",
                    patient.getName(), patient.getMedicalCondition(), patient.getPriority(), patient.getPriorityLevelName());

            int newPriority = readInt("Enter New Priority (1=CRITICAL, 2=VERY HIGH, 3=HIGH, 4=MEDIUM, 5=LOW): ", 1, 5);
            system.updatePriority(id, newPriority);

            System.out.println("\n[Success] Priority manually overridden and queue re-ordered successfully!");
            patient.displayDetails();

        } catch (InvalidPatientException e) {
            System.err.println("\n[Error] " + e.getMessage());
        }
    }

    private static void handleTreatNextPatient() {
        System.out.println("\n---------------- TREAT NEXT PATIENT -----------------");
        try {
            EmergencyPatient treated = system.treatNextPatient();
            System.out.println("\n==================================================");
            System.out.println("                 TREATING PATIENT                 ");
            System.out.println("==================================================");
            System.out.printf("  ID        : %d%n", treated.getPatientId());
            System.out.printf("  Name      : %s%n", treated.getName());
            System.out.printf("  Condition : %s%n", treated.getMedicalCondition());
            System.out.printf("  Priority  : %d (%s)%n", treated.getPriority(), treated.getPriorityLevelName());
            System.out.printf("  Status    : %s%n", treated.getTreatmentTime());
            System.out.println("==================================================");
            System.out.println("Patient moved to Treatment History.");
        } catch (InvalidPatientException e) {
            System.err.println("\n" + e.getMessage());
        }
    }

    private static void handleRemovePatient() {
        System.out.println("\n----------------- REMOVE PATIENT --------------------");
        int id = readPositiveInt("Enter Patient ID to remove: ");
        try {
            EmergencyPatient patient = system.searchPatient(id);
            System.out.printf("Patient Found: %s, Condition: %s, Priority: %d (%s)%n",
                    patient.getName(), patient.getMedicalCondition(), patient.getPriority(), patient.getPriorityLevelName());

            boolean confirm = readYesNo("Are you sure you want to remove this patient from the waiting queue? (Y/N): ");
            if (confirm) {
                EmergencyPatient removed = system.removePatient(id);
                System.out.printf("[Success] Patient ID %d (%s) removed from waiting queue.%n", removed.getPatientId(), removed.getName());
            } else {
                System.out.println("[Cancelled] Patient was not removed.");
            }
        } catch (InvalidPatientException e) {
            System.err.println("\n[Error] " + e.getMessage());
        }
    }

    private static void handleDisplayTreatmentHistory() {
        System.out.println("\n=========================================================================================");
        System.out.println("                                TREATMENT HISTORY                                        ");
        System.out.println("=========================================================================================");

        List<EmergencyPatient> history = system.getTreatmentHistory();
        if (history.isEmpty()) {
            System.out.println("No treatment history recorded yet.");
            return;
        }

        System.out.printf("%-4s | %-6s | %-18s | %-16s | %-14s | %-26s%n",
                "No.", "ID", "Name", "Condition", "Priority", "Treatment Time");
        System.out.println("-----+--------+--------------------+------------------+----------------+---------------------------");

        int count = 1;
        for (EmergencyPatient p : history) {
            String prioStr = String.format("%d - %s", p.getPriority(), p.getPriorityLevelName());
            System.out.printf("%-4d | %-6d | %-18s | %-16s | %-14s | %-26s%n",
                    count++,
                    p.getPatientId(),
                    truncate(p.getName(), 18),
                    truncate(p.getMedicalCondition(), 16),
                    truncate(prioStr, 14),
                    p.getTreatmentTime()
            );
        }
        System.out.println("=========================================================================================");
        System.out.printf("Total Patients Treated: %d%n", history.size());
    }

    private static void handleMultiplePatientRegistration() {
        System.out.println("\n------------- MULTIPLE PATIENT REGISTRATION -------------");
        int count = readInt("How many patients do you want to register? (1 - 10): ", 1, 10);

        for (int i = 1; i <= count; i++) {
            System.out.printf("%n>>> Registering Patient %d of %d <<<%n", i, count);
            handleRegisterPatient();
        }
    }

    private static void handleConcurrentRegistrationDemo() {
        System.out.println("\n========= CONCURRENT PATIENT REGISTRATION DEMO ==========");
        System.out.println("Spawning 3 concurrent registration threads (simulating 3 triage desks)...");

        try {
            EmergencyPatient p1 = new EmergencyPatient(501, "Aarav Sharma", 52, "Male", "9876543210", "Cardiac Arrest", 1, "Desk A - Emergency EMS", true);
            EmergencyPatient p2 = new EmergencyPatient(502, "Fatima Khan", 29, "Female", "9876543211", "High Fever", 4, "Desk B - Walk-in clinic", false);
            EmergencyPatient p3 = new EmergencyPatient(503, "Rajesh Kumar", 36, "Male", "9876543212", "Severe Bleeding", 2, "Desk C - Trauma ambulance", true);

            PatientRegistrationThread t1 = new PatientRegistrationThread("RegistrationThread-1", system, p1);
            PatientRegistrationThread t2 = new PatientRegistrationThread("RegistrationThread-2", system, p2);
            PatientRegistrationThread t3 = new PatientRegistrationThread("RegistrationThread-3", system, p3);

            t1.start();
            t2.start();
            t3.start();

            t1.join();
            t2.join();
            t3.join();

            System.out.println("\n[Concurrency Success] All registration threads completed safely!");
            System.out.printf("Current Waiting Queue Size: %d%n", system.getWaitingCount());

        } catch (Exception e) {
            System.err.println("[Error] Concurrent registration error: " + e.getMessage());
        }
    }

    private static void handleConcurrentUpdateDemo() {
        System.out.println("\n============ CONCURRENT PRIORITY UPDATE DEMO ============");
        System.out.println("Spawning concurrent update threads modifying patients concurrently...");

        try {
            ensureSamplePatient(601, "Vikram Sen", 42, "Male", "9876500001", "Minor Injury", 5);
            ensureSamplePatient(602, "Meera Nair", 31, "Female", "9876500002", "Fracture", 3);
            ensureSamplePatient(603, "David Wilson", 65, "Male", "9876500003", "High Fever", 4);

            System.out.println("Before concurrent updates:");
            handleDisplayWaitingPatients();

            PriorityUpdateThread u1 = new PriorityUpdateThread("UpdateThread-1", system, 601, "Severe Bleeding");
            PriorityUpdateThread u2 = new PriorityUpdateThread("UpdateThread-2", system, 602, "Cardiac Arrest");
            PriorityUpdateThread u3 = new PriorityUpdateThread("UpdateThread-3", system, 603, 2);

            u1.start();
            u2.start();
            u3.start();

            u1.join();
            u2.join();
            u3.join();

            System.out.println("\n[Concurrency Success] All update threads completed safely!");
            System.out.println("Queue re-ordered safely according to new clinical priorities:");
            handleDisplayWaitingPatients();

        } catch (Exception e) {
            System.err.println("[Error] Concurrent update error: " + e.getMessage());
        }
    }

    private static void ensureSamplePatient(int id, String name, int age, String gender, String phone, String cond, int prio) {
        try {
            EmergencyPatient p = new EmergencyPatient(id, name, age, gender, phone, cond, prio, "Demo Patient", false);
            system.registerPatient(p);
        } catch (InvalidPatientException ignored) {}
    }

    private static void handleFilePersistenceMenu() {
        System.out.println("\n---------------- FILE PERSISTENCE MENU ------------------");
        System.out.println("1. Save Treatment History to File");
        System.out.println("2. Reload Treatment History from File");
        int subChoice = readInt("Enter choice (1-2): ", 1, 2);

        if (subChoice == 1) {
            try {
                system.saveTreatmentHistoryToFile();
                System.out.printf("[Success] Treatment history saved to: %s%n", system.getHistoryFilePath());
            } catch (Exception e) {
                System.err.println("[Error] Failed to save history: " + e.getMessage());
            }
        } else {
            int reloaded = system.loadTreatmentHistoryFromFile();
            System.out.printf("[Success] Loaded %d records from file.%n", reloaded);
        }
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = scanner.nextLine().trim();
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) return val;
                System.out.printf("[Error] Value must be between %d and %d. Please try again.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid integer input. Please enter numbers only.");
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = scanner.nextLine().trim();
                int val = Integer.parseInt(line);
                if (val > 0) return val;
                System.out.println("[Error] Value must be a positive integer (> 0).");
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid integer. Please enter digits only.");
            }
        }
    }

    private static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("[Error] This field cannot be left blank.");
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("no")) return false;
            System.out.println("[Error] Please enter 'Y' for Yes or 'N' for No.");
        }
    }

    private static void promptEnterKey() {
        System.out.print("\nPress ENTER to return to the main menu...");
        scanner.nextLine();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}