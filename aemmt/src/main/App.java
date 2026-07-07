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
import configuration.*;
import genetic.Crossover;
import genetic.DefaultFitnessCalculator;
import genetic.DistanceFitnessCalculator;
import genetic.FitnessCalculator;
import genetic.FuelFitnessCalculator;
import genetic.Individual;
import genetic.Mutation;
import genetic.Population;
import genetic.SelectionUtils;
import genetic.TimeFitnessCalculator;

public class App {
    // VRP Variables
    public static int numVehicles;
    public static int vehicleCapacity;
    public static int numClients;
    public static int VEHICLE_SPEED = 1; // Solomon instances: travel time = euclidean distance
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
    // Penalidades equalizadas com o NSGA-III para garantir comparacao justa:
    // - 100.000 por violacao (capacidade ou janela de tempo)
    // - 200.000 por cliente ausente (garante cobertura total)
    public static double WEIGHT_NUM_VIOLATIONS = 100000.0;
    public static double WEIGHT_MISSING_CLIENT = 200000.0;
    public static double WEIGHT_TOTAL_COST = 0.75;

    // EAs Variables
    public static int pop_size = 600;
    public static int sub_pop_size = (int) Math.floor((double) pop_size / 4);
    public static double elitismRate = 0.1;
    public static int QUANTITYSELECTEDTOURNAMENT = 2;
    public static int tournamentSize = 2;
    public static double mutationRate = 0.01; // 1% mutation rate
    public static double interRouteMutationRate = 1.0; // Always apply inter-route mutation when mutation occurs
    public static int numGenerations = 3000; // Increased to allow more evolution
    public static int nextIndividualId = pop_size; // Inicializa com pop_size

    // Variável estática para armazenar o nome da instância (sem extensão)
    public static String instanceName = "";
    public static String executionSuffix = "";

    // Variáveis para armazenar rotas inicial e final
    public static Individual initialBestIndividual = null;
    public static Individual finalBestIndividual = null;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        int algorithmChoice = 1; // Default: Multi-Objetivo
        int instanceTypeChoice = 1; // Default: Solomon
        int instanceChoice = 0;

