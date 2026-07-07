package genetic;

import java.util.List;

import vrp.Client;

public interface FitnessCalculator {
    double calculateFitness(Individual individual, List<Client> clients);
}
