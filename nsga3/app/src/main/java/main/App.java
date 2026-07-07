package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import vrp.BenchMarkReader;
import vrp.Client;
import vrp.ProblemInstance;
import genetic.Crossover;
import genetic.DefaultFitnessCalculator;
import genetic.DistanceFitnessCalculator;
import genetic.FuelFitnessCalculator;
import genetic.Individual;
import genetic.Mutation;
import genetic.Population;
import genetic.SelectionUtils;
import genetic.TimeFitnessCalculator;
import genetic.nsga.VRPNSGAIIIRunner;

public class App {
    // VRP Variables
    public static int numVehicles;
    public static int vehicleCapacity;
    public static int numClients;
    public static List<Client> clients;
    public static int VEHICLE_SPEED = 1; // Solomon standard: distance = time
    public static int NUM_FUEL_TYPES = 3;
    // Calibracao para frota VUC (Veiculo Urbano de Carga) homogenea
    // Precos: media ANP 2026 (1o trimestre)
    // Consumos: ciclo urbano VUC, com razao etanol/gasolina ~70% (poder calorifico)
    public static double G_FUEL_PRICE = 5.80;        // R$/L
    public static double E_FUEL_PRICE = 4.10;        // R$/L
    public static double D_FUEL_PRICE = 6.20;        // R$/L (diesel S-10)
    public static double G_FUEL_CONSUMPTION = 8.5;   // km/L
    public static double E_FUEL_CONSUMPTION = 6.0;   // km/L (~70% da gasolina)
    public static double D_FUEL_CONSUMPTION = 7.0;   // km/L (VUC urbano)
    public static double WEIGHT_NUM_VEHICLES = 0.25;
    public static double WEIGHT_NUM_VIOLATIONS = 0.5;
    public static double WEIGHT_TOTAL_COST = 0.75;

    // EAs Variables
    public static int pop_size = 900;
    public static int sub_pop_size = (int) Math.floor((double) pop_size / 3);
    public static double elitismRate = 0.1;
    public static int QUANTITYSELECTEDTOURNAMENT = 2;
    public static int tournamentSize = 2;
    public static double mutationRate = 0.01; // 1% mutation rate
    public static double interRouteMutationRate = 1.0; // Always apply inter-route mutation when mutation occurs
    public static int numGenerations = 3000;
    public static int nextIndividualId = pop_size; // Inicializa com pop_size

    // Variável estática para armazenar o nome da instância (sem extensão)
    public static String instanceName = "";

    // Sufixo único para diferenciar execuções (ex: "_exec01")
    public static String executionSuffix = "";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        int algorithmChoice = 4; // Default: NSGA-III
        int instanceTypeChoice = 1; // Default: Solomon
        int instanceChoice = 0;

