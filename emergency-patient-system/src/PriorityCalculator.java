import java.util.List;

/**
 * PriorityCalculator defines the contract for calculating medical priority
 * and retrieving priority level descriptions.
 * 
 * Demonstrates:
 * 1. Interface (OOP Abstraction)
 * 2. Separation of concerns (Triage logic separated from patient data)
 * 3. Extensibility (allows different calculator implementations)
 */
public interface PriorityCalculator {

    /**
     * Calculates the emergency priority for a given medical condition.
     * Lower numeric value represents higher emergency priority (1 = Highest, 5 = Lowest).
     * 
     * @param condition The medical condition of the patient
     * @return Integer priority level (1 to 5)
     * @throws InvalidPatientException if the condition is invalid or unrecognized
     */
    int calculatePriority(String condition) throws InvalidPatientException;

    /**
     * Returns the human-readable text name for a priority level.
     * 
     * @param priority Numeric priority (1 to 5)
     * @return String description (e.g. "CRITICAL", "VERY HIGH")
     */
    String getPriorityLevelName(int priority);

    /**
     * Checks if a numeric priority is within valid bounds (1 to 5).
     * 
     * @param priority Priority to check
     * @return true if 1 <= priority <= 5, false otherwise
     */
    boolean isValidPriority(int priority);

    /**
     * Returns a list of standard supported medical conditions.
     * 
     * @return List of condition names
     */
    List<String> getSupportedConditions();
}
