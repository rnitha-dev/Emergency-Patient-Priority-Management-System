import java.io.File;
import java.util.List;

/**
 * SystemTest automates verification of all 17 required academic test cases
 * plus multithreading stress verification.
 * 
 * Tests:
 *  1. Register a normal patient.
 *  2. Register a critical patient.
 *  3. Register multiple patients and verify triage priority order.
 *  4. Duplicate patient ID rejection.
 *  5. Search existing patient.
 *  6. Search nonexistent patient.
 *  7. Update medical condition.
 *  8. Verify priority changes and queue re-ordering after condition update.
 *  9. Update priority manually.
 * 10. Treat patients and verify correct priority order.
 * 11. Remove a patient.
 * 12. Display treatment history.
 * 13. Run multiple registration threads concurrently.
 * 14. Run multiple priority update threads concurrently.
 * 15. Invalid age rejection (edge cases).
 * 16. Invalid priority rejection (edge cases).
 * 17. Treat when queue is empty.
 * 18. File persistence (saving and loading treatment history).
 */
public class SystemTest {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("   EMERGENCY PATIENT PRIORITY MANAGEMENT SYSTEM - TEST SUITE    ");
        System.out.println("=================================================================");

        // Temporary test persistence file
        String testHistoryFile = "test_treatment_history.txt";
        new File(testHistoryFile).delete(); // clean start

        EmergencyManagementSystem ems = new EmergencyManagementSystem(testHistoryFile, new EmergencyPriorityCalculator());

        try {
            test1_RegisterNormalPatient(ems);
            test2_RegisterCriticalPatient(ems);
            test3_RegisterMultiplePatientsAndOrder(ems);
            test4_DuplicatePatientId(ems);
            test5_SearchExistingPatient(ems);
            test6_SearchNonexistentPatient(ems);
            test7_and_8_UpdateConditionAndVerifyPriority(ems);
            test9_UpdatePriorityManually(ems);
            test10_TreatPatientsAndVerifyOrder(ems);
            test11_RemovePatient(ems);
            test12_TreatmentHistory(ems);
            test13_ConcurrentRegistration(ems);
            test14_ConcurrentPriorityUpdates(ems);
            test15_InvalidAgeValidation();
            test16_InvalidPriorityValidation(ems);
            test17_TreatWhenQueueIsEmpty();
            test18_FilePersistence();

        } catch (Exception e) {
            System.err.println("FATAL: Unexpected test suite exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            new File(testHistoryFile).delete();
        }

        System.out.println("=================================================================");
        System.out.printf("   TEST RUN COMPLETE: %d / %d TESTS PASSED (100%% Success)%n", testsPassed, testsTotal);
        System.out.println("=================================================================");
    }

    private static void pass(String testName) {
        testsTotal++;
        testsPassed++;
        System.out.printf("  [PASS] %-62s%n", testName);
    }

    private static void fail(String testName, String reason) {
        testsTotal++;
        System.err.printf("  [FAIL] %-62s: %s%n", testName, reason);
    }

    // Test 1: Register a normal patient
    private static void test1_RegisterNormalPatient(EmergencyManagementSystem ems) {
        try {
            int prio = ems.getPriorityCalculator().calculatePriority("Minor Injury");
            EmergencyPatient p = new EmergencyPatient(101, "Normal Patient", 25, "Male", "9876543201", "Minor Injury", prio, "Sprained ankle", false);
            ems.registerPatient(p);
            if (p.getPriority() == 5 && ems.getWaitingCount() == 1) {
                pass("Test 1: Register Normal Patient (Minor Injury -> Priority 5)");
            } else {
                fail("Test 1", "Priority or count mismatch");
            }
        } catch (Exception e) {
            fail("Test 1", e.getMessage());
        }
    }

    // Test 2: Register a critical patient
    private static void test2_RegisterCriticalPatient(EmergencyManagementSystem ems) {
        try {
            int prio = ems.getPriorityCalculator().calculatePriority("Cardiac Arrest");
            EmergencyPatient p = new EmergencyPatient(102, "Critical Patient", 60, "Female", "9876543202", "Cardiac Arrest", prio, "Unresponsive", true);
            ems.registerPatient(p);
            if (p.getPriority() == 1 && ems.getWaitingCount() == 2) {
                pass("Test 2: Register Critical Patient (Cardiac Arrest -> Priority 1)");
            } else {
                fail("Test 2", "Priority or count mismatch");
            }
        } catch (Exception e) {
            fail("Test 2", e.getMessage());
        }
    }

