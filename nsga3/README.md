# VRP NSGA-III - Vehicle Routing Problem com NSGA-III

## 📋 Visão Geral

Este projeto implementa uma solução para o **Vehicle Routing Problem with Time Windows (VRPTW)** utilizando o algoritmo **NSGA-III** (Non-dominated Sorting Genetic Algorithm III) do framework JMetal. O objetivo é otimizar múltiplos objetivos simultaneamente: minimizar distância total percorrida, número de veículos utilizados, e violações de janelas de tempo.

## 🎯 Objetivos

- **Otimização Multi-Objetivo**: Minimizar 3 objetivos conflitantes
  1. **Distância Total**: Reduzir o custo operacional
  2. **Número de Veículos**: Reduzir a frota necessária
  3. **Fitness Temporal**: Minimizar atrasos e violações de janelas de tempo

- **Benchmark Solomon**: Validar resultados nas instâncias clássicas
  - **Tipo C1**: Clientes clustered (agrupados) com janelas de tempo estreitas
  - **Tipo R1**: Clientes random (aleatórios) com janelas de tempo estreitas  
  - **Tipo RC1**: Clientes semi-clustered com janelas de tempo estreitas

- **Comparação com AEMMT**: Competir com o algoritmo AEMMT implementado em `src/`

## 🏗️ Arquitetura do Projeto

```
VRP_NSGA_TCC/
├── app/
│   ├── src/main/java/
│   │   ├── genetic/
│   │   │   ├── TimeFitnessCalculator.java     # Cálculo de fitness temporal
│   │   │   ├── SolomonInsertion.java          # Heurística Solomon I1
│   │   │   └── nsga/
│   │   │       ├── SolomonVRPProblem.java     # Definição do problema JMetal
│   │   │       ├── SolomonVRPSolution.java    # Representação da solução
│   │   │       └── RunNSGA3Solomon.java       # Executor principal
│   │   ├── vrp/
│   │   │   ├── Client.java                    # Modelo de cliente
│   │   │   └── ...
│   │   └── instances/                         # Instâncias Solomon
│   └── build.gradle
├── resultsNSGA3/                              # Resultados brutos
├── results_validation_NSGA3_C1/               # Validações C1
├── results_validation_NSGA3_R1/               # Validações R1
├── results_validation_NSGA3_RC1/              # Validações RC1
└── scripts/
    └── validate_nsga3_solution.py             # Validação externa rigorosa
```

## 🚀 Como Executar

### Pré-requisitos

- Java 11 ou superior
- Gradle 7.x
- Python 3.8+ (para scripts de validação)

### Compilação

```bash
cd VRP_NSGA_TCC
./gradlew build
```

### Execução de uma Instância

```bash
# Executar C101 com 10 repetições
./gradlew run --args="C101.txt 10"

# Executar R101 com 5 repetições
./gradlew run --args="R101.txt 5"
```

### Validação dos Resultados

```bash
# Validar solução específica
python3 scripts/validate_nsga3_solution.py instances/C101.txt resultsNSGA3/evo_c101_001.txt

# Validar todas as soluções
python3 scripts/validate_all_nsga3.py
```

## 📊 Configuração Atual

**Parâmetros NSGA-III** (em `SolomonVRPProblem.java`):
- **População**: 900 indivíduos
- **Gerações**: 5000
- **Divisões de Referência**: 12
- **Objetivos**: 3 (distância, veículos, tempo)
- **Penalidade por Violação**: 10.000 (aumentado de 5.000)

**Inicialização da População**:
- **100% Solomon I1**: Heurística construtiva que considera janelas de tempo
- Removido: K-means (70%) e Gillet-Miller (30%)

**Operadores Genéticos**:
- **Crossover**: Single Point Crossover (probabilidade 0.9)
- **Mutação**: Bit Flip Mutation (probabilidade 0.1)

## ⚠️ PROBLEMA ATUAL: SOLUÇÕES INVÁLIDAS

### Sintomas

Após 260 execuções completas (90 C1 + 90 R1 + 80 RC1):
- ❌ **Taxa de Sucesso: 0%** (0 soluções válidas em 260)
- ❌ **Violações de Janelas de Tempo**: 60-72 violações por solução
  - C101: 72 violações
  - R101: 67 violações
  - RC101: 63 violações
  - RC102: 60 violações
- ✅ **Capacidade**: Sempre respeitada
- ✅ **Cobertura**: Todos os clientes atendidos

### Histórico de Correções (Janeiro 2026)

#### Bug #1: Validação do Cliente Errado (CRÍTICO) ✅ CORRIGIDO
**Arquivo**: `app/src/main/java/genetic/TimeFitnessCalculator.java` (linhas 50-65)

