import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Patient represents the base entity for all patients admitted to the hospital.
 * 
 * Demonstrates:
 * 1. Classes and Objects
 * 2. Encapsulation (private fields, validated getters and setters)
 * 3. Base class for Inheritance
 * 4. Method Overriding (displayDetails, toString, equals, hashCode)
 * 5. Constructors (multiple/overloaded constructors)
 */
public class Patient {

    // Encapsulated private attributes
    private int patientId;
    private String name;
    private int age;
    private String gender;
    private String phoneNumber;
    private String medicalCondition;
    private int priority;
    private long registrationTime;

    /**
     * Parameterized Constructor with complete validation.
     */
    public Patient(int patientId, String name, int age, String gender, 
                   String phoneNumber, String medicalCondition, int priority) 
                   throws InvalidPatientException {
        setPatientId(patientId);
        setName(name);
        setAge(age);
        setGender(gender);
        setPhoneNumber(phoneNumber);
        setMedicalCondition(medicalCondition);
        setPriority(priority);
        this.registrationTime = System.currentTimeMillis();
    }

    /**
     * Overloaded constructor allowing explicit registration time (useful for testing or file restoration).
     */
    public Patient(int patientId, String name, int age, String gender, 
                   String phoneNumber, String medicalCondition, int priority, long registrationTime) 
                   throws InvalidPatientException {
        setPatientId(patientId);
        setName(name);
        setAge(age);
        setGender(gender);
        setPhoneNumber(phoneNumber);
        setMedicalCondition(medicalCondition);
        setPriority(priority);
        this.registrationTime = registrationTime;
    }

    // ==========================================
    // GETTERS AND SETTERS (Encapsulation)
    // ==========================================

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) throws InvalidPatientException {
        if (patientId <= 0) {
            throw new InvalidPatientException("Patient ID must be a positive integer (found: " + patientId + ").");
        }
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidPatientException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidPatientException("Patient name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) throws InvalidPatientException {
        if (age < 1 || age > 120) {
            throw new InvalidPatientException("Age must be between 1 and 120 (found: " + age + ").");
        }
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) throws InvalidPatientException {
        if (gender == null || gender.trim().isEmpty()) {
            throw new InvalidPatientException("Gender cannot be empty.");
        }
        this.gender = gender.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) throws InvalidPatientException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new InvalidPatientException("Phone number cannot be empty.");
        }
        this.phoneNumber = phoneNumber.trim();
    }

    public String getMedicalCondition() {
        return medicalCondition;
    }

    public void setMedicalCondition(String medicalCondition) throws InvalidPatientException {
        if (medicalCondition == null || medicalCondition.trim().isEmpty()) {
            throw new InvalidPatientException("Medical condition cannot be empty.");
        }
        this.medicalCondition = medicalCondition.trim();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) throws InvalidPatientException {
        if (priority < 1 || priority > 5) {
            throw new InvalidPatientException("Priority must be between 1 (CRITICAL) and 5 (LOW) (found: " + priority + ").");
        }
        this.priority = priority;
    }

    public long getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(long registrationTime) {
        this.registrationTime = registrationTime;
    }

    /**
     * Formats registration timestamp into human-readable format.
     */
    public String getFormattedRegistrationTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(registrationTime));
    }

    /**
     * Returns the textual description of the patient's numeric priority.
     */
    public String getPriorityLevelName() {
        switch (priority) {
            case 1: return "CRITICAL";
            case 2: return "VERY HIGH";
            case 3: return "HIGH";
            case 4: return "MEDIUM";
            case 5: return "LOW";
            default: return "UNKNOWN";
        }
    }

    /**
     * Displays patient details to standard output.
     * Can be overridden by child classes (Polymorphism).
     */
    public void displayDetails() {
        System.out.println("--------------------------------------------------");
        System.out.printf("  Patient ID       : %d%n", patientId);
        System.out.printf("  Name             : %s%n", name);
        System.out.printf("  Age / Gender     : %d / %s%n", age, gender);
        System.out.printf("  Phone Number     : %s%n", phoneNumber);
        System.out.printf("  Condition        : %s%n", medicalCondition);
        System.out.printf("  Priority         : %d - %s%n", priority, getPriorityLevelName());
        System.out.printf("  Registered At    : %s%n", getFormattedRegistrationTime());
        System.out.println("--------------------------------------------------");
    }

    @Override
    public String toString() {
        return String.format("[ID=%d, Name='%s', Age=%d, Condition='%s', Priority=%d (%s)]",
                patientId, name, age, medicalCondition, priority, getPriorityLevelName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Patient patient = (Patient) o;
        return patientId == patient.patientId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId);
    }
}
