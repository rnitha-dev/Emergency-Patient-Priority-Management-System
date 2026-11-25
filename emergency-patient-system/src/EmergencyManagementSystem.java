import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * EmergencyManagementSystem manages the triage waiting queue, patient registration,
 * dynamic priority updates, treatment workflow, and treatment history.
 * 
 * Demonstrates:
 * 1. Collections:
 *    - PriorityQueue (Triage queue sorted by Priority then Registration Time)
 *    - HashMap (Fast O(1) patient lookup and duplicate prevention)
 *    - ArrayList (Sequential treatment history)
 * 2. Multithreading & Synchronization:
 *    - synchronized methods protecting shared resources against race conditions.
 *    - Atomic PriorityQueue update (remove -> modify -> re-insert) to preserve min-heap invariant.
 * 3. File Handling:
 *    - BufferedReader, BufferedWriter, FileReader, FileWriter.
 * 4. Custom Exception Handling:
 *    - Throws InvalidPatientException for business validation failures.
 */
public class EmergencyManagementSystem {

    // Thread-safe Emergency Queue: sorted by priority ASC, then registration time ASC
    private final PriorityQueue<EmergencyPatient> waitingQueue;

    // Fast lookup map: patientId -> EmergencyPatient
    private final Map<Integer, EmergencyPatient> patientMap;

    // Sequential list storing completed treatments
    private final List<EmergencyPatient> treatmentHistory;

    // Triage Priority Calculator (Interface reference demonstrating Abstraction & Polymorphism)
    private final PriorityCalculator priorityCalculator;

    // File path for persisting treatment records
    private final String historyFilePath;

    /**
     * Default constructor initializing data structures and priority comparator.
     */
    public EmergencyManagementSystem() {
        this("treatment_history.txt", new EmergencyPriorityCalculator());
    }

    /**
     * Parameterized constructor for custom persistence path and priority calculator.
     */
    public EmergencyManagementSystem(String historyFilePath, PriorityCalculator priorityCalculator) {
        this.historyFilePath = historyFilePath;
        this.priorityCalculator = priorityCalculator;

        // Custom comparator for Triage Priority Queue:
        // Rule 1: Lower numeric priority value has higher emergency urgency (1 before 2).
        // Rule 2: If priorities are identical, earlier registration time is served first (FIFO within priority).
        Comparator<EmergencyPatient> triageComparator = new Comparator<EmergencyPatient>() {
            @Override
            public int compare(EmergencyPatient p1, EmergencyPatient p2) {
                int priorityComparison = Integer.compare(p1.getPriority(), p2.getPriority());
                if (priorityComparison != 0) {
                    return priorityComparison;
                }
                return Long.compare(p1.getRegistrationTime(), p2.getRegistrationTime());
            }
        };

        this.waitingQueue = new PriorityQueue<>(triageComparator);
        this.patientMap = new HashMap<>();
        this.treatmentHistory = new ArrayList<>();
    }

    public PriorityCalculator getPriorityCalculator() {
        return priorityCalculator;
    }

    public String getHistoryFilePath() {
        return historyFilePath;
    }

    // =========================================================================
    // FEATURE 1: REGISTER PATIENT (Thread-safe)
    // =========================================================================
    /**
     * Registers a new patient into the triage system.
     * Prevents duplicate patient IDs and inserts atomically into both queue and map.
     * 
     * @param patient Emergency patient to register
     * @throws InvalidPatientException if patient is null or ID already exists
     */
    public synchronized void registerPatient(EmergencyPatient patient) throws InvalidPatientException {
        if (patient == null) {
            throw new InvalidPatientException("Cannot register a null patient record.");
        }

        int id = patient.getPatientId();
        if (patientMap.containsKey(id)) {
            throw new InvalidPatientException("Duplicate Patient ID: " + id + " is already registered in the waiting queue.");
        }

        // Add to both data structures
        waitingQueue.offer(patient);
        patientMap.put(id, patient);
    }

    // =========================================================================
    // FEATURE 2: DISPLAY WAITING PATIENTS (Thread-safe snapshot)
    // =========================================================================
    /**
     * Returns a sorted snapshot copy of all currently waiting patients in treatment order.
     * Safe against concurrent modifications during display.
     */
    public synchronized List<EmergencyPatient> getWaitingPatients() {
        // PriorityQueue.iterator() does NOT guarantee traversal in sorted heap order.
        // Therefore, we copy the elements and sort them using the same comparator.
        List<EmergencyPatient> snapshot = new ArrayList<>(waitingQueue);
        snapshot.sort(waitingQueue.comparator());
        return snapshot;
    }