        // Se argumentos foram passados via linha de comando
        if (args.length > 0) {
            try {
                instanceChoice = Integer.parseInt(args[0]);

                // Verificar se foi passado o tipo de algoritmo (argumento 2)
                if (args.length > 1) {
                    algorithmChoice = Integer.parseInt(args[1]);
                }

                // Verificar se foi passado o tipo de instância (argumento 3)
                if (args.length > 2) {
                    instanceTypeChoice = Integer.parseInt(args[2]);
                }

                // Verificar se foi passado o sufixo de execução (argumento 4)
                if (args.length > 3) {
                    executionSuffix = args[3];
                }

                System.out.println("Executando em modo automático:");
                System.out.println("  Instância: " + instanceChoice);
                System.out.println("  Algoritmo: " + (algorithmChoice == 4 ? "NSGA-III"
                        : algorithmChoice == 1 ? "Multi-Objetivo" : algorithmChoice == 2 ? "Mono-Objetivo" : "Debug"));
                System.out.println("  Tipo: " + (instanceTypeChoice == 1 ? "Solomon" : "Gehring-Homberg"));

            } catch (NumberFormatException e) {
                System.out.println("Erro: Argumento inválido. Use um número de instância.");
                scanner.close();
                return;
            }
        } else {
            // Menu interativo (modo original)
            System.out.println("=== MENU DE SELEÇÃO DE ALGORITMO ===");
            System.out.println("1 - Algoritmo Multi-Objetivo");
            System.out.println("2 - Algoritmo Mono-Objetivo");
            System.out.println("3 - Algoritmo Passo a Passo (para depuração)");
            System.out.println("4 - NSGA-III (JMetal)");
            System.out.print("Digite sua escolha (1, 2, 3 ou 4): ");

            while (algorithmChoice != 1 && algorithmChoice != 2 && algorithmChoice != 3 && algorithmChoice != 4) {
                try {
                    algorithmChoice = Integer.parseInt(scanner.nextLine().trim());

                    if (algorithmChoice != 1 && algorithmChoice != 2 && algorithmChoice != 3 && algorithmChoice != 4) {
                        System.out.print("Opção inválida. Digite 1, 2, 3 ou 4: ");
                    }
                } catch (NumberFormatException e) {
                    System.out.print("Entrada inválida. Digite 1, 2, 3 ou 4: ");
                }
            }

            // Menu para escolher o tipo de instância
            System.out.println("\n=== MENU DE SELEÇÃO DE TIPO DE INSTÂNCIA ===");
            System.out.println("1 - Instâncias Solomon");
            System.out.println("2 - Instâncias Gehring-Homberg");
            System.out.print("Escolha o tipo de instância (1 ou 2): ");

            while (instanceTypeChoice != 1 && instanceTypeChoice != 2) {
                try {
                    instanceTypeChoice = Integer.parseInt(scanner.nextLine().trim());

                    if (instanceTypeChoice != 1 && instanceTypeChoice != 2) {
                        System.out.print("Opção inválida. Digite 1 ou 2: ");
                    }
                } catch (NumberFormatException e) {
                    System.out.print("Entrada inválida. Digite 1 ou 2: ");
                }
            }
        }

        // Seleciona o diretório de instâncias com base na escolha
        String baseInstancePath;
        if (instanceTypeChoice == 1) {
            baseInstancePath = "instances/solomon/";
            System.out.println("\nTipo selecionado: Instâncias Solomon");
        } else {
            baseInstancePath = "instances/gehring_homberg/";
            System.out.println("\nTipo selecionado: Instâncias Gehring-Homberg");
        }

        // Menu para escolher a instância
        System.out.println("\n=== MENU DE SELEÇÃO DE INSTÂNCIA ===");

        // Tentar diferentes caminhos possíveis
        String[] possiblePaths = {
                "app/src/main/java/" + baseInstancePath,
                "src/main/java/" + baseInstancePath,
                baseInstancePath
        };

        File folder = null;
        String instancePath = null;

        for (String path : possiblePaths) {
            File testFolder = new File(path);
            if (testFolder.exists() && testFolder.isDirectory()) {
                folder = testFolder;
                instancePath = path;
                break;
            }
        }

