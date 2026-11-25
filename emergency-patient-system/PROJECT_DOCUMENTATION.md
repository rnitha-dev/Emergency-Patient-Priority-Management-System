# Emergency Patient Priority Management System - Academic Documentation

## 1. Project Title
**Emergency Patient Priority Management System (EPPMS)**

---

## 2. Abstract
In hospital emergency departments, timely prioritization of incoming patients directly influences clinical outcomes and survival rates. Traditional first-come-first-served (FCFS) queuing fails to accommodate medical urgency. The Emergency Patient Priority Management System is a Java console application modeling a hospital emergency triage ward. It dynamically assigns emergency priority levels (1 - CRITICAL to 5 - LOW) based on medical conditions, maintains a thread-safe Priority Queue sorted by clinical urgency and arrival timestamp, facilitates atomic updates of medical conditions and priorities, tracks treatment histories, and safely handles concurrent intake and triage updates through multi-threading and synchronization.

---

## 3. Problem Statement
Hospital emergency rooms frequently experience sudden influxes of patients presenting conditions ranging from mild injuries to immediately life-threatening cardiovascular crises. If managed using standard sequential queues:
1. Critical patients experience dangerous delays behind patients with minor ailments.
2. Changes in patient condition while waiting in the ER are not dynamically reflected in queue order.
3. Multiple intake desks operating simultaneously risk race conditions, inconsistent records, or lost updates without thread synchronization.
4. Manual triage record-keeping lacks persistent audit trails of treatment events.

---

## 4. Objectives
- Implement an automated, condition-based triage priority calculation engine.
- Structure an emergency waiting queue using a Java PriorityQueue with a two-tier comparator (Priority ascending, Arrival timestamp ascending).
- Ensure thread safety across concurrent patient registrations and condition updates using synchronized methods.
- Demonstrate core Object-Oriented Programming (OOP) concepts: Encapsulation, Inheritance, Polymorphism, Abstraction, and Custom Exception Handling.
- Support complete CRUD-style lifecycle management: registration, searching, updating, treating, removing, and historical audit logging.
- Implement file persistence for saving and restoring treatment records without external libraries.

---

## 5. System Features
1. Patient Registration: Capture complete patient demographics with instant clinical priority assignment.
2. Waiting Queue Display: Formatted real-time list of waiting patients in strict treatment sequence.
3. Patient Search: Fast O(1) lookup by ID in active queue, falling back to treatment history.
4. Condition Update & Heap Re-Ordering: Dynamically modifies condition, recalculates priority, and safely refreshes the PriorityQueue.
5. Manual Priority Override: Allows clinical triage staff to adjust priority between 1 and 5.
6. Next Patient Treatment: Dequeues highest priority patient, logs treatment time, and records into history.
7. Patient Removal: Removes patients from active queue upon triage discharge or transfer.
8. Treatment History Audit: Chronological log of treated patients with treatment timestamps.
9. Sequential Batch Registration: Intake wizard for registering multiple patients sequentially.
10. Concurrent Registration Demo: Multi-threaded simulation of multiple intake desks registering patients simultaneously.
11. Concurrent Priority Update Demo: Multi-threaded simulation of nurses/doctors modifying patient conditions concurrently.
12. Persistent Storage: Text file storage (treatment_history.txt) using BufferedReader and BufferedWriter.

---

## 6. Technologies Used
- Programming Language: Java SE (Standard Edition)
- Build Tools: Standard javac compiler and java JVM
- Data Structures: PriorityQueue, HashMap, ArrayList
- Concurrency: Thread, synchronized methods, start(), join()
- File I/O: FileReader, FileWriter, BufferedReader, BufferedWriter
- Design Paradigm: Object-Oriented Programming (Pure Standard Java, Zero External Dependencies)

---

## 7. OOP Concepts Demonstrated
- Classes and Objects: Real-world triage entities modeled as Patient, EmergencyPatient, EmergencyManagementSystem.
- Encapsulation: All fields private with validated getters and setters (age 1-120, priority 1-5).
- Inheritance: EmergencyPatient extends Patient, reusing core demographics while adding ER triage fields.
- Polymorphism: Runtime polymorphism via overridden displayDetails() and interface reference PriorityCalculator.
- Abstraction: PriorityCalculator interface decouples calculation contract from implementation.
- Interfaces: PriorityCalculator defines required contract.
- Method Overriding: @Override on displayDetails(), toString(), equals(), hashCode().
- Constructors: Parameterized and overloaded constructors ensuring valid object state.
- Custom Exception: InvalidPatientException extends Exception for domain validation.

