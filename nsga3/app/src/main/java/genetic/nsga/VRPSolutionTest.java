package genetic.nsga;

import genetic.Individual;
import main.App;
import vrp.Client;
import java.util.ArrayList;
import java.util.List;

public class VRPSolutionTest {

    public static void main(String[] args) {

        // Configurar parâmetros básicos
        App.numVehicles = 3;
        App.numClients = 6; // Incluindo depósito (0)
        App.vehicleCapacity = 100;

        // Criar uma lista de clientes de teste
        List<Client> clients = createTestClients();

        // Teste 1: Criar VRPSolution diretamente
        System.out.println("\n1. Testando criação direta da VRPSolution:");
        VRPSolution solution1 = new VRPSolution(App.numVehicles, App.numClients, 3);

        // Definir rotas de exemplo
        solution1.setClientInRoute(0, 0, 0); // Veículo 0: depósito
        solution1.setClientInRoute(0, 1, 1); // Veículo 0: cliente 1
        solution1.setClientInRoute(0, 2, 2); // Veículo 0: cliente 2
        solution1.setClientInRoute(0, 3, 0); // Veículo 0: volta ao depósito

        solution1.setClientInRoute(1, 0, 0); // Veículo 1: depósito
        solution1.setClientInRoute(1, 1, 3); // Veículo 1: cliente 3
        solution1.setClientInRoute(1, 2, 0); // Veículo 1: volta ao depósito

        solution1.setClientInRoute(2, 0, 0); // Veículo 2: depósito
        solution1.setClientInRoute(2, 1, 4); // Veículo 2: cliente 4
        solution1.setClientInRoute(2, 2, 5); // Veículo 2: cliente 5
        solution1.setClientInRoute(2, 3, 0); // Veículo 2: volta ao depósito

        // Definir objetivos de exemplo
        solution1.setObjective(0, 120.5); // Distância
        solution1.setObjective(1, 85.3); // Tempo
        solution1.setObjective(2, 45.7); // Combustível

        solution1.printRoutes();

        // Teste 2: Criar Individual e converter para VRPSolution
        System.out.println("\n2. Testando conversão Individual → VRPSolution:");
        Individual individual = createTestIndividual();
        VRPSolution solution2 = new VRPSolution(individual, 3);

        solution2.printRoutes();

        // Teste 3: Converter VRPSolution de volta para Individual
        System.out.println("\n3. Testando conversão VRPSolution → Individual:");
        Individual convertedIndividual = solution2.toIndividual();
        convertedIndividual.printRoutes();

        // Teste 4: Verificar métodos do JMetal
        System.out.println("\n4. Testando métodos do JMetal:");
        System.out.println("Número de variáveis: " + solution1.numberOfVariables());
        System.out.println("Número de objetivos: " + solution1.numberOfObjectives());
        System.out.println("Variáveis: " + solution1.variables().size() + " elementos");
        System.out.println("Objetivos: [" + solution1.objectives()[0] + ", " +
                solution1.objectives()[1] + ", " + solution1.objectives()[2] + "]");

        // Teste 5: Teste de cópia
        System.out.println("\n5. Testando método copy():");
        VRPSolution copy = (VRPSolution) solution1.copy();
        copy.printRoutes();

        System.out.println("\n=== TESTE CONCLUÍDO COM SUCESSO! ===");
    }

    private static List<Client> createTestClients() {
        List<Client> clients = new ArrayList<>();

        // Cliente 0 (depósito)
        clients.add(new Client(0, 0.0, 0.0, 0, 0, 1000, 0));

        // Clientes de teste
        clients.add(new Client(1, 10.0, 20.0, 15, 0, 500, 10));
        clients.add(new Client(2, 30.0, 40.0, 20, 0, 600, 15));
        clients.add(new Client(3, 50.0, 10.0, 10, 0, 400, 5));
        clients.add(new Client(4, 20.0, 60.0, 25, 0, 700, 20));
        clients.add(new Client(5, 40.0, 30.0, 12, 0, 800, 8));

        return clients;
    }

    private static Individual createTestIndividual() {
        Individual individual = new Individual(1, 150.0, 120.5, 85.3, 45.7);

        // Definir rotas de exemplo
        individual.setClientInRoute(0, 0, 0); // Veículo 0: depósito
        individual.setClientInRoute(0, 1, 1); // Veículo 0: cliente 1
        individual.setClientInRoute(0, 2, 3); // Veículo 0: cliente 3
        individual.setClientInRoute(0, 3, 0); // Veículo 0: volta ao depósito

        individual.setClientInRoute(1, 0, 0); // Veículo 1: depósito
        individual.setClientInRoute(1, 1, 2); // Veículo 1: cliente 2
        individual.setClientInRoute(1, 2, 0); // Veículo 1: volta ao depósito

        individual.setClientInRoute(2, 0, 0); // Veículo 2: depósito
        individual.setClientInRoute(2, 1, 4); // Veículo 2: cliente 4
        individual.setClientInRoute(2, 2, 5); // Veículo 2: cliente 5
        individual.setClientInRoute(2, 3, 0); // Veículo 2: volta ao depósito

        return individual;
    }
}
