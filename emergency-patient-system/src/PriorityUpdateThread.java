/**
 * PriorityUpdateThread simulates concurrent triage re-evaluations and priority
 * updates performed by doctors or triage nurses while patients are waiting.
 * 
 * Demonstrates:
 * 1. Multithreading (extends java.lang.Thread)
 * 2. Overriding run() method
 * 3. Thread creation, start(), join()
 * 4. Safe concurrent modification of shared PriorityQueue
 * 5. Exception handling during concurrent updates
 */
public class PriorityUpdateThread extends Thread {

    private final EmergencyManagementSystem system;
    private final int patientId;
    private final String newCondition;
    private final int newPriority;
    private final boolean isConditionUpdate;

    /**
     * Constructor for updating medical condition (which automatically recalculates priority).
     */
    public PriorityUpdateThread(String threadName, EmergencyManagementSystem system, int patientId, String newCondition) {
        super(threadName);
        this.system = system;
        this.patientId = patientId;
        this.newCondition = newCondition;
        this.newPriority = -1;
        this.isConditionUpdate = true;
    }

    /**
     * Constructor for manually overriding priority (1 to 5).
     */
    public PriorityUpdateThread(String threadName, EmergencyManagementSystem system, int patientId, int newPriority) {
        super(threadName);
        this.system = system;
        this.patientId = patientId;
        this.newCondition = null;
        this.newPriority = newPriority;
        this.isConditionUpdate = false;
    }

    @Override
    public void run() {
        try {
            // Simulate slight triage assessment time
            Thread.sleep((long) (Math.random() * 100) + 50);

            if (isConditionUpdate) {
                system.updateMedicalCondition(patientId, newCondition);
                EmergencyPatient patient = system.searchPatient(patientId);
                System.out.printf("[%s] Patient %d condition updated to '%s' -> New Priority: %d (%s)%n",
                        getName(), patientId, newCondition, patient.getPriority(), patient.getPriorityLevelName());
            } else {
                system.updatePriority(patientId, newPriority);
                EmergencyPatient patient = system.searchPatient(patientId);
                System.out.printf("[%s] Patient %d priority manually updated to: %d (%s)%n",
                        getName(), patientId, newPriority, patient.getPriorityLevelName());
            }

        } catch (InvalidPatientException e) {
            System.err.printf("[%s] UPDATE FAILED for Patient %d: %s%n",
                    getName(), patientId, e.getMessage());
        } catch (InterruptedException e) {
            System.err.printf("[%s] Thread interrupted: %s%n", getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
