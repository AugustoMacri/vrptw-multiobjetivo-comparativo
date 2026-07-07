package genetic.nsga;

import org.uma.jmetal.solution.Solution;
import genetic.Individual;
import main.App;

import java.util.*;

/**
 * VRPSolution - Representacao compativel com JMetal para o problema VRP
 *
 * Adapta a representacao de rotas do VRP (matriz de veiculos x clientes)
 * para ser compativel com o framework JMetal, incluindo suporte a constraints.
 */
public class VRPSolution implements Solution<Integer> {

    // Representacao das rotas: [veiculo][posicao] = clienteId
    private int[][] routes;

    // Objetivos do problema (distancia, tempo, combustivel)
    private double[] objectives;

    // Constraints (violacoes de restricoes: valores negativos = violacao)
    private double[] constraintValues;

    // Atributos adicionais do JMetal
    private Map<Object, Object> attributes;

    private int numVehicles;
    private int numClients;

    public VRPSolution(int numVehicles, int numClients, int numberOfObjectives) {
        this.numVehicles = numVehicles;
        this.numClients = numClients;
        this.routes = new int[numVehicles][numClients];
        this.objectives = new double[numberOfObjectives];
        this.constraintValues = new double[3]; // capacidade, time windows, clientes faltantes
        this.attributes = new HashMap<>();

        for (int v = 0; v < numVehicles; v++) {
            for (int c = 0; c < numClients; c++) {
                this.routes[v][c] = -1;
            }
        }
    }

    public VRPSolution(Individual individual, int numberOfObjectives) {
        this(App.numVehicles, App.numClients, numberOfObjectives);
        setFromIndividual(individual);
    }

    public void setFromIndividual(Individual individual) {
        int[][] individualRoutes = individual.getRoute();
        for (int v = 0; v < numVehicles; v++) {
            for (int c = 0; c < numClients; c++) {
                this.routes[v][c] = individualRoutes[v][c];
            }
        }
        this.objectives[0] = individual.getFitnessDistance();
        this.objectives[1] = individual.getFitnessTime();
        this.objectives[2] = individual.getFitnessFuel();
    }

    public Individual toIndividual() {
        Individual individual = new Individual(-1, 0, 0, 0, 0);
        individual.setRoute(this.routes);
        individual.setFitnessDistance(this.objectives[0]);
        individual.setFitnessTime(this.objectives[1]);
        individual.setFitnessFuel(this.objectives[2]);
        return individual;
    }

    @Override
    public List<Integer> variables() {
        List<Integer> vars = new ArrayList<>();
        for (int v = 0; v < numVehicles; v++) {
            for (int c = 0; c < numClients; c++) {
                vars.add(routes[v][c]);
            }
        }
        return vars;
    }

    @Override
    public double[] objectives() {
        return objectives.clone();
    }

    @Override
    public double[] constraints() {
        return constraintValues.clone();
    }

    @Override
    public Map<Object, Object> attributes() {
        return attributes;
    }

    @Override
    public Solution<Integer> copy() {
        VRPSolution copy = new VRPSolution(numVehicles, numClients, objectives.length);

        for (int v = 0; v < numVehicles; v++) {
            for (int c = 0; c < numClients; c++) {
                copy.routes[v][c] = this.routes[v][c];
            }
        }

        System.arraycopy(this.objectives, 0, copy.objectives, 0, objectives.length);
        System.arraycopy(this.constraintValues, 0, copy.constraintValues, 0, constraintValues.length);

        // Deep copy attributes (valores imutaveis como Integer/Double sao seguros)
        copy.attributes = new HashMap<>(this.attributes);

        return copy;
    }

    public int[][] getRoutes() {
        return routes;
    }

    public void setRoutes(int[][] routes) {
        for (int v = 0; v < numVehicles && v < routes.length; v++) {
            for (int c = 0; c < numClients && c < routes[v].length; c++) {
                this.routes[v][c] = routes[v][c];
            }
        }
    }

    public void setClientInRoute(int vehicle, int position, int clientId) {
        if (vehicle >= 0 && vehicle < numVehicles && position >= 0 && position < numClients) {
            this.routes[vehicle][position] = clientId;
        }
    }

    public int getClientInRoute(int vehicle, int position) {
        if (vehicle >= 0 && vehicle < numVehicles && position >= 0 && position < numClients) {
            return this.routes[vehicle][position];
        }
        return -1;
    }

    public void setObjective(int index, double value) {
        if (index >= 0 && index < objectives.length) {
            objectives[index] = value;
        }
    }

    public double getObjective(int index) {
        if (index >= 0 && index < objectives.length) {
            return objectives[index];
        }
        return 0.0;
    }

    /** Define o valor de uma constraint. Negativo = violacao. */
    public void setConstraint(int index, double value) {
        if (index >= 0 && index < constraintValues.length) {
            constraintValues[index] = value;
        }
    }

    public double getConstraint(int index) {
        if (index >= 0 && index < constraintValues.length) {
            return constraintValues[index];
        }
        return 0.0;
    }

    public int numberOfObjectives() {
        return objectives.length;
    }

    public int numberOfVariables() {
        return numVehicles * numClients;
    }

    public void setAttributes(Map<Object, Object> attributes) {
        this.attributes = attributes;
    }

    public void printRoutes() {
        System.out.println("=== Rotas da VRPSolution ===");
        for (int v = 0; v < numVehicles; v++) {
            System.out.print("Vehicle " + v + ": ");
            for (int c = 0; c < numClients; c++) {
                int clientId = routes[v][c];
                if (clientId == -1)
                    break;
                System.out.print(clientId + " ");
            }
            System.out.println();
        }

        System.out.println("Objetivos:");
        System.out.printf("Distancia: %.2f, Tempo: %.2f, Combustivel: %.2f%n",
                objectives[0], objectives[1], objectives[2]);
    }
}
