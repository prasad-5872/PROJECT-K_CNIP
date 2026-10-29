# Reliable Emergency Communication Network Under Congested Conditions

## Project title
Reliable Emergency Communication Network Under Congested Conditions

## 1. Problem statement
A disaster management network connects an emergency control center with police, fire services, ambulances, field teams, and emergency shelters. During an emergency, a large number of messages may be generated while some communication links may become unavailable or congested. The project develops a reliable emergency communication network that demonstrates routing, error recovery, congestion control, and performance analysis under changing network conditions.

## 2. Objective
The main objective of this project is to design and simulate an emergency communication system that continues to function efficiently even when:
- communication links fail,
- traffic volume increases unexpectedly,
- signals are corrupted or lost,
- routing paths need to be recalculated dynamically,
- congestion affects message delivery.

The system helps understand how information can be delivered reliably in emergency scenarios where time and accuracy are critical.

## 3. Real-world motivation
In disaster response operations, coordination between emergency units is essential. A broken or overloaded communication path can delay rescue, medical support, and evacuation. This project models the realistic behavior of a mission-critical communication network and demonstrates how network algorithms support resilience and performance.

## 4. Key concepts implemented
This project covers the following networking and communication principles:
- Dijkstra shortest-path routing
- Distance Vector routing updates
- Routing under link failure scenarios
- CRC-based error detection
- Go-Back-N retransmission strategy
- Leaky Bucket congestion control
- Packet loss estimation
- Throughput calculation
- Average delay measurement
- Delivery ratio analysis

## 5. System model
The network is represented as a graph with emergency nodes:
- Control Center
- Police
- Fire Service
- Ambulance
- Field Team
- Shelter A
- Shelter B

Each connection between nodes is treated as a communication link with a cost or weight. The system can:
1. compute the best route between two nodes,
2. disable a link to represent a failure,
3. recalculate paths using active links,
4. inspect converged routing tables,
5. simulate errors and retransmissions,
6. evaluate traffic behavior under congestion,
7. report performance metrics for analysis.

## 6. Functional features
- Interactive Java Swing GUI
- Live network topology visualization
- Dijkstra route calculation
- Distance Vector table generation
- Link toggling to simulate failure scenarios
- CRC self-check for corrupted data
- End-to-end network simulation under changed conditions
- Traffic shaping using leaky bucket logic
- Performance analysis with delivery ratio, loss, throughput, and delay

## 7. Project structure
- `src/emergency/EmergencyNetworkApp.java` - main Java Swing application
- `src/emergency/NetworkModel.java` - graph and link model
- `src/emergency/RoutingAlgorithms.java` - routing algorithms
- `src/emergency/ProtocolSimulator.java` - CRC and protocol logic
- `src/emergency/SimulationEngine.java` - simulation logic and metrics
- `src/emergency/congestion/` - congestion and traffic control logic
- `src/emergency/model/` - network model classes
- `src/emergency/routing/` - path and route data structures
- `src/emergency/ui/` - GUI components and panels
- `test/emergency/ProjectSelfTest.java` - project validation tests

## 8. Execution instructions
### Run the GUI application
From the repository root:

```bash
javac -d out src/emergency/*.java
java -cp out emergency.EmergencyNetworkApp
```

### Run the self-tests
```bash
javac -d out src/emergency/*.java test/emergency/ProjectSelfTest.java
java -cp out emergency.ProjectSelfTest
```

Expected result:
```text
PASS: 7 checks
```

## 9. Metrics used for analysis
The simulation reports the following network performance indicators:
- Delivery ratio
- Packet loss
- Throughput
- Average delay
- Retransmissions
- Corruption occurrences
- Leaky bucket drops

These metrics help evaluate how effectively the emergency communication network performs under different conditions.

## 10. Technologies used
- Java
- Swing (GUI)
- Standard Java libraries
- Networking and simulation concepts

## 11. Summary
This project is a realistic educational simulation of a disaster communication network. It demonstrates how network routing, recovery strategies, error detection, and congestion management work together to maintain communication reliability in emergency environments.

## 12. Assignment relevance
This repository directly matches the problem statement:
- Reliable emergency communication network
- Congestion conditions
- Routing and recovery
- CRC, Go-Back-N, and Leaky Bucket
- Delay, packet loss, and throughput
- Java-based implementation