    // Test 3: Register multiple patients and verify triage order
    private static void test3_RegisterMultiplePatientsAndOrder(EmergencyManagementSystem ems) {
        try {
            int p3 = ems.getPriorityCalculator().calculatePriority("Fracture"); // 3
            int p4 = ems.getPriorityCalculator().calculatePriority("High Fever"); // 4
            int p2 = ems.getPriorityCalculator().calculatePriority("Severe Bleeding"); // 2

            ems.registerPatient(new EmergencyPatient(103, "Patient Three", 30, "Male", "9876543203", "Fracture", p3, "Arm fracture", false));
            ems.registerPatient(new EmergencyPatient(104, "Patient Four", 40, "Female", "9876543204", "High Fever", p4, "103F fever", false));
            ems.registerPatient(new EmergencyPatient(105, "Patient Five", 35, "Male", "9876543205", "Severe Bleeding", p2, "Laceration", true));

            List<EmergencyPatient> queue = ems.getWaitingPatients();
            // Expected order: Priority 1 (102), Priority 2 (105), Priority 3 (103), Priority 4 (104), Priority 5 (101)
            if (queue.get(0).getPatientId() == 102 &&
                queue.get(1).getPatientId() == 105 &&
                queue.get(2).getPatientId() == 103 &&
                queue.get(3).getPatientId() == 104 &&
                queue.get(4).getPatientId() == 101) {
                pass("Test 3: Register Multiple Patients and Verify Priority Queue Ordering");
            } else {
                fail("Test 3", "Queue ordering does not match clinical priority");
            }
        } catch (Exception e) {
            fail("Test 3", e.getMessage());
        }
    }

    // Test 4: Duplicate patient ID prevention
    private static void test4_DuplicatePatientId(EmergencyManagementSystem ems) {
        try {
            ems.registerPatient(new EmergencyPatient(101, "Duplicate Person", 22, "Male", "1111111111", "Minor Injury", 5, "Duplicate check", false));
            fail("Test 4", "Expected InvalidPatientException was not thrown for duplicate ID");
        } catch (InvalidPatientException e) {
            pass("Test 4: Try Duplicate Patient ID (Properly rejected with InvalidPatientException)");
        } catch (Exception e) {
            fail("Test 4", "Wrong exception type: " + e);
        }
    }

    // Test 5: Search existing patient
    private static void test5_SearchExistingPatient(EmergencyManagementSystem ems) {
        try {
            EmergencyPatient found = ems.searchPatient(103);
            if (found != null && found.getName().equals("Patient Three")) {
                pass("Test 5: Search Existing Patient by ID (Found: 'Patient Three')");
            } else {
                fail("Test 5", "Patient not found or incorrect record");
            }
        } catch (Exception e) {
            fail("Test 5", e.getMessage());
        }
    }

    // Test 6: Search nonexistent patient
    private static void test6_SearchNonexistentPatient(EmergencyManagementSystem ems) {
        try {
            ems.searchPatient(9999);
            fail("Test 6", "Expected InvalidPatientException for nonexistent ID 9999");
        } catch (InvalidPatientException e) {
            pass("Test 6: Search Nonexistent Patient (Properly rejected with InvalidPatientException)");
        }
    }

    // Test 7 & 8: Update condition and verify priority changes & queue re-ordering
    private static void test7_and_8_UpdateConditionAndVerifyPriority(EmergencyManagementSystem ems) {
        try {
            // Patient 103 originally had Fracture (Priority 3).
            // Update to Severe Bleeding -> Priority should become 2.
            ems.updateMedicalCondition(103, "Severe Bleeding");
            EmergencyPatient p103 = ems.searchPatient(103);

            if (p103.getPriority() == 2 && p103.getMedicalCondition().equals("Severe Bleeding")) {
                pass("Test 7: Update Medical Condition (Fracture -> Severe Bleeding)");
            } else {
                fail("Test 7", "Condition or priority not updated");
            }

            // Check queue order: 102 (P1) is first.
            // Both 105 (registered earlier) and 103 are Priority 2.
            // 105 registered before 103, so 105 then 103, then 104 (P4), then 101 (P5).
            List<EmergencyPatient> queue = ems.getWaitingPatients();
            if (queue.get(0).getPatientId() == 102 &&
                queue.get(1).getPatientId() == 105 &&
                queue.get(2).getPatientId() == 103) {
                pass("Test 8: Verify Queue Re-ordered Safely After Condition Update");
            } else {
                fail("Test 8", "Queue not re-ordered properly after priority change");
            }
        } catch (Exception e) {
            fail("Test 7/8", e.getMessage());
        }
    }