        // Se argumentos foram passados via linha de comando
        // Sintaxe: java main.App <instance_num> [exec_suffix]
        //   instance_num: numero da instancia (1-9 = C1, 18-26 = R1, 41-48 = RC1)
        //   exec_suffix:  opcional, ex: "_exec01" - sufixo anexado aos arquivos
        if (args.length > 0) {
            try {
                instanceChoice = Integer.parseInt(args[0]);
                System.out.println("Executando em modo automático com instância: " + instanceChoice);
                if (args.length > 1) {
                    executionSuffix = args[1];
                    System.out.println("Sufixo de execucao: " + executionSuffix);
                }
            } catch (NumberFormatException e) {
                System.out.println("Erro: Argumento inválido. Use um número de instância.");
                return;
            }
        } else {
            // Menu interativo (modo original)
            System.out.println("=== MENU DE SELEÇÃO DE ALGORITMO ===");
            System.out.println("1 - Algoritmo Multi-Objetivo");
            System.out.println("2 - Algoritmo Mono-Objetivo");
            System.out.println("3 - Algoritmo Passo a Passo (para depuração)");
            System.out.print("Digite sua escolha (1, 2 ou 3): ");

            while (algorithmChoice != 1 && algorithmChoice != 2 && algorithmChoice != 3) {
                try {
                    algorithmChoice = Integer.parseInt(scanner.nextLine().trim());

                    if (algorithmChoice != 1 && algorithmChoice != 2 && algorithmChoice != 3) {
                        System.out.print("Opção inválida. Digite 1, 2 ou 3: ");
                    }
                } catch (NumberFormatException e) {
                    System.out.print("Entrada inválida. Digite 1, 2 ou 3: ");
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
        String instancePath;
        if (instanceTypeChoice == 1) {
            instancePath = "src/instances/solomon/";
            System.out.println("\nTipo selecionado: Instâncias Solomon");
        } else {
            instancePath = "src/instances/gehring_homberg/";
            System.out.println("\nTipo selecionado: Instâncias Gehring-Homberg");
        }

        // Menu para escolher a instância
        System.out.println("\n=== MENU DE SELEÇÃO DE INSTÂNCIA ===");
        File folder = new File(instancePath);

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

        BenchMarkReader reader = new BenchMarkReader();

        try {
            ProblemInstance instance = reader.readInstaces(selectedInstancePath);

            numVehicles = instance.getNumVehicles();
            vehicleCapacity = instance.getVehicleCapacity();
            numClients = instance.getClients().size(); // Total size including depot (array size)

            System.out.println("Number of vehicles: " + numVehicles);
            System.out.println("Vehicle capacity: " + vehicleCapacity);
            System.out.println("Number of clients (total including depot): " + numClients);

            if (algorithmChoice == 1) {
                // Executar algoritmo Multi-Objetivo (código existente)
                runMultiObjectiveAlgorithm(instance);
            } else if (algorithmChoice == 2) {
                // Executar algoritmo Mono-Objetivo
                runMonoObjectiveAlgorithm(instance);
            } else if (algorithmChoice == 3) {
                // Executar versão que roda o algoritmo passo a passo, para deputação
                runDebugAlgorithm(instance);
            } else {
                System.out.println("Opção inválida. Encerrando o programa.");
            }

        } catch (IOException e) {
            System.out.println("Error reading the file");
            e.printStackTrace();
        }
    }

    private static void debugVehicleUsage(Individual individual, List<Client> clients) {
        int vehiclesUsed = 0;

        System.out.println("\n=== DEBUG: USO DE VEÍCULOS ===");

        for (int v = 0; v < App.numVehicles; v++) {
            boolean hasClients = false;
            int clientCount = 0;
            int totalDemand = 0;

            for (int c = 0; c < App.numClients - 1; c++) {
                int clientId = individual.getRoute()[v][c];
                if (clientId != -1 && clientId != 0) {
                    hasClients = true;
                    clientCount++;
                    // Encontrar o cliente na lista para pegar a demanda
                    for (Client client : clients) {
                        if (client.getId() == clientId) {
                            totalDemand += client.getDemand();
                            break;
                        }
                    }
                }
            }

            if (hasClients) {
                vehiclesUsed++;
                System.out.println("Veículo " + v + ": " + clientCount + " clientes, demanda total: " + totalDemand
                        + "/" + App.vehicleCapacity);
            }
        }

        System.out.println("\nTotal de veículos usados: " + vehiclesUsed);
        System.out.println("Total de veículos disponíveis: " + App.numVehicles);
        System.out.println("================================\n");
    }

    private static void debugVehicleRoute(Individual individual, int vehicleIndex, List<Client> clients) {
        System.out.println("\n=== ROTA DO VEÍCULO " + vehicleIndex + " ===");

        Client depot = clients.get(0); // Depósito
        System.out.println("Depósito (0) [X: " + depot.getX() + ", Y: " + depot.getY() + "]");

        double totalDistance = 0;
        Client prevClient = depot;
        int clientCount = 0;
        Client lastClient = null;

        // Processar todos os clientes da rota
        for (int c = 0; c < App.numClients - 1; c++) {
            int clientId = individual.getRoute()[vehicleIndex][c];

            // Parar se encontrar -1 (fim da rota)
            if (clientId == -1) {
                break;
            }

            // Pular o depósito se aparecer na rota (não deveria, mas por garantia)
            if (clientId == 0) {
                continue;
            }

            Client currentClient = clients.get(clientId);

            // Calcular distância do ponto anterior (depósito ou cliente anterior)
            double dist = Math.sqrt(Math.pow(currentClient.getX() - prevClient.getX(), 2) +
                    Math.pow(currentClient.getY() - prevClient.getY(), 2));

            System.out.println("  ↓ distância: " + String.format("%.2f", dist));
            System.out.println("Cliente " + clientId + " [X: " + currentClient.getX() +
                    ", Y: " + currentClient.getY() + "]");

            totalDistance += dist;
            prevClient = currentClient;
            lastClient = currentClient;
            clientCount++;
        }

        // Distância do último cliente de volta ao depósito
        if (lastClient != null) {
            double distLastToDepot = Math.sqrt(Math.pow(depot.getX() - lastClient.getX(), 2) +
                    Math.pow(depot.getY() - lastClient.getY(), 2));
            totalDistance += distLastToDepot;
            System.out.println("  ↓ distância: " + String.format("%.2f", distLastToDepot));
            System.out.println("Depósito (0) [X: " + depot.getX() + ", Y: " + depot.getY() + "]");
        }

        System.out.println("\n📊 RESUMO DA ROTA:");
        System.out.println("Número de clientes atendidos: " + clientCount);
        System.out.println("Distância total desta rota: " + String.format("%.2f", totalDistance));
        System.out.println("================================\n");
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

        // Inicializa a tabela de nao-dominancia (Pareto) com individuos viaveis.
        // Para cada individuo, calcula os 3 fitness e tenta inserir na Pareto.
        System.out.println("Populando tabela de nao-dominancia com individuos iniciais...");
        for (Individual ind : individuals) {
            ind.setFitnessDistance(new DistanceFitnessCalculator().calculateFitness(ind, clients));
            ind.setFitnessTime(new TimeFitnessCalculator().calculateFitness(ind, clients));
            ind.setFitnessFuel(new FuelFitnessCalculator().calculateFitness(ind, clients));
            population.getParetoTable().tryInsert(ind);
        }
        System.out.println("Tabela de Pareto inicializada com " + population.getParetoTable().size() + " individuos nao-dominados");

        // //
        // -----------------------------------------------------------------------------------------------------
        double first_fitness_before_evolution = population.getSubPopPonderation().stream()
                .mapToDouble(Individual::getFitness)
                .min()
                .orElse(Double.MAX_VALUE);

        // Armazenar o melhor indivíduo inicial
        initialBestIndividual = population.getSubPopPonderation().stream()
                .min(Comparator.comparingDouble(Individual::getFitness))
                .map(ind -> copyIndividual(ind))
                .orElse(null);

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

        // Armazenar o melhor indivíduo final (melhor em distância)
        finalBestIndividual = population.getSubPopDistance().stream()
                .min(Comparator.comparingDouble(Individual::getFitnessDistance))
                .map(ind -> {
                    System.out.println("\n=== DEBUG: Selecionando melhor indivíduo ===");
                    System.out.println("ID: " + ind.getId());
                    System.out.println("getFitnessDistance(): " + ind.getFitnessDistance());
                    System.out.println("getFitness(): " + ind.getFitness());
                    Individual copy = copyIndividual(ind);
                    System.out.println("\nApós cópia:");
                    System.out.println("getFitnessDistance(): " + copy.getFitnessDistance());
                    System.out.println("getFitness(): " + copy.getFitness());
                    return copy;
                })
                .orElse(null);

        // Salvar os resultados em um arquivo (incluindo a frente de Pareto)
        saveResultsToFile(generationsList, bestDistanceFitnessList, bestTimeFitnessList,
                bestFuelFitnessList, bestPonderationFitnessList, instance.getClients(),
                population.getParetoTable());

        // Salvar a tabela de nao-dominancia (Pareto) em arquivo separado
        savePareto(population.getParetoTable());

        // Resumo da Pareto no console
        System.out.println();
        System.out.println("=== TABELA DE NAO-DOMINANCIA (PARETO) ===");
        System.out.println("Tamanho final: " + population.getParetoTable().size() + " solucoes nao-dominadas");
        System.out.println("Diagnostico de insercoes:");
        System.out.println("  Tentativas: " + population.getParetoTable().getAttemptCount());
        System.out.println("  Inseridas:  " + population.getParetoTable().getInsertCount());
        System.out.println("  Rejeitadas (dominadas):    " + population.getParetoTable().getRejectedDominated());
        System.out.println("  Rejeitadas (equivalentes): " + population.getParetoTable().getRejectedEquivalent());
        Individual paretoBestDist = population.getParetoTable().getBestByDistance();
        Individual paretoBestTime = population.getParetoTable().getBestByTime();
        Individual paretoBestFuel = population.getParetoTable().getBestByFuel();
        if (paretoBestDist != null) {
            System.out.println(String.format("Melhor distancia na Pareto: %.2f", paretoBestDist.getFitnessDistance()));
        }
        if (paretoBestTime != null) {
            System.out.println(String.format("Melhor tempo na Pareto:     %.2f", paretoBestTime.getFitnessTime()));
        }
        if (paretoBestFuel != null) {
            System.out.println(String.format("Melhor combustivel Pareto:  %.2f", paretoBestFuel.getFitnessFuel()));
        }

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

        // Debug: Analisar uso de veículos do melhor indivíduo da subpopulação de
        // ponderação
        Individual bestPonderation = population.getSubPopPonderation().stream()
                .min(Comparator.comparingDouble(Individual::getFitness))
                .orElse(null);

        if (bestPonderation != null) {
            debugVehicleUsage(bestPonderation, instance.getClients());

            // Mostrar detalhes das rotas de cada veículo usado
            System.out.println("\n=== DETALHAMENTO DAS ROTAS ===");
            for (int v = 0; v < App.numVehicles; v++) {
                // Verificar se o veículo tem clientes
                boolean hasClients = false;
                for (int c = 0; c < App.numClients - 1; c++) {
                    int clientId = bestPonderation.getRoute()[v][c];
                    if (clientId != -1 && clientId != 0) {
                        hasClients = true;
                        break;
                    }
                }

                if (hasClients) {
                    debugVehicleRoute(bestPonderation, v, instance.getClients());
                }
            }
        }

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
        Individual filho = Crossover.onePointCrossing(parents.get(0), parents.get(1), clients);
        System.out.println("Filho gerado pelo crossover.");

        // Mutação (intra-rota + inter-rota)
        Mutation.mutateCombined(filho, App.mutationRate, App.interRouteMutationRate, clients);
        System.out.println("Filho após mutação combinada (intra + inter-rota).");

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

    /**
     * Cria uma cópia profunda de um indivíduo
     */
    private static Individual copyIndividual(Individual source) {
        Individual copy = new Individual(source.getId(),
                source.getFitness(),
                source.getFitnessDistance(),
                source.getFitnessTime(),
                source.getFitnessFuel());

        // Copiar rotas
        int[][] sourceRoute = source.getRoute();
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                copy.setClientInRoute(v, c, sourceRoute[v][c]);
            }
        }

        return copy;
    }

    /**
     * Formata as rotas de um indivíduo para salvar em arquivo
     */
    private static String formatRoutesForFile(Individual individual, List<Client> clients, String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(label).append("\n");
        sb.append("=".repeat(80)).append("\n\n");

        // DEBUG: Mostrar fitness do indivíduo
        sb.append(String.format("Fitness Distance do indivíduo: %.2f\n\n", individual.getFitnessDistance()));

        int vehiclesUsed = 0;
        double totalDistance = 0;

        for (int v = 0; v < App.numVehicles; v++) {
            boolean hasClients = false;
            List<Integer> routeClients = new ArrayList<>();

            // Coletar clientes da rota
            for (int c = 0; c < App.numClients - 1; c++) {
                int clientId = individual.getRoute()[v][c];
                if (clientId == -1)
                    break;
                if (clientId != 0) {
                    hasClients = true;
                    routeClients.add(clientId);
                }
            }

            if (hasClients) {
                vehiclesUsed++;
                Client depot = clients.get(0);
                double routeDistance = 0;
                int capacity = 0;

                sb.append(String.format("Veículo %d: ", v));
                sb.append("Depósito(0)");

                Client prevClient = depot;
                for (int clientId : routeClients) {
                    Client currentClient = clients.get(clientId);
                    double dist = Math.sqrt(Math.pow(currentClient.getX() - prevClient.getX(), 2) +
                            Math.pow(currentClient.getY() - prevClient.getY(), 2));
                    routeDistance += dist;
                    capacity += currentClient.getDemand();

                    sb.append(String.format(" -> Cliente(%d)", clientId));
                    prevClient = currentClient;
                }

                // Volta ao depósito
                double distLastToDepot = Math.sqrt(Math.pow(depot.getX() - prevClient.getX(), 2) +
                        Math.pow(depot.getY() - prevClient.getY(), 2));
                routeDistance += distLastToDepot;
                totalDistance += routeDistance;

                sb.append(" -> Depósito(0)\n");
                sb.append(String.format("    Clientes: %d | Demanda: %d/%d | Distância: %.2f\n\n",
                        routeClients.size(), capacity, App.vehicleCapacity, routeDistance));
            }
        }

        sb.append(String.format("Total de veículos usados: %d\n", vehiclesUsed));
        sb.append(String.format("Distância total: %.2f\n", totalDistance));
        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }

    private static void saveResultsToFile(List<Integer> generations,
            List<Double> distanceFitness,
            List<Double> timeFitness,
            List<Double> fuelFitness,
            List<Double> ponderationFitness,
            List<Client> clients,
            genetic.ParetoTable paretoTable) {

        try {
            // Criar diretório de resultados se não existir
            File resultsDir = new File("resultsMulti");
            if (!resultsDir.exists()) {
                resultsDir.mkdir();
            }

            // Usar nome da instância se disponível, senão usar timestamp
            String fileName;
            if (!instanceName.isEmpty()) {
                fileName = "resultsMulti/evo_" + instanceName + executionSuffix + ".txt";
            } else {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                fileName = "resultsMulti/evolution_results_" + timestamp + ".txt";
            }

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

            // Adicionar rotas inicial e final
            if (initialBestIndividual != null && finalBestIndividual != null) {
                writer.println("\n");
                writer.println(
                        formatRoutesForFile(initialBestIndividual, clients, "ROTAS INICIAIS (Antes da Evolução)"));
                writer.println("\n");
                writer.println(formatRoutesForFile(finalBestIndividual, clients, "ROTAS FINAIS (Após 3000 Gerações)"));
            }

            // Adiciona a FRENTE DE PARETO (tabela de nao-dominancia)
            if (paretoTable != null) {
                writer.println();
                writer.println("================================================================================");
                writer.println("FRENTE DE PARETO (TABELA DE NAO-DOMINANCIA)");
                writer.println("================================================================================");
                writer.println("# Algoritmo: AEMMT");
                writer.println("# Instancia: " + (instanceName.isEmpty() ? "(desconhecida)" : instanceName));
                writer.println("# Execucao:  " + (executionSuffix.isEmpty() ? "(unica)" : executionSuffix));
                writer.println("# Tamanho:   " + paretoTable.size() + " solucoes nao-dominadas");
                writer.println("# Formato:   idx | distancia | tempo | combustivel | veiculos");
                writer.println("# Diagnostico: tentativas=" + paretoTable.getAttemptCount()
                        + " inseridas=" + paretoTable.getInsertCount()
                        + " rejeitadas_dominadas=" + paretoTable.getRejectedDominated()
                        + " rejeitadas_equivalentes=" + paretoTable.getRejectedEquivalent());
                writer.println();

                List<Individual> paretoList = paretoTable.getAll();
                int idx = 1;
                for (Individual ind : paretoList) {
                    int usedVehicles = countUsedVehicles(ind);
                    writer.println(String.format("  %3d | %12.4f | %12.4f | %12.4f | %d",
                            idx, ind.getFitnessDistance(), ind.getFitnessTime(),
                            ind.getFitnessFuel(), usedVehicles));
                    idx++;
                }
                writer.println();
                writer.println("(Detalhamento das rotas de cada solucao da Pareto disponivel em pareto_"
                        + instanceName + executionSuffix + ".txt se gerado em modo verbose.)");
            }

            writer.close();
            System.out.println("\nResultados salvos em: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao salvar resultados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Salva a tabela de nao-dominancia (Pareto) em arquivo dedicado.
     * Formato: # Pareto front - <instancia> - <N> solucoes
     *          # distancia tempo combustivel num_veiculos
     *          1234.56 5678.90 600.00 12
     *          ...
     */
    private static void savePareto(genetic.ParetoTable paretoTable) {
        try {
            File resultsDir = new File("resultsMulti");
            if (!resultsDir.exists()) {
                resultsDir.mkdir();
            }

            String fileName;
            if (!instanceName.isEmpty()) {
                fileName = "resultsMulti/pareto_" + instanceName + executionSuffix + ".txt";
            } else {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                fileName = "resultsMulti/pareto_" + timestamp + ".txt";
            }

            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName));

            writer.println("# AEMMT - Frente de Pareto (tabela de nao-dominancia)");
            writer.println("# Instancia: " + (instanceName.isEmpty() ? "(desconhecida)" : instanceName));
            writer.println("# Tamanho: " + paretoTable.size() + " solucoes nao-dominadas");
            writer.println("# Formato: distancia<TAB>tempo<TAB>combustivel<TAB>num_veiculos");
            writer.println();

            for (Individual ind : paretoTable.getAll()) {
                int usedVehicles = countUsedVehicles(ind);
                writer.println(String.format("%.6f\t%.6f\t%.6f\t%d",
                        ind.getFitnessDistance(),
                        ind.getFitnessTime(),
                        ind.getFitnessFuel(),
                        usedVehicles));
            }

            writer.close();
            System.out.println("Frente de Pareto salva em: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao salvar Pareto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int countUsedVehicles(Individual ind) {
        int used = 0;
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                int cid = ind.getRoute()[v][c];
                if (cid == -1) break;
                if (cid > 0) {
                    used++;
                    break;
                }
            }
        }
        return used;
    }

    private static void saveMultiStatistics(double bestPonderationFitness, double avgPonderationFitness,
            double stdDeviationPonderation) {
        try {
            // Criar diretório de resultados se não existir
            File resultsDir = new File("resultsMulti/stats");
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }

            // Usar nome da instância se disponível, senão usar timestamp
            String fileName;
            if (!instanceName.isEmpty()) {
                fileName = "resultsMulti/stats/stats_" + instanceName + executionSuffix + ".txt";
            } else {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                fileName = "resultsMulti/stats/multi_stats_" + timestamp + ".txt";
            }

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
