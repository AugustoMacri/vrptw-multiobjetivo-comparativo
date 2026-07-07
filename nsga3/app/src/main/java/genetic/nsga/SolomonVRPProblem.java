package genetic.nsga;

import org.uma.jmetal.problem.Problem;
import genetic.*;
import vrp.ProblemInstance;
import vrp.Client;
import main.App;
import java.util.*;

/**
 * Problema VRP para JMetal usando instancias Solomon e VRPSolution.
 *
 * 3 objetivos: Distancia, Tempo, Combustivel
 * 3 constraints: violacoes de capacidade, time windows, clientes faltantes
 *
 * Inclui instrumentacao de debug para rastrear evolucao.
 */
public class SolomonVRPProblem implements Problem<VRPSolution> {
    private final ProblemInstance instance;
    private final DistanceFitnessCalculator distanceCalculator;
    private final TimeFitnessCalculator timeCalculator;
    private final FuelFitnessCalculator fuelCalculator;
    private final List<Client> clients;
    private final int numVehicles;
    private final int numClients;
    private final int numVariables;
    private final int numObjectives;
    private final int numConstraints;
    private final String problemName;

    // Contadores de debug
    private int solutionCounter = 0;
    private int evaluationCounter = 0;

    // Rastreamento de evolucao
    private double bestDistance = Double.MAX_VALUE;
    private double bestTime = Double.MAX_VALUE;
    private double bestFuel = Double.MAX_VALUE;
    private int logInterval = 5000; // Logar a cada N avaliacoes

    // Historico de evolucao: lista de snapshots [evalCount, bestDist, bestTime, bestFuel]
    private final List<double[]> evolutionHistory = new ArrayList<>();

    // Rastreamento de diversidade
    private final Set<String> uniqueSolutionFingerprints = new HashSet<>();

    public SolomonVRPProblem(ProblemInstance instance) {
        this.instance = instance;
        this.clients = instance.getClients();
        this.numVehicles = instance.getNumVehicles();
        this.numClients = instance.getClients().size();

        App.numVehicles = numVehicles;
        App.numClients = numClients;
        App.vehicleCapacity = instance.getVehicleCapacity();

        this.distanceCalculator = new DistanceFitnessCalculator();
        this.timeCalculator = new TimeFitnessCalculator();
        this.fuelCalculator = new FuelFitnessCalculator();

        this.numVariables = numVehicles * numClients;
        this.numObjectives = 3;
        this.numConstraints = 3; // capacidade, time windows, clientes faltantes
        this.problemName = "SolomonVRP";
    }

    @Override
    public int numberOfVariables() { return numVariables; }

    @Override
    public int numberOfObjectives() { return numObjectives; }

    @Override
    public int numberOfConstraints() { return numConstraints; }

    @Override
    public String name() { return problemName; }

    @Override
    public VRPSolution createSolution() {
        VRPSolution solution = new VRPSolution(numVehicles, numClients, numberOfObjectives());

        // Solomon I1 para todas as solucoes (identico ao AEMMT)
        Individual individual = initializeWithSolomonI1();
        solution.setRoutes(individual.getRoute());

        evaluate(solution);

        solutionCounter++;
        if (solutionCounter == 1) {
            System.out.println("DEBUG: Primeira solucao criada - Dist: " +
                    String.format("%.2f", solution.getObjective(0)) +
                    ", Tempo: " + String.format("%.2f", solution.getObjective(1)) +
                    ", Comb: " + String.format("%.2f", solution.getObjective(2)));
        }
        if (solutionCounter % 100 == 0) {
            System.out.println("DEBUG: " + solutionCounter + " solucoes criadas na inicializacao");
        }

        return solution;
    }

    private Individual initializeWithSolomonI1() {
        List<Client> clientsCopy = new ArrayList<>(clients);
        return SolomonInsertion.createIndividual(clientsCopy, App.vehicleCapacity, App.numVehicles);
    }

