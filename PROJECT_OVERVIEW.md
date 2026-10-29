# Project K High-Level Overview

This repository is built around the emergency communication network project described in the assignment.

## Business / academic goal
The project aims to model an emergency communication infrastructure that remains operational even when links fail, traffic becomes heavy, and messages may be corrupted or lost. The system helps understand how routing algorithms and congestion-control mechanisms improve reliability.

## High-level system behavior
1. A set of emergency nodes is represented as a network.
2. Active links form a communication graph.
3. Dijkstra routing selects the best path between source and destination.
4. Distance Vector routing produces routing tables for each node.
5. Failing links trigger alternate path discovery.
6. Data frames are checked for corruption using CRC.
7. Lost or corrupt frames are retried using Go-Back-N logic.
8. Traffic is shaped using the Leaky Bucket method.
9. Simulation metrics report delivery ratio, delay, throughput, and drops.

## Why this project matters
Emergency response systems depend on reliable communication to coordinate rescue operations. This simulation models that challenge and demonstrates methods used to keep information flowing despite congestion and failures.

## Outcome
The project gives a practical, visual understanding of:
- routing in communication networks,
- resilience under failures,
- error detection and recovery,
- traffic control in congested conditions,
- analysis of operational performance metrics.
