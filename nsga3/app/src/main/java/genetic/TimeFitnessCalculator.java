package genetic;

import java.util.List;

import main.App;
import vrp.Client;

public class TimeFitnessCalculator implements FitnessCalculator {

    @Override
    public double calculateFitness(Individual individual, List<Client> clients) {
        double totalTime = 0;
        int numViolations = 0;
        Client depot = clients.get(0); // Depósito sempre no índice 0

        for (int v = 0; v < App.numVehicles; v++) {
            double currentTime = 0;
            double vehicleDistance = 0;
            int numViolationsVehicle = 0;
            Client firstClient = null;
            Client lastClient = null;

            // Distância do depósito ao primeiro cliente
            int firstClientId = individual.getRoute()[v][0];
            if (firstClientId != -1) {
                firstClient = clients.get(firstClientId);
                double depotToFirstDistance = calculateDistance(depot, firstClient);
                vehicleDistance += depotToFirstDistance;
                currentTime += depotToFirstDistance / App.VEHICLE_SPEED; // Travel time = distance (Solomon)

                // ✅ CORREÇÃO: Verificar janela de tempo do primeiro cliente
                // Se chegar ANTES de readyTime, veículo ESPERA (não é violação)
                // Só é violação se chegar DEPOIS de dueTime
                if (currentTime < firstClient.getReadyTime()) {
                    // Esperar até readyTime - NÃO é violação
                    currentTime = firstClient.getReadyTime();
                } else if (currentTime > firstClient.getDueTime()) {
                    // Chegou atrasado - É VIOLAÇÃO
                    numViolations++;
                    numViolationsVehicle++;
                }

                currentTime += firstClient.getServiceTime(); // Add service time
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

                // ✅ CORREÇÃO CRÍTICA: Verificar janela de tempo do NEXT CLIENT
                // Após viajar (currentTime += distance), o veículo está no nextClient!
                // Se chegar ANTES de readyTime, veículo ESPERA (não é violação)
                // Só é violação se chegar DEPOIS de dueTime
                if (currentTime < nextClient.getReadyTime()) {
                    // Esperar até readyTime - NÃO é violação
                    currentTime = nextClient.getReadyTime();
                } else if (currentTime > nextClient.getDueTime()) {
                    // Chegou atrasado - É VIOLAÇÃO
                    numViolations++;
                    numViolationsVehicle++;
                }

                currentTime += nextClient.getServiceTime(); // Add service time do cliente ATUAL (nextClient)
            }

            // Distância do último cliente de volta ao depósito
            if (lastClient != null) {
                double lastToDepotDistance = calculateDistance(lastClient, depot);
                vehicleDistance += lastToDepotDistance;
                currentTime += lastToDepotDistance / App.VEHICLE_SPEED; // Travel time = distance (Solomon)
            }

            // Adding the time
            totalTime += currentTime;

            // Debugging
            // System.out.printf("Vehicle %d | Time: %.2f | TotalTime (vai no fitness): %.2f
            // | ViolationsVehicle: %d | Violations: %d | SPEED: %d | Distance: %.2f%n",v,
            // currentTime, totalTime, numViolationsVehicle, numViolations,
            // App.VEHICLE_SPEED, vehicleDistance);

        }

        // Calculating the total cost of the Individual
        double fitnessTime = (numViolations * App.WEIGHT_NUM_VIOLATIONS) + (totalTime * 0.5); // Tava dando 50 pq total
                                                                                              // time era 0, e já que
                                                                                              // dava 100 violações com
                                                                                              // peso de 0,50, então
                                                                                              // dava 50 po

        return fitnessTime;

    }

    // Function to calculate the distance between two clients
    private double calculateDistance(Client c1, Client c2) {
        return Math.sqrt(Math.pow(c1.getX() - c2.getX(), 2) + Math.pow(c1.getY() - c2.getY(), 2));
    }

}
