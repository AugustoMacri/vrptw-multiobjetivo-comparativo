package genetic.nsga;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import java.util.ArrayList;
import java.util.List;

import main.App;
import vrp.Client;

/**
 * Crossover de um ponto para VRP, inspirado no AEMMT.
 *
 * Opera diretamente na matriz de rotas (nao lineariza).
 * Usa repair conservativo: remove duplicatas, insere faltantes
 * tentando posicao do pai original, depois melhor posicao viavel.
 */
public class VRPSinglePointCrossover implements CrossoverOperator<VRPSolution> {
    private double crossoverProbability;
    private JMetalRandom randomGenerator;

    public VRPSinglePointCrossover(double crossoverProbability) {
        this.crossoverProbability = crossoverProbability;
        this.randomGenerator = JMetalRandom.getInstance();
    }

    @Override
    public List<VRPSolution> execute(List<VRPSolution> parents) {
        if (parents.size() != 2) {
            throw new RuntimeException("VRPSinglePointCrossover requires exactly 2 parents");
        }

        List<VRPSolution> offspring = new ArrayList<>();

        if (randomGenerator.nextDouble() < crossoverProbability) {
            VRPSolution parent1 = parents.get(0);
            VRPSolution parent2 = parents.get(1);

            VRPSolution child1 = createChild(parent1, parent2);
            VRPSolution child2 = createChild(parent2, parent1);

            offspring.add(child1);
            offspring.add(child2);
        } else {
            offspring.add((VRPSolution) parents.get(0).copy());
            offspring.add((VRPSolution) parents.get(1).copy());
        }

        return offspring;
    }

    /**
     * Cria um filho a partir de dois pais usando crossover de um ponto por coluna.
     * Identico ao AEMMT: normaliza, corta por coluna, repara, desnormaliza.
     */
    private VRPSolution createChild(VRPSolution primaryParent, VRPSolution secondaryParent) {
        int numVehicles = App.numVehicles;
        int numClients = App.numClients;

        int[][] parent1Routes = primaryParent.getRoutes();
        int[][] parent2Routes = secondaryParent.getRoutes();

        // Normalizar rotas dos pais (extrair clientes validos, preencher com 0)
        int[][] normP1 = normalizeRoute(parent1Routes, numVehicles, numClients);
        int[][] normP2 = normalizeRoute(parent2Routes, numVehicles, numClients);

        // Gerar ponto de corte aleatorio (1 a numClients-1)
        int cut = randomGenerator.nextInt(1, numClients - 1);

        // Criar rota do filho: colunas 0..cut-1 do pai1, colunas cut..fim do pai2
        int[][] childRoute = new int[numVehicles][numClients];
        for (int v = 0; v < numVehicles; v++) {
            for (int c = 0; c < cut; c++) {
                childRoute[v][c] = normP1[v][c];
            }
            for (int c = cut; c < numClients; c++) {
                childRoute[v][c] = normP2[v][c];
            }
        }

        // Repair conservativo (identico ao AEMMT)
        repairRoute(childRoute, normP1, normP2, numVehicles, numClients);

        // Desnormalizar (compactar, remover zeros intermediarios)
        int[][] denormalized = denormalizeRoute(childRoute, numVehicles, numClients);

        // Reparar violacoes de capacidade
        repairCapacityViolations(denormalized, numVehicles, numClients);

        // Criar VRPSolution
        VRPSolution child = (VRPSolution) primaryParent.copy();
        child.setRoutes(denormalized);
        return child;
    }

    /** Normaliza rota: extrai clientes validos (>0), preenche resto com 0. */
    private int[][] normalizeRoute(int[][] route, int numVehicles, int numClients) {
        int[][] normalized = new int[numVehicles][numClients];
        for (int v = 0; v < numVehicles; v++) {
            int pos = 0;
            for (int c = 0; c < numClients; c++) {
                int clientId = route[v][c];
                if (clientId > 0) {
                    normalized[v][pos++] = clientId;
                }
            }
            for (; pos < numClients; pos++) {
                normalized[v][pos] = 0;
            }
        }
        return normalized;
    }

    /** Desnormaliza rota: remove zeros, preenche com -1. */
    private int[][] denormalizeRoute(int[][] route, int numVehicles, int numClients) {
        int[][] denormalized = new int[numVehicles][numClients];
        for (int v = 0; v < numVehicles; v++) {
            int pos = 0;
            for (int c = 0; c < numClients; c++) {
                if (route[v][c] != 0 && route[v][c] != -1) {
                    denormalized[v][pos++] = route[v][c];
                }
            }
            for (; pos < numClients; pos++) {
                denormalized[v][pos] = -1;
            }
        }
        return denormalized;
    }

