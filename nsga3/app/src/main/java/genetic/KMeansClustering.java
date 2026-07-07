package genetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vrp.Client;

public class KMeansClustering {

    private static class Cluster {
        private double centroidX;
        private double centroidY;
        private List<Client> clients;

        public Cluster(double x, double y) {
            this.centroidX = x;
            this.centroidY = y;
            this.clients = new ArrayList<>();
        }

        public void addClient(Client client) {
            clients.add(client);
        }

        public void clearClients() {
            clients.clear();
        }

        public void updateCentroid() {
            if (clients.isEmpty())
                return;

            double sumX = 0;
            double sumY = 0;

            for (Client client : clients) {
                sumX += client.getX();
                sumY += client.getY();
            }

            centroidX = sumX / clients.size();
            centroidY = sumY / clients.size();
        }

        public double getCentroidX() {
            return centroidX;
        }

        public double getCentroidY() {
            return centroidY;
        }

        public List<Client> getClients() {
            return clients;
        }
    }

    /**
     * Aplica K-means clustering nos clientes para criar grupos geográficos
     * 
     * @param clients       Lista de todos os clientes (incluindo depósito no índice
     *                      0)
     * @param k             Número de clusters (geralmente o número de veículos)
     * @param maxIterations Número máximo de iterações
     * @return Lista de clusters, cada um contendo seus clientes
     */
    public static List<List<Client>> clusterClients(List<Client> clients, int k, int maxIterations) {
        if (clients == null || clients.size() <= 1) {
            throw new IllegalArgumentException("Lista de clientes inválida");
        }

        // Remove o depósito (índice 0) da clusterização
        List<Client> clientsWithoutDepot = new ArrayList<>();
        Client depot = clients.get(0);

        for (int i = 1; i < clients.size(); i++) {
            clientsWithoutDepot.add(clients.get(i));
        }

        if (clientsWithoutDepot.isEmpty()) {
            return new ArrayList<>();
        }

        // Ajusta k se necessário
        k = Math.min(k, clientsWithoutDepot.size());

        // Inicializa clusters com k-means++
        List<Cluster> clusters = initializeClustersKMeansPlusPlus(clientsWithoutDepot, k, depot);

        // Itera até convergência ou máximo de iterações
        boolean converged = false;
        int iteration = 0;

        while (!converged && iteration < maxIterations) {
            // Limpa os clientes de cada cluster
            for (Cluster cluster : clusters) {
                cluster.clearClients();
            }

            // Atribui cada cliente ao cluster mais próximo
            for (Client client : clientsWithoutDepot) {
                Cluster nearestCluster = findNearestCluster(client, clusters);
                nearestCluster.addClient(client);
            }

            // Atualiza centróides
            converged = true;
            for (Cluster cluster : clusters) {
                double oldX = cluster.getCentroidX();
                double oldY = cluster.getCentroidY();

                cluster.updateCentroid();

                // Verifica se houve mudança significativa
                if (Math.abs(oldX - cluster.getCentroidX()) > 0.01 ||
                        Math.abs(oldY - cluster.getCentroidY()) > 0.01) {
                    converged = false;
                }
            }

            iteration++;
        }

        // Converte clusters para lista de listas de clientes
        List<List<Client>> result = new ArrayList<>();
        for (Cluster cluster : clusters) {
            if (!cluster.getClients().isEmpty()) {
                result.add(new ArrayList<>(cluster.getClients()));
            }
        }

        return result;
    }

    /**
     * Inicializa clusters usando K-means++ para melhor distribuição inicial
     * Considera a distância ao depósito para melhor inicialização
     */
    private static List<Cluster> initializeClustersKMeansPlusPlus(List<Client> clients, int k, Client depot) {
        List<Cluster> clusters = new ArrayList<>();
        Random random = new Random();

        // Primeiro centróide: cliente mais próximo do depósito
        Client firstClient = clients.stream()
                .min((c1, c2) -> Double.compare(
                        calculateDistance(c1, depot),
                        calculateDistance(c2, depot)))
                .orElse(clients.get(0));

        clusters.add(new Cluster(firstClient.getX(), firstClient.getY()));

        // Seleciona os próximos k-1 centróides
        for (int i = 1; i < k; i++) {
            double[] distances = new double[clients.size()];
            double totalDistance = 0;

            // Calcula distância de cada cliente ao cluster mais próximo
            for (int j = 0; j < clients.size(); j++) {
                Client client = clients.get(j);
                double minDist = Double.MAX_VALUE;

                for (Cluster cluster : clusters) {
                    double dist = calculateDistance(
                            client.getX(), client.getY(),
                            cluster.getCentroidX(), cluster.getCentroidY());
                    minDist = Math.min(minDist, dist);
                }

                distances[j] = minDist * minDist; // Distância ao quadrado
                totalDistance += distances[j];
            }

            // Seleciona próximo centróide usando distribuição de probabilidade
            double threshold = random.nextDouble() * totalDistance;
            double cumulativeDistance = 0;

            for (int j = 0; j < clients.size(); j++) {
                cumulativeDistance += distances[j];
                if (cumulativeDistance >= threshold) {
                    Client selectedClient = clients.get(j);
                    clusters.add(new Cluster(selectedClient.getX(), selectedClient.getY()));
                    break;
                }
            }
        }

        return clusters;
    }

    /**
     * Encontra o cluster mais próximo para um cliente
     */
    private static Cluster findNearestCluster(Client client, List<Cluster> clusters) {
        Cluster nearest = clusters.get(0);
        double minDistance = calculateDistance(
                client.getX(), client.getY(),
                nearest.getCentroidX(), nearest.getCentroidY());

        for (int i = 1; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            double distance = calculateDistance(
                    client.getX(), client.getY(),
                    cluster.getCentroidX(), cluster.getCentroidY());

            if (distance < minDistance) {
                minDistance = distance;
                nearest = cluster;
            }
        }

        return nearest;
    }

    /**
     * Calcula distância euclidiana entre dois pontos
     */
    private static double calculateDistance(Client c1, Client c2) {
        return Math.sqrt(Math.pow(c1.getX() - c2.getX(), 2) +
                Math.pow(c1.getY() - c2.getY(), 2));
    }

    private static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
