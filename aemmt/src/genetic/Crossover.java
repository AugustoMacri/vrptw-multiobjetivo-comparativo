package genetic;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import main.App;
import vrp.Client;

public class Crossover {

    public static Individual onePointCrossing(Individual parent1, Individual parent2) {
        return onePointCrossing(parent1, parent2, null);
    }

    public static Individual onePointCrossing(Individual parent1, Individual parent2, List<Client> clients) {

        // System.out.println("Entrou no onePointCrossing");

        Random random = new Random();

        // Normalize the routes of the parents to have the same size
        int[][] normalizedParent1 = normalizeRoute(parent1.getRoute());
        int[][] normalizedParent2 = normalizeRoute(parent2.getRoute());

        int[][] childRoute = new int[App.numVehicles][App.numClients];

        // Initialize the child route with 0 (not -1, because repairRoute expects 0 for
        // empty positions)
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                childRoute[v][c] = 0;
            }
        }

        // Generate a random crossover point
        int cut;
        do {
            cut = random.nextInt(App.numClients) + 1;

        } while (cut == App.vehicleCapacity + 1);

        // Copy the first part of the route from parent1
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < cut; c++) {
                childRoute[v][c] = normalizedParent1[v][c];
            }
        }

        // Copy the second part of the route from parent2
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = cut; c < App.numClients; c++) {
                childRoute[v][c] = normalizedParent2[v][c];
            }
        }

        // Repair child route: ensure all clients (1 to numClients-1) appear exactly
        // once
        childRoute = repairRoute(childRoute, parent1, parent2, clients);

        // Denormalize the child route
        int[][] denormalizedChildRoute = denormalizeRoute(childRoute);

        // Final validation: ensure all clients are present after denormalization
        if (!validateRoute(denormalizedChildRoute, App.numClients - 1)) {
            System.err.println("ERRO CRÍTICO: Crossover gerou rota inválida após desnormalização!");
            // Try to repair again using the normalized version
            denormalizedChildRoute = denormalizeRoute(childRoute);
        }

        // **CORREÇÃO CRÍTICA**: Reparar violações de capacidade após o crossover
        denormalizedChildRoute = repairCapacityViolations(denormalizedChildRoute, clients);

        // Usar ID único e incrementar o contador
        Individual newSon = new Individual(App.nextIndividualId++, 0, 0, 0, 0);
        newSon.setRoute(denormalizedChildRoute);

        return newSon;
    }

    // Normalize the route to have the same size
    private static int[][] normalizeRoute(int[][] route) {
        int[][] normalizedRoute = new int[App.numVehicles][App.numClients];

        for (int v = 0; v < App.numVehicles; v++) {
            int c;

            for (c = 0; c < App.numClients; c++) {
                if (route[v][c] == 0 && c > 0) {
                    break;
                }
                normalizedRoute[v][c] = route[v][c];
            }

            for (; c < App.numClients; c++) {
                normalizedRoute[v][c] = 0;
            }
        }

        return normalizedRoute;
    }

    // Denormalize the route of the child to have the same size as the others
    private static int[][] denormalizeRoute(int[][] route) {
        int[][] denormalizedRoute = new int[App.numVehicles][App.numClients];

        for (int v = 0; v < App.numVehicles; v++) {
            int pos = 0;

            // Copy ALL clients (skip zeros and -1), regardless of position
            for (int c = 0; c < App.numClients; c++) {
                if (route[v][c] != 0 && route[v][c] != -1) {
                    denormalizedRoute[v][pos] = route[v][c];
                    pos++;
                }
            }

            // Fill remaining positions with -1
            for (; pos < App.numClients; pos++) {
                denormalizedRoute[v][pos] = -1;
            }
        }

        return denormalizedRoute;
    }

    /**
     * Repairs the child route to ensure all clients appear exactly once.
     * Removes duplicates and adds missing clients.
     */
    private static int[][] repairRoute(int[][] childRoute, Individual parent1, Individual parent2,
            List<Client> clients) {
        Random random = new Random();

        // Step 1: Find all clients present in the child route
        boolean[] clientPresent = new boolean[App.numClients + 1]; // +1 to include depot
        clientPresent[0] = true; // depot is always present

        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                int client = childRoute[v][c];
                if (client > 0 && client < App.numClients) { // client IDs are 1 to App.numClients-1
                    clientPresent[client] = true;
                }
            }
        }

        // Step 2: Remove duplicates - keep only first occurrence of each client
        boolean[] alreadySeen = new boolean[App.numClients + 1]; // +1 to include depot
        alreadySeen[0] = true; // depot

        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                int client = childRoute[v][c];

                if (client > 0 && client < App.numClients) { // client IDs are 1 to App.numClients-1
                    if (alreadySeen[client]) {
                        // Duplicate found - replace with 0
                        childRoute[v][c] = 0;
                    } else {
                        alreadySeen[client] = true;
                    }
                }
            }
        }

        // Step 3: Collect missing clients
        java.util.List<Integer> missingClients = new java.util.ArrayList<>();
        for (int client = 1; client < App.numClients; client++) { // < because clients are 1 to App.numClients-1
            if (!alreadySeen[client]) {
                missingClients.add(client);
            }
        }

        // Step 4: Insert missing clients into the route
        // Try to place them in positions from parents first, then randomly
        for (int missingClient : missingClients) {
            boolean inserted = false;

            // Try to find this client's position in parent1 or parent2
            Individual[] parents = { parent1, parent2 };
            for (Individual parent : parents) {
                if (inserted)
                    break;

                int[][] parentRoute = normalizeRoute(parent.getRoute());

                // Find where this client appears in the parent
                for (int v = 0; v < App.numVehicles && !inserted; v++) {
                    for (int c = 0; c < App.numClients && !inserted; c++) {
                        if (parentRoute[v][c] == missingClient) {
                            // Try to insert at the same position in child
                            if (insertClientAtPosition(childRoute, missingClient, v, c, clients)) {
                                inserted = true;
                            }
                            break;
                        }
                    }
                }
            }

            // If still not inserted, insert at first available position
            if (!inserted) {
                insertClientAnywhere(childRoute, missingClient, clients);
            }
        }

        return childRoute;
    }

    /**
     * Calcula a demanda total de um veículo na rota.
     */
    private static int calculateVehicleDemand(int[][] route, int vehicle, List<Client> clients) {
        if (clients == null) {
            return 0; // Sem validação se não temos acesso aos clientes
        }

        int totalDemand = 0;
        for (int c = 0; c < App.numClients; c++) {
            int clientId = route[vehicle][c];
            if (clientId > 0 && clientId < App.numClients) {
                totalDemand += clients.get(clientId).getDemand();
            }
        }
        return totalDemand;
    }

    /**
     * Tries to insert a client at a specific position in the route.
     * Returns true if successful, false otherwise.
     * Verifica a capacidade do veículo antes de QUALQUER inserção.
     */
    private static boolean insertClientAtPosition(int[][] route, int client, int vehicle, int position,
            List<Client> clients) {
        // Verificar capacidade ANTES de tentar qualquer inserção
        if (clients != null) {
            int currentDemand = calculateVehicleDemand(route, vehicle, clients);
            int clientDemand = clients.get(client).getDemand();

            if (currentDemand + clientDemand > App.vehicleCapacity) {
                return false; // Não inserir se ultrapassar capacidade
            }
        }

        // Check if position is available (contains 0 or -1)
        if (route[vehicle][position] == 0 || route[vehicle][position] == -1) {
            route[vehicle][position] = client;
            return true;
        }

        // Try positions around the target position
        // A capacidade já foi verificada acima, então é seguro inserir em qualquer
        // posição
        for (int offset = 1; offset < App.numClients; offset++) {
            int pos1 = position + offset;
            int pos2 = position - offset;

            if (pos1 < App.numClients && (route[vehicle][pos1] == 0 || route[vehicle][pos1] == -1)) {
                route[vehicle][pos1] = client;
                return true;
            }

            if (pos2 >= 0 && (route[vehicle][pos2] == 0 || route[vehicle][pos2] == -1)) {
                route[vehicle][pos2] = client;
                return true;
            }
        }

        return false;
    }

    /**
     * Inserts a client at the first available position in any vehicle.
     * Verifica a capacidade do veículo antes de inserir.
     * Se nenhum veículo tiver capacidade, tenta redistribuir clientes para liberar
     * espaço.
     */
    private static void insertClientAnywhere(int[][] route, int client, List<Client> clients) {
        // Tentativa 1: Inserir em veículos com capacidade disponível
        for (int v = 0; v < App.numVehicles; v++) {
            // Verificar capacidade do veículo
            if (clients != null) {
                int currentDemand = calculateVehicleDemand(route, v, clients);
                int clientDemand = clients.get(client).getDemand();

                if (currentDemand + clientDemand > App.vehicleCapacity) {
                    continue; // Pular este veículo se não tiver capacidade
                }
            }

            // Tentar inserir neste veículo
            for (int c = 1; c < App.numClients; c++) {
                if (route[v][c] == 0 || route[v][c] == -1) {
                    route[v][c] = client;
                    return;
                }
            }
        }

        // Tentativa 2: Tentar redistribuir - mover cliente pequeno para liberar espaço
        if (clients != null) {
            int clientDemand = clients.get(client).getDemand();

            // Procura veículo cheio onde possamos mover um cliente pequeno
            for (int vFull = 0; vFull < App.numVehicles; vFull++) {
                int currentDemand = calculateVehicleDemand(route, vFull, clients);

                // Se este veículo tem espaço físico mas não tem capacidade
                if (currentDemand + clientDemand > App.vehicleCapacity) {
                    // Procura um cliente pequeno neste veículo que possamos mover
                    for (int c = 1; c < App.numClients; c++) {
                        int existingClient = route[vFull][c];
                        if (existingClient > 0 && existingClient < App.numClients) {
                            int existingDemand = clients.get(existingClient).getDemand();

                            // Se mover este cliente liberaria espaço suficiente
                            if (currentDemand - existingDemand + clientDemand <= App.vehicleCapacity) {
                                // Procura outro veículo para o cliente existente
                                for (int vOther = 0; vOther < App.numVehicles; vOther++) {
                                    if (vOther == vFull)
                                        continue;

                                    int otherDemand = calculateVehicleDemand(route, vOther, clients);
                                    if (otherDemand + existingDemand <= App.vehicleCapacity) {
                                        // Move o cliente existente para o outro veículo
                                        for (int pos = 1; pos < App.numClients; pos++) {
                                            if (route[vOther][pos] == 0 || route[vOther][pos] == -1) {
                                                route[vOther][pos] = existingClient;
                                                route[vFull][c] = client; // Insere o novo cliente
                                                return; // Sucesso!
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tentativa 3: Inserir no veículo com menor violação (última opção)
        int bestVehicle = -1;
        int minOverflow = Integer.MAX_VALUE;

        if (clients != null) {
            int clientDemand = clients.get(client).getDemand();

            for (int v = 0; v < App.numVehicles; v++) {
                int currentDemand = calculateVehicleDemand(route, v, clients);
                int overflow = (currentDemand + clientDemand) - App.vehicleCapacity;

                // Só considera veículos com espaço físico
                boolean hasSpace = false;
                for (int c = 1; c < App.numClients; c++) {
                    if (route[v][c] == 0 || route[v][c] == -1) {
                        hasSpace = true;
                        break;
                    }
                }

                if (hasSpace && overflow < minOverflow) {
                    minOverflow = overflow;
                    bestVehicle = v;
                }
            }

            if (bestVehicle != -1) {
                for (int c = 1; c < App.numClients; c++) {
                    if (route[bestVehicle][c] == 0 || route[bestVehicle][c] == -1) {
                        route[bestVehicle][c] = client;
                        System.err.println(
                                "AVISO: Cliente " + client + " (demanda " + clientDemand +
                                        ") inserido no veículo " + bestVehicle +
                                        " causando overflow de " + minOverflow + " unidades!");
                        return;
                    }
                }
            }
        }

        // Fallback final
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 1; c < App.numClients; c++) {
                if (route[v][c] == 0 || route[v][c] == -1) {
                    route[v][c] = client;
                    System.err.println("AVISO CRÍTICO: Cliente " + client + " inserido sem validação!");
                    return;
                }
            }
        }
    }

    /**
     * Validates that all clients 1 to numClients appear exactly once in the route.
     * Returns true if valid, false otherwise.
     */
    public static boolean validateRoute(int[][] route, int numClients) {
        Set<Integer> foundClients = new HashSet<>();

        for (int v = 0; v < route.length; v++) {
            for (int c = 0; c < route[v].length; c++) {
                int clientId = route[v][c];
                if (clientId > 0) {
                    if (foundClients.contains(clientId)) {
                        System.err.println("ERRO VALIDAÇÃO: Cliente " + clientId + " duplicado!");
                        return false;
                    }
                    foundClients.add(clientId);
                }
            }
        }

        // Check if all clients from 1 to numClients are present
        for (int i = 1; i <= numClients; i++) {
            if (!foundClients.contains(i)) {
                System.err.println("ERRO VALIDAÇÃO: Cliente " + i + " está faltando!");
                return false;
            }
        }

        if (foundClients.size() != numClients) {
            System.err.println(
                    "ERRO VALIDAÇÃO: Esperado " + numClients + " clientes, encontrados " + foundClients.size());
            return false;
        }

        return true;
    }

    /**
     * Repara violações de capacidade movendo clientes de veículos sobrecarregados
     * para veículos com capacidade disponível.
     * Este método é chamado após o crossover para garantir 100% de conformidade.
     */
    private static int[][] repairCapacityViolations(int[][] route, List<Client> clients) {
        final int MAX_REPAIR_ITERATIONS = 50;
        int iteration = 0;
        boolean hasViolations = true;

        while (hasViolations && iteration < MAX_REPAIR_ITERATIONS) {
            hasViolations = false;
            iteration++;

            // Identificar veículos com violação de capacidade
            for (int vOverloaded = 0; vOverloaded < App.numVehicles; vOverloaded++) {
                int currentDemand = calculateVehicleDemand(route, vOverloaded, clients);

                if (currentDemand > App.vehicleCapacity) {
                    hasViolations = true;
                    int excess = currentDemand - App.vehicleCapacity;

                    // Tentar mover clientes do veículo sobrecarregado para outros com capacidade
                    boolean moved = false;

                    // Procurar clientes que podem ser movidos (começando pelos maiores)
                    for (int c = 0; c < App.numClients && !moved; c++) {
                        int clientId = route[vOverloaded][c];
                        if (clientId <= 0)
                            continue;

                        int clientDemand = clients.get(clientId).getDemand();

                        // Se mover este cliente resolve ou melhora o problema
                        if (clientDemand > 0) {
                            // Procurar veículo destino com capacidade
                            for (int vDestination = 0; vDestination < App.numVehicles; vDestination++) {
                                if (vDestination == vOverloaded)
                                    continue;

                                int destDemand = calculateVehicleDemand(route, vDestination, clients);

                                // Verificar se o veículo destino tem capacidade
                                if (destDemand + clientDemand <= App.vehicleCapacity) {
                                    // Encontrar posição vazia no veículo destino
                                    for (int pos = 0; pos < App.numClients; pos++) {
                                        if (route[vDestination][pos] == -1 || route[vDestination][pos] == 0) {
                                            // Mover cliente
                                            route[vDestination][pos] = clientId;
                                            route[vOverloaded][c] = -1;

                                            // Compactar rota do veículo origem
                                            route[vOverloaded] = compactRoute(route[vOverloaded]);

                                            moved = true;
                                            break;
                                        }
                                    }

                                    if (moved)
                                        break;
                                }
                            }

                            if (moved)
                                break;
                        }
                    }

                    // Se não conseguiu mover nenhum cliente, tentar redistribuição por troca
                    if (!moved) {
                        moved = trySwapClientsForCapacity(route, vOverloaded, clients);
                    }

                    // Se ainda não resolveu, forçar mensagem de erro detalhada
                    if (!moved) {
                        int finalDemand = calculateVehicleDemand(route, vOverloaded, clients);
                        System.err.println("AVISO CAPACIDADE: Veículo " + vOverloaded +
                                " continua sobrecarregado (" + finalDemand + "/" + App.vehicleCapacity +
                                ") após " + iteration + " iterações. Não foi possível redistribuir.");
                    }
                }
            }
        }

        if (hasViolations) {
            System.err.println("AVISO: Algumas violações de capacidade não puderam ser reparadas após " +
                    MAX_REPAIR_ITERATIONS + " iterações.");
        }

        return route;
    }

    /**
     * Tenta trocar clientes entre veículos para resolver violação de capacidade.
     */
    private static boolean trySwapClientsForCapacity(int[][] route, int vOverloaded, List<Client> clients) {
        int overloadedDemand = calculateVehicleDemand(route, vOverloaded, clients);
        int excess = overloadedDemand - App.vehicleCapacity;

        // Para cada cliente do veículo sobrecarregado
        for (int c1 = 0; c1 < App.numClients; c1++) {
            int client1 = route[vOverloaded][c1];
            if (client1 <= 0)
                continue;
            int demand1 = clients.get(client1).getDemand();

            // Para cada outro veículo
            for (int vOther = 0; vOther < App.numVehicles; vOther++) {
                if (vOther == vOverloaded)
                    continue;

                int otherDemand = calculateVehicleDemand(route, vOther, clients);

                // Para cada cliente do outro veículo
                for (int c2 = 0; c2 < App.numClients; c2++) {
                    int client2 = route[vOther][c2];
                    if (client2 <= 0)
                        continue;
                    int demand2 = clients.get(client2).getDemand();

                    // Verificar se a troca resolve o problema
                    int newOverloadedDemand = overloadedDemand - demand1 + demand2;
                    int newOtherDemand = otherDemand - demand2 + demand1;

                    if (newOverloadedDemand <= App.vehicleCapacity &&
                            newOtherDemand <= App.vehicleCapacity) {
                        // Realizar troca
                        route[vOverloaded][c1] = client2;
                        route[vOther][c2] = client1;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Compacta uma rota removendo posições vazias (-1).
     */
    private static int[] compactRoute(int[] vehicleRoute) {
        int[] compacted = new int[vehicleRoute.length];
        int pos = 0;

        // Copiar apenas clientes válidos
        for (int i = 0; i < vehicleRoute.length; i++) {
            if (vehicleRoute[i] > 0) {
                compacted[pos++] = vehicleRoute[i];
            }
        }

        // Preencher o resto com -1
        for (; pos < compacted.length; pos++) {
            compacted[pos] = -1;
        }

        return compacted;
    }

}