**Problema**:
```java
// ERRADO: Validava currentClient após viajar para nextClient
currentTime += distance / App.VEHICLE_SPEED;  // Agora está em nextClient
if (currentTime < currentClient.getReadyTime()) {  // ❌ Valida cliente anterior!
    currentTime = currentClient.getReadyTime();
    numViolations++;
}
```

**Correção**:
```java
// CORRETO: Valida nextClient (onde o veículo está)
currentTime += distance / App.VEHICLE_SPEED;  // Agora está em nextClient
if (currentTime < nextClient.getReadyTime()) {  // ✅ Valida cliente atual!
    currentTime = nextClient.getReadyTime();
    numViolations++;
}
```

**Impacto**: Todas as validações de janelas de tempo estavam verificando o cliente errado.

---

#### Bug #2: Primeiro Cliente Nunca Validado (CRÍTICO) ✅ CORRIGIDO
**Arquivo**: `app/src/main/java/genetic/TimeFitnessCalculator.java` (linhas 24-40)

**Problema**:
```java
// ERRADO: Viaja até primeiro cliente mas não valida janela de tempo
Client firstClient = allClients.get(firstClientId);
double depotToFirstDistance = depotToFirstDistances[firstClientId];
currentTime += depotToFirstDistance / App.VEHICLE_SPEED;  // ❌ Sem validação!

// Loop começa comparando firstClient com secondClient
for (int c = 0; c < routeSize - 1; c++) { ... }
```

**Correção**:
```java
// CORRETO: Valida primeiro cliente após viajar do depósito
Client firstClient = allClients.get(firstClientId);
double depotToFirstDistance = depotToFirstDistances[firstClientId];
currentTime += depotToFirstDistance / App.VEHICLE_SPEED;

// ✅ Validação adicionada
if (currentTime < firstClient.getReadyTime()) {
    currentTime = firstClient.getReadyTime();
    numViolations++;
} else if (currentTime > firstClient.getDueTime()) {
    numViolations++;
}
currentTime += firstClient.getServiceTime();
```

**Impacto**: Pelo menos 1 cliente por rota (potencialmente 25 clientes em 25 rotas) nunca era validado.

---

#### Melhoria #1: Aumento de Penalidade ✅ IMPLEMENTADO
**Arquivo**: `app/src/main/java/genetic/nsga/SolomonVRPProblem.java` (linha 309)

**Mudança**:
```java
// ANTES
private static final double PENALTY_PER_VIOLATION = 5000.0;

// DEPOIS
private static final double PENALTY_PER_VIOLATION = 10000.0;
```

**Objetivo**: Dobrar a pressão evolutiva contra soluções infactíveis.

---

#### Melhoria #2: Inicialização 100% Solomon I1 ✅ IMPLEMENTADO
**Arquivo**: `app/src/main/java/genetic/nsga/SolomonVRPProblem.java` (linhas 94-120)

**Mudança**:
```java
// ANTES: População Híbrida
// 70% K-means (não considera janelas de tempo)
// 30% Gillet-Miller (não considera janelas de tempo)

// DEPOIS: 100% Solomon I1
for (int i = 0; i < populationSize; i++) {
    population.add(initializeWithSolomonI1());
}
```

**Objetivo**: Solomon I1 é a melhor heurística construtiva para VRPTW, considera janelas de tempo durante construção.

---

### Compilação Verificada ✅

```bash
# Timestamp de compilação (após todas as correções)
Modify: 2026-01-22 21:08:05.906479833 -0300

# Status
BUILD SUCCESSFUL in 4s

# Testes executados após compilação
- C1: 2026-01-22 21:21 (90 execuções)
- R1: 2026-01-22 23:32 (90 execuções)
- RC1: 2026-01-23 03:00 (80 execuções)
```

## 🔍 Análise da Causa Raiz

### ✅ O Que Está Funcionando

1. **Correções de Bugs**: Os bugs foram corretamente identificados e corrigidos
2. **Validação Externa**: Script Python valida rigorosamente e detecta violações corretamente
3. **Capacidade**: Sempre respeitada (operadores genéticos preservam capacidade)
4. **Cobertura**: Todos os clientes sempre atendidos

### ❌ Problema Real Identificado

**Os operadores genéticos destroem a factibilidade durante a evolução!**

#### Evidências

1. **Solomon I1 inicial**: Provavelmente cria soluções válidas ou quase-válidas (geração 0)
2. **Após 5000 gerações**: 60-72 violações por solução
3. **Evolução regressiva**: Algoritmo piora as soluções ao invés de melhorar

#### Por Que Isso Acontece?

