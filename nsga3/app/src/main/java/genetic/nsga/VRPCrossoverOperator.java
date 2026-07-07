package genetic.nsga;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Operador de cruzamento específico para VRPSolution
 * Implementa um cruzamento baseado em ordem preservando a estrutura de rotas
 */
public class VRPCrossoverOperator implements CrossoverOperator<VRPSolution> {
    private double crossoverProbability;
    private JMetalRandom randomGenerator;

    public VRPCrossoverOperator(double crossoverProbability) {
        this.crossoverProbability = crossoverProbability;
        this.randomGenerator = JMetalRandom.getInstance();
    }

    @Override
    public List<VRPSolution> execute(List<VRPSolution> parents) {
        if (parents.size() != 2) {
            throw new RuntimeException("VRPCrossoverOperator.execute: invalid number of parents");
        }

        List<VRPSolution> offspring = new ArrayList<>();

        if (randomGenerator.nextDouble() < crossoverProbability) {
            // Realizar cruzamento
            VRPSolution parent1 = parents.get(0);
            VRPSolution parent2 = parents.get(1);

            // Criar dois filhos
            VRPSolution child1 = (VRPSolution) parent1.copy();
            VRPSolution child2 = (VRPSolution) parent2.copy();

            // Aplicar cruzamento de rotas (PMX - Partially Mapped Crossover adaptado)
            crossoverRoutes(child1, parent2);
            crossoverRoutes(child2, parent1);

            offspring.add(child1);
            offspring.add(child2);
        } else {
            // Sem cruzamento, retornar cópias dos pais
            offspring.add((VRPSolution) parents.get(0).copy());
            offspring.add((VRPSolution) parents.get(1).copy());
        }

        return offspring;
    }

    /**
     * Aplica cruzamento entre rotas de dois pais
     */
    private void crossoverRoutes(VRPSolution child, VRPSolution otherParent) {
        int[][] childRoutes = child.getRoutes();
        int[][] parentRoutes = otherParent.getRoutes();

        // Para cada veículo, trocar algumas rotas com o outro pai
        for (int v = 0; v < childRoutes.length; v++) {
            if (randomGenerator.nextDouble() < 0.5) {
                // Trocar segmento da rota
                int routeLength = 0;
                for (int c = 0; c < childRoutes[v].length; c++) {
                    if (childRoutes[v][c] == -1)
                        break;
                    routeLength++;
                }

                if (routeLength > 2) { // Só trocar se há clientes além do depósito
                    int start = 1 + randomGenerator.nextInt(0, routeLength - 2);
                    int end = start + randomGenerator.nextInt(1, routeLength - start);

                    // Copiar segmento do outro pai
                    for (int i = start; i < end && i < parentRoutes[v].length; i++) {
                        if (parentRoutes[v][i] != -1) {
                            childRoutes[v][i] = parentRoutes[v][i];
                        }
                    }
                }
            }
        }

        child.setRoutes(childRoutes);
    }

    @Override
    public double crossoverProbability() {
        return crossoverProbability;
    }

    @Override
    public int numberOfRequiredParents() {
        return 2;
    }

    @Override
    public int numberOfGeneratedChildren() {
        return 2;
    }
}