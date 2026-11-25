# Emergency Patient Priority Management System

A complete console-based Java Object-Oriented Programming (OOP) course project for hospital emergency department triage management.

## Project Structure
```
emergency-patient-system/
|-- src/
|   |-- InvalidPatientException.java      # Custom checked exception
|   |-- PriorityCalculator.java           # Interface defining priority contract
|   |-- EmergencyPriorityCalculator.java  # Interface implementation with triage rules
|   |-- Patient.java                      # Base encapsulated patient class
|   |-- EmergencyPatient.java             # Child class demonstrating inheritance & polymorphism
|   |-- EmergencyManagementSystem.java    # Thread-safe triage controller with PriorityQueue
|   |-- PatientRegistrationThread.java    # Multithreading worker for concurrent registration
|   |-- PriorityUpdateThread.java         # Multithreading worker for concurrent updates
|   |-- Main.java                         # Interactive menu-driven console interface
|   +-- SystemTest.java                   # Automated test suite covering all 18 test cases
|-- bin/                                  # Compiled .class bytecode
|-- treatment_history.txt                 # Persistent storage file for treated patients
|-- PROJECT_DOCUMENTATION.md              # 20-section comprehensive academic documentation
+-- README.md                             # Quickstart and run instructions
```

## How to Compile
From the emergency-patient-system directory:
```bash
javac -d bin src/*.java
```

## How to Run the Automated Test Suite (18 Tests)
```bash
java -cp bin SystemTest
```

## How to Run the Interactive System Menu
```bash
java -cp bin Main
```
