package genetic.nsga;

import vrp.*;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.NSGAIIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Versão simplificada do executor do NSGA-III para teste inicial
 */
public class SimpleVRPRunner {

    // Parâmetros reduzidos para teste
    private static final int POPULATION_SIZE = 20;
    private static final int MAX_EVALUATIONS = 500;
    private static final double CROSSOVER_PROBABILITY = 0.8;
    private static final double MUTATION_PROBABILITY = 0.2;

    public static void main(String[] args) {
        System.out.println("=== Teste Simples do NSGA-III para VRP ===\n");

        try {
            // Criar instância pequena para teste
            ProblemInstance instance = createSimpleProblemInstance();
            SolomonVRPProblem problem = new SolomonVRPProblem(instance);

            System.out.println("Problema: " + instance.getClients().size() + " clientes, " +
                    instance.getNumVehicles() + " veículos");
            System.out.println("População: " + POPULATION_SIZE);
            System.out.println("Avaliações: " + MAX_EVALUATIONS + "\n");

            // Usar operadores mais simples
            CrossoverOperator<VRPSolution> crossover = new VRPCrossoverOperator(CROSSOVER_PROBABILITY);
            MutationOperator<VRPSolution> mutation = new VRPMutationOperator(MUTATION_PROBABILITY);
            SelectionOperator<List<VRPSolution>, VRPSolution> selection = new BinaryTournamentSelection<>(
                    new RankingAndCrowdingDistanceComparator<>());

            // Configurar NSGA-III
            Algorithm<List<VRPSolution>> algorithm = new NSGAIIIBuilder<>(problem)
                    .setCrossoverOperator(crossover)
                    .setMutationOperator(mutation)
                    .setSelectionOperator(selection)
                    .setMaxIterations(MAX_EVALUATIONS / POPULATION_SIZE)
                    .setPopulationSize(POPULATION_SIZE)
                    .build();

            System.out.println("Executando algoritmo...");
            long startTime = System.currentTimeMillis();
            algorithm.run();
            long endTime = System.currentTimeMillis();

            List<VRPSolution> population = algorithm.getResult();

            System.out.println("\n=== Resultados ===");
            System.out.println("Tempo: " + (endTime - startTime) + " ms");
            System.out.println("Soluções encontradas: " + population.size());

            // Mostrar as 3 melhores soluções para cada objetivo
            showBestSolutions(population);

            // Salvar resultados
            saveResults(population, "simple_vrp_results.txt");

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ProblemInstance createSimpleProblemInstance() {
        List<Client> clients = new ArrayList<>();

        // Depósito
        clients.add(new Client(0, 25.0, 25.0, 0, 0, 480, 0));

        // 6 clientes em posições simples
        clients.add(new Client(1, 30.0, 25.0, 10, 0, 480, 15));
        clients.add(new Client(2, 20.0, 25.0, 8, 0, 480, 12));
        clients.add(new Client(3, 25.0, 30.0, 12, 0, 480, 18));
        clients.add(new Client(4, 25.0, 20.0, 15, 0, 480, 20));
        clients.add(new Client(5, 35.0, 30.0, 7, 0, 480, 10));
        clients.add(new Client(6, 15.0, 20.0, 9, 0, 480, 14));

        return new ProblemInstance(2, 40, clients); // 2 veículos, capacidade 40
    }

    private static void showBestSolutions(List<VRPSolution> population) {
        if (population.isEmpty()) {
            System.out.println("Nenhuma solução encontrada!");
            return;
        }

        VRPSolution bestDistance = population.stream()
                .min(Comparator.comparing(s -> s.getObjective(0)))
                .orElse(population.get(0));

        VRPSolution bestTime = population.stream()
                .min(Comparator.comparing(s -> s.getObjective(1)))
                .orElse(population.get(0));

        VRPSolution bestFuel = population.stream()
                .min(Comparator.comparing(s -> s.getObjective(2)))
                .orElse(population.get(0));

        System.out.println("\nMelhor Distância: " +
                String.format("%.2f", bestDistance.getObjective(0)) +
                " (Tempo: " + String.format("%.2f", bestDistance.getObjective(1)) +
                ", Combustível: " + String.format("%.2f", bestDistance.getObjective(2)) + ")");

        System.out.println("Melhor Tempo: " +
                String.format("%.2f", bestTime.getObjective(1)) +
                " (Distância: " + String.format("%.2f", bestTime.getObjective(0)) +
                ", Combustível: " + String.format("%.2f", bestTime.getObjective(2)) + ")");

        System.out.println("Melhor Combustível: " +
                String.format("%.2f", bestFuel.getObjective(2)) +
                " (Distância: " + String.format("%.2f", bestFuel.getObjective(0)) +
                ", Tempo: " + String.format("%.2f", bestFuel.getObjective(1)) + ")");

        // Mostrar as rotas da melhor solução por distância
        System.out.println("\nRotas da melhor solução (distância):");
        printRoutes(bestDistance);
    }

    private static void printRoutes(VRPSolution solution) {
        int[][] routes = solution.getRoutes();
        for (int v = 0; v < routes.length; v++) {
            System.out.print("Veículo " + v + ": ");
            List<Integer> route = new ArrayList<>();

            for (int c = 0; c < routes[v].length; c++) {
                if (routes[v][c] != -1) {
                    route.add(routes[v][c]);
                }
            }
            System.out.println(route);
        }
    }

    private static void saveResults(List<VRPSolution> population, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("# Simple VRP NSGA-III Results\n");
            writer.write("# Distance Time Fuel\n");

            for (VRPSolution solution : population) {
                writer.write(String.format("%.4f %.4f %.4f\n",
                        solution.getObjective(0),
                        solution.getObjective(1),
                        solution.getObjective(2)));
            }

            System.out.println("\nResultados salvos em: " + filename);
        } catch (IOException e) {
            System.err.println("Erro ao salvar: " + e.getMessage());
        }
    }
}