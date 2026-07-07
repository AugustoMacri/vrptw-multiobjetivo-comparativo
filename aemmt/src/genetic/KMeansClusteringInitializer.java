package genetic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import main.App;
import vrp.Client;

/**
 * Classe para inicialização de população usando K-means clustering geográfico.
 * Agrupa clientes geograficamente próximos antes de construir rotas,
 * melhorando a qualidade da solução inicial.
 */
public class KMeansClusteringInitializer {

    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.01;
    private Random random;

    public KMeansClusteringInitializer() {
        this.random = new Random();
    }

    /**
     * Inicializa um indivíduo usando K-means clustering
     */
    public Individual initializeWithClustering(int individualId, List<Client> clients, int numClusters) {
        Individual individual = new Individual(individualId, 0, 0, 0, 0);

        // Passo 1: Aplicar K-means clustering
        List<List<Client>> clusters = performKMeans(clients, numClusters);

        // Passo 2: Construir rotas a partir dos clusters
        buildRoutesFromClusters(individual, clusters, clients);

        return individual;
    }

    /**
     * Executa o algoritmo K-means para agrupar clientes geograficamente
     */
    private List<List<Client>> performKMeans(List<Client> clients, int k) {
        // Remover o depósito da lista de clientes para clustering
        List<Client> clientsWithoutDepot = new ArrayList<>();
        for (Client c : clients) {
            if (c.getId() != 0) {
                clientsWithoutDepot.add(c);
            }
        }

        if (clientsWithoutDepot.isEmpty()) {
            return new ArrayList<>();
        }

        // Ajustar k se for maior que o número de clientes
        k = Math.min(k, clientsWithoutDepot.size());

        // Inicializar centroides aleatoriamente
        List<Centroid> centroids = initializeCentroids(clientsWithoutDepot, k);

        // Iterar até convergência
        boolean converged = false;
        int iteration = 0;

        while (!converged && iteration < MAX_ITERATIONS) {
            // Atribuir clientes aos clusters mais próximos
            List<List<Client>> clusters = assignClientsToClusters(clientsWithoutDepot, centroids);

            // Atualizar centroides
            List<Centroid> newCentroids = updateCentroids(clusters);

            // Verificar convergência
            converged = checkConvergence(centroids, newCentroids);
            centroids = newCentroids;
            iteration++;
        }

        // Retornar clusters finais
        return assignClientsToClusters(clientsWithoutDepot, centroids);
    }

    /**
     * Inicializa centroides usando K-means++
     */
    private List<Centroid> initializeCentroids(List<Client> clients, int k) {
        List<Centroid> centroids = new ArrayList<>();

        // Primeiro centroide aleatório
        Client firstClient = clients.get(random.nextInt(clients.size()));
        centroids.add(new Centroid(firstClient.getX(), firstClient.getY()));

        // Selecionar centroides subsequentes com probabilidade proporcional à distância
        for (int i = 1; i < k; i++) {
            double[] distances = new double[clients.size()];
            double totalDistance = 0;

            // Calcular distância mínima de cada cliente aos centroides existentes
            for (int j = 0; j < clients.size(); j++) {
                Client client = clients.get(j);
                double minDist = Double.MAX_VALUE;

                for (Centroid centroid : centroids) {
                    double dist = calculateDistance(client.getX(), client.getY(),
                            centroid.x, centroid.y);
                    minDist = Math.min(minDist, dist);
                }

                distances[j] = minDist * minDist; // Usar distância ao quadrado
                totalDistance += distances[j];
            }

            // Selecionar próximo centroide com probabilidade proporcional à distância
            double randomValue = random.nextDouble() * totalDistance;
            double cumulative = 0;

            for (int j = 0; j < clients.size(); j++) {
                cumulative += distances[j];
                if (cumulative >= randomValue) {
                    Client selected = clients.get(j);
                    centroids.add(new Centroid(selected.getX(), selected.getY()));
                    break;
                }
            }
        }

        return centroids;
    }

    /**
     * Atribui cada cliente ao cluster do centroide mais próximo
     */
    private List<List<Client>> assignClientsToClusters(List<Client> clients, List<Centroid> centroids) {
        List<List<Client>> clusters = new ArrayList<>();
        for (int i = 0; i < centroids.size(); i++) {
            clusters.add(new ArrayList<>());
        }

        for (Client client : clients) {
            int nearestCentroidIndex = findNearestCentroid(client, centroids);
            clusters.get(nearestCentroidIndex).add(client);
        }

        return clusters;
    }