    public synchronized int getWaitingCount() {
        return waitingQueue.size();
    }

    // =========================================================================
    // FEATURE 3: SEARCH PATIENT (Thread-safe)
    // =========================================================================
    /**
     * Searches for a patient by ID in both active waiting queue and treatment history.
     * 
     * @param patientId ID of the patient
     * @return EmergencyPatient instance found
     * @throws InvalidPatientException if patient is not found
     */
    public synchronized EmergencyPatient searchPatient(int patientId) throws InvalidPatientException {
        // First check active waiting patients (O(1) lookup)
        EmergencyPatient activePatient = patientMap.get(patientId);
        if (activePatient != null) {
            return activePatient;
        }

        // Then search treatment history
        for (EmergencyPatient treated : treatmentHistory) {
            if (treated.getPatientId() == patientId) {
                return treated;
            }
        }

        throw new InvalidPatientException("Patient with ID " + patientId + " not found in the system.");
    }

    // =========================================================================
    // FEATURE 4: UPDATE MEDICAL CONDITION (Thread-safe)
    // =========================================================================
    /**
     * Updates a patient's medical condition and automatically recalculates priority.
     * Correctly removes and reinserts the patient into the PriorityQueue to restore heap ordering.
     * 
     * @param patientId Patient ID
     * @param newCondition New medical condition
     * @throws InvalidPatientException if patient not found or condition invalid
     */
    public synchronized void updateMedicalCondition(int patientId, String newCondition) throws InvalidPatientException {
        EmergencyPatient patient = patientMap.get(patientId);
        if (patient == null) {
            throw new InvalidPatientException("Active waiting patient with ID " + patientId + " not found.");
        }

        // Calculate new priority using the priority calculator interface
        int newPriority = priorityCalculator.calculatePriority(newCondition);

        // Safe update process for PriorityQueue:
        // 1. Remove patient from queue
        // 2. Modify patient fields
        // 3. Re-insert patient into queue
        waitingQueue.remove(patient);
        patient.setMedicalCondition(newCondition);
        patient.setPriority(newPriority);
        waitingQueue.offer(patient);
    }

    // =========================================================================
    // FEATURE 5: UPDATE PRIORITY MANUALLY (Thread-safe)
    // =========================================================================
    /**
     * Manually overrides a patient's priority (e.g., triage doctor decision).
     * Re-orders PriorityQueue safely.
     * 
     * @param patientId Patient ID
     * @param newPriority Priority level (1 to 5)
     * @throws InvalidPatientException if patient not found or priority invalid
     */
    public synchronized void updatePriority(int patientId, int newPriority) throws InvalidPatientException {
        if (!priorityCalculator.isValidPriority(newPriority)) {
            throw new InvalidPatientException("Invalid priority: " + newPriority + ". Must be between 1 and 5.");
        }

        EmergencyPatient patient = patientMap.get(patientId);
        if (patient == null) {
            throw new InvalidPatientException("Active waiting patient with ID " + patientId + " not found.");
        }

        // Safe update process:
        waitingQueue.remove(patient);
        patient.setPriority(newPriority);
        waitingQueue.offer(patient);
    }

    // =========================================================================
    // FEATURE 6: TREAT NEXT PATIENT (Thread-safe)
    // =========================================================================
    /**
     * Dequeues the highest priority patient from the queue, marks them treated,
     * adds them to treatment history, and logs the action.
     * 
     * @return The treated EmergencyPatient
     * @throws InvalidPatientException if queue is empty
     */
    public synchronized EmergencyPatient treatNextPatient() throws InvalidPatientException {
        if (waitingQueue.isEmpty()) {
            throw new InvalidPatientException("No patients waiting in the emergency queue.");
        }

        // Highest priority patient is at the head of the PriorityQueue
        EmergencyPatient treatedPatient = waitingQueue.poll();
        patientMap.remove(treatedPatient.getPatientId());

        // Record treatment timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedTime = sdf.format(new Date());
        treatedPatient.setTreatmentTime("Treated at " + formattedTime);

        // Add to treatment history
        treatmentHistory.add(treatedPatient);

        // Auto-save history record to file
        try {
            appendRecordToFile(treatedPatient);
        } catch (IOException e) {
            System.err.println("[Warning] Failed to append treated patient to file: " + e.getMessage());
        }

        return treatedPatient;
    }

