import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class Main {

    /**
     * Class to represent an edge in the network flow graph
     */
    static class Edge {
        int from;      // Source node
        int to;        // Destination node
        int capacity;  // Edge capacity
        int flow;      // Current flow through edge
        Edge residual; // Residual edge reference

        public Edge(int from, int to, int capacity) {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
            this.flow = 0;
        }

        // Remaining capacity on this edge
        public int remainingCapacity() {
            return capacity - flow;
        }

        // Add flow to this edge
        public void augment(int bottleneck) {
            flow += bottleneck;
            residual.flow -= bottleneck;
        }

        @Override
        public String toString() {
            return String.format("Edge(%d → %d, flow: %d/%d)", from, to, flow, capacity);
        }
    }

    /**
     * FlowNetwork class to represent the entire network
     */
    static class FlowNetwork {
        private int n;                  // Number of nodes
        private List<Edge>[] graph;     // Adjacency list
        private int source;             // Source node
        private int sink;               // Sink/target node

        @SuppressWarnings("unchecked")
        public FlowNetwork(int n) {
            this.n = n;
            this.source = 0;            // Node 0 is always the source
            this.sink = n - 1;          // Last node is always the sink

            // Initialize adjacency list
            graph = new ArrayList[n];
            for (int i = 0; i < n; i++) {
                graph[i] = new ArrayList<>();
            }
        }

        // Add a directed edge to the graph
        public void addEdge(int from, int to, int capacity) {
            // Forward edge
            Edge forward = new Edge(from, to, capacity);
            // Backward edge (residual)
            Edge backward = new Edge(to, from, 0);

            // Connect the residual edges
            forward.residual = backward;
            backward.residual = forward;

            // Add edges to the adjacency list
            graph[from].add(forward);
            graph[to].add(backward);
        }

        // Get all edges from the graph
        public List<Edge> getAllEdges() {
            List<Edge> edges = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                for (Edge edge : graph[i]) {
                    if (edge.capacity > 0) { // Only include forward edges
                        edges.add(edge);
                    }
                }
            }
            return edges;
        }

        public int getN() {
            return n;
        }

        public int getSource() {
            return source;
        }

        public int getSink() {
            return sink;
        }

        public List<Edge>[] getGraph() {
            return graph;
        }
    }

    /**
     * Ford-Fulkerson algorithm implementation using BFS to find augmenting paths (Edmonds-Karp algorithm)
     */
    static class FordFulkerson {
        private FlowNetwork network;
        private List<String> steps; // For storing intermediate steps

        public FordFulkerson(FlowNetwork network) {
            this.network = network;
            this.steps = new ArrayList<>();
        }

        // Find the maximum flow
        public int findMaxFlow() {
            int maxFlow = 0;
            steps.clear();

            // While there exists an augmenting path
            while (true) {
                // Use BFS to find an augmenting path
                Edge[] parentEdges = new Edge[network.getN()];
                boolean[] visited = new boolean[network.getN()];
                Queue<Integer> queue = new LinkedList<>();

                visited[network.getSource()] = true;
                queue.offer(network.getSource());

                // BFS
                while (!queue.isEmpty() && !visited[network.getSink()]) {
                    int node = queue.poll();

                    // Check all neighbors
                    for (Edge edge : network.getGraph()[node]) {
                        int next = edge.to;

                        // If not visited and there's remaining capacity
                        if (!visited[next] && edge.remainingCapacity() > 0) {
                            parentEdges[next] = edge;
                            visited[next] = true;
                            queue.offer(next);
                        }
                    }
                }

                // If sink wasn't reached, no augmenting path exists
                if (!visited[network.getSink()]) {
                    break;
                }

                // Find bottleneck capacity (minimum remaining capacity in the path)
                int bottleneck = Integer.MAX_VALUE;
                for (Edge edge = parentEdges[network.getSink()]; edge != null; edge = parentEdges[edge.from]) {
                    bottleneck = Math.min(bottleneck, edge.remainingCapacity());
                }

                // Augment flow along the path
                StringBuilder pathStr = new StringBuilder();
                pathStr.append("Augmenting path: ");

                for (Edge edge = parentEdges[network.getSink()]; edge != null; edge = parentEdges[edge.from]) {
                    pathStr.append(edge.from).append(" → ").append(edge.to);
                    if (parentEdges[edge.from] != null) {
                        pathStr.append(", ");
                    }
                    edge.augment(bottleneck);
                }

                // Record this step
                steps.add(pathStr.toString() + " with flow " + bottleneck);

                // Increase maxFlow
                maxFlow += bottleneck;

                // Record current state of the graph
                steps.add("Current flow: " + maxFlow);
                StringBuilder currentFlows = new StringBuilder();
                for (Edge edge : network.getAllEdges()) {
                    if (edge.flow > 0) {
                        currentFlows.append("f(").append(edge.from).append(",").append(edge.to)
                                .append(") = ").append(edge.flow).append(", ");
                    }
                }
                if (currentFlows.length() > 2) {
                    currentFlows.delete(currentFlows.length() - 2, currentFlows.length());
                }
                steps.add(currentFlows.toString());
                steps.add("-----");
            }

            return maxFlow;
        }

        // Get the intermediate steps information
        public List<String> getSteps() {
            return steps;
        }

        // Get the final flow on each edge
        public List<String> getFinalFlow() {
            List<String> result = new ArrayList<>();
            for (Edge edge : network.getAllEdges()) {
                if (edge.flow > 0) {
                    result.add(String.format("f(%d,%d) = %d", edge.from, edge.to, edge.flow));
                }
            }
            return result;
        }
    }

    /**
     * Parse input file and create a flow network
     */
    public static FlowNetwork parseInputFile(String filename) throws FileNotFoundException {
        File file = new File(filename);
        Scanner scanner = new Scanner(file);

        // Read number of nodes
        int n = scanner.nextInt();
        FlowNetwork network = new FlowNetwork(n);

        // Read edges
        while (scanner.hasNextInt()) {
            int from = scanner.nextInt();
            int to = scanner.nextInt();
            int capacity = scanner.nextInt();

            network.addEdge(from, to, capacity);
        }

        scanner.close();
        return network;
    }

    public static void main(String[] args) {
        try {
            String filename = "ladder_5.txt";
            if (args.length > 0) {
                filename = args[0];
            }

            System.out.println("Reading network from file: " + filename);
            FlowNetwork network = parseInputFile(filename);

            System.out.println("Network has " + network.getN() + " nodes.");
            System.out.println("Source: " + network.getSource() + ", Sink: " + network.getSink());

            FordFulkerson solver = new FordFulkerson(network);
            int maxFlow = solver.findMaxFlow();

            System.out.println("\n=== Intermediate Steps ===");
            for (String step : solver.getSteps()) {
                System.out.println(step);
            }

            System.out.println("\n=== Final Result ===");
            System.out.println("Maximum flow: " + maxFlow);
            System.out.println("Flow details:");
            for (String flow : solver.getFinalFlow()) {
                System.out.println(flow);
            }

        } catch (FileNotFoundException e) {
            System.err.println("Error: Input file not found.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}