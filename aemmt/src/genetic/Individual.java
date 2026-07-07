package genetic;

import java.util.List;

import main.App;
import vrp.Client;

public class Individual {
    private int id;
    private int[][] route;
    private double fitness;
    private double fitnessDistance;
    private double fitnessTime;
    private double fitnessFuel;

    public Individual(int id, double fitness, double fitnessDistance, double fitnessTime,
            double fitnessFuel) {
        this.id = id;
        this.route = new int[App.numVehicles][App.numClients];
        this.fitness = fitness;
        this.fitnessDistance = fitnessDistance;
        this.fitnessTime = fitnessTime;
        this.fitnessFuel = fitnessFuel;

        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                this.route[v][c] = -1;
            }
        }
    }

    // Function just to print the routes of the vehicles
    public void printRoutes() {
        for (int v = 0; v < App.numVehicles; v++) {
            System.out.print("Vehicle " + v + ":");
            for (int c = 0; c < App.numClients; c++) {
                int clientId = this.route[v][c];
                if (clientId == -1)
                    break;
                System.out.print(clientId + " ");
            }
            System.out.println("");
        }
        System.out.println();
    }

    // Function to clone every parameter of the individual
    public Individual deepCopy() {
        Individual clone = new Individual(this.id, this.fitness, this.fitnessDistance, this.fitnessTime,
                this.fitnessFuel);
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                clone.route[v][c] = this.route[v][c];
            }
        }
        return clone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[][] getRoute() {
        return route;
    }

    public void setRoute(int[][] route) {
        for (int v = 0; v < App.numVehicles; v++) {
            for (int c = 0; c < App.numClients; c++) {
                this.route[v][c] = route[v][c];
            }
        }
    }

    public void setClientInRoute(int vehicle, int position, int clientId) {
        this.route[vehicle][position] = clientId;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getFitnessDistance() {
        return fitnessDistance;
    }

    public void setFitnessDistance(double fitnessDistance) {
        this.fitnessDistance = fitnessDistance;
    }

    public double getFitnessTime() {
        return fitnessTime;
    }

    public void setFitnessTime(double fitnessTime) {
        this.fitnessTime = fitnessTime;
    }

    public double getFitnessFuel() {
        return fitnessFuel;
    }

    public void setFitnessFuel(double fitnessFuel) {
        this.fitnessFuel = fitnessFuel;
    }

}