    // =========================================================================
    // FEATURE 7: REMOVE PATIENT (Thread-safe)
    // =========================================================================
    /**
     * Removes an active patient from the triage queue without treatment.
     * 
     * @param patientId Patient ID
     * @return Removed EmergencyPatient instance
     * @throws InvalidPatientException if patient not found
     */
    public synchronized EmergencyPatient removePatient(int patientId) throws InvalidPatientException {
        EmergencyPatient patient = patientMap.get(patientId);
        if (patient == null) {
            throw new InvalidPatientException("Active waiting patient with ID " + patientId + " not found.");
        }

        waitingQueue.remove(patient);
        patientMap.remove(patientId);
        return patient;
    }

    // =========================================================================
    // FEATURE 8: TREATMENT HISTORY (Thread-safe)
    // =========================================================================
    /**
     * Returns a defensive copy of the treatment history.
     */
    public synchronized List<EmergencyPatient> getTreatmentHistory() {
        return new ArrayList<>(treatmentHistory);
    }

    // =========================================================================
    // FILE HANDLING (Thread-safe persistence)
    // =========================================================================
    /**
     * Appends a single treated patient record to the history file.
     */
    private synchronized void appendRecordToFile(EmergencyPatient patient) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyFilePath, true))) {
            writer.write(serializePatient(patient));
            writer.newLine();
        }
    }

    /**
     * Saves the entire treatment history to the persistent text file.
     */
    public synchronized void saveTreatmentHistoryToFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyFilePath, false))) {
            for (EmergencyPatient patient : treatmentHistory) {
                writer.write(serializePatient(patient));
                writer.newLine();
            }
        }
    }

    /**
     * Loads treatment history records from the persistent file at startup.
     * Skips malformed lines gracefully.
     */
    public synchronized int loadTreatmentHistoryFromFile() {
        File file = new File(historyFilePath);
        if (!file.exists()) {
            return 0;
        }

        int loadedCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                try {
                    EmergencyPatient patient = deserializePatient(line);
                    // Avoid duplicating into treatment history if already loaded
                    boolean alreadyPresent = false;
                    for (EmergencyPatient p : treatmentHistory) {
                        if (p.getPatientId() == patient.getPatientId()) {
                            alreadyPresent = true;
                            break;
                        }
                    }
                    if (!alreadyPresent) {
                        treatmentHistory.add(patient);
                        loadedCount++;
                    }
                } catch (Exception ex) {
                    System.err.println("[Warning] Skipping corrupt record line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[Warning] Failed to read treatment history file: " + e.getMessage());
        }
        return loadedCount;
    }

    /**
     * Serializes an EmergencyPatient into a clean CSV record.
     */
    private String serializePatient(EmergencyPatient p) {
        return String.format("%d|%s|%d|%s|%s|%s|%d|%d|%s|%b|%s",
                p.getPatientId(),
                p.getName().replace("|", " "),
                p.getAge(),
                p.getGender().replace("|", " "),
                p.getPhoneNumber().replace("|", " "),
                p.getMedicalCondition().replace("|", " "),
                p.getPriority(),
                p.getRegistrationTime(),
                p.getTriageNotes().replace("|", " "),
                p.isTraumaCase(),
                p.getTreatmentTime().replace("|", " ")
        );
    }

    /**
     * Deserializes a pipe-delimited record into an EmergencyPatient instance.
     */
    private EmergencyPatient deserializePatient(String line) throws InvalidPatientException {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 11) {
            throw new InvalidPatientException("Invalid file record format: expected 11 fields.");
        }

        int id = Integer.parseInt(parts[0].trim());
        String name = parts[1].trim();
        int age = Integer.parseInt(parts[2].trim());
        String gender = parts[3].trim();
        String phone = parts[4].trim();
        String condition = parts[5].trim();
        int priority = Integer.parseInt(parts[6].trim());
        long regTime = Long.parseLong(parts[7].trim());
        String triageNotes = parts[8].trim();
        boolean trauma = Boolean.parseBoolean(parts[9].trim());
        String treatTime = parts[10].trim();

        return new EmergencyPatient(id, name, age, gender, phone, condition, priority, regTime, triageNotes, trauma, treatTime);
    }
}
