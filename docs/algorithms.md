# SystemTwin — Core Algorithms & Mathematical Formulations

This document provides formal descriptions, mathematical formulations, and algorithmic specifications for the core algorithms implemented in SystemTwin.

---

## 1. Graph Failure Propagation (BFS Cascading)

### Problem Statement
When a component $c_{root}$ experiences a fault or enters `FAILED` state, all downstream systems that depend upon $c_{root}$ (directly or transitively) will either degrade in performance or suffer cascading failure.

### Algorithmic Formulation
We maintain a directed dependency graph $G = (V, E)$, where directed edge $(u, v) \in E$ denotes that component $u$ depends on component $v$.

The set of directly affected dependents for node $v$ is:
$$\text{Dependents}(v) = \{ u \in V \mid (u, v) \in E \}$$

The cascading propagation is executed using level-order Breadth-First Search (BFS):
1. **Hop 0**: Set $c_{root}$ state to `FAILED`, CPU = 0%, and enqueue $c_{root}$.
2. **Hop 1 (Direct Dependents)**: For each $u \in \text{Dependents}(c_{root})$, set state to `DEGRADED` (or `FAILED` if immediate mode enabled), CPU += 40%, latency elevated. Enqueue $u$.
3. **Hop $k \ge 2$ (Transitive Cascades)**: Downstream nodes depending on degraded/failed parents transition to `FAILED` due to missing upstream prerequisites.

---

## 2. Systemic Criticality & Single Point of Failure (SPOF) Scoring

### Formula
For each node $v \in V$:
- **Blast Radius $B(v)$**: Number of downstream components reachable from $v$ via directed paths in the reverse dependency graph:
  $$B(v) = |\text{Reachable}_{\text{downstream}}(v)|$$
- **Upstream Dependency Risk $U(v)$**: Number of prerequisite dependencies that $v$ requires:
  $$U(v) = |\text{Reachable}_{\text{upstream}}(v)|$$
- **Direct Dependents $D(v)$**: $|\text{Dependents}(v)|$

The composite **Criticality Score $S(v) \in [0, 100]$** is defined as:
$$S(v) = \min\left(100, \; 50 \cdot \frac{B(v)}{|V| - 1} + 30 \cdot \min\left(1, \frac{2 \cdot D(v)}{|V|-1}\right) + 20 \cdot \frac{U(v)}{|V| - 1}\right)$$

### Single Point of Failure (SPOF) Rule:
A component $v$ is classified as a Single Point of Failure if its blast radius affects at least 35% of the entire system:
$$\text{isSPOF}(v) \iff B(v) \ge \max\left(2, \; \lceil 0.35 \cdot |V| \rceil\right)$$

---

## 3. Cycle Detection (3-Color DFS) & Topological Ordering

- **Cycle Detection**: Nodes are colored:
  - `0 (WHITE)`: Unvisited.
  - `1 (GRAY)`: Currently in recursion stack.
  - `2 (BLACK)`: Fully explored.
  If an edge points to a `GRAY` node during DFS, a directed cycle exists.
- **Topological Sorting (Kahn's Algorithm)**:
  Maintains in-degree array $deg^-(v)$. Nodes with $deg^-(v) = 0$ are enqueued and removed iteratively, updating remaining node in-degrees until topological order is determined.

---

## 4. Rolling Statistical Z-Score Anomaly Detection

### Formulation (Welford's Algorithm)
For each sensor metric stream $X = \{x_1, x_2, \dots, x_N\}$, a sliding window of capacity $W = 30$ observations is maintained. To avoid catastrophic floating-point cancellation, sample mean and sample variance are calculated via **Welford's algorithm (1962)**:

$$\mu_k = \mu_{k-1} + \frac{x_k - \mu_{k-1}}{k}$$

$$M_{2,k} = M_{2,k-1} + (x_k - \mu_{k-1})(x_k - \mu_k)$$

Sample variance $s^2$ and sample standard deviation $\sigma$:
$$s^2 = \frac{M_{2,n}}{n - 1}, \quad \sigma = \sqrt{\max(0, s^2)}$$

The standard score $Z_t$ for a new reading $x_t$ is:
$$Z_t = \frac{|x_t - \mu|}{\sigma}$$

An anomaly event is triggered when $|Z_t| \ge 3.0$ (representing a 3-sigma confidence of 99.7%):
- **Medium Severity**: $3.0 \le Z_t < 3.5$
- **High Severity**: $3.5 \le Z_t < 4.5$
- **Critical Severity**: $Z_t \ge 4.5$

---

## 5. Topological Root Cause Localization

### Formulation
When multiple components generate anomaly alerts within time interval $\Delta T$:
1. Anomalies are ordered by timestamp $t_1 \le t_2 \le \dots \le t_m$.
2. For each unique anomalous component $c \in \mathcal{A}$, its downstream reachability coverage is computed:
   $$\text{Coverage}(c) = \frac{|\{ a \in \mathcal{A} \mid a = c \lor a \in \text{Reachable}_{\text{downstream}}(c) \}|}{|\mathcal{A}|}$$
3. The candidate score is maximized:
   $$\text{Score}(c) = 0.65 \cdot \text{Coverage}(c) + 0.35 \cdot \mathbf{1}_{\{c = c_{\text{earliest}}\}}$$
3b. A candidate also earns the 0.35 root-position bonus when **no other anomalous component sits upstream of it** (a topological source), not only when it raised the earliest anomaly. This avoids penalising a correct culprit whose readings happen to be processed later within the same simulation tick.
4. The candidate with $\max \text{Score}(c)$ is declared the primary root cause with propagation chain reconstructed from BFS traversal.
