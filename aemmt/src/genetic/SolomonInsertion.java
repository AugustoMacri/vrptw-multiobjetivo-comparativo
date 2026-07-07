package genetic;

import java.util.*;
import main.App;
import vrp.Client;

/**
 * Solomon I1 Insertion Heuristic for VRPTW
 * Creates feasible initial solutions that respect time windows
 */
public class SolomonInsertion {

    private static final double ALPHA = 1.0; // Weight for distance
    private static final double BETA = 1.0; // Weight for time
    private static final double GAMMA = 1.0; // Weight for time window urgency

    /**
     * Creates a single individual using Solomon I1 insertion heuristic
     * 
     * @param clients         List of all clients (including depot at index 0)
     * @param vehicleCapacity Maximum vehicle capacity
     * @param numVehicles     Maximum number of vehicles available
     * @return Individual with feasible routes
     */
    public static Individual createIndividual(List<Client> clients, int vehicleCapacity, int numVehicles) {
        Individual individual = new Individual(App.nextIndividualId++, 0, 0, 0, 0);
        int[][] routes = new int[numVehicles][App.numClients];

        // Initialize all positions with -1
        for (int v = 0; v < numVehicles; v++) {
            Arrays.fill(routes[v], -1);
        }

        Set<Integer> unrouted = new HashSet<>();
        for (int i = 1; i < clients.size(); i++) {
            unrouted.add(i);
        }

        Client depot = clients.get(0);
        int currentVehicle = 0;
        int[] routeSize = new int[numVehicles];
        double[] routeLoad = new double[numVehicles];
        double[] routeTime = new double[numVehicles];

        // Build routes until all clients are routed or vehicles exhausted
        while (!unrouted.isEmpty() && currentVehicle < numVehicles) {

            // Find seed customer (farthest from depot with earliest due time)
            int seedClient = findSeedCustomer(unrouted, clients, depot);

            if (seedClient == -1)
                break;

            // Start new route with seed
            routes[currentVehicle][0] = seedClient;
            routeSize[currentVehicle] = 1;
            routeLoad[currentVehicle] = clients.get(seedClient).getDemand();

            // Calculate time to reach seed
            double distToSeed = distance(depot, clients.get(seedClient));
            double arrivalTime = distToSeed / App.VEHICLE_SPEED; // Travel time = distance (Solomon)
            routeTime[currentVehicle] = Math.max(arrivalTime, clients.get(seedClient).getReadyTime())
                    + clients.get(seedClient).getServiceTime();

            unrouted.remove(seedClient);

            // Keep inserting customers into this route
            boolean inserted = true;
            while (inserted && !unrouted.isEmpty()) {
                inserted = false;

                BestInsertion best = findBestInsertion(
                        routes[currentVehicle],
                        routeSize[currentVehicle],
                        routeLoad[currentVehicle],
                        routeTime[currentVehicle],
                        unrouted,
                        clients,
                        depot,
                        vehicleCapacity);

                if (best != null) {
                    // Insert customer at best position
                    insertCustomer(routes[currentVehicle], routeSize[currentVehicle], best.position, best.customer);
                    routeSize[currentVehicle]++;
                    routeLoad[currentVehicle] += clients.get(best.customer).getDemand();
                    routeTime[currentVehicle] = best.newTime;
                    unrouted.remove(best.customer);
                    inserted = true;
                }
            }

            currentVehicle++;
        }

        // Handle remaining unrouted customers with intelligent insertion
        if (!unrouted.isEmpty()) {
            System.out.println("⚠️  Solomon I1: " + unrouted.size() + " clientes não roteados após construção inicial");

            // Try to insert remaining customers into existing routes with violations
            // allowed
            List<Integer> remaining = new ArrayList<>(unrouted);

            for (int client : remaining) {
                double bestCost = Double.POSITIVE_INFINITY;
                int bestVehicle = -1;
                int bestPosition = -1;
                double bestTime = 0;

                // Try inserting into any existing route (even with violations)
                for (int v = 0; v < currentVehicle; v++) {
                    Client clientObj = clients.get(client);

                    // Try each position in this route (ignore capacity for now in fallback)
                    for (int pos = 0; pos <= routeSize[v]; pos++) {
                        InsertionCost cost = calculateInsertionCost(
                                routes[v], routeSize[v], pos, client, clients, depot, false);

                        if (cost != null && cost.totalCost < bestCost) {
                            // Check if capacity is still OK
                            if (routeLoad[v] + clientObj.getDemand() <= vehicleCapacity) {
                                bestCost = cost.totalCost;
                                bestVehicle = v;
                                bestPosition = pos;
                                bestTime = cost.newTime;
                            }
                        }
                    }
                }

                // Insert into best found position
                if (bestVehicle != -1) {
                    insertCustomer(routes[bestVehicle], routeSize[bestVehicle], bestPosition, client);
                    routeSize[bestVehicle]++;
                    routeLoad[bestVehicle] += clients.get(client).getDemand();
                    routeTime[bestVehicle] = bestTime;
                    unrouted.remove(Integer.valueOf(client));
                    System.out.println("   ✓ Cliente " + client + " inserido no veículo " + bestVehicle);
                } else if (currentVehicle < numVehicles) {
                    // Create new route for this client
                    routes[currentVehicle][0] = client;
                    routeSize[currentVehicle] = 1;
                    routeLoad[currentVehicle] = clients.get(client).getDemand();
                    unrouted.remove(Integer.valueOf(client));
                    System.out.println("   ✓ Cliente " + client + " em novo veículo " + currentVehicle);
                    currentVehicle++;
                } else {
                    // Last resort: try to find ANY vehicle where capacity allows
                    // Even if it causes time window violations
                    boolean inserted = false;
                    for (int v = 0; v < currentVehicle; v++) {
                        Client clientObj = clients.get(client);
                        if (routeLoad[v] + clientObj.getDemand() <= vehicleCapacity) {
                            // Can fit capacity-wise, insert at end
                            routes[v][routeSize[v]] = client;
                            routeSize[v]++;
                            routeLoad[v] += clientObj.getDemand();
                            unrouted.remove(Integer.valueOf(client));
                            System.out.println("   ⚠️  Cliente " + client + " inserido no fim do veículo " + v +
                                    " (pode violar janelas de tempo)");
                            inserted = true;
                            break;
                        }
                    }

                    if (!inserted) {
                        // Absolutely cannot insert - would need capacity violation
                        System.err.println("   ❌ ERRO CRÍTICO: Cliente " + client +
                                " não pode ser inserido sem violar capacidade!");
                        // Try smallest demand vehicle and insert anyway
                        int smallestLoadVehicle = 0;
                        double smallestLoad = routeLoad[0];
                        for (int v = 1; v < currentVehicle; v++) {
                            if (routeLoad[v] < smallestLoad) {
                                smallestLoad = routeLoad[v];
                                smallestLoadVehicle = v;
                            }
                        }
                        routes[smallestLoadVehicle][routeSize[smallestLoadVehicle]] = client;
                        routeSize[smallestLoadVehicle]++;
                        routeLoad[smallestLoadVehicle] += clients.get(client).getDemand();
                        unrouted.remove(Integer.valueOf(client));
                        System.err.println("   ⚠️  Cliente " + client + " FORÇADO no veículo " +
                                smallestLoadVehicle + " (VIOLARÁ CAPACIDADE!)");
                    }
                }
            }

            if (unrouted.isEmpty()) {
                System.out.println("✅ Todos os clientes foram roteados!");
            } else {
                System.out.println("❌ ERRO: " + unrouted.size() + " clientes ainda não roteados: " + unrouted);
            }
        }

        individual.setRoute(routes);
        return individual;
    }

