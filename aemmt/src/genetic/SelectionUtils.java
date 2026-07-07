package genetic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import main.App;

public class SelectionUtils {

    private static final Random random = new Random();

    // Elitism fuction to monoObjetctive version
    public static void elitism(List<Individual> population, List<Individual> nextPopulation, double elitismRate) {
    int numElites = (int) Math.ceil(population.size() * App.elitismRate);

    // Sorts the population by fitness (lower fitness = better)
    List<Individual> sortedPopulation = new ArrayList<>(population);
    sortedPopulation.sort(Comparator.comparingDouble(Individual::getFitness));

    // Substitui os indivíduos nas primeiras posições da próxima população
    for (int i = 0; i < numElites && i < sortedPopulation.size(); i++) {
        nextPopulation.set(i, sortedPopulation.get(i).deepCopy());
    }
}

    // Elitism function to multiObjetctive version
    public static void selectElite(List<Individual> subPop, List<Individual> nextPop, int fitnessType,
            int elitismRate) {

        List<Individual> sortedSubPop = new ArrayList<>(subPop);

        switch (fitnessType) {
            case 0:
                sortedSubPop.sort(Comparator.comparingDouble(Individual::getFitnessDistance));
                break;
            case 1:
                sortedSubPop.sort(Comparator.comparingDouble(Individual::getFitnessTime));
                break;
            case 2:
                sortedSubPop.sort(Comparator.comparingDouble(Individual::getFitnessFuel));
                break;
            case 3:
                sortedSubPop.sort(Comparator.comparingDouble(Individual::getFitness));
                break;
            default:
                throw new IllegalArgumentException("Invalid fitness type");
        }

        // Select the top individuals based on the elitism rate
        for (int i = 0; i < elitismRate && i < sortedSubPop.size(); i++) {
            Individual elite = sortedSubPop.get(i).deepCopy();
            nextPop.set(i, elite); // Substituir na posição i
        }
    }

    public static Individual tournamentSelection(List<Individual> subPop, int tournamentSize,
            Set<Integer> previousWinners, int fitnessType) {

        Individual parent = null;

        while (parent == null) {

            List<Individual> tournament = new ArrayList<>();

            // Select two random individual from the subpopulation to participate in the
            // tournament
            for (int j = 0; j < tournamentSize; j++) {
                int randomIndex = random.nextInt(subPop.size());
                tournament.add(subPop.get(randomIndex));
            }

            Individual winner = Collections.min(tournament, (ind1, ind2) -> {
                switch (fitnessType) {
                    case 0:
                        return Double.compare(ind1.getFitnessDistance(), ind2.getFitnessDistance());
                    case 1:
                        return Double.compare(ind1.getFitnessTime(), ind2.getFitnessTime());
                    case 2:
                        return Double.compare(ind1.getFitnessFuel(), ind2.getFitnessFuel());
                    case 3:
                        return Double.compare(ind1.getFitness(), ind2.getFitness());
                    default:
                        throw new IllegalArgumentException("Invalid fitness type");
                }
            });

            // Verify if the individual is already in the parents list
            if (!previousWinners.contains(winner.getId())) {
                parent = winner;
            }
        }

        return parent;

    }

    public static Individual tournamentSelectionMono(List<Individual> pop, int tournamentSize,
            Set<Integer> previousWinners) {

        Individual parent = null;

        while (parent == null) {

            List<Individual> tournament = new ArrayList<>();

            // Select tournamentSize random individuals from the subpopulation to
            // participate in the tournament
            for (int j = 0; j < tournamentSize; j++) {
                int randomIndex = random.nextInt(pop.size());
                tournament.add(pop.get(randomIndex));
            }

            // Select the best individual from the tournament
            Individual winner = Collections.min(tournament, Comparator.comparingDouble(Individual::getFitness));

            // Verify if the individual is already in the parents list
            if (!previousWinners.contains(winner.getId())) {
                parent = winner;
            }
        }

        return parent;

    }

    /*
     * Se não me engano é alguma cosia do tipo, pode cruzar dois indivíduos de duas
     * subpopulações diferentes
     * Mas ai quando seleciona as duas populações iguais, ele salva o id para evitar
     * que o mesmo indivíduo seja escolhido duas vezes
     */
    public static List<Individual> subPopSelection(Population population) {
        Random rand = new Random();

        // Lista de subpopulacoes escalares disponiveis (indices 0..3)
        List<List<Individual>> subPopulations = List.of(
                population.getSubPopDistance(),
                population.getSubPopTime(),
                population.getSubPopFuel(),
                population.getSubPopPonderation());

        ParetoTable paretoTable = population.getParetoTable();

        List<Individual> selectedParents = new ArrayList<>();

        // Keep track of the ID of the winners (evita pegar o mesmo individuo 2x)
        Set<Integer> previousWinners = new HashSet<>();

        // Seleciona dois pais, cada um vindo de uma subpopulacao sorteada
        for (int i = 0; i < 2; i++) {
            // Sorteia entre 5 fontes:
            //   0 = Distance, 1 = Time, 2 = Fuel, 3 = Ponderation, 4 = Pareto
            // A Pareto so participa do sorteio se tiver pelo menos 2 individuos
            // (para que o "torneio" tenha sentido); caso contrario reamostra entre 0..3.
            int sourceIndex;
            if (paretoTable != null && paretoTable.size() >= 2) {
                sourceIndex = rand.nextInt(5);
            } else {
                sourceIndex = rand.nextInt(4);
            }

            Individual parent;
            if (sourceIndex == 4) {
                // Selecao da Pareto: pega um aleatorio (sem fitness escalar para torneio)
                parent = paretoTable.getRandomMember();
                if (parent != null && previousWinners.contains(parent.getId())) {
                    // Reamostra se ja foi escolhido
                    parent = paretoTable.getRandomMember();
                }
            } else {
                List<Individual> selectedSubPop = subPopulations.get(sourceIndex);
                int fitnessType = sourceIndex; // 0=Dist, 1=Time, 2=Fuel, 3=Ponderada
                parent = tournamentSelection(selectedSubPop, App.tournamentSize, previousWinners, fitnessType);
            }

            if (parent != null) {
                selectedParents.add(parent);
                previousWinners.add(parent.getId());
            }
        }

        // Fallback: se nao conseguiu 2 pais (raro), reabastece da subPopDistance
        while (selectedParents.size() < 2) {
            Individual fallback = tournamentSelection(
                    population.getSubPopDistance(), App.tournamentSize, previousWinners, 0);
            if (fallback != null) {
                selectedParents.add(fallback);
                previousWinners.add(fallback.getId());
            } else {
                break;
            }
        }

        return selectedParents;
    }

    public static List<Individual> parentSelectionMono(List<Individual> population) {
        List<Individual> selectedParents = new ArrayList<>();
        Set<Integer> previousWinners = new HashSet<>();

        Individual parent1 = tournamentSelectionMono(population, App.tournamentSize, previousWinners);
        selectedParents.add(parent1);
        previousWinners.add(parent1.getId());

        Individual parent2 = tournamentSelectionMono(population, App.tournamentSize, previousWinners);
        selectedParents.add(parent2);

        // Debugging output
        System.out.println("Parent 1: " + parent1.getId() + " - Fitness: " + parent1.getFitness());
        System.out.println("Parent 2: " + parent2.getId() + " - Fitness: " + parent2.getFitness());
        System.out.println("Selected parents: " + selectedParents.size());

        return selectedParents;
    }

}