/**
 * InvalidPatientException is a custom checked exception representing invalid
 * patient domain data or illegal operations in the Emergency Department system.
 * 
 * Demonstrates:
 * 1. User-Defined Custom Exception
 * 2. Inheritance (extends java.lang.Exception)
 * 3. Exception propagation and handling
 */
public class InvalidPatientException extends Exception {

    /**
     * Constructs a new InvalidPatientException with the specified detail message.
     * 
     * @param message Descriptive error message
     */
    public InvalidPatientException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidPatientException with a message and a root cause.
     * 
     * @param message Descriptive error message
     * @param cause Underlying cause throwable
     */
    public InvalidPatientException(String message, Throwable cause) {
        super(message, cause);
    }
}