    /**
     * Repair conservativo: remove duplicatas, insere faltantes.
     * NAO destroi a estrutura - so corrige problemas pontuais.
     */
    private void repairRoute(int[][] childRoute, int[][] parent1Norm, int[][] parent2Norm,
                             int numVehicles, int numClients) {
        // Passo 1: Remover duplicatas (manter primeira ocorrencia)
        boolean[] seen = new boolean[numClients];
        for (int v = 0; v < numVehicles; v++) {
            for (int c = 0; c < numClients; c++) {
                int clientId = childRoute[v][c];
                if (clientId > 0 && clientId < numClients) {
                    if (seen[clientId]) {
                        childRoute[v][c] = 0; // Duplicata -> remover
                    } else {
                        seen[clientId] = true;
                    }
                }
            }
        }

        // Passo 2: Coletar clientes faltantes
        List<Integer> missing = new ArrayList<>();
        for (int id = 1; id < numClients; id++) {
            if (!seen[id]) missing.add(id);
        }

        // Passo 3: Inserir faltantes tentando posicao dos pais primeiro
        List<Client> clients = App.clients;
        int[][][] parents = {parent1Norm, parent2Norm};

        for (int missingClient : missing) {
            boolean inserted = false;
            for (int[][] parentRoute : parents) {
                if (inserted) break;
                for (int v = 0; v < numVehicles && !inserted; v++) {
                    for (int c = 0; c < numClients && !inserted; c++) {
                        if (parentRoute[v][c] == missingClient) {
                            if (insertAtPosition(childRoute, missingClient, v, c,
                                    numVehicles, numClients, clients)) {
                                inserted = true;
                            }
                        }
                    }
                }
            }
            if (!inserted) {
                insertAnywhere(childRoute, missingClient, numVehicles, numClients, clients);
            }
        }
    }

    /** Tenta inserir cliente na posicao especificada ou adjacente. Verifica capacidade. */
    private boolean insertAtPosition(int[][] route, int clientId, int vehicle, int position,
                                     int numVehicles, int numClients, List<Client> clients) {
        if (clients != null) {
            int currentDemand = calculateVehicleDemand(route, vehicle, numClients, clients);
            int clientDemand = clients.get(clientId).getDemand();
            if (currentDemand + clientDemand > App.vehicleCapacity) return false;
        }
        // Tentar posicao exata
        if (position >= 0 && position < numClients &&
                (route[vehicle][position] == 0 || route[vehicle][position] == -1)) {
            route[vehicle][position] = clientId;
            return true;
        }
        // Tentar posicoes adjacentes
        for (int offset = 1; offset < numClients; offset++) {
            int pos1 = position + offset;
            int pos2 = position - offset;
            if (pos1 < numClients && (route[vehicle][pos1] == 0 || route[vehicle][pos1] == -1)) {
                route[vehicle][pos1] = clientId;
                return true;
            }
            if (pos2 >= 0 && (route[vehicle][pos2] == 0 || route[vehicle][pos2] == -1)) {
                route[vehicle][pos2] = clientId;
                return true;
            }
        }
        return false;
    }

