# Project K High-Level Overview

This repository implements a realistic emergency communication network simulation, designed to reflect how communication systems behave during disaster management operations.

## 1. Purpose of the project
The project models a communication network used by emergency response teams to coordinate rescue, medical assistance, and evacuation operations. In real life, these systems must remain dependable even when links fail, messages are delayed, and network traffic becomes heavy.

## 2. Real-world scenario
Consider a disaster site where the control center must communicate with:
- police units,
- fire stations,
- ambulances,
- field teams,
- shelters.

During emergencies, messages can be numerous and time-sensitive. Some links may fail, some frames may be corrupted, and some nodes may receive traffic beyond normal capacity. A good communication network should recover quickly and maintain acceptable performance.

## 3. What the system does
The project simulates the following behavior:
1. Nodes are connected through an emergency network graph.
2. Dijkstra’s algorithm finds the shortest available communication path.
3. Distance Vector routing generates routing tables for each node.
4. If a communication link fails, the system adapts to alternate routes.
5. Frames are checked for corruption using CRC.
6. Lost or damaged data is retried with Go-Back-N behavior.
7. Traffic is controlled using the Leaky Bucket algorithm.
8. Performance metrics such as packet loss and throughput are measured.

## 4. Why this is realistic
This project reflects actual networking concerns in critical systems:
- availability under failures,
- resilience during congestion,
- error detection and recovery,
- path recalculation under changing topology,
- performance evaluation for operational readiness.

## 5. Academic and technical value
This project is useful for understanding how communication systems are designed under difficult conditions. It demonstrates how core networking theories are applied in practice in scenarios where reliability is more important than raw speed.

## 6. Main outcomes
The implementation provides a clear understanding of:
- route selection in dynamic networks,
- packet reliability under errors,
- congestion control strategies,
- network behavior under stress,
- decision-making in emergency communication infrastructure.

## 7. Final view
The repository is a complete educational simulation of a reliable emergency communication network and is aligned with the assignment topic: “Reliable Emergency Communication Network Under Congested Conditions.”
