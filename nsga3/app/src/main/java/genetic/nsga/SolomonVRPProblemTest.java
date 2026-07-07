package genetic.nsga;

import genetic.*;
import vrp.*;
import java.util.*;

/**
 * Teste simples para verificar se o SolomonVRPProblem está funcionando
 * corretamente
 */
public class SolomonVRPProblemTest {

    public static void main(String[] args) {
        System.out.println("=== Teste do SolomonVRPProblem ===\n");

        // Criar uma instância de problema simples para teste
        ProblemInstance instance = createTestInstance();

        // Criar o problema JMetal
        SolomonVRPProblem problem = new SolomonVRPProblem(instance);

        // Mostrar informações básicas do problema
        System.out.println("Informações do Problema:");
        System.out.println("Nome: " + problem.name());
        System.out.println("Número de variáveis: " + problem.numberOfVariables());
        System.out.println("Número de objetivos: " + problem.numberOfObjectives());
        System.out.println("Número de restrições: " + problem.numberOfConstraints());
        System.out.println();

        // Gerar e testar algumas soluções
        System.out.println("=== Testando Geração e Avaliação de Soluções ===\n");

        for (int i = 1; i <= 2; i++) {
            System.out.println("Solução " + i + ":");

            // Criar uma nova solução
            VRPSolution solution = problem.createSolution();

            // Mostrar as rotas geradas
            printSolutionRoutes(solution);

            // Mostrar os objetivos calculados
            System.out.println("Objetivos:");
            System.out.println("  Distância: " + String.format("%.2f", solution.getObjective(0)));
            System.out.println("  Tempo: " + String.format("%.2f", solution.getObjective(1)));
            System.out.println("  Combustível: " + String.format("%.2f", solution.getObjective(2)));

            // isFeasible removido na reescrita do SolomonVRPProblem
            // Viabilidade agora e verificada via constraints no evaluate()
            System.out.println("  Constraints: cap=" + solution.getConstraint(0) +
                    ", tw=" + solution.getConstraint(1) + ", missing=" + solution.getConstraint(2));

            // Testar conversão Individual <-> VRPSolution
            Individual ind = solution.toIndividual();
            VRPSolution converted = new VRPSolution(ind, solution.numberOfObjectives());

            System.out.println("  Conversão Individual<->VRPSolution: " +
                    (routesAreEqual(solution.getRoutes(), converted.getRoutes()) ? "OK" : "ERRO"));

            System.out.println();
        }

        System.out.println("\n=== Teste Concluído com Sucesso! ===");
    }

    /**
     * Cria uma instância de teste simples com 5 clientes e 2 veículos
     */
    private static ProblemInstance createTestInstance() {
        List<Client> clients = new ArrayList<>();

        // Depósito (cliente 0)
        clients.add(new Client(0, 10.0, 10.0, 0, 0, 100, 0));

        // Clientes 1-4
        clients.add(new Client(1, 15.0, 10.0, 5, 0, 100, 10));
        clients.add(new Client(2, 10.0, 15.0, 3, 0, 100, 8));
        clients.add(new Client(3, 5.0, 10.0, 7, 0, 100, 12));
        clients.add(new Client(4, 10.0, 5.0, 4, 0, 100, 6));

        return new ProblemInstance(2, 20, clients); // 2 veículos, capacidade 20
    }

    /**
     * Imprime as rotas de uma solução de forma legível
     */
    private static void printSolutionRoutes(VRPSolution solution) {
        int[][] routes = solution.getRoutes();
        System.out.println("Rotas:");

        for (int v = 0; v < routes.length; v++) {
            System.out.print("  Veículo " + v + ": ");
            List<Integer> route = new ArrayList<>();

            for (int c = 0; c < routes[v].length; c++) {
                if (routes[v][c] != -1) {
                    route.add(routes[v][c]);
                }
            }

            if (route.isEmpty()) {
                System.out.println("(vazio)");
            } else {
                System.out.println(route.toString().replace("[", "").replace("]", ""));
            }
        }
    }

    /**
     * Verifica se duas matrizes de rotas são iguais
     */
    private static boolean routesAreEqual(int[][] routes1, int[][] routes2) {
        if (routes1.length != routes2.length)
            return false;

        for (int v = 0; v < routes1.length; v++) {
            if (routes1[v].length != routes2[v].length)
                return false;
            for (int c = 0; c < routes1[v].length; c++) {
                if (routes1[v][c] != routes2[v][c])
                    return false;
            }
        }
        return true;
    }
}