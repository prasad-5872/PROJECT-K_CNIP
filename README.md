# Reliable Emergency Communication Network Under Congested Conditions (Java GUI)

A runnable Java Swing educational simulator demonstrating:
- Dijkstra shortest-path routing over active weighted links
- iterative Distance Vector routing-table convergence
- link failure and alternate route selection
- CRC-8 corruption detection
- simplified Go-Back-N ARQ loss/error recovery
- Leaky Bucket traffic shaping and queue overflow
- delivery ratio, throughput, average delay, retransmissions, corruption, and bucket drops

## Requirements
- JDK 17 or later
- No third-party libraries; uses Java Swing and the standard library

## Run the GUI
From this directory:

```bash
javac -d out src/emergency/*.java
java -cp out emergency.EmergencyNetworkApp
```

A desktop environment is required for Swing. On a headless server, run tests instead.

## Run tests
```bash
javac -d out src/emergency/*.java test/emergency/ProjectSelfTest.java
java -cp out emergency.ProjectSelfTest
```

Expected ending:
```
PASS: 7 checks
```

## GUI walkthrough
1. Select source and destination, then click **Find shortest route (Dijkstra)**.
2. Select a communication link and click **Toggle selected link UP / DOWN**. Failed links turn red; route recalculates.
3. Click **Recompute Distance Vector tables** to print converged routing tables.
4. Click **Run CRC self-test** to verify the original message and show corruption detection.
5. Adjust message count, frame error/loss rate, traffic arrivals, bucket capacity, and leak rate.
6. Click **Run end-to-end simulation** and inspect the event log and metrics.

## Algorithms and assumptions
- The topology is an undirected graph. Edge costs are abstract routing weights, not measured network latency.
- Dijkstra computes a minimum-cost route using active links.
- Distance Vector uses iterative Bellman-Ford-style neighbor advertisements until stable, with a bounded iteration count.
- CRC-8 uses polynomial `0x07` (x^8 + x^2 + x + 1). It is a teaching example; CRC-32 is more common in production systems.
- Go-Back-N is modeled probabilistically. When a frame fails, the oldest unacknowledged frame becomes the next base and the outstanding window is retried. This is a simulation, not a real transport protocol.
- The Leaky Bucket simulation accepts arrivals up to queue capacity and drains at a fixed rate per tick.
- Performance figures are generated from model assumptions. They are not benchmarks of a deployed emergency network.

## Metric definitions
- Delivery ratio = successfully delivered messages / generated messages.
- Packet loss = generated messages not delivered / generated messages.
- Throughput = successfully delivered payload bits / modeled elapsed time.
- Average delay = mean modeled delay among successfully delivered messages.
- Retransmissions and corrupted frames are counted by the Go-Back-N model.
- Leaky Bucket drops are reported separately from message delivery counts, so they are not double-counted.

## Suggested advanced experiments
1. Run at error rates 0%, 10%, 25%, and 40%; record delivery ratio and retransmissions.
2. Disable the Control Center–Police link and compare route cost/path.
3. Disable several links to partition the graph; confirm Dijkstra reports no route.
4. Increase offered traffic while keeping bucket capacity fixed; compare bucket drops.
5. Increase bucket capacity and observe the tradeoff between drops and queueing.
6. Export the metrics manually into a spreadsheet and graph throughput/delay against error rate.

## Important scope note
This is a self-contained GUI simulation, not a real radio/network implementation and not an NS-2 project. It does not connect to emergency services or transmit actual messages. For a research-grade simulator, add event timestamps, packet queues per link, configurable bandwidth/propagation delay, ACK timers, sequence numbers, and repeated trials with fixed random seeds.