        if (folder == null) {
            folder = new File(possiblePaths[0]);
            instancePath = possiblePaths[0];
        }

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Erro: Diretório de instâncias não encontrado: " + instancePath);
            scanner.close();
            return;
        }

        File[] instanceFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (instanceFiles == null || instanceFiles.length == 0) {
            System.out.println("Erro: Nenhuma instância encontrada no diretório: " + instancePath);
            scanner.close();
            return;
        }

        // Ordenar arquivos alfabeticamente
        Arrays.sort(instanceFiles, Comparator.comparing(File::getName));

        // Se não foi passado argumento, mostrar menu
        if (args.length == 0) {
            // Mostrar as instâncias disponíveis
            for (int i = 0; i < instanceFiles.length; i++) {
                System.out.println((i + 1) + " - " + instanceFiles[i].getName());
            }

            System.out.print("Escolha uma instância (1-" + instanceFiles.length + "): ");
            while (instanceChoice < 1 || instanceChoice > instanceFiles.length) {
                try {
                    instanceChoice = Integer.parseInt(scanner.nextLine().trim());

                    if (instanceChoice < 1 || instanceChoice > instanceFiles.length) {
                        System.out.print("Opção inválida. Digite um número entre 1 e " + instanceFiles.length + ": ");
                    }
                } catch (NumberFormatException e) {
                    System.out.print("Entrada inválida. Digite um número entre 1 e " + instanceFiles.length + ": ");
                }
            }
        } else {
            // Validar instanceChoice passado via argumento
            if (instanceChoice < 1 || instanceChoice > instanceFiles.length) {
                System.out.println("Erro: Número de instância inválido. Deve estar entre 1 e " + instanceFiles.length);
                scanner.close();
                return;
            }
        }

        String selectedInstancePath = instanceFiles[instanceChoice - 1].getPath();
        String selectedFileName = instanceFiles[instanceChoice - 1].getName();

        // Extrair nome da instância sem extensão (ex: "C101" de "C101.txt")
        instanceName = selectedFileName.toLowerCase().replaceAll("\\.txt$", "");

        System.out.println("\nInstância selecionada: " + selectedFileName);

        // Fechar o scanner depois de usar
        scanner.close();

        try {
            ProblemInstance instance = BenchMarkReader.readInstaces(selectedInstancePath);

            numVehicles = instance.getNumVehicles();
            vehicleCapacity = instance.getVehicleCapacity();
            numClients = instance.getClients().size();
            clients = instance.getClients();

            System.out.println("Number of vehicles: " + numVehicles);
            System.out.println("Vehicle capacity: " + vehicleCapacity);
            System.out.println("Number of clients: " + numClients);

            if (algorithmChoice == 1) {
                // Executar algoritmo Multi-Objetivo (código existente)
                runMultiObjectiveAlgorithm(instance);
            } else if (algorithmChoice == 2) {
                // Executar algoritmo Mono-Objetivo
                runMonoObjectiveAlgorithm(instance);
            } else if (algorithmChoice == 3) {
                // Executar versão que roda o algoritmo passo a passo, para deputação
                runDebugAlgorithm(instance);
            } else if (algorithmChoice == 4) {
                // Executar NSGA-III com JMetal
                VRPNSGAIIIRunner.runNSGAIII(instance);
            } else {
                System.out.println("Opção inválida. Encerrando o programa.");
            }

        } catch (IOException e) {
            System.out.println("Error reading the file");
            e.printStackTrace();
        }
    }

    private static void runMultiObjectiveAlgorithm(ProblemInstance instance) {

        long startTime = System.currentTimeMillis();

        // Criando lista vazia de indivíduos
        List<Individual> individuals = new ArrayList<>();

        int get_the_first = 0; // Just to get the first fitness before evolution

        // Inicializando população
        Population population = new Population(individuals);
        population.initializePopulation(instance.getClients());
        population.distributeSubpopulations();

        // Calculando o fitness de cada subpopulação
        System.out.println("Calculando fitness para todas as subpopulações...");
        List<Client> clients = instance.getClients();

        for (Individual ind : population.getSubPopDistance()) {
            double fitnessDistance = new DistanceFitnessCalculator().calculateFitness(ind, clients);
            ind.setFitnessDistance(fitnessDistance);
        }

        for (Individual ind : population.getSubPopTime()) {
            double fitnessTime = new TimeFitnessCalculator().calculateFitness(ind, clients);
            ind.setFitnessTime(fitnessTime);
        }

        for (Individual ind : population.getSubPopFuel()) {
            double fitnessFuel = new FuelFitnessCalculator().calculateFitness(ind, clients);
            ind.setFitnessFuel(fitnessFuel);
        }

        for (Individual ind : population.getSubPopPonderation()) {
            double fitnessPond = new DefaultFitnessCalculator().calculateFitness(ind, clients);
            ind.setFitness(fitnessPond);
        }

        // //
        // -----------------------------------------------------------------------------------------------------
        double first_fitness_before_evolution = population.getSubPopPonderation().stream()
                .mapToDouble(Individual::getFitness)
                .min()
                .orElse(Double.MAX_VALUE);

        // System.exit(0);
        // //
        // -----------------------------------------------------------------------------------------------------

        // Inicializando as subpopulações auxiliares para a próxima geração
        List<Individual> nextSubPopDistance = new ArrayList<>();
        List<Individual> nextSubPopTime = new ArrayList<>();
        List<Individual> nextSubPopFuel = new ArrayList<>();
        List<Individual> nextSubPopPonderation = new ArrayList<>();
        for (int i = 0; i < sub_pop_size; i++) {
            nextSubPopDistance.add(new Individual(-1, 0, 0, 0, 0));
            nextSubPopTime.add(new Individual(-1, 0, 0, 0, 0));
            nextSubPopFuel.add(new Individual(-1, 0, 0, 0, 0));
            nextSubPopPonderation.add(new Individual(-1, 0, 0, 0, 0));
        }

        int elitismSize = Math.max(1, (int) (sub_pop_size * elitismRate));
        int generationsBeforeComparison = 5;
        int selectionType = 2; // Tournament
        int crossingType = 1; // One-point

        // Criação de listas para armazenar os melhores fitness a cada 100 gerações
        List<Double> bestDistanceFitnessList = new ArrayList<>();
        List<Double> bestTimeFitnessList = new ArrayList<>();
        List<Double> bestFuelFitnessList = new ArrayList<>();
        List<Double> bestPonderationFitnessList = new ArrayList<>();
        List<Integer> generationsList = new ArrayList<>();

        for (int generation = 0; generation < numGenerations; generation++) {
            System.out.println("\nGeração: " + generation);

            try {
                System.out.println("Iniciando evolução...");

                population.evolvePopMulti(
                        generation,
                        population.getSubPopDistance(), nextSubPopDistance,
                        population.getSubPopTime(), nextSubPopTime,
                        population.getSubPopFuel(), nextSubPopFuel,
                        population.getSubPopPonderation(), nextSubPopPonderation,
                        instance.getClients(),
                        elitismSize,
                        generationsBeforeComparison,
                        selectionType,
                        crossingType);

                System.out.println("Evolução concluída!");

                // A cada 100 gerações ou na última geração, salvamos os melhores fitness
                if (generation % 100 == 0 || generation == numGenerations - 1) {
                    System.out.println("--- Estatísticas da geração " + generation + " ---");

                    // Encontrar o melhor fitness para cada subpopulação
                    double bestDistanceFitness = findBestFitness(population.getSubPopDistance(),
                            individual -> individual.getFitnessDistance());

                    double bestTimeFitness = findBestFitness(population.getSubPopTime(),
                            individual -> individual.getFitnessTime());

                    double bestFuelFitness = findBestFitness(population.getSubPopFuel(),
                            individual -> individual.getFitnessFuel());

                    double bestPonderationFitness = findBestFitness(population.getSubPopPonderation(),
                            individual -> individual.getFitness());

                    // Condition just to get the first best fitness
                    if (get_the_first == 0) {
                        bestPonderationFitness = first_fitness_before_evolution;
                        get_the_first = 1;
                    }

                    // Adicionando às listas
                    bestDistanceFitnessList.add(bestDistanceFitness);
                    bestTimeFitnessList.add(bestTimeFitness);
                    bestFuelFitnessList.add(bestFuelFitness);
                    bestPonderationFitnessList.add(bestPonderationFitness);
                    generationsList.add(generation);

                    System.out.println("Melhor Fitness Distance: " + bestDistanceFitness);
                    System.out.println("Melhor Fitness Time: " + bestTimeFitness);
                    System.out.println("Melhor Fitness Fuel: " + bestFuelFitness);
                    System.out.println("Melhor Fitness Ponderado: " + bestPonderationFitness);
                }
            } catch (Exception e) {
                System.out.println("ERRO NA GERAÇÃO " + generation + ": " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }

        // Salvar os resultados em um arquivo
        saveResultsToFile(generationsList, bestDistanceFitnessList, bestTimeFitnessList,
                bestFuelFitnessList, bestPonderationFitnessList);

        // Calcular estatísticas da última geração para a subpopulação de ponderação
        double bestPonderationFitness = findBestFitness(population.getSubPopPonderation(),
                Individual::getFitness);

        // Calcular média do fitness
        double avgPonderationFitness = population.getSubPopPonderation().stream()
                .mapToDouble(Individual::getFitness)
                .average()
                .orElse(0.0);

        // Calcular desvio padrão
        double meanPonderationFitness = avgPonderationFitness;
        double variancePonderation = population.getSubPopPonderation().stream()
                .mapToDouble(ind -> Math.pow(ind.getFitness() - meanPonderationFitness, 2))
                .sum() / population.getSubPopPonderation().size();
        double stdDeviationPonderation = Math.sqrt(variancePonderation);

        // Calculando o tempo de execução
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Salvar estatísticas em um arquivo separado
        saveMultiStatistics(bestPonderationFitness, avgPonderationFitness, stdDeviationPonderation);

        System.out.println("Tempo em milissegundos: " + executionTime + " ms");

        // // Printar IDs de todos os indivíduos em cada subpopulação
        // System.out.println("\n--- IDs dos indivíduos em cada subpopulação ---");

        // System.out.print("SubPopDistance IDs: ");
        // for (Individual ind : population.getSubPopDistance()) {
        // System.out.print(ind.getId() + " " +
        // "(Fitness Distance: " + ind.getFitnessDistance() + ") ");
        // }
        // System.out.println();

        // System.out.print("SubPopTime IDs: ");
        // for (Individual ind : population.getSubPopTime()) {
        // System.out.print(ind.getId() + " " +
        // "(Fitness Time: " + ind.getFitnessTime() + ") ");
        // }
        // System.out.println();

        // System.out.print("SubPopFuel IDs: ");
        // for (Individual ind : population.getSubPopFuel()) {
        // System.out.print(ind.getId() + " " +
        // "(Fitness Fuel: " + ind.getFitnessFuel() + ") ");
        // }
        // System.out.println();

        // System.out.print("SubPopPonderation IDs: ");
        // for (Individual ind : population.getSubPopPonderation()) {
        // System.out.print(ind.getId() + " " +
        // "(Fitness Pond: " + ind.getFitness() + ") ");
        // }
        System.out.println();
    }

    private static void runMonoObjectiveAlgorithm(ProblemInstance instance) {

        long startTime = System.currentTimeMillis();

        System.out.println("=== INICIANDO ALGORITMO MONO-OBJETIVO ===");

        // Variable to store the first fitness before comparison
        int get_the_first = 0;

        // Criando lista vazia de indivíduos
        List<Individual> individuals = new ArrayList<>();

        // Inicializando população
        Population population = new Population(individuals);
        population.initializePopulation(instance.getClients());

        // Calculando fitness para todos os indivíduos usando DefaultFitnessCalculator
        System.out.println("Calculando fitness para população...");
        List<Client> clients = instance.getClients();

        for (Individual ind : individuals) {
            double fitness = new DefaultFitnessCalculator().calculateFitness(ind, clients);
            ind.setFitness(fitness);
        }

        // //
        // -----------------------------------------------------------------------------------------------------
        double first_fitness_before_evolution = individuals.stream()
                .mapToDouble(Individual::getFitness)
                .min()
                .orElse(Double.MAX_VALUE);

        // System.exit(0);
        // //
        // -----------------------------------------------------------------------------------------------------

        // Inicializando a população auxiliar para a próxima geração
        List<Individual> nextPopulation = new ArrayList<>();
        for (int i = 0; i < App.pop_size; i++) {
            nextPopulation.add(new Individual(-1, 0, 0, 0, 0));
        }

        // Parâmetros para evolução
        int elitismSize = Math.max(1, (int) (App.pop_size * App.elitismRate));
        int generationsBeforeComparison = 5;

        // Lista para armazenar os melhores fitness a cada 100 gerações
        List<Double> bestFitnessList = new ArrayList<>();
        List<Integer> generationsList = new ArrayList<>();

        for (int generation = 0; generation < numGenerations; generation++) {
            System.out.println("\nGeração: " + generation);

            try {
                System.out.println("Iniciando evolução...");

                population.evolvePopMono(
                        generation,
                        individuals,
                        nextPopulation,
                        instance.getClients(),
                        elitismSize,
                        generationsBeforeComparison);

                System.out.println("Evolução concluída!");

                // A cada 100 gerações ou na última geração, salvamos o melhor fitness
                if (generation % 100 == 0 || generation == numGenerations - 1) {
                    System.out.println("--- Estatísticas da geração " + generation + " ---");

                    // Encontrar o melhor fitness (menor valor)
                    double bestFitness = findBestFitness(individuals, Individual::getFitness);

                    // Condition just to get the first best fitness
                    if (get_the_first == 0) {
                        bestFitness = first_fitness_before_evolution;
                        get_the_first = 1;
                    }

                    // Adicionando às listas
                    bestFitnessList.add(bestFitness);
                    generationsList.add(generation);

                    System.out.println("Melhor Fitness: " + bestFitness);
                }
            } catch (Exception e) {
                System.out.println("ERRO NA GERAÇÃO " + generation + ": " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }

        // Salvar os resultados em um arquivo
        saveMonoResults(generationsList, bestFitnessList);

        // Encontrar e imprimir o melhor indivíduo final
        // Individual bestIndividual = individuals.stream()
        // .min(Comparator.comparingDouble(Individual::getFitness))
        // .orElse(null);

        // Calcular estatísticas da última geração
        double bestFinalFitness = findBestFitness(individuals, Individual::getFitness);

        // Calcular média do fitness
        double avgFitness = individuals.stream()
                .mapToDouble(Individual::getFitness)
                .average()
                .orElse(0.0);

        // Calcular desvio padrão
        double meanFitness = avgFitness;
        double variance = individuals.stream()
                .mapToDouble(ind -> Math.pow(ind.getFitness() - meanFitness, 2))
                .sum() / individuals.size();
        double stdDeviation = Math.sqrt(variance);

        // Calcular tempo de execução
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Salvar estatísticas em um arquivo separado
        saveMonoStatistics(bestFinalFitness, avgFitness, stdDeviation, executionTime);

        System.out.println("Tempo em milissegundos: " + executionTime + " ms");

        System.out.println("\n=== ALGORITMO MONO-OBJETIVO CONCLUÍDO ===");
    }

    private static void saveMonoResults(List<Integer> generations, List<Double> fitness) {
        try {
            // Criar diretório de resultados se não existir
            File resultsDir = new File("resultsMono");
            if (!resultsDir.exists()) {
                resultsDir.mkdir();
            }

            // Obter timestamp atual para nome do arquivo
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "resultsMono/mono_results_" + timestamp + ".txt";

            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName));

            // Escrever cabeçalho com números das gerações
            writer.print("Subpopulação\\Geração\t");
            for (Integer gen : generations) {
                writer.print("g" + gen + "\t");
            }
            writer.println();

            // Escrever resultados do fitness mono-objetivo
            writer.print("Mono-Objetivo\t");
            for (Double fit : fitness) {
                // Substituir ponto por vírgula para formato brasileiro
                String formattedFitness = String.format("%.2f", fit).replace('.', ',');
                writer.print(formattedFitness + "\t");
            }
            writer.println();

            writer.close();
            System.out.println("\nResultados salvos em: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao salvar resultados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDebugAlgorithm(ProblemInstance instance) {
        System.out.println("\n=== EXECUTANDO ALGORITMO DE DEBUG (MONO-OBJETIVO) ===");

        // Lista original de clientes da instância
        List<Client> clients = instance.getClients();

        // Criando uma população de debug
        List<Individual> individuals = new ArrayList<>();
        Population population = new Population(individuals);

        // Inicialização da população
        population.initializePopulation(clients);

        // Calculando o fitness de cada indivíduo (DefaultFitnessCalculator)
        System.out.println("\n=== CALCULANDO FITNESS ===");
        for (Individual ind : individuals) {
            double fitness = new DefaultFitnessCalculator().calculateFitness(ind, clients);
            ind.setFitness(fitness);
            System.out.println("Indivíduo " + ind.getId() + " - Fitness: " + fitness);
        }

        // Seleção por torneio (dois pais)
        System.out.println("\n=== INICIANDO SELEÇÃO POR TORNEIO ===");
        List<Individual> parents = SelectionUtils.parentSelectionMono(individuals);

        System.out.println("Pais selecionados:");
        for (int i = 0; i < parents.size(); i++) {
            Individual p = parents.get(i);
            System.out.println("Pai " + (i + 1) + ": ID=" + p.getId() + ", Fitness=" + p.getFitness());
        }

        // Cruzamento (one-point crossover)
        System.out.println("\n=== INICIANDO CROSSOVER ===");
        Individual filho = Crossover.onePointCrossing(parents.get(0), parents.get(1));
        System.out.println("Filho gerado pelo crossover.");

        // Mutação
        Mutation.mutate(filho, App.mutationRate);
        System.out.println("Filho após mutação.");

        // Calcular fitness do filho
        double filhoFitness = new DefaultFitnessCalculator().calculateFitness(filho, clients);
        filho.setFitness(filhoFitness);
        System.out.println("Filho - Fitness: " + filhoFitness);

        // Mostrar rota do filho
        System.out.println("\nID do filho: " + filho.getId());
        System.out.println("\nRota do filho:");
        filho.printRoutes();

        System.out.println("\n=== DEBUG CONCLUÍDO ===");
    }

    /**
     * Encontra o melhor fitness (menor valor) em uma lista de indivíduos
     * 
     * @param individuals      Lista de indivíduos
     * @param fitnessExtractor Função para extrair o valor de fitness do indivíduo
     * @return O melhor valor de fitness (menor valor)
     */
    private static double findBestFitness(List<Individual> individuals,
            java.util.function.Function<Individual, Double> fitnessExtractor) {
        return individuals.stream()
                .mapToDouble(ind -> fitnessExtractor.apply(ind))
                .min()
                .orElse(Double.MAX_VALUE);
    }

    private static void saveResultsToFile(List<Integer> generations,
            List<Double> distanceFitness,
            List<Double> timeFitness,
            List<Double> fuelFitness,
            List<Double> ponderationFitness) {

        try {
            // Criar diretório de resultados se não existir
            File resultsDir = new File("resultsMulti");
            if (!resultsDir.exists()) {
                resultsDir.mkdir();
            }

            // Obter timestamp atual para nome do arquivo
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "resultsMulti/evolution_results_" + timestamp + ".txt";

            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName));

            // Escrever cabeçalho com números das gerações
            writer.print("Subpopulação\\Geração\t");
            for (Integer gen : generations) {
                writer.print("g" + gen + "\t");
            }
            writer.println();

            // Escrever resultados de cada subpopulação
            writer.print("subPopDistance\t");
            for (Double fitness : distanceFitness) {
                writer.print(String.format("%.2f\t", fitness));
            }
            writer.println();

            writer.print("subPopTime\t");
            for (Double fitness : timeFitness) {
                writer.print(String.format("%.2f\t", fitness));
            }
            writer.println();

            writer.print("subPopFuel\t");
            for (Double fitness : fuelFitness) {
                writer.print(String.format("%.2f\t", fitness));
            }
            writer.println();

            writer.print("subPopPonderation\t");
            for (Double fitness : ponderationFitness) {
                writer.print(String.format("%.2f\t", fitness));
            }
            writer.println();

            writer.close();
            System.out.println("\nResultados salvos em: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao salvar resultados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveMultiStatistics(double bestPonderationFitness, double avgPonderationFitness,
            double stdDeviationPonderation) {
        try {
            // Criar diretório de resultados se não existir
            File resultsDir = new File("resultsMulti/stats");
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }

            // Obter timestamp atual para nome do arquivo
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "resultsMulti/stats/multi_stats_" + timestamp + ".txt";

            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName));

            // Escrever estatísticas apenas da subpopulação de ponderação
            writer.println("Estatísticas da Execução Multi-Objetivo (Subpopulação de Ponderação)");
            writer.println("=================================================================");
            writer.println("Melhor Fitness: " + String.format("%.2f", bestPonderationFitness));
            writer.println("Fitness Médio: " + String.format("%.2f", avgPonderationFitness));
            writer.println("Desvio Padrão: " + String.format("%.2f", stdDeviationPonderation));

            writer.close();
            System.out.println("\nEstatísticas salvas em: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao salvar estatísticas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveMonoStatistics(double bestFitness, double avgFitness, double stdDeviation, double time) {
        try {
            // Criar diretório de resultados se não existir
            File resultsDir = new File("resultsMono/stats");
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }

            // Obter timestamp atual para nome do arquivo
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "resultsMono/stats/mono_stats_" + timestamp + ".txt";

            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName));

            // Escrever estatísticas
            writer.println("Estatísticas da Execução Mono-Objetivo");
            writer.println("=====================================");
            writer.println("Melhor Fitness: " + String.format("%.2f", bestFitness));
            writer.println("Fitness Médio: " + String.format("%.2f", avgFitness));
            writer.println("Desvio Padrão: " + String.format("%.2f", stdDeviation));
            writer.println("Tempo de Execução: " + String.format("%.2f", time) + " ms");

            writer.close();
            System.out.println("\nEstatísticas salvas em: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao salvar estatísticas: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
