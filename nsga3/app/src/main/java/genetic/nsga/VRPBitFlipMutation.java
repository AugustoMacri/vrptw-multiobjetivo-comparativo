package genetic.nsga;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import java.util.ArrayList;
import java.util.List;

import main.App;
import vrp.Client;

/**
 * Mutacao VRP inspirada no AEMMT.
 *
 * Dois operadores combinados (identicos ao AEMMT Mutation.java):
 * 1. Intra-rota: swap de 2 clientes dentro de cada veiculo
 * 2. Inter-rota: troca de clientes entre 2 veiculos diferentes (respeita capacidade)
 *
 * NAO faz conversao para representacao linear.
 * NAO faz reconstrucao total (repair destrutivo).
 * Trabalha diretamente na matriz de rotas preservando a estrutura.
 */
public class VRPBitFlipMutation implements MutationOperator<VRPSolution> {
    private double mutationProbability;
    private JMetalRandom randomGenerator;

    public VRPBitFlipMutation(double mutationProbability) {
        this.mutationProbability = mutationProbability;
        this.randomGenerator = JMetalRandom.getInstance();
    }

    @Override
    public VRPSolution execute(VRPSolution solution) {
        // CRITICO: jMetal ignora o retorno de execute() - modifica IN-PLACE!
        // (confirmado no codigo-fonte de AbstractGeneticAlgorithm do jMetal)
        int[][] routes = solution.getRoutes();
        int numVehicles = App.numVehicles;
        int numClients = App.numClients;

        // 1. Intra-rota: gate de 1% por individuo (como AEMMT)
        int mutateChance = (int) (1.0 / mutationProbability);
        if (mutateChance <= 0) mutateChance = 1;
        int roll = randomGenerator.nextInt(0, mutateChance - 1);
        if (roll == 0) {
            mutateIntraRoute(routes, numVehicles, numClients);
        }

        // 2. Inter-rota: SEMPRE aplica (como AEMMT interRouteRate=1.0)
        mutateInterRoute(routes, numVehicles, numClients);

        // Nao precisa setRoutes - routes e referencia direta ao array interno
        return solution;
    }

    /**
     * Mutacao intra-rota: para cada veiculo, swap de 2 clientes aleatorios.
     * Identico ao AEMMT Mutation.mutate().
     */
    private void mutateIntraRoute(int[][] routes, int numVehicles, int numClients) {
        for (int v = 0; v < numVehicles; v++) {
            // Coletar posicoes com clientes validos
            List<Integer> validPositions = new ArrayList<>();
            for (int c = 0; c < numClients; c++) {
                if (routes[v][c] > 0) {
                    validPositions.add(c);
                }
            }

            // Precisa de pelo menos 4 clientes para swap (identico ao AEMMT)
            if (validPositions.size() < 4) continue;

            // Escolher 2 posicoes diferentes
            int idx1, idx2;
            do {
                idx1 = validPositions.get(randomGenerator.nextInt(0, validPositions.size() - 1));
                idx2 = validPositions.get(randomGenerator.nextInt(0, validPositions.size() - 1));
            } while (idx1 == idx2);

            // Swap
            int temp = routes[v][idx1];
            routes[v][idx1] = routes[v][idx2];
            routes[v][idx2] = temp;
        }
    }

    /**
     * Mutacao inter-rota: troca clientes entre 2 veiculos diferentes.
     * Verifica capacidade antes da troca. Identico ao AEMMT Mutation.mutateInterRoute().
     */
    private void mutateInterRoute(int[][] routes, int numVehicles, int numClients) {
        List<Client> clients = App.clients;
        if (clients == null) return;

        // Encontrar 2 veiculos diferentes com clientes
        int vehicle1 = -1, vehicle2 = -1;
        int maxAttempts = 50;
        int attempts = 0;

        while (attempts < maxAttempts) {
            vehicle1 = randomGenerator.nextInt(0, numVehicles - 1);
            vehicle2 = randomGenerator.nextInt(0, numVehicles - 1);
            if (vehicle1 == vehicle2) { attempts++; continue; }

            int count1 = 0, count2 = 0;
            for (int c = 0; c < numClients; c++) {
                if (routes[vehicle1][c] > 0) count1++;
                if (routes[vehicle2][c] > 0) count2++;
            }
            if (count1 > 0 && count2 > 0) break;
            attempts++;
        }

        if (attempts >= maxAttempts) return;

        // Selecionar cliente aleatorio de cada veiculo
        int client1Pos = findRandomClientPos(routes[vehicle1], numClients);
        int client2Pos = findRandomClientPos(routes[vehicle2], numClients);
        if (client1Pos == -1 || client2Pos == -1) return;

        // Verificar capacidade antes da troca
        int clientId1 = routes[vehicle1][client1Pos];
        int clientId2 = routes[vehicle2][client2Pos];

        int demand1Total = 0, demand2Total = 0;
        for (int c = 0; c < numClients; c++) {
            int id1 = routes[vehicle1][c];
            int id2 = routes[vehicle2][c];
            if (id1 > 0 && id1 < clients.size()) demand1Total += clients.get(id1).getDemand();
            if (id2 > 0 && id2 < clients.size()) demand2Total += clients.get(id2).getDemand();
        }

        int clientDemand1 = clients.get(clientId1).getDemand();
        int clientDemand2 = clients.get(clientId2).getDemand();

        int newDemand1 = demand1Total - clientDemand1 + clientDemand2;
        int newDemand2 = demand2Total - clientDemand2 + clientDemand1;

        // So trocar se ambos veiculos ficam dentro da capacidade
        if (newDemand1 > App.vehicleCapacity || newDemand2 > App.vehicleCapacity) {
            return;
        }

        // Realizar troca
        routes[vehicle1][client1Pos] = clientId2;
        routes[vehicle2][client2Pos] = clientId1;
    }

    /**
     * Encontra uma posicao aleatoria com cliente valido (>0) no veiculo.
     */
    private int findRandomClientPos(int[] vehicleRoute, int numClients) {
        List<Integer> validPositions = new ArrayList<>();
        for (int c = 0; c < numClients; c++) {
            if (vehicleRoute[c] > 0) {
                validPositions.add(c);
            }
        }
        if (validPositions.isEmpty()) return -1;
        return validPositions.get(randomGenerator.nextInt(0, validPositions.size() - 1));
    }

    @Override
    public double mutationProbability() {
        return mutationProbability;
    }
}
