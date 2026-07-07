package genetic;

import java.util.List;

import main.App;
import vrp.Client;

public class TimeFitnessCalculator implements FitnessCalculator {

    @Override
    public double calculateFitness(Individual individual, List<Client> clients) {
        double totalTime = 0;
        int numViolations = 0;
        int missingClients = 0;
        boolean[] visited = new boolean[App.numClients];
        visited[0] = true;
        Client depot = clients.get(0); // Depósito sempre no índice 0

        for (int v = 0; v < App.numVehicles; v++) {
            double currentTime = 0;
            double vehicleDistance = 0;
            int numViolationsVehicle = 0;
            int vehicleLoad = 0;
            Client firstClient = null;
            Client lastClient = null;

            // Marca clientes visitados e calcula demanda da rota
            for (int c = 0; c < App.numClients; c++) {
                int cid = individual.getRoute()[v][c];
                if (cid == -1) break;
                if (cid > 0 && cid < App.numClients) {
                    visited[cid] = true;
                    vehicleLoad += clients.get(cid).getDemand();
                }
            }

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

            // Capacity violation (conta por veiculo sobrecarregado)
            if (vehicleLoad > App.vehicleCapacity) {
                numViolations++;
            }
        }

        // Conta clientes ausentes
        for (int i = 1; i < App.numClients; i++) {
            if (!visited[i])
                missingClients++;
        }

        // Penalidades equalizadas com NSGA-III
        double fitnessTime = (totalTime * 0.5)
                + (numViolations * App.WEIGHT_NUM_VIOLATIONS)
                + (missingClients * App.WEIGHT_MISSING_CLIENT);

        return fitnessTime;

    }

    // Function to calculate the distance between two clients
    private double calculateDistance(Client c1, Client c2) {
        return Math.sqrt(Math.pow(c1.getX() - c2.getX(), 2) + Math.pow(c1.getY() - c2.getY(), 2));
    }

}