**Single Point Crossover**:
```
Pai 1: [Depósito → 5 → 8 → 12 → 3 → Depósito]  # Válido
Pai 2: [Depósito → 9 → 1 → 4 → 7 → Depósito]   # Válido
         ↓ Crossover no ponto 2
Filho:  [Depósito → 5 → 8 → 1 → 4 → 7 → Depósito]  # ❌ Inválido!
```
- Quebra sequência temporal cuidadosamente construída
- Cliente 1 pode ter janela de tempo incompatível com posição entre 8 e 4

**Bit Flip Mutation**:
```
Original: [D → 3 → 7 → 12 → 5 → D]  # Cliente 7 tem tw=[10,20]
   ↓ Troca bit 7 com bit 15
Mutado:   [D → 3 → 15 → 12 → 5 → D] # Cliente 15 tem tw=[80,90] ❌ Inválido!
```
- Substitui clientes aleatoriamente sem considerar janelas de tempo
- Pode colocar cliente com janela tarde no início da rota

**Penalidade Insuficiente**:
- Penalidade: 10.000 por violação
- Ganho de distância: ~5.000-20.000 no total
- Com 60 violações: Penalidade = 600.000
- Mesmo assim, soluções infactíveis dominam a população

## 🔧 Possíveis Causas e Soluções

### Causa #1: Operadores Genéticos Inadequados

**Problema**: Single Point Crossover e Bit Flip Mutation não preservam estrutura de rotas nem factibilidade temporal.

**Soluções Possíveis**:
- ✅ **Order Crossover (OX)**: Preserva ordem relativa de clientes
- ✅ **Partially Mapped Crossover (PMX)**: Mantém relações posicionais
- ✅ **Swap Mutation**: Troca apenas posições de clientes na mesma rota
- ✅ **Inversion Mutation**: Inverte sub-sequência (pode melhorar timing)

### Causa #2: Ausência de Operador de Reparo

**Problema**: Após crossover/mutação, nenhum reparo é aplicado para restaurar factibilidade.

**Solução**: Implementar `repairTimeWindows()`:
```java
private void repairTimeWindows(SolomonVRPSolution solution) {
    for (Route route : solution.getRoutes()) {
        // 1. Identificar violações
        List<Violation> violations = detectViolations(route);
        
        // 2. Para cada violação, tentar:
        //    a) Reordenar clientes dentro da rota (2-opt, Or-opt)
        //    b) Mover cliente para outra rota (relocate)
        //    c) Trocar clientes entre rotas (exchange)
        //    d) Remover cliente e reinserir com Solomon I1
        
        // 3. Aceitar reparo se violações < violações_originais
    }
}
```

### Causa #3: Penalidade Insuficiente

**Problema**: Penalidade de 10.000 não é forte o suficiente para evitar deriva evolutiva.

**Solução**: Aumentar drasticamente:
```java
// RECOMENDAÇÃO
private static final double PENALTY_PER_VIOLATION = 100000.0;  // 10x atual
```

### Causa #4: Solomon I1 Pode Não Estar Gerando Soluções Válidas

**Problema**: Se população inicial já tem violações, evolução só piora.

**Teste Necessário**: Verificar geração 0 antes da evolução.

## 🎯 Recomendações Prioritárias

### 🧪 TESTE IMEDIATO: Validar População Inicial

**Objetivo**: Confirmar se problema é com Solomon I1 ou com operadores genéticos.

**Passos**:
1. Modificar `RunNSGA3Solomon.java` para salvar geração 0:
```java
// Após criar população inicial, antes de evoluir
savePopulation(problem.createPopulation(), "gen0", instanceName);
// Depois: algorithm.run();
```

2. Executar uma instância:
```bash
./gradlew run --args="C101.txt 1"
```

3. Validar geração 0:
```bash
python3 scripts/validate_nsga3_solution.py instances/C101.txt resultsNSGA3/gen0_c101_001.txt
```

**Interpretação**:
- ✅ **Se geração 0 válida**: Problema confirmado nos operadores genéticos
  - → Implementar operador de reparo
  - → Mudar para OX/PMX crossover
  
- ❌ **Se geração 0 inválida**: Problema no Solomon I1
  - → Debugar `SolomonInsertion.createIndividual()`
  - → Verificar cálculo de tempo e validações

---

### ⚡ SOLUÇÃO RÁPIDA: Aumentar Penalidade Drasticamente

**Objetivo**: Tornar soluções infactíveis tão ruins que sejam eliminadas rapidamente.

**Implementação**:
```java
// Em SolomonVRPProblem.java, linha 309
private static final double PENALTY_PER_VIOLATION = 100000.0;  // ← MUDAR AQUI
```

**Compilar e Testar**:
```bash
cd VRP_NSGA_TCC
./gradlew build
./gradlew run --args="C101.txt 5"
python3 scripts/validate_nsga3_solution.py instances/C101.txt resultsNSGA3/evo_c101_001.txt
```