---

## 8. System Architecture
Console UI (Main / SystemTest)
         |
         v
EmergencyManagementSystem
  - PriorityQueue<EmergencyPatient>
  - HashMap<Integer, EmergencyPatient>
  - ArrayList<EmergencyPatient>
  - PriorityCalculator reference
  - treatment_history.txt persistence

---

## 9. Class Diagram (ASCII UML)

+---------------------------------------------------------------+
|                     <<interface>>                             |
|                  PriorityCalculator                           |
+---------------------------------------------------------------+
| + calculatePriority(condition: String): int                  |
| + getPriorityLevelName(priority: int): String                 |
| + isValidPriority(priority: int): boolean                     |
| + getSupportedConditions(): List<String>                      |
+---------------------------------------------------------------+
                                ^
                                | implements
+---------------------------------------------------------------+
|                  EmergencyPriorityCalculator                  |
+---------------------------------------------------------------+
| - conditionPriorityMap: Map<String, Integer>                  |
| - displayNamesMap: Map<String, String>                        |
+---------------------------------------------------------------+
| + calculatePriority(condition: String): int                  |
| + getPriorityLevelName(priority: int): String                 |
| + isValidPriority(priority: int): boolean                     |
| + getSupportedConditions(): List<String>                      |
| + addCondition(condition: String, priority: int): void        |
+---------------------------------------------------------------+

+---------------------------------------------------------------+
|                            Patient                            |
+---------------------------------------------------------------+
| - patientId: int                                              |
| - name: String                                                |
| - age: int                                                    |
| - gender: String                                              |
| - phoneNumber: String                                         |
| - medicalCondition: String                                    |
| - priority: int                                               |
| - registrationTime: long                                      |
+---------------------------------------------------------------+
| + Patient(id, name, age, gender, phone, cond, prio)           |
| + getPatientId(): int                                         |
| + setPatientId(id: int): void                                 |
| + getName(): String                                           |
| + setName(name: String): void                                 |
| + getAge(): int                                               |
| + setAge(age: int): void                                      |
| + getMedicalCondition(): String                               |
| + setMedicalCondition(cond: String): void                     |
| + getPriority(): int                                          |
| + setPriority(prio: int): void                                |
| + getFormattedRegistrationTime(): String                      |
| + getPriorityLevelName(): String                              |
| + displayDetails(): void                                      |
| + toString(): String                                          |
| + equals(o: Object): boolean                                  |
| + hashCode(): int                                             |
+---------------------------------------------------------------+
                                ^
                                | extends (Inheritance)
+---------------------------------------------------------------+
|                       EmergencyPatient                        |
+---------------------------------------------------------------+
| - triageNotes: String                                         |
| - isTraumaCase: boolean                                       |
| - treatmentTime: String                                       |
+---------------------------------------------------------------+
| + EmergencyPatient(...)                                       |
| + getTriageNotes(): String                                    |
| + setTriageNotes(notes: String): void                         |
| + isTraumaCase(): boolean                                     |
| + setTraumaCase(trauma: boolean): void                        |
| + getTreatmentTime(): String                                  |
| + setTreatmentTime(time: String): void                        |
| + displayDetails(): void (@Override Polymorphism)             |
| + toString(): String (@Override)                              |
+---------------------------------------------------------------+

+---------------------------------------------------------------+
|                   EmergencyManagementSystem                   |
+---------------------------------------------------------------+
| - waitingQueue: PriorityQueue<EmergencyPatient>               |
| - patientMap: Map<Integer, EmergencyPatient>                  |
| - treatmentHistory: List<EmergencyPatient>                    |
| - priorityCalculator: PriorityCalculator                      |
| - historyFilePath: String                                     |
+---------------------------------------------------------------+
| + synchronized registerPatient(p: EmergencyPatient): void     |
| + synchronized treatNextPatient(): EmergencyPatient           |
| + synchronized updateMedicalCondition(id: int, c: String)     |
| + synchronized updatePriority(id: int, p: int): void          |
| + synchronized removePatient(id: int): EmergencyPatient       |
| + synchronized searchPatient(id: int): EmergencyPatient       |
| + synchronized getWaitingPatients(): List<EmergencyPatient>   |
| + synchronized getTreatmentHistory(): List<EmergencyPatient>  |
| + synchronized saveTreatmentHistoryToFile(): void             |
| + synchronized loadTreatmentHistoryFromFile(): int            |
+---------------------------------------------------------------+

