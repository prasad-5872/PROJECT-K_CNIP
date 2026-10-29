# Project Report: Reliable Emergency Communication Network Under Congested Conditions

## Abstract
This project simulates a disaster-response communication network connecting an emergency control center, police, fire service, ambulances, field teams, and shelters. It demonstrates shortest-path routing, routing-table convergence, link failure recovery, frame corruption detection, retransmission, traffic shaping, and performance measurement in a Java Swing application.

## Problem Statement
During disasters, traffic surges, corrupted or lost frames, and failed communication links can delay critical messages. The goal is to model these conditions and study routing and reliability mechanisms.

## Objectives
1. Find efficient routes using Dijkstra's algorithm.
2. maintain distance-vector routing tables.
3. demonstrate alternative routing after link failure.
4. detect modified frames using CRC.
5. recover from frame loss with Go-Back-N ARQ.
6. regulate burst traffic with a Leaky Bucket.
7. measure delivery ratio, loss, throughput, delay, and retransmissions.

## System Design
The network is modeled as an undirected weighted graph. GUI controls let the operator disable links, select source/destination, configure traffic/error rates, and execute simulation experiments. The event log records routes, routing tables, CRC results, and performance metrics.

## Implementation
- Language: Java (JDK 17+)
- GUI: Java Swing
- Routing: Dijkstra and iterative Distance Vector
- Error detection: CRC-8 polynomial 0x07
- Error recovery: simplified Go-Back-N ARQ
- Congestion control: Leaky Bucket
- Tests: standalone Java self-test class

## Testing Plan
| Test | Action | Expected result |
|---|---|---|
| Shortest route | Run Dijkstra on default graph | Finite minimum-cost path |
| Single-link failure | Disable one link | Alternate path if available |
| Disconnected graph | Disable all paths to destination | No route |
| CRC valid frame | Check original bytes | Valid |
| CRC corrupted frame | Flip a payload bit | Invalid |
| Go-Back-N, 0% error | Run transfer | All frames delivered |
| Bucket overflow | Arrivals exceed capacity | Dropped arrivals reported |
| High error rate | Increase error slider | Generally more retransmissions and delay |

## Results
Insert screenshots of the GUI and paste measured results from repeated runs. Do not claim sample values as experimental findings. Use at least 5 trials per setting and report the mean if this is for a formal evaluation.

## Limitations
The model abstracts link timing and uses probabilistic protocol behavior. It is not a production emergency communication network and does not implement real sockets, radio links, authentication, encryption, or real NS-2 integration.

## Conclusion
The simulator provides an interactive demonstration of how routing, error detection/recovery, and traffic shaping can support reliable message delivery under selected failure and congestion scenarios.
