package genetic.nsga;

import vrp.*;
import genetic.DefaultFitnessCalculator;
import genetic.Individual;
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
 * Executor principal do NSGA-III para o problema VRP.
 * Inclui diagnosticos completos de evolucao e populacao.
 */
public class VRPNSGAIIIRunner {

    private static final int POPULATION_SIZE = 600;
    private static final int MAX_ITERATIONS = 3000;
    private static final double CROSSOVER_PROBABILITY = 1.0;
    private static final double MUTATION_PROBABILITY = 0.01;
    // Para 3 objetivos: divisions=33 gera C(35,2)=595 reference points -> pop ~595
    // Alternativa pop~900: divisions=41 gera C(43,2)=903
    private static final int NUMBER_OF_DIVISIONS = 33;

    public static void main(String[] args) {
        System.out.println("=== NSGA-III para Problema de Roteamento de Veiculos ===\n");
        try {
            ProblemInstance instance;
            if (args.length > 0) {
                String instancePath = args[0];
                System.out.println("Carregando instancia Solomon: " + instancePath);
                instance = BenchMarkReader.readInstaces(instancePath);
                System.out.println("Instancia carregada - Clientes: " + (instance.getClients().size() - 1) +
                        ", Veiculos: " + instance.getNumVehicles() +
                        ", Capacidade: " + instance.getVehicleCapacity());
            } else {
                System.out.println("Usando instancia de teste padrao...");
                instance = createProblemInstance();
            }
            runNSGAIII(instance);
        } catch (Exception e) {
            System.err.println("Erro durante a execucao: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void runNSGAIII(ProblemInstance instance) {
        System.out.println("\n=== NSGA-III para Problema de Roteamento de Veiculos ===\n");

        try {
            SolomonVRPProblem problem = new SolomonVRPProblem(instance);

            System.out.println("Configuracao do Problema:");
            System.out.println("- Clientes: " + (instance.getClients().size() - 1) +
                    ", Veiculos: " + instance.getNumVehicles() +
                    ", Capacidade: " + instance.getVehicleCapacity());
            System.out.println("- Objetivos: Distancia, Tempo, Combustivel");
            System.out.println("- Constraints: Capacidade, Time Windows, Cobertura\n");

            // Seed aleatoria
            long seed = System.nanoTime() ^ System.currentTimeMillis() ^ System.identityHashCode(instance);
            org.uma.jmetal.util.pseudorandom.JMetalRandom.getInstance().setSeed(seed);
            System.out.println("DEBUG: Seed JMetal: " + seed);

            // Operadores
            CrossoverOperator<VRPSolution> crossover = new VRPSinglePointCrossover(CROSSOVER_PROBABILITY);
            MutationOperator<VRPSolution> mutation = new VRPBitFlipMutation(MUTATION_PROBABILITY);
            SelectionOperator<List<VRPSolution>, VRPSolution> selection =
                    new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

            // NSGA-III com divisoes configuradas para pop ~900
            Algorithm<List<VRPSolution>> algorithm = new NSGAIIIBuilder<>(problem)
                    .setCrossoverOperator(crossover)
                    .setMutationOperator(mutation)
                    .setSelectionOperator(selection)
                    .setMaxIterations(MAX_ITERATIONS)
                    .setPopulationSize(POPULATION_SIZE)
                    .setNumberOfDivisions(NUMBER_OF_DIVISIONS)
                    .build();

            System.out.println("Configuracao do NSGA-III:");
            System.out.println("- Pop solicitada: " + POPULATION_SIZE);
            System.out.println("- Geracoes: " + MAX_ITERATIONS);
            System.out.println("- Crossover: OnePoint por coluna (Pc=" + CROSSOVER_PROBABILITY + ")");
            System.out.println("- Mutacao: Swap intra+inter rota (Pm=" + MUTATION_PROBABILITY + ")");
            System.out.println("- Selecao: Binary Tournament\n");
            System.out.println("Iniciando execucao...\n");

            long startTime = System.currentTimeMillis();
            algorithm.run();
            long endTime = System.currentTimeMillis();
            long computingTime = endTime - startTime;

            List<VRPSolution> population = algorithm.getResult();

            // === DIAGNOSTICO ===
            System.out.println("\n========================================");
            System.out.println("=== DIAGNOSTICO DA EXECUCAO NSGA-III ===");
            System.out.println("========================================");
            System.out.println("Tempo de execucao: " + computingTime + " ms");
            System.out.println("Solucoes criadas na inicializacao: " + problem.getSolutionCounter());
            System.out.println("Total de avaliacoes: " + problem.getEvaluationCounter());
            System.out.println("Solucoes na frente de Pareto final: " + population.size());
            System.out.println("Solucoes unicas observadas: " + problem.getUniqueSolutionCount());

            int popReal = problem.getSolutionCounter();
            if (popReal > 0 && problem.getEvaluationCounter() > popReal) {
                int geracoesEstimadas = (problem.getEvaluationCounter() - popReal) / popReal;
                System.out.println("Populacao efetiva (real): " + popReal);
                System.out.println("Geracoes estimadas: " + geracoesEstimadas);
            }

            if (popReal < POPULATION_SIZE * 0.5) {
                System.out.println("AVISO: Populacao real (" + popReal +
                        ") muito menor que solicitada (" + POPULATION_SIZE +
                        "). jMetal ajustou para reference points.");
            }

            System.out.println("\nMelhores objetivos ao final:");
            System.out.println("- Dist: " + String.format("%.2f", problem.getBestDistance()));
            System.out.println("- Tempo: " + String.format("%.2f", problem.getBestTime()));
            System.out.println("- Comb: " + String.format("%.2f", problem.getBestFuel()));
            System.out.println("========================================\n");

            // Analise e salvamento
            analyzeResults(population, instance);
            saveNsga3EvolutionLike(population, computingTime, instance, problem);
            saveParetoFrontToFile(population, computingTime, instance);

            System.out.println("\n=== Execucao Concluida com Sucesso! ===");

        } catch (Exception e) {
            System.err.println("Erro durante a execucao: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ProblemInstance createProblemInstance() {
        List<Client> clients = new ArrayList<>();
        clients.add(new Client(0, 50.0, 50.0, 0, 0, 480, 0));
        clients.add(new Client(1, 60.0, 60.0, 10, 0, 480, 15));
        clients.add(new Client(2, 65.0, 65.0, 8, 0, 480, 12));
        clients.add(new Client(3, 58.0, 62.0, 12, 0, 480, 18));
        clients.add(new Client(4, 60.0, 40.0, 15, 0, 480, 20));
        clients.add(new Client(5, 65.0, 35.0, 7, 0, 480, 10));
        clients.add(new Client(6, 62.0, 38.0, 9, 0, 480, 14));
        clients.add(new Client(7, 40.0, 40.0, 11, 0, 480, 16));
        clients.add(new Client(8, 35.0, 35.0, 13, 0, 480, 19));
        clients.add(new Client(9, 38.0, 42.0, 6, 0, 480, 9));
        clients.add(new Client(10, 40.0, 60.0, 14, 0, 480, 22));
        clients.add(new Client(11, 42.0, 65.0, 5, 0, 480, 8));
        clients.add(new Client(12, 38.0, 58.0, 16, 0, 480, 25));
        return new ProblemInstance(4, 50, clients);
    }

    private static void analyzeResults(List<VRPSolution> population, ProblemInstance instance) {
        if (population.isEmpty()) {
            System.out.println("Nenhuma solucao encontrada!");
            return;
        }

        double minDist = Double.MAX_VALUE, minTime = Double.MAX_VALUE, minFuel = Double.MAX_VALUE;
        double maxDist = Double.MIN_VALUE, maxTime = Double.MIN_VALUE, maxFuel = Double.MIN_VALUE;
        VRPSolution bestDistance = population.get(0);
        VRPSolution bestTime = population.get(0);
        VRPSolution bestFuel = population.get(0);

        for (VRPSolution s : population) {
            double d = s.getObjective(0), t = s.getObjective(1), f = s.getObjective(2);
            if (d < minDist) { minDist = d; bestDistance = s; }
            if (t < minTime) { minTime = t; bestTime = s; }
            if (f < minFuel) { minFuel = f; bestFuel = s; }
            maxDist = Math.max(maxDist, d);
            maxTime = Math.max(maxTime, t);
            maxFuel = Math.max(maxFuel, f);
        }

        System.out.println("=== Frente de Pareto ===");
        System.out.println("Dist: [" + String.format("%.2f", minDist) + " - " + String.format("%.2f", maxDist) + "]");
        System.out.println("Tempo: [" + String.format("%.2f", minTime) + " - " + String.format("%.2f", maxTime) + "]");
        System.out.println("Comb: [" + String.format("%.2f", minFuel) + " - " + String.format("%.2f", maxFuel) + "]");

        System.out.println("\nMelhor Distancia:");
        printSolutionDetails(bestDistance, instance);
        System.out.println("\nMelhor Tempo:");
        printSolutionDetails(bestTime, instance);
        System.out.println("\nMelhor Combustivel:");
        printSolutionDetails(bestFuel, instance);
    }

    private static void printSolutionDetails(VRPSolution solution, ProblemInstance instance) {
        System.out.println("Objetivos: Dist=" + String.format("%.2f", solution.getObjective(0)) +
                ", Tempo=" + String.format("%.2f", solution.getObjective(1)) +
                ", Comb=" + String.format("%.2f", solution.getObjective(2)));

        int[][] routes = solution.getRoutes();
        for (int v = 0; v < routes.length; v++) {
            List<Integer> route = new ArrayList<>();
            int totalDemand = 0;
            for (int c = 0; c < routes[v].length; c++) {
                if (routes[v][c] > 0) {
                    route.add(routes[v][c]);
                    totalDemand += instance.getClients().get(routes[v][c]).getDemand();
                }
            }
            if (!route.isEmpty()) {
                System.out.println("  Veiculo " + v + ": " + route +
                        " (Carga: " + totalDemand + "/" + instance.getVehicleCapacity() + ")");
            }
        }
    }

    private static void saveNsga3EvolutionLike(List<VRPSolution> population, long computingTime,
            ProblemInstance instance, SolomonVRPProblem problem) {
        double minDist = population.stream().mapToDouble(s -> s.getObjective(0)).min().orElse(Double.NaN);
        double minTime = population.stream().mapToDouble(s -> s.getObjective(1)).min().orElse(Double.NaN);
        double minFuel = population.stream().mapToDouble(s -> s.getObjective(2)).min().orElse(Double.NaN);
        double avgDist = population.stream().mapToDouble(s -> s.getObjective(0)).average().orElse(Double.NaN);
        double avgTime = population.stream().mapToDouble(s -> s.getObjective(1)).average().orElse(Double.NaN);
        double avgFuel = population.stream().mapToDouble(s -> s.getObjective(2)).average().orElse(Double.NaN);

        List<Client> clients = instance.getClients();
        DefaultFitnessCalculator calc = new DefaultFitnessCalculator();

        double[] weighted = population.stream()
                .mapToDouble(s -> calc.calculateFitness(s.toIndividual(), clients))
                .toArray();
        double minWeighted = Arrays.stream(weighted).min().orElse(Double.NaN);
        double avgWeighted = Arrays.stream(weighted).average().orElse(Double.NaN);

        String dir = "resultsNSGA3";
        String fileName;
        if (!main.App.instanceName.isEmpty()) {
            fileName = dir + "/evo_" + main.App.instanceName + main.App.executionSuffix + ".txt";
        } else {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            fileName = dir + "/evolution_results_" + timestamp + ".txt";
        }

        try {
            java.io.File resultsDir = new java.io.File(dir);
            if (!resultsDir.exists()) resultsDir.mkdirs();

            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write("Subpopulacao\\Geracao\tgFinal\n");
                writer.write(String.format("NSGA3_DistanciaMin\t%.2f\n", minDist));
                writer.write(String.format("NSGA3_TempoMin\t%.2f\n", minTime));
                writer.write(String.format("NSGA3_CombustivelMin\t%.2f\n", minFuel));
                writer.write(String.format("NSGA3_DistanciaMedia\t%.2f\n", avgDist));
                writer.write(String.format("NSGA3_TempoMedio\t%.2f\n", avgTime));
                writer.write(String.format("NSGA3_CombustivelMedio\t%.2f\n", avgFuel));
                writer.write(String.format("NSGA3_FitnessPonderadoMin\t%.2f\n", minWeighted));
                writer.write(String.format("NSGA3_FitnessPonderadoMedio\t%.2f\n", avgWeighted));
                writer.write(String.format("NSGA3_TamanhoFrente\t%d\n", population.size()));
                writer.write(String.format("TempoExecucao(ms)\t%d\n", computingTime));
                writer.write(String.format("Parametros\tPop=%d; Geracoes=%d; Pc=%.2f; Pm=%.2f\n",
                        POPULATION_SIZE, MAX_ITERATIONS, CROSSOVER_PROBABILITY, MUTATION_PROBABILITY));

                // Diagnostico
                writer.write(String.format("\n=== DIAGNOSTICO ===\n"));
                writer.write(String.format("Solucoes criadas: %d\n", problem.getSolutionCounter()));
                writer.write(String.format("Total avaliacoes: %d\n", problem.getEvaluationCounter()));
                writer.write(String.format("Solucoes unicas: %d\n", problem.getUniqueSolutionCount()));

                // Historico de evolucao
                List<double[]> history = problem.getEvolutionHistory();
                if (!history.isEmpty()) {
                    writer.write("\n=== HISTORICO DE EVOLUCAO ===\n");
                    writer.write("Eval\tBestDist\tBestTime\tBestFuel\n");
                    for (double[] snapshot : history) {
                        writer.write(String.format("%.0f\t%.2f\t%.2f\t%.2f\n",
                                snapshot[0], snapshot[1], snapshot[2], snapshot[3]));
                    }
                }

                // Melhor solucao
                writer.write("\n=== MELHOR SOLUCAO (Fitness Ponderado Minimo) ===\n");
                VRPSolution bestSolution = population.stream()
                        .min(Comparator.comparingDouble(s -> calc.calculateFitness(s.toIndividual(), clients)))
                        .orElse(null);

                if (bestSolution != null) {
                    Individual bestInd = bestSolution.toIndividual();
                    int[][] route = bestInd.getRoute();

                    writer.write(String.format("Fitness Ponderado: %.6f\n", calc.calculateFitness(bestInd, clients)));
                    writer.write(String.format("Distancia: %.2f\n", bestSolution.getObjective(0)));
                    writer.write(String.format("Tempo: %.2f\n", bestSolution.getObjective(1)));
                    writer.write(String.format("Combustivel: %.2f\n\n", bestSolution.getObjective(2)));

                    for (int v = 0; v < main.App.numVehicles; v++) {
                        List<Integer> vehicleClients = new ArrayList<>();
                        int totalDemand = 0;
                        for (int c = 0; c < route[v].length; c++) {
                            int clientId = route[v][c];
                            if (clientId > 0 && clientId < clients.size()) {
                                vehicleClients.add(clientId);
                                totalDemand += clients.get(clientId).getDemand();
                            }
                        }
                        if (!vehicleClients.isEmpty()) {
                            writer.write(String.format("Veiculo %d: %d clientes, demanda: %d/%d, rota: %s\n",
                                    v, vehicleClients.size(), totalDemand, main.App.vehicleCapacity,
                                    vehicleClients.toString()));
                        }
                    }
                }
            }
            System.out.println("Resultados salvos em: " + fileName);
        } catch (IOException e) {
            System.err.println("Erro ao salvar resultados: " + e.getMessage());
        }
    }

    private static void saveParetoFrontToFile(List<VRPSolution> population, long computingTime,
            ProblemInstance instance) {
        String dir = "resultsNSGA3";
        String fileName;
        if (!main.App.instanceName.isEmpty()) {
            fileName = dir + "/pareto_" + main.App.instanceName + main.App.executionSuffix + ".txt";
        } else {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            fileName = dir + "/pareto_front_" + timestamp + ".txt";
        }
        try {
            java.io.File resultsDir = new java.io.File(dir);
            if (!resultsDir.exists()) resultsDir.mkdirs();

            List<Client> clients = instance.getClients();
            DefaultFitnessCalculator calc = new DefaultFitnessCalculator();

            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write("# VRP NSGA-III - Frente de Pareto\n");
                writer.write("# Tempo: " + computingTime + " ms | Tamanho: " + population.size() + "\n");
                writer.write("# Formato: Distancia Tempo Combustivel FitnessPonderado\n\n");

                for (VRPSolution solution : population) {
                    double fitnessPond = calc.calculateFitness(solution.toIndividual(), clients);
                    writer.write(String.format("%.6f %.6f %.6f %.6f\n",
                            solution.getObjective(0), solution.getObjective(1),
                            solution.getObjective(2), fitnessPond));
                }
            }
            System.out.println("Frente de Pareto salva em: " + fileName);
        } catch (IOException e) {
            System.err.println("Erro ao salvar frente de Pareto: " + e.getMessage());
        }
    }
}
