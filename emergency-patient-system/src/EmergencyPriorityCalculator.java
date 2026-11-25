import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EmergencyPriorityCalculator implements the PriorityCalculator interface.
 * It provides standard emergency department triage priority calculation based
 * on clinical severity rules.
 * 
 * Demonstrates:
 * 1. Interface implementation (Polymorphism & Abstraction)
 * 2. Collections (Map / LinkedHashMap)
 * 3. Encapsulation (private mapping table with public accessor/mutator)
 * 4. Extensibility (ability to add new conditions dynamically)
 */
public class EmergencyPriorityCalculator implements PriorityCalculator {

    // Internal mapping of normalized condition name -> priority score (1 to 5)
    // LinkedHashMap preserves insertion order for display purposes
    private final Map<String, Integer> conditionPriorityMap;
    // Map storing the original display name of the condition
    private final Map<String, String> displayNamesMap;

    /**
     * Constructs an EmergencyPriorityCalculator and registers default emergency conditions.
     */
    public EmergencyPriorityCalculator() {
        this.conditionPriorityMap = new LinkedHashMap<>();
        this.displayNamesMap = new LinkedHashMap<>();
        initializeDefaultConditions();
    }

    /**
     * Initializes default clinical conditions and their triage priority levels.
     */
    private void initializeDefaultConditions() {
        registerConditionInternal("Cardiac Arrest", 1);
        registerConditionInternal("Severe Bleeding", 2);
        registerConditionInternal("Fracture", 3);
        registerConditionInternal("High Fever", 4);
        registerConditionInternal("Minor Injury", 5);
    }

    private void registerConditionInternal(String condition, int priority) {
        String key = normalizeCondition(condition);
        conditionPriorityMap.put(key, priority);
        displayNamesMap.put(key, condition.trim());
    }

    /**
     * Normalizes a condition string for case-insensitive and whitespace-tolerant matching.
     */
    private String normalizeCondition(String condition) {
        if (condition == null) {
            return "";
        }
        return condition.trim().toLowerCase();
    }

    @Override
    public int calculatePriority(String condition) throws InvalidPatientException {
        if (condition == null || condition.trim().isEmpty()) {
            throw new InvalidPatientException("Medical condition cannot be empty or null.");
        }

        String key = normalizeCondition(condition);
        Integer priority = conditionPriorityMap.get(key);

        if (priority == null) {
            throw new InvalidPatientException(
                "Unrecognized medical condition: '" + condition.trim() + 
                "'. Supported conditions are: " + getSupportedConditions()
            );
        }

        return priority;
    }

    @Override
    public String getPriorityLevelName(int priority) {
        switch (priority) {
            case 1:
                return "CRITICAL";
            case 2:
                return "VERY HIGH";
            case 3:
                return "HIGH";
            case 4:
                return "MEDIUM";
            case 5:
                return "LOW";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public boolean isValidPriority(int priority) {
        return priority >= 1 && priority <= 5;
    }

    @Override
    public List<String> getSupportedConditions() {
        return new ArrayList<>(displayNamesMap.values());
    }

    /**
     * Adds or updates a medical condition with a specific priority level.
     * Demonstrates dynamic extensibility of the priority calculator.
     * 
     * @param condition The condition name
     * @param priority Priority score (1 to 5)
     * @throws InvalidPatientException if the condition is blank or priority is invalid
     */
    public void addCondition(String condition, int priority) throws InvalidPatientException {
        if (condition == null || condition.trim().isEmpty()) {
            throw new InvalidPatientException("Condition name cannot be empty.");
        }
        if (!isValidPriority(priority)) {
            throw new InvalidPatientException("Priority must be between 1 (CRITICAL) and 5 (LOW).");
        }
        registerConditionInternal(condition, priority);
    }
}
