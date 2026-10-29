# Reliable Emergency Communication Network Under Congested Conditions

## Project title
Reliable Emergency Communication Network Under Congested Conditions

## Problem statement
A disaster management network connects an emergency control center with police, fire services, ambulances, field teams, and emergency shelters. During an emergency, a large number of messages may be generated while some communication links may fail or become congested. The project develops a reliable emergency communication network that demonstrates routing, error recovery, congestion control, and performance analysis under changing network conditions.

## Objective
The goal of this project is to simulate a resilient emergency communication system that can:
- route messages efficiently under network changes,
- adapt when communication links fail,
- detect corrupted frames,
- recover from transmission errors,
- regulate network traffic under congestion,
- measure communication performance metrics under load.

## Core concepts implemented
- Dijkstra shortest path routing
- Distance Vector routing updates
- Routing under link failures
- CRC-8 error detection
- Go-Back-N retransmission modeling
- Leaky Bucket congestion control
- Packet loss analysis
- Throughput measurement
- Average delay estimation
- Delivery ratio and reliability assessment

## System design overview
The network is represented as a graph of emergency nodes:
- Control Center
- Police
- Fire Service
- Ambulance
- Field Team
- Shelter A
- Shelter B

Connections between nodes are modeled as weighted links. The system can:
1. compute the shortest valid route between nodes,
2. disable a link to simulate a failure,
3. recalculate active routes,
4. examine routing tables produced by distance-vector convergence,
5. run congestion and reliability simulations,
6. display the final metrics and event log.

## Project architecture
- `src/emergency/EmergencyNetworkApp.java`  -> Swing GUI application entry point
- `src/emergency/NetworkModel.java`        -> communication topology and link states
- `src/emergency/RoutingAlgorithms.java`   -> Dijkstra and distance-vector logic
- `src/emergency/ProtocolSimulator.java`   -> CRC and message simulation utilities
- `src/emergency/SimulationEngine.java`     -> end-to-end network simulation
- `src/emergency/congestion/*`            -> leaky bucket and congestion logic
- `src/emergency/model/*`                  -> graph entities and network model classes
- `src/emergency/routing/*`                -> route and routing table classes
- `src/emergency/ui/*`                    -> GUI panels and interface components
- `test/emergency/ProjectSelfTest.java`    -> validation tests

## Features
- Real-time network topology visualization
- Route selection using shortest path algorithms
- Link toggle to simulate outages
- Automatic route recalculation
- Error detection with CRC
- Packet loss and retransmission behavior simulation
- Traffic shaping using leaky bucket logic
- Console and GUI-based performance metrics

## Run the GUI application
From the repository root:

```bash
javac -d out src/emergency/*.java
java -cp out emergency.EmergencyNetworkApp
```

## Run the test suite
```bash
javac -d out src/emergency/*.java test/emergency/ProjectSelfTest.java
java -cp out emergency.ProjectSelfTest
```

Expected output:
```text
PASS: 7 checks
```

## Technologies used
- Java
- Swing GUI
- Standard Java libraries
- Network simulation concepts and algorithms

## Summary
This repository implements a realistic educational simulation of an emergency communication network. It demonstrates how routing, failure recovery, congestion control, and error detection work together to maintain reliable communication in a disaster scenario.