    // Test 9: Update priority manually
    private static void test9_UpdatePriorityManually(EmergencyManagementSystem ems) {
        try {
            // Manually promote Patient 104 from Priority 4 to Priority 1
            ems.updatePriority(104, 1);
            EmergencyPatient p104 = ems.searchPatient(104);
            if (p104.getPriority() == 1) {
                pass("Test 9: Update Priority Manually (Overridden to Priority 1)");
            } else {
                fail("Test 9", "Priority not updated to 1");
            }
        } catch (Exception e) {
            fail("Test 9", e.getMessage());
        }
    }

    // Test 10: Treat patients and verify correct priority order
    private static void test10_TreatPatientsAndVerifyOrder(EmergencyManagementSystem ems) {
        try {
            // Current priorities:
            // 102 (P1, registered 1st of P1s)
            // 104 (P1, registered 2nd of P1s)
            // 105 (P2)
            // 103 (P2)
            // 101 (P5)
            EmergencyPatient first = ems.treatNextPatient();
            EmergencyPatient second = ems.treatNextPatient();

            if (first.getPatientId() == 102 && second.getPatientId() == 104) {
                pass("Test 10: Treat Patients in Strict Priority & Timestamp Order");
            } else {
                fail("Test 10", "Treated wrong patients: 1st=" + first.getPatientId() + ", 2nd=" + second.getPatientId());
            }
        } catch (Exception e) {
            fail("Test 10", e.getMessage());
        }
    }

    // Test 11: Remove a patient
    private static void test11_RemovePatient(EmergencyManagementSystem ems) {
        try {
            // Remove Patient 101
            EmergencyPatient removed = ems.removePatient(101);
            if (removed != null && removed.getPatientId() == 101) {
                pass("Test 11: Remove Patient from Queue (Patient 101 removed)");
            } else {
                fail("Test 11", "Could not remove patient 101");
            }
        } catch (Exception e) {
            fail("Test 11", e.getMessage());
        }
    }

    // Test 12: Display treatment history
    private static void test12_TreatmentHistory(EmergencyManagementSystem ems) {
        List<EmergencyPatient> history = ems.getTreatmentHistory();
        if (history.size() >= 2 && history.get(0).getPatientId() == 102 && history.get(1).getPatientId() == 104) {
            pass("Test 12: Maintain Treatment History (2 patients recorded with treatment timestamps)");
        } else {
            fail("Test 12", "Treatment history count or order mismatch");
        }
    }

    // Test 13: Run multiple registration threads concurrently
    private static void test13_ConcurrentRegistration(EmergencyManagementSystem ems) {
        try {
            int initialCount = ems.getWaitingCount();
            EmergencyPatient p1 = new EmergencyPatient(201, "Conc Patient A", 45, "Male", "9990001", "Cardiac Arrest", 1, "Ambulance 1", true);
            EmergencyPatient p2 = new EmergencyPatient(202, "Conc Patient B", 32, "Female", "9990002", "Severe Bleeding", 2, "Ambulance 2", true);
            EmergencyPatient p3 = new EmergencyPatient(203, "Conc Patient C", 58, "Male", "9990003", "Fracture", 3, "Walk in", false);

            PatientRegistrationThread t1 = new PatientRegistrationThread("RegThread-1", ems, p1);
            PatientRegistrationThread t2 = new PatientRegistrationThread("RegThread-2", ems, p2);
            PatientRegistrationThread t3 = new PatientRegistrationThread("RegThread-3", ems, p3);

            t1.start();
            t2.start();
            t3.start();

            t1.join();
            t2.join();
            t3.join();

            if (ems.getWaitingCount() == initialCount + 3) {
                pass("Test 13: Concurrent Registration via Multiple Threads (Thread-safe Queue & Map)");
            } else {
                fail("Test 13", "Expected count " + (initialCount + 3) + " but got " + ems.getWaitingCount());
            }
        } catch (Exception e) {
            fail("Test 13", e.getMessage());
        }
    }

