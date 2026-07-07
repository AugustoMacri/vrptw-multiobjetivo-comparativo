package jmetaltest;

import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.NSGAIIIBuilder;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import java.util.List;

public class JMetalTest {
    public static void main(String[] args) {
        System.out.println("=== TESTE JMETAL NSGA-III ===");

        var problem = new ZDT1();
        System.out.println("Problema: ZDT1");
        System.out.println("Número de variáveis: " + problem.numberOfVariables());
        System.out.println("Número de objetivos: " + problem.numberOfObjectives());

        var crossover = new SBXCrossover(1.0, 20.0);
        var mutation = new PolynomialMutation(1.0 / problem.numberOfVariables(), 20.0);
        var selection = new BinaryTournamentSelection<DoubleSolution>();

        System.out.println("\nConfigurando NSGA-III...");
        var algorithm = new NSGAIIIBuilder<>(problem)
                .setCrossoverOperator(crossover)
                .setMutationOperator(mutation)
                .setSelectionOperator(selection)
                .setMaxIterations(250)
                .setPopulationSize(100)
                .setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
                .build();

        System.out.println("Executando algoritmo...");
        long startTime = System.currentTimeMillis();
        algorithm.run();
        long endTime = System.currentTimeMillis();

        List<DoubleSolution> result = algorithm.getResult();
        System.out.println("\n=== RESULTADOS ===");
        System.out.println("Tempo de execução: " + (endTime - startTime) + " ms");
        System.out.println("Número de soluções na frente de Pareto: " + result.size());

        if (!result.isEmpty()) {
            System.out.println("\nPrimeiras 5 soluções:");
            for (int i = 0; i < Math.min(5, result.size()); i++) {
                DoubleSolution solution = result.get(i);
                System.out.printf("Solução %d: f1=%.4f, f2=%.4f%n",
                        i + 1, solution.objectives()[0], solution.objectives()[1]);
            }
        }

        System.out.println("\n=== TESTE CONCLUÍDO ===");
    }
}