+-----------------------------+   +-----------------------------+
|  PatientRegistrationThread  |   |     PriorityUpdateThread    |
|       extends Thread        |   |        extends Thread       |
+-----------------------------+   +-----------------------------+
| - system: EMS               |   | - system: EMS               |
| - patient: EmergencyPatient |   | - patientId: int            |
+-----------------------------+   | - newCondition: String      |
| + run(): void               |   | - newPriority: int          |
+-----------------------------+   +-----------------------------+
                                  | + run(): void               |
                                  +-----------------------------+

---

## 10. Use-Case Diagram Description
- Actor: Triage Nurse: Registers incoming patients, assigns notes, updates condition upon reassessment.
- Actor: Attending Emergency Doctor: Manually overrides priority; calls Treat Next Patient to accept emergency cases.
- Actor: Hospital Administrator: Searches patient records, views waiting queue, removes patients, checks history.
- Actor: Concurrent Intake Desks (Threads): Registers multiple patients concurrently without race conditions.

---

## 11. Data Structures Used
1. PriorityQueue<EmergencyPatient>: Min-Heap sorted by (1) Priority ascending, (2) Registration Time ascending.
2. HashMap<Integer, EmergencyPatient>: Fast O(1) patient ID lookup and duplicate prevention.
3. ArrayList<EmergencyPatient>: Dynamic chronological treatment history list.

---

## 12. Algorithms
1. Patient Registration: Validates inputs, calculates priority, synchronizes on EMS, ensures unique ID, inserts into PriorityQueue and HashMap.
2. Priority Calculation: Normalizes condition name, checks mapping table, returns priority (1 to 5) or throws InvalidPatientException.
3. Patient Search: Looks up in HashMap (O(1)); if absent, scans treatmentHistory list (O(m)); if absent, throws InvalidPatientException.
4. Condition Update & Heap Re-ordering: In synchronized block, finds patient, calculates new priority, removes from PriorityQueue, updates fields, and re-inserts into PriorityQueue.
5. Priority Update: In synchronized block, validates priority (1-5), removes from PriorityQueue, updates priority, re-inserts into PriorityQueue.
6. Treat Next Patient: In synchronized block, checks queue not empty, polls highest-priority patient, removes from HashMap, sets treatment timestamp, adds to treatmentHistory, appends to file.
7. Patient Removal: In synchronized block, finds patient, removes from PriorityQueue and HashMap.
8. Concurrent Registration: Spawns PatientRegistrationThread workers, invokes start(), runs synchronized registration, and invokes join().
9. Concurrent Priority Update: Spawns PriorityUpdateThread workers, runs synchronized update operations, and joins threads.

---

## 13. Multithreading & Synchronization Explanation
- Shared resources: PriorityQueue, HashMap, and ArrayList are shared across multiple threads.
- Synchronization mechanism: Public mutator methods are synchronized on the EmergencyManagementSystem instance monitor.
- Atomic Heap Update: Updating an element's priority while inside a Java PriorityQueue does not trigger re-heapification. The element is atomically removed, updated, and re-inserted inside a synchronized method.
- Thread management: Custom threads extend Thread and use start() for asynchronous execution and join() for coordination.

---

## 14. Exception Handling Explanation
- Custom Exception: InvalidPatientException extends java.lang.Exception.
- Handled errors: Age outside [1, 120], blank names, duplicate patient IDs, invalid priority values outside [1, 5], unrecognized conditions, treating empty queue, searching nonexistent patients.
- Resilient CLI: Main catches exceptions, prints friendly error messages, and returns cleanly to menu without crashing.

---

## 15. Test Cases and Verification
All 18 comprehensive test cases pass with 100% success rate in SystemTest.java.

---

## 16. Advantages
- Lifesaving medical triage priority scheduling.
- Fair FIFO resolution for equal urgency cases.
- Thread-safe concurrent intake and triage updates.
- Modular OOP design suitable for academic viva and practical extension.

---

## 17. Limitations & Future Enhancements
- Future GUI (JavaFX / Swing).
- Relational database storage (PostgreSQL / MySQL).
- Real-time vitals telemetry integration (SpO2, Blood Pressure).