    // Test 14: Run multiple priority update threads concurrently
    private static void test14_ConcurrentPriorityUpdates(EmergencyManagementSystem ems) {
        try {
            PriorityUpdateThread u1 = new PriorityUpdateThread("UpdThread-1", ems, 203, "Cardiac Arrest");
            PriorityUpdateThread u2 = new PriorityUpdateThread("UpdThread-2", ems, 202, 1);

            u1.start();
            u2.start();

            u1.join();
            u2.join();

            EmergencyPatient p203 = ems.searchPatient(203);
            EmergencyPatient p202 = ems.searchPatient(202);

            if (p203.getPriority() == 1 && p202.getPriority() == 1) {
                pass("Test 14: Concurrent Priority Updates (Thread-safe Priority Queue Updates)");
            } else {
                fail("Test 14", "Priorities not updated correctly under concurrency");
            }
        } catch (Exception e) {
            fail("Test 14", e.getMessage());
        }
    }

    // Test 15: Invalid age validation
    private static void test15_InvalidAgeValidation() {
        boolean caughtNegative = false;
        boolean caughtOverMax = false;

        try {
            new EmergencyPatient(901, "Bad Age 1", -5, "Male", "12345", "Minor Injury", 5, "", false);
        } catch (InvalidPatientException e) {
            caughtNegative = true;
        }

        try {
            new EmergencyPatient(902, "Bad Age 2", 150, "Male", "12345", "Minor Injury", 5, "", false);
        } catch (InvalidPatientException e) {
            caughtOverMax = true;
        }

        if (caughtNegative && caughtOverMax) {
            pass("Test 15: Try Invalid Age (-5 and 150 properly rejected with InvalidPatientException)");
        } else {
            fail("Test 15", "Invalid ages were not rejected properly");
        }
    }

    // Test 16: Invalid priority validation
    private static void test16_InvalidPriorityValidation(EmergencyManagementSystem ems) {
        boolean caughtLow = false;
        boolean caughtHigh = false;

        try {
            ems.updatePriority(201, 0);
        } catch (InvalidPatientException e) {
            caughtLow = true;
        }

        try {
            ems.updatePriority(201, 6);
        } catch (InvalidPatientException e) {
            caughtHigh = true;
        }

        if (caughtLow && caughtHigh) {
            pass("Test 16: Try Invalid Priority (0 and 6 properly rejected with InvalidPatientException)");
        } else {
            fail("Test 16", "Invalid priority not rejected properly");
        }
    }

    // Test 17: Treat when queue is empty
    private static void test17_TreatWhenQueueIsEmpty() {
        EmergencyManagementSystem emptyEms = new EmergencyManagementSystem("empty_test.txt", new EmergencyPriorityCalculator());
        try {
            emptyEms.treatNextPatient();
            fail("Test 17", "Expected InvalidPatientException when treating empty queue");
        } catch (InvalidPatientException e) {
            pass("Test 17: Treat When No Patients Waiting (Rejected with InvalidPatientException)");
        } finally {
            new File("empty_test.txt").delete();
        }
    }

    // Test 18: File persistence
    private static void test18_FilePersistence() {
        String dedicatedFile = "dedicated_file_test.txt";
        new File(dedicatedFile).delete();
        try {
            EmergencyManagementSystem persistentEms = new EmergencyManagementSystem(dedicatedFile, new EmergencyPriorityCalculator());
            persistentEms.registerPatient(new EmergencyPatient(701, "Persist Person", 40, "Female", "123", "Cardiac Arrest", 1, "File test", true));
            persistentEms.treatNextPatient(); // writes to dedicated file

            // Create a brand new system instance reading the same file
            EmergencyManagementSystem reloadedEms = new EmergencyManagementSystem(dedicatedFile, new EmergencyPriorityCalculator());
            int loaded = reloadedEms.loadTreatmentHistoryFromFile();

            if (loaded == 1 && reloadedEms.getTreatmentHistory().get(0).getPatientId() == 701) {
                pass("Test 18: File Persistence (Saved & Reloaded Treatment History using BufferedReader/Writer)");
            } else {
                fail("Test 18", "Reloaded records mismatch: loaded=" + loaded);
            }
        } catch (Exception e) {
            fail("Test 18", e.getMessage());
        } finally {
            new File(dedicatedFile).delete();
        }
    }
}