    /**
     * Find seed customer: farthest from depot with earliest due time
     */
    private static int findSeedCustomer(Set<Integer> unrouted, List<Client> clients, Client depot) {
        int bestClient = -1;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int clientId : unrouted) {
            Client client = clients.get(clientId);
            double dist = distance(depot, client);
            double urgency = 1.0 / (client.getDueTime() + 1); // Earlier due time = higher urgency
            double score = dist * urgency;

            if (score > bestScore) {
                bestScore = score;
                bestClient = clientId;
            }
        }

        return bestClient;
    }

    /**
     * Find best insertion position for any unrouted customer
     * If no feasible insertion found, accepts violations with penalties
     */
    private static BestInsertion findBestInsertion(
            int[] route,
            int routeSize,
            double currentLoad,
            double currentTime,
            Set<Integer> unrouted,
            List<Client> clients,
            Client depot,
            int vehicleCapacity) {

        BestInsertion best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        BestInsertion bestWithViolation = null;
        double bestViolationCost = Double.POSITIVE_INFINITY;

        for (int customer : unrouted) {
            Client client = clients.get(customer);

            // Check capacity
            if (currentLoad + client.getDemand() > vehicleCapacity) {
                continue;
            }

            // Try inserting at each position in the route
            for (int pos = 0; pos <= routeSize; pos++) {
                InsertionCost cost = calculateInsertionCost(
                        route, routeSize, pos, customer, clients, depot, false);

                if (cost != null) {
                    if (cost.hasViolation) {
                        // Track best insertion with violation as backup
                        if (cost.totalCost < bestViolationCost) {
                            bestViolationCost = cost.totalCost;
                            bestWithViolation = new BestInsertion(customer, pos, cost.newTime);
                        }
                    } else {
                        // Prefer insertions without violations
                        if (cost.totalCost < bestCost) {
                            bestCost = cost.totalCost;
                            best = new BestInsertion(customer, pos, cost.newTime);
                        }
                    }
                }
            }
        }

        // ONLY return feasible insertion (no violations)
        // If no feasible insertion found, return null to force new vehicle
        return best;
    }

    /**
     * Calculate cost of inserting customer at specific position
     */
    private static InsertionCost calculateInsertionCost(
            int[] route,
            int routeSize,
            int position,
            int customer,
            List<Client> clients,
            Client depot,
            boolean rejectViolations) {

        Client newClient = clients.get(customer);
        double currentTime = 0;
        double distanceIncrease = 0;
        double timeViolation = 0;

        // Calculate time up to insertion point
        Client prevClient = depot;
        for (int i = 0; i < position; i++) {
            Client curr = clients.get(route[i]);
            double dist = distance(prevClient, curr);
            currentTime += dist / App.VEHICLE_SPEED; // Travel time = distance (Solomon)

            if (currentTime < curr.getReadyTime()) {
                currentTime = curr.getReadyTime();
            }
            if (currentTime > curr.getDueTime()) {
                timeViolation += (currentTime - curr.getDueTime());
            }

            currentTime += curr.getServiceTime();
            prevClient = curr;
        }

        // Calculate insertion cost
        Client nextClient = (position < routeSize) ? clients.get(route[position]) : depot;

        double distPrevToNew = distance(prevClient, newClient);
        double distNewToNext = distance(newClient, nextClient);
        double distPrevToNext = distance(prevClient, nextClient);

        distanceIncrease = distPrevToNew + distNewToNext - distPrevToNext;

        // Time at new customer
        double arrivalAtNew = currentTime + distPrevToNew / App.VEHICLE_SPEED; // Travel time = distance (Solomon)
        double startServiceAtNew = Math.max(arrivalAtNew, newClient.getReadyTime());

        // Check if new customer violates time window
        if (arrivalAtNew > newClient.getDueTime()) {
            timeViolation += (arrivalAtNew - newClient.getDueTime()) * 10000; // Heavy penalty
        }

        double timeAfterNew = startServiceAtNew + newClient.getServiceTime();

        // Check impact on remaining customers
        double timeShift = timeAfterNew - currentTime;
        for (int i = position; i < routeSize; i++) {
            Client curr = clients.get(route[i]);
            double dist = (i == position) ? distNewToNext : distance(clients.get(route[i - 1]), curr);
            timeAfterNew += dist / App.VEHICLE_SPEED; // Travel time = distance (Solomon)

            if (timeAfterNew < curr.getReadyTime()) {
                timeAfterNew = curr.getReadyTime();
            }
            if (timeAfterNew > curr.getDueTime()) {
                timeViolation += (timeAfterNew - curr.getDueTime()) * 10000; // Heavy penalty
            }

            timeAfterNew += curr.getServiceTime();
        }

        // Return to depot
        Client lastClient = (routeSize > 0) ? clients.get(route[routeSize - 1]) : newClient;
        double returnTime = timeAfterNew + distance(lastClient, depot) / App.VEHICLE_SPEED; // Travel time = distance
                                                                                            // (Solomon)

        if (returnTime > depot.getDueTime()) {
            timeViolation += (returnTime - depot.getDueTime()) * 100; // Penalty for late return
        }

        // Total cost considers distance increase and time violations
        double totalCost = ALPHA * distanceIncrease + BETA * timeViolation;
        boolean hasViolation = (timeViolation > 0);

        // Optionally reject insertions with violations (only in strict mode)
        if (rejectViolations && hasViolation) {
            return null;
        }

        return new InsertionCost(totalCost, timeAfterNew, hasViolation);
    }

    /**
     * Insert customer at specified position in route
     */
    private static void insertCustomer(int[] route, int routeSize, int position, int customer) {
        // Shift customers to the right
        for (int i = routeSize; i > position; i--) {
            route[i] = route[i - 1];
        }
        route[position] = customer;
    }

    /**
     * Calculate Euclidean distance between two clients
     */
    private static double distance(Client c1, Client c2) {
        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Helper classes
    static class BestInsertion {
        int customer;
        int position;
        double newTime;

        BestInsertion(int customer, int position, double newTime) {
            this.customer = customer;
            this.position = position;
            this.newTime = newTime;
        }
    }

    static class InsertionCost {
        double totalCost;
        double newTime;
        boolean hasViolation;

        InsertionCost(double totalCost, double newTime, boolean hasViolation) {
            this.totalCost = totalCost;
            this.newTime = newTime;
            this.hasViolation = hasViolation;
        }
    }
}
