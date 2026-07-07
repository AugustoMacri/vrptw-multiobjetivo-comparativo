package genetic.nsga;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Operador de mutação específico para VRPSolution
 * Implementa mutações típicas do VRP como swap, inserção e 2-opt
 */
public class VRPMutationOperator implements MutationOperator<VRPSolution> {
    private double mutationProbability;
    private JMetalRandom randomGenerator;

    public VRPMutationOperator(double mutationProbability) {
        this.mutationProbability = mutationProbability;
        this.randomGenerator = JMetalRandom.getInstance();
    }

    @Override
    public VRPSolution execute(VRPSolution solution) {
        VRPSolution mutatedSolution = (VRPSolution) solution.copy();

        if (randomGenerator.nextDouble() < mutationProbability) {
            // Aplicar diferentes tipos de mutação aleatoriamente
            double mutationType = randomGenerator.nextDouble();

            if (mutationType < 0.33) {
                swapMutation(mutatedSolution);
            } else if (mutationType < 0.66) {
                insertionMutation(mutatedSolution);
            } else {
                inversionMutation(mutatedSolution);
            }
        }

        return mutatedSolution;
    }

    /**
     * Mutação por troca: troca dois clientes de posição (podem estar em rotas
     * diferentes)
     */
    private void swapMutation(VRPSolution solution) {
        int[][] routes = solution.getRoutes();

        // Encontrar todos os clientes (exceto depósito)
        int totalClients = 0;
        for (int v = 0; v < routes.length; v++) {
            for (int c = 1; c < routes[v].length - 1; c++) { // Ignora depósitos
                if (routes[v][c] != -1 && routes[v][c] != 0) {
                    totalClients++;
                }
            }
        }

        if (totalClients < 2)
            return; // Não há clientes suficientes para trocar

        // Selecionar duas posições aleatórias com clientes
        int[] positions = findTwoRandomClientPositions(routes);
        if (positions != null) {
            int v1 = positions[0], c1 = positions[1];
            int v2 = positions[2], c2 = positions[3];

            // Trocar os clientes
            int temp = routes[v1][c1];
            routes[v1][c1] = routes[v2][c2];
            routes[v2][c2] = temp;

            solution.setRoutes(routes);
        }
    }

    /**
     * Mutação por inserção: move um cliente para uma posição diferente
     */
    private void insertionMutation(VRPSolution solution) {
        int[][] routes = solution.getRoutes();

        // Encontrar um cliente aleatório para mover
        int[] clientPos = findRandomClientPosition(routes);
        if (clientPos == null)
            return;

        int sourceVehicle = clientPos[0];
        int sourcePosition = clientPos[1];
        int client = routes[sourceVehicle][sourcePosition];

        // Remover o cliente da posição atual
        for (int i = sourcePosition; i < routes[sourceVehicle].length - 1; i++) {
            routes[sourceVehicle][i] = routes[sourceVehicle][i + 1];
        }
        routes[sourceVehicle][routes[sourceVehicle].length - 1] = -1;

        // Escolher nova posição aleatória
        int targetVehicle = randomGenerator.nextInt(0, routes.length);
        int routeLength = 0;
        for (int c = 0; c < routes[targetVehicle].length; c++) {
            if (routes[targetVehicle][c] == -1)
                break;
            routeLength++;
        }

        if (routeLength < routes[targetVehicle].length) {
            int targetPosition = Math.max(1, randomGenerator.nextInt(1, Math.max(2, routeLength))); // Não na primeira
                                                                                                    // posição
                                                                                                    // (depósito)

            // Mover elementos para abrir espaço
            for (int i = routeLength; i > targetPosition && i < routes[targetVehicle].length; i--) {
                if (i - 1 >= 0) {
                    routes[targetVehicle][i] = routes[targetVehicle][i - 1];
                }
            }

            // Inserir o cliente na nova posição
            if (targetPosition < routes[targetVehicle].length) {
                routes[targetVehicle][targetPosition] = client;
            }

            solution.setRoutes(routes);
        }
    }

    /**
     * Mutação por inversão: inverte uma subsequência de uma rota (2-opt)
     */
    private void inversionMutation(VRPSolution solution) {
        int[][] routes = solution.getRoutes();

        // Escolher um veículo aleatório
        int vehicle = randomGenerator.nextInt(0, routes.length);

        // Encontrar o tamanho da rota
        int routeLength = 0;
        for (int c = 0; c < routes[vehicle].length; c++) {
            if (routes[vehicle][c] == -1)
                break;
            routeLength++;
        }

        if (routeLength > 3) { // Precisa de pelo menos 4 elementos (incluindo depósitos)
            int start = randomGenerator.nextInt(1, Math.min(routeLength - 2, routes[vehicle].length - 2));
            int end = randomGenerator.nextInt(start + 1, Math.min(routeLength - 1, routes[vehicle].length - 1));

            // Inverter a subsequência
            while (start < end) {
                int temp = routes[vehicle][start];
                routes[vehicle][start] = routes[vehicle][end];
                routes[vehicle][end] = temp;
                start++;
                end--;
            }

            solution.setRoutes(routes);
        }
    }

    /**
     * Encontra duas posições aleatórias que contêm clientes (não depósito)
     */
    private int[] findTwoRandomClientPositions(int[][] routes) {
        int attempts = 0;
        while (attempts < 100) { // Limite de tentativas
            int v1 = randomGenerator.nextInt(0, routes.length);
            int v2 = randomGenerator.nextInt(0, routes.length);

            if (routes[v1].length > 2 && routes[v2].length > 2) {
                int c1 = randomGenerator.nextInt(1, routes[v1].length - 1);
                int c2 = randomGenerator.nextInt(1, routes[v2].length - 1);

                if (c1 < routes[v1].length && c2 < routes[v2].length &&
                        routes[v1][c1] != -1 && routes[v1][c1] != 0 &&
                        routes[v2][c2] != -1 && routes[v2][c2] != 0 &&
                        !(v1 == v2 && c1 == c2)) {
                    return new int[] { v1, c1, v2, c2 };
                }
            }
            attempts++;
        }
        return null;
    }

    /**
     * Encontra uma posição aleatória que contém um cliente (não depósito)
     */
    private int[] findRandomClientPosition(int[][] routes) {
        int attempts = 0;
        while (attempts < 100) { // Limite de tentativas
            int v = randomGenerator.nextInt(0, routes.length);
            if (routes[v].length > 2) { // Deve ter pelo menos espaço para depósito-cliente-depósito
                int c = randomGenerator.nextInt(1, routes[v].length - 1);

                if (c < routes[v].length && routes[v][c] != -1 && routes[v][c] != 0) {
                    return new int[] { v, c };
                }
            }
            attempts++;
        }
        return null;
    }

    @Override
    public double mutationProbability() {
        return mutationProbability;
    }
}