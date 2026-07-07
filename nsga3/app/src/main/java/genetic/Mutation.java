package genetic;

import java.util.Random;
import java.util.List;
import main.App;
import vrp.Client;

public class Mutation {

    public static void mutate(Individual individual, double mutationRate) {
        Random random = new Random();

        // Decide if the individual will be mutated
        int mutateIndividual = random.nextInt((int) (1.0 / mutationRate));

        // int mutateIndividual = 0;

        if (mutateIndividual != 0) {
            return;
        }

        // System.out.println("Mutating individual: " + individual.getId());

        int[][] route = individual.getRoute();

        for (int v = 0; v < App.numVehicles; v++) {

            java.util.List<Integer> validClients = new java.util.ArrayList<>();
            for (int c = 0; c < App.numClients; c++) {
                if (route[v][c] != 0 && route[v][c] != -1) {
                    validClients.add(c);
                }
            }

            // Only mutate if there are at least 4 clients in the route
            if (validClients.size() < 4)
                continue;

            int point1, point2;

            // System.err.println("Real clients: " + validClients.size());

            do {
                point1 = validClients.get(random.nextInt(validClients.size()));
                point2 = validClients.get(random.nextInt(validClients.size()));
            } while (point1 == point2);

            // Swap the two points in the route
            // System.out.println("Mutating vehicle " + v + ": swapping " + route[v][point1]
            // + " and " + route[v][point2]);

            int temp = route[v][point1];
            route[v][point1] = route[v][point2];
            route[v][point2] = temp;
        }

        individual.setRoute(route);
    }

    /**
     * Inter-route mutation: moves clients between different vehicles
     * This helps escape local optima when initial clustering creates infeasible
     * solutions
     * 
     * @param individual   Individual to mutate
     * @param mutationRate Probability of mutation occurring
     * @param clients      List of clients to check capacity constraints
     */
    public static void mutateInterRoute(Individual individual, double mutationRate, List<Client> clients) {
        Random random = new Random();

        // Decide if mutation occurs
        if (random.nextDouble() > mutationRate) {
            return;
        }

        int[][] route = individual.getRoute();

        // Find two different vehicles with clients
        int vehicle1 = -1, vehicle2 = -1;
        int attempts = 0;
        int maxAttempts = 50;

        while (attempts < maxAttempts) {
            vehicle1 = random.nextInt(App.numVehicles);
            vehicle2 = random.nextInt(App.numVehicles);

            if (vehicle1 == vehicle2) {
                attempts++;
                continue;
            }

            // Count clients in each vehicle
            int count1 = 0, count2 = 0;
            for (int c = 0; c < App.numClients - 1; c++) {
                if (route[vehicle1][c] != -1 && route[vehicle1][c] != 0)
                    count1++;
                if (route[vehicle2][c] != -1 && route[vehicle2][c] != 0)
                    count2++;
            }

            // Both vehicles must have at least 1 client
            if (count1 > 0 && count2 > 0) {
                break;
            }
            attempts++;
        }

        if (attempts >= maxAttempts) {
            return; // Couldn't find suitable vehicles
        }

        // Select random client from each vehicle
        int client1Pos = -1, client2Pos = -1;

        for (int c = 0; c < App.numClients - 1; c++) {
            if (route[vehicle1][c] != -1 && route[vehicle1][c] != 0 && random.nextDouble() < 0.3) {
                client1Pos = c;
                break;
            }
        }

        for (int c = 0; c < App.numClients - 1; c++) {
            if (route[vehicle2][c] != -1 && route[vehicle2][c] != 0 && random.nextDouble() < 0.3) {
                client2Pos = c;
                break;
            }
        }

        if (client1Pos == -1 || client2Pos == -1) {
            return; // Couldn't select clients
        }

        // **VERIFICAÇÃO DE CAPACIDADE ANTES DA TROCA**
        // Calculate current demands for both vehicles
        double demand1 = 0, demand2 = 0;
        for (int c = 0; c < App.numClients - 1; c++) {
            if (route[vehicle1][c] != -1 && route[vehicle1][c] != 0) {
                demand1 += clients.get(route[vehicle1][c]).getDemand();
            }
            if (route[vehicle2][c] != -1 && route[vehicle2][c] != 0) {
                demand2 += clients.get(route[vehicle2][c]).getDemand();
            }
        }

        // Get demands of clients to swap
        int clientId1 = route[vehicle1][client1Pos];
        int clientId2 = route[vehicle2][client2Pos];
        double clientDemand1 = clients.get(clientId1).getDemand();
        double clientDemand2 = clients.get(clientId2).getDemand();

        // Calculate new demands after swap
        double newDemand1 = demand1 - clientDemand1 + clientDemand2;
        double newDemand2 = demand2 - clientDemand2 + clientDemand1;

        // Only swap if both vehicles remain within capacity
        if (newDemand1 > App.vehicleCapacity || newDemand2 > App.vehicleCapacity) {
            return; // Skip mutation to preserve capacity constraint
        }

        // Swap clients between vehicles (capacity is guaranteed)
        int temp = route[vehicle1][client1Pos];
        route[vehicle1][client1Pos] = route[vehicle2][client2Pos];
        route[vehicle2][client2Pos] = temp;

        individual.setRoute(route);
    }

    /**
     * Combined mutation: applies both intra-route and inter-route mutations
     * 
     * @param individual     Individual to mutate
     * @param mutationRate   Base mutation rate
     * @param interRouteRate Probability of inter-route mutation (recommended: 0.3)
     * @param clients        List of clients to check capacity constraints
     */
    public static void mutateCombined(Individual individual, double mutationRate, double interRouteRate,
            List<Client> clients) {
        Random random = new Random();

        // Apply standard intra-route mutation
        mutate(individual, mutationRate);

        // Apply inter-route mutation with specified probability
        if (random.nextDouble() < interRouteRate) {
            mutateInterRoute(individual, 1.0, clients); // Always mutate if selected, with capacity check
        }
    }
}