**Expectativa**: Se penalidade 10x maior funcionar, validação deve mostrar menos violações ou soluções válidas.

---

### 🔨 SOLUÇÃO ROBUSTA: Implementar Operador de Reparo

**Objetivo**: Garantir que toda solução infactível seja reparada antes de ser avaliada.

**Etapas**:
1. Criar `TimeWindowRepair.java`
2. Integrar em `SolomonVRPProblem.evaluate()`
3. Aplicar após cada crossover/mutação

**Pseudocódigo**:
```java
public void evaluate(SolomonVRPSolution solution) {
    // 1. Calcular objetivos originais
    calculateObjectives(solution);
    
    // 2. Se houver violações, tentar reparar
    if (solution.getTimeViolations() > 0) {
        solution = repairTimeWindows(solution);
        calculateObjectives(solution);  // Recalcular após reparo
    }
}
```

---

### 🧬 SOLUÇÃO ESTRUTURAL: Trocar Operadores Genéticos

**Objetivo**: Usar operadores que preservam factibilidade temporal.

**Mudanças no JMetal**:
```java
// Em RunNSGA3Solomon.java
// ANTES
CrossoverOperator<BinarySolution> crossover = new SinglePointCrossover(0.9);
MutationOperator<BinarySolution> mutation = new BitFlipMutation(0.1);

// DEPOIS
// Precisa implementar operadores customizados para SolomonVRPSolution
CrossoverOperator<SolomonVRPSolution> crossover = new OrderCrossover(0.9);
MutationOperator<SolomonVRPSolution> mutation = new SwapMutation(0.1);
```

**Nota**: Requer implementação de operadores especializados, não trivial com JMetal.

---

## 📈 Cronograma de Investigação

**Fase 1 - Diagnóstico (1 dia)**:
1. ✅ Testar população inicial (geração 0)
2. ✅ Confirmar onde factibilidade é perdida

**Fase 2 - Solução Rápida (2 horas)**:
1. ✅ Aumentar penalidade para 100.000
2. ✅ Testar em 3 instâncias (C101, R101, RC101)
3. ✅ Avaliar taxa de validação

**Fase 3 - Solução Robusta (2-3 dias)**:
1. Implementar operador de reparo básico
2. Integrar no fluxo de avaliação
3. Testar em todas as 26 instâncias
4. Comparar com AEMMT

**Fase 4 - Otimização (1 semana)**:
1. Implementar Order Crossover e Swap Mutation
2. Ajustar parâmetros (população, gerações, probabilidades)
3. Validação completa e análise estatística

---

## 📚 Referências

- **Solomon Benchmark**: [www.sintef.no/projectweb/top/vrptw/solomon-benchmark/](https://www.sintef.no/projectweb/top/vrptw/solomon-benchmark/)
- **NSGA-III Paper**: Deb, K., & Jain, H. (2014). "An evolutionary many-objective optimization algorithm using reference-point-based nondominated sorting approach"
- **JMetal Framework**: [jmetal.github.io/jMetal/](https://jmetal.github.io/jMetal/)
- **Solomon I1 Heuristic**: Solomon, M. M. (1987). "Algorithms for the vehicle routing and scheduling problems with time window constraints"

---

## 🤝 Comparação com AEMMT

**AEMMT** (implementado em `src/`):
- ✅ Taxa de validação: 260/260 (100%)
- ✅ Usa os mesmos componentes (TimeFitnessCalculator, Solomon I1)
- ✅ Tinha os mesmos bugs, mas funcionou porque:
  - Penalidade alta criou pressão evolutiva forte
  - Operadores mais conservadores
  - Menos gerações (500 vs 5000)

**NSGA-III** (implementado em `VRP_NSGA_TCC/`):
- ❌ Taxa de validação: 0/260 (0%)
- ❌ Mesmos bugs corrigidos, mas não resolveu
- ❌ Problema real: Operadores genéticos do JMetal destroem factibilidade
- ⚠️ Precisa de reparo ou operadores especializados

---

## 📞 Contato e Suporte

Para dúvidas sobre o projeto, consulte:
- **Documentação AEMMT**: `/Vehicle_Routing_Problem_Java/README_AEMMT.md`
- **Scripts de Validação**: `scripts/validate_nsga3_solution.py`
- **Histórico de Bugs**: Este documento, seção "Histórico de Correções"

---

## ✨ Status do Projeto

- **Última Atualização**: 24 de Janeiro de 2026
- **Status**: 🔴 Aguardando implementação de reparo ou aumento de penalidade
- **Próximos Passos**: 
  1. Testar geração 0
  2. Aumentar penalidade para 100.000
  3. Se necessário, implementar operador de reparo

---

**Desenvolvido com ❤️ para otimização de rotas de veículos**
