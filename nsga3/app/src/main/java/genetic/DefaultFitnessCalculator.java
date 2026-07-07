package genetic;

import java.util.List;

import main.App;
import vrp.Client;

public class DefaultFitnessCalculator implements FitnessCalculator {

    @Override
    public double calculateFitness(Individual individual, List<Client> clients) {
        double totalDistance = 0;
        double totalTime = 0;
        double totalFuel = 0;
        int numViolations = 0;
        int usedVehicles = 0;
        Client depot = clients.get(0); // Depósito sempre no índice 0

        for (int v = 0; v < App.numVehicles; v++) {
            double currentTime = 0;
            double vehicleDistance = 0;
            int numViolationsVehicle = 0;
            Client firstClient = null;
            Client lastClient = null;

            // Marca se o veiculo tem clientes
            boolean hasClients = false;
            for (int c = 0; c < App.numClients; c++) {
                int cid = individual.getRoute()[v][c];
                if (cid == -1) break;
                if (cid > 0) hasClients = true;
            }
            if (hasClients) usedVehicles++;

            // Distância do depósito ao primeiro cliente
            int firstClientId = individual.getRoute()[v][0];
            if (firstClientId != -1) {
                firstClient = clients.get(firstClientId);
                double depotToFirstDistance = calculateDistance(depot, firstClient);
                vehicleDistance += depotToFirstDistance;
                currentTime += depotToFirstDistance / App.VEHICLE_SPEED; // Travel time = distance (Solomon)

                // Janela de tempo do primeiro cliente:
                // - chega ANTES de readyTime -> ESPERA (nao eh violacao)
                // - chega DEPOIS de dueTime -> VIOLACAO
                if (currentTime < firstClient.getReadyTime()) {
                    currentTime = firstClient.getReadyTime();
                } else if (currentTime > firstClient.getDueTime()) {
                    numViolations++;
                    numViolationsVehicle++;
                }
                currentTime += firstClient.getServiceTime();
            }

            for (int c = 0; c < App.numClients - 1; c++) {
                int currentClientId = individual.getRoute()[v][c];
                int nextClientId = individual.getRoute()[v][c + 1];

                if (currentClientId == -1 || nextClientId == -1)
                    break;

                // Get the current client and next client
                Client currentClient = clients.get(currentClientId);
                Client nextClient = clients.get(nextClientId);

                lastClient = nextClient; // Rastreia último cliente

                // Calculating the distance between the current client and the next client
                double distance = calculateDistance(currentClient, nextClient);
                vehicleDistance += distance;

                // Calculating the time
                currentTime += distance / App.VEHICLE_SPEED; // Travel time = distance (Solomon)

                // Apos viajar, veiculo esta no nextClient. Aplica logica de janela de tempo:
                // - chega ANTES de readyTime -> ESPERA (nao eh violacao)
                // - chega DEPOIS de dueTime -> VIOLACAO
                if (currentTime < nextClient.getReadyTime()) {
                    currentTime = nextClient.getReadyTime();
                } else if (currentTime > nextClient.getDueTime()) {
                    numViolations++;
                    numViolationsVehicle++;
                }

                currentTime += nextClient.getServiceTime(); // service time do cliente atual (nextClient)
            }

            // Distância do último cliente de volta ao depósito
            if (lastClient != null) {
                double lastToDepotDistance = calculateDistance(lastClient, depot);
                vehicleDistance += lastToDepotDistance;
                currentTime += lastToDepotDistance / App.VEHICLE_SPEED; // Travel time = distance (Solomon)
            }

            // Calculating the fuel cost
            double fuelCost = fuelCost(vehicleDistance);

            // Adding the total distance, time and fuel
            totalDistance += vehicleDistance;
            totalTime += currentTime;
            totalFuel += fuelCost;
        }

        // Calculating the total cost of the Individual
        double totalCost = (totalDistance * 1.0) + (totalTime * 0.5) + (totalFuel * 0.75);

        // Calculating the final fitness
        // - Penaliza VEICULOS UTILIZADOS (nao mais a frota total)
        // - Penalidade hard (100k por violacao, 200k por cliente ausente) eh aplicada
        //   no SolomonVRPProblem.evaluate(), por isso aqui usa apenas WEIGHT_NUM_VIOLATIONS (0.5)
        double fitness = (usedVehicles * App.WEIGHT_NUM_VEHICLES) + (numViolations * App.WEIGHT_NUM_VIOLATIONS)
                + totalCost;

        return fitness;
    }

    // Function to calculate the distance between two clients
    private double calculateDistance(Client c1, Client c2) {
        return Math.sqrt(Math.pow(c1.getX() - c2.getX(), 2) + Math.pow(c1.getY() - c2.getY(), 2));
    }

    // Function to calculate the fuel cost
    private double fuelCost(double distance) {
        double gasolineCost = Math.round(distance / App.G_FUEL_CONSUMPTION) * App.G_FUEL_PRICE;
        double ethanolCost = Math.round(distance / App.E_FUEL_CONSUMPTION) * App.E_FUEL_PRICE;
        double dieselCost = Math.round(distance / App.D_FUEL_CONSUMPTION) * App.D_FUEL_PRICE;

        return Math.min(gasolineCost, Math.min(ethanolCost, dieselCost));
    }

}
