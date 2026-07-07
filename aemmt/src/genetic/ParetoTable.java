package genetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tabela de nao-dominancia (Fronteira de Pareto) do AEMMT.
 *
 * Armazena individuos nao-dominados considerando os 3 objetivos do problema:
 *   - fitnessDistance (minimizar)
 *   - fitnessTime     (minimizar)
 *   - fitnessFuel     (minimizar)
 *
 * Conforme proposta original de Brasil (2013), funciona como repositorio que
 * aproxima a Fronteira de Pareto. Nesta implementacao, alem disso, participa
 * como 5a subpopulacao na selecao de pais do torneio do AEMMT, contribuindo
 * com material genetico de solucoes nao-dominadas para o cruzamento.
 *
 * A tabela cresce sem limite de tamanho - em problemas com 3 objetivos
 * raramente passa de algumas centenas de individuos.
 */
public class ParetoTable {

    private final List<Individual> nonDominated;
    private final Random random;

    // Contadores para diagnostico
    private long attemptCount = 0;
    private long insertCount = 0;
    private long rejectedDominated = 0;
    private long rejectedEquivalent = 0;

    public ParetoTable() {
        this.nonDominated = new ArrayList<>();
        this.random = new Random();
    }

    public long getAttemptCount() { return attemptCount; }
    public long getInsertCount() { return insertCount; }
    public long getRejectedDominated() { return rejectedDominated; }
    public long getRejectedEquivalent() { return rejectedEquivalent; }

    /**
     * Tenta inserir um candidato na tabela.
     *
     * Regras:
     *   1) Se o candidato for dominado por ALGUM individuo ja presente, rejeita.
     *   2) Caso contrario, remove todos os individuos que o candidato domina e
     *      insere o candidato.
     *   3) Solucoes equivalentes (mesmos 3 fitness) sao ignoradas para evitar
     *      duplicatas que apenas inflam a tabela.
     *
     * @return true se o candidato foi inserido, false caso contrario.
     */
    public boolean tryInsert(Individual candidate) {
        if (candidate == null)
            return false;

        attemptCount++;

        // Passo 1: verifica se o candidato eh dominado ou equivalente a algum existente
        for (Individual existing : nonDominated) {
            if (dominates(existing, candidate)) {
                rejectedDominated++;
                return false;
            }
            if (isEquivalent(existing, candidate)) {
                rejectedEquivalent++;
                return false;
            }
        }

        // Passo 2: remove todos os que o candidato domina
        nonDominated.removeIf(ind -> dominates(candidate, ind));

        // Passo 3: insere o candidato (copia defensiva)
        nonDominated.add(candidate.deepCopy());
        insertCount++;
        return true;
    }

    /**
     * Retorna true se o individuo "a" domina "b".
     * Dominacao classica de Pareto para minimizacao:
     *   a domina b se: a <= b em todos objetivos E a < b em pelo menos um.
     */
    private boolean dominates(Individual a, Individual b) {
        boolean strictlyBetterInAny = false;

        if (a.getFitnessDistance() > b.getFitnessDistance())
            return false;
        if (a.getFitnessTime() > b.getFitnessTime())
            return false;
        if (a.getFitnessFuel() > b.getFitnessFuel())
            return false;

        if (a.getFitnessDistance() < b.getFitnessDistance())
            strictlyBetterInAny = true;
        if (a.getFitnessTime() < b.getFitnessTime())
            strictlyBetterInAny = true;
        if (a.getFitnessFuel() < b.getFitnessFuel())
            strictlyBetterInAny = true;

        return strictlyBetterInAny;
    }

    /**
     * Retorna true se ambos os individuos tem fitness identicos nos 3 objetivos.
     */
    private boolean isEquivalent(Individual a, Individual b) {
        return a.getFitnessDistance() == b.getFitnessDistance()
                && a.getFitnessTime() == b.getFitnessTime()
                && a.getFitnessFuel() == b.getFitnessFuel();
    }

    /**
     * Tenta inserir uma populacao inteira (usado para inicializacao).
     */
    public int tryInsertAll(List<Individual> individuals) {
        int inserted = 0;
        for (Individual ind : individuals) {
            if (tryInsert(ind))
                inserted++;
        }
        return inserted;
    }

    /**
     * Retorna um individuo aleatorio da tabela (usado na selecao de pais).
     * Retorna null se a tabela estiver vazia.
     */
    public Individual getRandomMember() {
        if (nonDominated.isEmpty())
            return null;
        return nonDominated.get(random.nextInt(nonDominated.size()));
    }

    public int size() {
        return nonDominated.size();
    }

    public boolean isEmpty() {
        return nonDominated.isEmpty();
    }

    public List<Individual> getAll() {
        return new ArrayList<>(nonDominated);
    }

    /**
     * Retorna o individuo com menor fitnessDistance da Pareto.
     */
    public Individual getBestByDistance() {
        return nonDominated.stream()
                .min((a, b) -> Double.compare(a.getFitnessDistance(), b.getFitnessDistance()))
                .orElse(null);
    }

    public Individual getBestByTime() {
        return nonDominated.stream()
                .min((a, b) -> Double.compare(a.getFitnessTime(), b.getFitnessTime()))
                .orElse(null);
    }

    public Individual getBestByFuel() {
        return nonDominated.stream()
                .min((a, b) -> Double.compare(a.getFitnessFuel(), b.getFitnessFuel()))
                .orElse(null);
    }
}
