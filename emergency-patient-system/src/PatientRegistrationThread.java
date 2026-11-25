/**
 * PatientRegistrationThread demonstrates multithreading in Java by simulating
 * concurrent patient registrations in the Emergency Department (e.g. multiple triage desks).
 * 
 * Demonstrates:
 * 1. Multithreading (extends java.lang.Thread)
 * 2. Overriding run() method
 * 3. Thread naming and concurrency tracking
 * 4. Concurrent access to shared EmergencyManagementSystem resource
 * 5. Exception handling within thread execution
 */
public class PatientRegistrationThread extends Thread {

    private final EmergencyManagementSystem system;
    private final EmergencyPatient patient;

    /**
     * Constructs a registration thread with a custom name, shared system, and patient to register.
     * 
     * @param threadName Identifier for the registration desk/thread
     * @param system Shared EmergencyManagementSystem
     * @param patient Patient to be registered
     */
    public PatientRegistrationThread(String threadName, EmergencyManagementSystem system, EmergencyPatient patient) {
        super(threadName); // Set thread name for easy debugging and log tracking
        this.system = system;
        this.patient = patient;
    }

    @Override
    public void run() {
        try {
            // Simulate slight network/intake latency between 50ms and 150ms
            Thread.sleep((long) (Math.random() * 100) + 50);

            // Access the thread-safe shared management system
            system.registerPatient(patient);

            System.out.printf("[%s] Patient %d (%s) registered successfully. Condition: %s | Priority: %d (%s)%n",
                    getName(),
                    patient.getPatientId(),
                    patient.getName(),
                    patient.getMedicalCondition(),
                    patient.getPriority(),
                    patient.getPriorityLevelName()
            );

        } catch (InvalidPatientException e) {
            System.err.printf("[%s] REGISTRATION FAILED for Patient %d: %s%n",
                    getName(), patient.getPatientId(), e.getMessage());
        } catch (InterruptedException e) {
            System.err.printf("[%s] Thread interrupted: %s%n", getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