    /**
     * Encontra o índice do centroide mais próximo do cliente
     */
    private int findNearestCentroid(Client client, List<Centroid> centroids) {
        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < centroids.size(); i++) {
            double dist = calculateDistance(client.getX(), client.getY(),
                    centroids.get(i).x, centroids.get(i).y);
            if (dist < minDistance) {
                minDistance = dist;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }

    /**
     * Atualiza os centroides calculando a média das posições dos clientes em cada
     * cluster
     */
    private List<Centroid> updateCentroids(List<List<Client>> clusters) {
        List<Centroid> newCentroids = new ArrayList<>();

        for (List<Client> cluster : clusters) {
            if (cluster.isEmpty()) {
                // Se o cluster está vazio, manter centroide aleatório
                newCentroids.add(new Centroid(random.nextDouble() * 100, random.nextDouble() * 100));
            } else {
                double sumX = 0, sumY = 0;
                for (Client client : cluster) {
                    sumX += client.getX();
                    sumY += client.getY();
                }
                newCentroids.add(new Centroid(sumX / cluster.size(), sumY / cluster.size()));
            }
        }

        return newCentroids;
    }

    /**
     * Verifica se os centroides convergiram
     */
    private boolean checkConvergence(List<Centroid> oldCentroids, List<Centroid> newCentroids) {
        for (int i = 0; i < oldCentroids.size(); i++) {
            double dist = calculateDistance(oldCentroids.get(i).x, oldCentroids.get(i).y,
                    newCentroids.get(i).x, newCentroids.get(i).y);
            if (dist > CONVERGENCE_THRESHOLD) {
                return false;
            }
        }
        return true;
    }

    /**
     * Constrói rotas a partir dos clusters, respeitando capacidade dos veículos
     */
    private void buildRoutesFromClusters(Individual individual, List<List<Client>> clusters, List<Client> allClients) {
        // Limpar rotas
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                individual.setClientInRoute(v, c, -1);
            }
        }

        boolean[] visited = new boolean[App.numClients];
        Arrays.fill(visited, false);
        visited[0] = true; // Depósito já visitado

        int vehicleIndex = 0;

        // Processar cada cluster
        for (List<Client> cluster : clusters) {
            if (cluster.isEmpty())
                continue;

            // Ordenar clientes do cluster por proximidade do depósito
            Client depot = allClients.get(0);
            cluster.sort((c1, c2) -> {
                double dist1 = calculateDistance(c1.getX(), c1.getY(), depot.getX(), depot.getY());
                double dist2 = calculateDistance(c2.getX(), c2.getY(), depot.getX(), depot.getY());
                return Double.compare(dist1, dist2);
            });

            // Construir rotas dentro do cluster
            int clusterClientIndex = 0;
            while (clusterClientIndex < cluster.size() && vehicleIndex < App.numVehicles) {
                int capacity = 0;
                int pos = 0;

                // Iniciar rota do veículo
                individual.setClientInRoute(vehicleIndex, pos, 0); // Depósito
                pos++;

                Client lastClient = depot;

                // Adicionar clientes à rota respeitando capacidade
                while (clusterClientIndex < cluster.size() && pos < App.numClients) {
                    Client client = cluster.get(clusterClientIndex);

                    // Verificar se já foi visitado
                    if (visited[client.getId()]) {
                        clusterClientIndex++;
                        continue;
                    }

                    // Verificar capacidade
                    if (capacity + client.getDemand() > App.vehicleCapacity) {
                        break;
                    }

                    // Adicionar cliente à rota
                    individual.setClientInRoute(vehicleIndex, pos, client.getId());
                    visited[client.getId()] = true;
                    capacity += client.getDemand();
                    lastClient = client;
                    pos++;
                    clusterClientIndex++;
                }

                // Fechar rota voltando ao depósito
                individual.setClientInRoute(vehicleIndex, pos, 0);
                vehicleIndex++;
            }
        }

        // Adicionar clientes não visitados (se houver) nas rotas restantes
        addRemainingClients(individual, visited, allClients, vehicleIndex);
    }

    /**
     * Adiciona clientes não visitados às rotas restantes
     */
    private void addRemainingClients(Individual individual, boolean[] visited, List<Client> clients, int startVehicle) {
        Client depot = clients.get(0);

        for (int v = startVehicle; v < App.numVehicles; v++) {
            int capacity = 0;
            int pos = 0;

            individual.setClientInRoute(v, pos, 0); // Depósito
            pos++;

            Client lastClient = depot;

            for (int clientId = 1; clientId < App.numClients; clientId++) {
                if (visited[clientId])
                    continue;

                Client client = clients.get(clientId);

                if (capacity + client.getDemand() > App.vehicleCapacity) {
                    continue;
                }

                individual.setClientInRoute(v, pos, clientId);
                visited[clientId] = true;
                capacity += client.getDemand();
                lastClient = client;
                pos++;

                if (pos > App.numClients) // Array has size numClients + 1, so max valid index is numClients
                    break;
            }

            individual.setClientInRoute(v, pos, 0); // Retornar ao depósito
        }
    }

    /**
     * Calcula distância euclidiana entre dois pontos
     */
    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Classe auxiliar para representar um centroide
     */
    private static class Centroid {
        double x;
        double y;

        Centroid(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
