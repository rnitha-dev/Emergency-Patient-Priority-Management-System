/**
 * EmergencyPatient extends the base Patient class to represent patients
 * admitted specifically into the Emergency Room (ER) / Triage ward.
 * 
 * Demonstrates:
 * 1. Inheritance (extends Patient)
 * 2. Polymorphism (overriding displayDetails and toString)
 * 3. super keyword to invoke parent constructor and parent methods
 * 4. Additional specialized fields (triageNotes, isTraumaCase, treatmentTime)
 */
public class EmergencyPatient extends Patient {

    private String triageNotes;
    private boolean isTraumaCase;
    private String treatmentTime; // recorded when treated

    /**
     * Primary constructor for an incoming emergency patient.
     */
    public EmergencyPatient(int patientId, String name, int age, String gender,
                            String phoneNumber, String medicalCondition, int priority,
                            String triageNotes, boolean isTraumaCase)
                            throws InvalidPatientException {
        super(patientId, name, age, gender, phoneNumber, medicalCondition, priority);
        this.triageNotes = (triageNotes != null) ? triageNotes.trim() : "Standard ER intake";
        this.isTraumaCase = isTraumaCase;
        this.treatmentTime = "Waiting for treatment";
    }

    /**
     * Overloaded constructor accepting an explicit registration time and treatment time
     * (used during history loading / data restoration).
     */
    public EmergencyPatient(int patientId, String name, int age, String gender,
                            String phoneNumber, String medicalCondition, int priority,
                            long registrationTime, String triageNotes, boolean isTraumaCase,
                            String treatmentTime)
                            throws InvalidPatientException {
        super(patientId, name, age, gender, phoneNumber, medicalCondition, priority, registrationTime);
        this.triageNotes = (triageNotes != null) ? triageNotes.trim() : "Standard ER intake";
        this.isTraumaCase = isTraumaCase;
        this.treatmentTime = (treatmentTime != null) ? treatmentTime.trim() : "Waiting for treatment";
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public String getTriageNotes() {
        return triageNotes;
    }

    public void setTriageNotes(String triageNotes) {
        this.triageNotes = (triageNotes != null) ? triageNotes.trim() : "Standard ER intake";
    }

    public boolean isTraumaCase() {
        return isTraumaCase;
    }

    public void setTraumaCase(boolean traumaCase) {
        isTraumaCase = traumaCase;
    }

    public String getTreatmentTime() {
        return treatmentTime;
    }

    public void setTreatmentTime(String treatmentTime) {
        this.treatmentTime = treatmentTime;
    }

    /**
     * Demonstrates Polymorphism / Method Overriding.
     * Calls super.displayDetails() to print common patient information,
     * then appends emergency-specific triage details.
     */
    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.printf("  Trauma Case      : %s%n", isTraumaCase ? "YES [RED ALERT]" : "NO");
        System.out.printf("  Triage Notes     : %s%n", triageNotes);
        System.out.printf("  Treatment Status : %s%n", treatmentTime);
        System.out.println("--------------------------------------------------");
    }

    @Override
    public String toString() {
        return String.format("[EmergencyPatient ID=%d, Name='%s', Priority=%d (%s), Trauma=%b, Status='%s']",
                getPatientId(), getName(), getPriority(), getPriorityLevelName(), isTraumaCase, treatmentTime);
    }
}