    @Override
    public VRPSolution evaluate(VRPSolution solution) {
        evaluationCounter++;

        Individual ind = solution.toIndividual();

        // Contar violacoes
        int capacityViolations = 0;
        int timeWindowViolations = 0;
        int[][] routes = solution.getRoutes();
        Client depot = clients.get(0);

        boolean[] clientVisited = new boolean[numClients];

        for (int v = 0; v < numVehicles; v++) {
            int capacityUsed = 0;
            double currentTime = depot.getReadyTime();
            Client previousClient = depot;

            for (int c = 0; c < numClients; c++) {
                int clientId = routes[v][c];
                if (clientId == -1) break;

                if (clientId > 0 && clientId < numClients) {
                    clientVisited[clientId] = true;
                }

                Client client = clients.get(clientId);
                capacityUsed += client.getDemand();

                double travelTime = calculateDistance(previousClient, client) / App.VEHICLE_SPEED;
                double arrivalTime = currentTime + travelTime;
                double startService = Math.max(arrivalTime, client.getReadyTime());

                if (arrivalTime > client.getDueTime()) {
                    timeWindowViolations++;
                }

                currentTime = startService + client.getServiceTime();
                previousClient = client;
            }

            if (capacityUsed > App.vehicleCapacity) {
                capacityViolations++;
            }

            if (previousClient != depot) {
                double returnTime = currentTime + calculateDistance(previousClient, depot) / App.VEHICLE_SPEED;
                if (returnTime > depot.getDueTime()) {
                    timeWindowViolations++;
                }
            }
        }

        int missingClients = 0;
        for (int i = 1; i < numClients; i++) {
            if (!clientVisited[i]) missingClients++;
        }

        // Calcular objetivos base
        double distancia = distanceCalculator.calculateFitness(ind, clients);
        double tempo = timeCalculator.calculateFitness(ind, clients);
        double combustivel = fuelCalculator.calculateFitness(ind, clients);

        // Penalidades (mantidas para guiar busca, mas agora com constraints tambem)
        double PENALTY_PER_VIOLATION = 100000.0;
        double PENALTY_PER_MISSING = 200000.0;
        double totalPenalty = (capacityViolations + timeWindowViolations) * PENALTY_PER_VIOLATION
                + missingClients * PENALTY_PER_MISSING;

        solution.setObjective(0, distancia + totalPenalty);
        solution.setObjective(1, tempo + totalPenalty);
        solution.setObjective(2, combustivel + totalPenalty);

        // Constraints (negativo = violacao, conforme convencao jMetal)
        solution.setConstraint(0, -capacityViolations);
        solution.setConstraint(1, -timeWindowViolations);
        solution.setConstraint(2, -missingClients);

        // Rastreamento de evolucao
        double dist = solution.getObjective(0);
        double time = solution.getObjective(1);
        double fuel = solution.getObjective(2);

        boolean improved = false;
        if (dist < bestDistance) { bestDistance = dist; improved = true; }
        if (time < bestTime) { bestTime = time; improved = true; }
        if (fuel < bestFuel) { bestFuel = fuel; improved = true; }

        // Fingerprint para diversidade
        if (evaluationCounter <= 50000) { // Limitar para nao estourar memoria
            StringBuilder fp = new StringBuilder();
            for (int v = 0; v < numVehicles; v++) {
                for (int c = 0; c < numClients; c++) {
                    if (routes[v][c] > 0) {
                        fp.append(v).append(':').append(routes[v][c]).append(',');
                    }
                }
            }
            uniqueSolutionFingerprints.add(fp.toString());
        }

        // Log periodico
        if (evaluationCounter % logInterval == 0) {
            System.out.println("[Eval " + evaluationCounter + "] Best: Dist=" +
                    String.format("%.2f", bestDistance) + ", Time=" +
                    String.format("%.2f", bestTime) + ", Fuel=" +
                    String.format("%.2f", bestFuel) +
                    " | Unique solutions: " + uniqueSolutionFingerprints.size());

            evolutionHistory.add(new double[]{evaluationCounter, bestDistance, bestTime, bestFuel});
        }

        return solution;
    }

    private double calculateDistance(Client c1, Client c2) {
        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Verifica viabilidade com base nas constraints ja calculadas.
     * Se a solucao ainda nao foi avaliada, avalia antes de verificar.
     */
    public boolean isFeasible(VRPSolution solution) {
        evaluate(solution);
        return solution.getConstraint(0) >= 0
                && solution.getConstraint(1) >= 0
                && solution.getConstraint(2) >= 0;
    }

    /**
     * Busca local leve para manter compatibilidade com demos/testes.
     * Atualmente retorna uma copia reavaliada da solucao original.
     */
    public VRPSolution localSearch(VRPSolution solution) {
        VRPSolution candidate = (VRPSolution) solution.copy();
        evaluate(candidate);
        return candidate;
    }

    // --- Getters para diagnostico ---

    public int getSolutionCounter() { return solutionCounter; }
    public int getEvaluationCounter() { return evaluationCounter; }
    public double getBestDistance() { return bestDistance; }
    public double getBestTime() { return bestTime; }
    public double getBestFuel() { return bestFuel; }
    public int getUniqueSolutionCount() { return uniqueSolutionFingerprints.size(); }
    public List<double[]> getEvolutionHistory() { return evolutionHistory; }

    public void setLogInterval(int interval) {
        this.logInterval = interval;
    }
}