    /** Insere cliente em qualquer veiculo com capacidade disponivel. */
    private void insertAnywhere(int[][] route, int clientId,
                                int numVehicles, int numClients, List<Client> clients) {
        int clientDemand = (clients != null && clientId < clients.size()) ? clients.get(clientId).getDemand() : 0;

        // Tentativa 1: veiculo com capacidade
        for (int v = 0; v < numVehicles; v++) {
            if (clients != null) {
                int demand = calculateVehicleDemand(route, v, numClients, clients);
                if (demand + clientDemand > App.vehicleCapacity) continue;
            }
            for (int c = 0; c < numClients; c++) {
                if (route[v][c] == 0 || route[v][c] == -1) {
                    route[v][c] = clientId;
                    return;
                }
            }
        }

        // Tentativa 2: redistribuir (mover cliente pequeno para liberar espaco)
        if (clients != null) {
            for (int vFull = 0; vFull < numVehicles; vFull++) {
                int currentDemand = calculateVehicleDemand(route, vFull, numClients, clients);
                if (currentDemand + clientDemand > App.vehicleCapacity) {
                    for (int c = 0; c < numClients; c++) {
                        int existing = route[vFull][c];
                        if (existing <= 0 || existing >= numClients) continue;
                        int existingDemand = clients.get(existing).getDemand();
                        if (currentDemand - existingDemand + clientDemand <= App.vehicleCapacity) {
                            for (int vOther = 0; vOther < numVehicles; vOther++) {
                                if (vOther == vFull) continue;
                                int otherDemand = calculateVehicleDemand(route, vOther, numClients, clients);
                                if (otherDemand + existingDemand <= App.vehicleCapacity) {
                                    for (int pos = 0; pos < numClients; pos++) {
                                        if (route[vOther][pos] == 0 || route[vOther][pos] == -1) {
                                            route[vOther][pos] = existing;
                                            route[vFull][c] = clientId;
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tentativa 3: forcado no primeiro veiculo com espaco fisico
        for (int v = 0; v < numVehicles; v++) {
            for (int c = 0; c < numClients; c++) {
                if (route[v][c] == 0 || route[v][c] == -1) {
                    route[v][c] = clientId;
                    return;
                }
            }
        }
    }

    /** Repara violacoes de capacidade movendo clientes ou trocando entre veiculos. */
    private void repairCapacityViolations(int[][] route, int numVehicles, int numClients) {
        List<Client> clients = App.clients;
        if (clients == null) return;

        for (int iteration = 0; iteration < 50; iteration++) {
            boolean hasViolation = false;
            for (int vOver = 0; vOver < numVehicles; vOver++) {
                int demand = calculateVehicleDemand(route, vOver, numClients, clients);
                if (demand <= App.vehicleCapacity) continue;
                hasViolation = true;

                boolean moved = false;
                for (int c = 0; c < numClients && !moved; c++) {
                    int cid = route[vOver][c];
                    if (cid <= 0) continue;
                    int cd = clients.get(cid).getDemand();
                    for (int vDest = 0; vDest < numVehicles && !moved; vDest++) {
                        if (vDest == vOver) continue;
                        int dd = calculateVehicleDemand(route, vDest, numClients, clients);
                        if (dd + cd <= App.vehicleCapacity) {
                            for (int pos = 0; pos < numClients; pos++) {
                                if (route[vDest][pos] == -1) {
                                    route[vDest][pos] = cid;
                                    route[vOver][c] = -1;
                                    compactRoute(route[vOver], numClients);
                                    moved = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!moved) {
                    trySwapForCapacity(route, vOver, numVehicles, numClients, clients);
                }
            }
            if (!hasViolation) break;
        }
    }

    private boolean trySwapForCapacity(int[][] route, int vOver,
                                       int numVehicles, int numClients, List<Client> clients) {
        int overDemand = calculateVehicleDemand(route, vOver, numClients, clients);
        for (int c1 = 0; c1 < numClients; c1++) {
            int cl1 = route[vOver][c1];
            if (cl1 <= 0) continue;
            int d1 = clients.get(cl1).getDemand();
            for (int vO = 0; vO < numVehicles; vO++) {
                if (vO == vOver) continue;
                int od = calculateVehicleDemand(route, vO, numClients, clients);
                for (int c2 = 0; c2 < numClients; c2++) {
                    int cl2 = route[vO][c2];
                    if (cl2 <= 0) continue;
                    int d2 = clients.get(cl2).getDemand();
                    if (overDemand - d1 + d2 <= App.vehicleCapacity &&
                            od - d2 + d1 <= App.vehicleCapacity) {
                        route[vOver][c1] = cl2;
                        route[vO][c2] = cl1;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void compactRoute(int[] vehicleRoute, int numClients) {
        int[] temp = new int[numClients];
        int pos = 0;
        for (int i = 0; i < numClients; i++) {
            if (vehicleRoute[i] > 0) temp[pos++] = vehicleRoute[i];
        }
        for (; pos < numClients; pos++) temp[pos] = -1;
        System.arraycopy(temp, 0, vehicleRoute, 0, numClients);
    }

    private int calculateVehicleDemand(int[][] route, int vehicle, int numClients, List<Client> clients) {
        int total = 0;
        for (int c = 0; c < numClients; c++) {
            int id = route[vehicle][c];
            if (id > 0 && id < clients.size()) total += clients.get(id).getDemand();
        }
        return total;
    }

    @Override
    public double crossoverProbability() {
        return crossoverProbability;
    }

    @Override
    public int numberOfRequiredParents() {
        return 2;
    }

    @Override
    public int numberOfGeneratedChildren() {
        return 2;
    }
}
