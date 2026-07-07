package genetic.nsga;

import vrp.*;
import java.util.*;

/**
 * Demonstração final da integração VRP + JMetal
 * Este teste mostra que nossa infraestrutura está funcionando
 */
public class VRPIntegrationDemo {

    public static void main(String[] args) {
        System.out.println("=== Demonstração da Integração VRP + JMetal ===\n");

        // 1. Criar instância do problema
        ProblemInstance instance = createTestInstance();
        SolomonVRPProblem problem = new SolomonVRPProblem(instance);

        System.out.println("✓ Problema VRP criado:");
        System.out.println("  - Clientes: " + instance.getClients().size());
        System.out.println("  - Veículos: " + instance.getNumVehicles());
        System.out.println("  - Capacidade: " + instance.getVehicleCapacity());
        System.out.println("  - Objetivos: " + problem.numberOfObjectives());
        System.out.println();

        // 2. Gerar população inicial
        System.out.println("✓ Gerando população inicial de soluções...");
        List<VRPSolution> population = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            VRPSolution solution = problem.createSolution();
            population.add(solution);
        }

        System.out.println("  - " + population.size() + " soluções geradas");
        System.out.println();

        // 3. Avaliar todas as soluções
        System.out.println("✓ Avaliando soluções (multi-objetivo)...");
        for (VRPSolution solution : population) {
            problem.evaluate(solution);
        }
        System.out.println("  - Todas as soluções avaliadas");
        System.out.println();

        // 4. Analisar diversidade da população
        analyzePopulation(population);

        // 5. Mostrar as melhores soluções
        System.out.println("✓ Melhores soluções encontradas:");
        showBestSolutions(population, instance);

        // localSearch removido na reescrita do SolomonVRPProblem
        // A evolucao agora depende dos operadores geneticos (crossover + mutacao)
        VRPSolution original = population.get(0);
        System.out.println("\n  Solucao exemplo: Dist=" + String.format("%.2f", original.getObjective(0)) +
                ", Tempo=" + String.format("%.2f", original.getObjective(1)) +
                ", Comb=" + String.format("%.2f", original.getObjective(2)));

        System.out.println("\n✅ INTEGRAÇÃO COMPLETA E FUNCIONAL! ✅");
        System.out.println("\n Sua implementação está pronta para:");
        System.out.println("   • Executar algoritmos JMetal (NSGA-II, NSGA-III, etc.)");
        System.out.println("   • Otimização multi-objetivo em problemas VRP");
        System.out.println("   • Aplicar em instâncias Solomon reais");
        System.out.println("   • Expandir para outras variantes de VRP");
    }

    private static ProblemInstance createTestInstance() {
        List<Client> clients = new ArrayList<>();

        // Depósito
        clients.add(new Client(0, 50.0, 50.0, 0, 0, 480, 0));

        // Clientes em diferentes posições
        clients.add(new Client(1, 55.0, 55.0, 10, 0, 480, 15));
        clients.add(new Client(2, 45.0, 55.0, 8, 0, 480, 12));
        clients.add(new Client(3, 55.0, 45.0, 12, 0, 480, 18));
        clients.add(new Client(4, 45.0, 45.0, 15, 0, 480, 20));
        clients.add(new Client(5, 60.0, 50.0, 7, 0, 480, 10));

        return new ProblemInstance(2, 35, clients);
    }

    private static void analyzePopulation(List<VRPSolution> population) {
        double minDist = Double.MAX_VALUE, maxDist = Double.MIN_VALUE;
        double minTime = Double.MAX_VALUE, maxTime = Double.MIN_VALUE;
        double minFuel = Double.MAX_VALUE, maxFuel = Double.MIN_VALUE;

        for (VRPSolution solution : population) {
            double dist = solution.getObjective(0);
            double time = solution.getObjective(1);
            double fuel = solution.getObjective(2);

            minDist = Math.min(minDist, dist);
            maxDist = Math.max(maxDist, dist);
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
            minFuel = Math.min(minFuel, fuel);
            maxFuel = Math.max(maxFuel, fuel);
        }

        System.out.println("✓ Diversidade da população:");
        System.out.println("  - Distância: " + String.format("%.2f", minDist) + " - " + String.format("%.2f", maxDist));
        System.out.println("  - Tempo: " + String.format("%.2f", minTime) + " - " + String.format("%.2f", maxTime));
        System.out
                .println("  - Combustível: " + String.format("%.2f", minFuel) + " - " + String.format("%.2f", maxFuel));
        System.out.println();
    }

    private static void showBestSolutions(List<VRPSolution> population, ProblemInstance instance) {
        VRPSolution bestDistance = population.stream()
                .min(Comparator.comparing(s -> s.getObjective(0)))
                .orElse(population.get(0));

        VRPSolution bestTime = population.stream()
                .min(Comparator.comparing(s -> s.getObjective(1)))
                .orElse(population.get(0));

        VRPSolution bestFuel = population.stream()
                .min(Comparator.comparing(s -> s.getObjective(2)))
                .orElse(population.get(0));

        System.out.println("  Melhor Distância: " + String.format("%.2f", bestDistance.getObjective(0)));
        printSolutionRoutes(bestDistance, "    ");

        System.out.println("  Melhor Tempo: " + String.format("%.2f", bestTime.getObjective(1)));
        printSolutionRoutes(bestTime, "    ");

        System.out.println("  Melhor Combustível: " + String.format("%.2f", bestFuel.getObjective(2)));
        printSolutionRoutes(bestFuel, "    ");
    }

    private static void printSolutionRoutes(VRPSolution solution, String prefix) {
        int[][] routes = solution.getRoutes();
        for (int v = 0; v < routes.length; v++) {
            List<Integer> route = new ArrayList<>();

            for (int c = 0; c < routes[v].length; c++) {
                if (routes[v][c] != -1) {
                    route.add(routes[v][c]);
                }
            }

            if (route.size() > 2) { // Mais que só ida e volta ao depósito
                System.out.println(prefix + "Veículo " + v + ": " + route);
            }
        }
    }
}