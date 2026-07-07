# Guia de Scripts NSGA-III

Este guia lista todos os scripts disponíveis para análise e validação dos resultados do NSGA-III.

## 📊 Scripts de Análise e Resumo

### 1. **Gerar Tabela de Melhores Resultados**
```bash
python3 scripts/summarize_best_results_nsga3.py
```

**O que faz:**
- Analisa todas as 10 execuções de cada instância (C101-C109, R101-R109, RC101-RC108)
- Identifica a melhor execução de cada instância (baseado na menor distância)
- Gera tabela com estatísticas por categoria (C1, R1, RC1)

**Saída:**
- `best_results_summary_nsga3.txt` - Tabela formatada
- `best_results_summary_nsga3.csv` - Dados em CSV

### 2. **Gerar Resumo de Resultados por Instância**
```bash
python3 scripts/generate_nsga3_results_summary.py
```

**O que faz:**
- Gera arquivos de resumo individuais para cada instância
- Similar ao formato `resultados_aemmt_*.txt` do AEMMT

## ✅ Scripts de Validação

### 3. **Validação Rigorosa Completa** (RECOMENDADO)
```bash
bash validate_all_rigorous_nsga3.sh
```

**O que faz:**
- Valida TODAS as soluções (C, R, RC) - aproximadamente 260 execuções
- Verifica:
  - Cobertura de clientes (todos visitados 1x)
  - Capacidade dos veículos
  - Janelas de tempo
- Gera relatórios detalhados passo a passo

**Saída:**
- `results_validation_NSGA3_C1_detailed/` - Relatórios detalhados C1
- `results_validation_NSGA3_R1_detailed/` - Relatórios detalhados R1
- `results_validation_NSGA3_RC1_detailed/` - Relatórios detalhados RC1

### 4. **Validar Solução Específica**
```bash
python3 scripts/validate_nsga3_solution.py <instancia.txt> <solucao.txt>
```

**Exemplo:**
```bash
python3 scripts/validate_nsga3_solution.py \
    app/src/main/java/instances/solomon/C101.txt \
    resultsNSGA3/evo_c101_exec01.txt
```

### 5. **Validar Todas as Soluções (Simples)**
```bash
python3 scripts/validate_all_nsga3.py
```

**O que faz:**
- Valida todas as 260 execuções
- Mostra resumo de violações
- Não gera relatórios detalhados

### 6. **Gerar Relatório Detalhado de Uma Solução**
```bash
python3 scripts/generate_detailed_validation_nsga3.py <instancia> <solucao> <saida>
```

**Exemplo:**
```bash
python3 scripts/generate_detailed_validation_nsga3.py \
    app/src/main/java/instances/solomon/C101.txt \
    resultsNSGA3/evo_c101_exec01.txt \
    relatorio_c101_exec01.txt
```

## 🗺️ Scripts de Visualização

### 7. **Gerar Mapas de Rotas**
```bash
bash generate_route_maps_nsga3.sh [instancia|all_c1]
```

**Exemplos:**
```bash
# Gerar mapas apenas para C101
bash generate_route_maps_nsga3.sh C101

# Gerar mapas para todas as instâncias C1
bash generate_route_maps_nsga3.sh all_c1
```

**O que faz:**
- Gera mapas visuais das rotas
- Cria visualizações inicial e final para cada execução

### 8. **Gerar Todos os Mapas**
```bash
bash generate_all_maps.sh
```

**O que faz:**
- Gera mapas para TODAS as instâncias (C, R, RC)
- Demora bastante tempo!

## 📋 Workflow Recomendado

Para análise completa dos resultados, execute na seguinte ordem:

### 1. **Validar todas as soluções**
```bash
bash validate_all_rigorous_nsga3.sh
```
Isso valida e gera relatórios detalhados de todas as execuções.

### 2. **Gerar tabela de melhores resultados**
```bash
python3 scripts/summarize_best_results_nsga3.py
```
Isso cria a tabela com os melhores resultados de cada instância.

### 3. **Gerar mapas das melhores soluções** (opcional)
```bash
# Escolha instâncias específicas ou gere todos
bash generate_route_maps_nsga3.sh C101
bash generate_route_maps_nsga3.sh R101
bash generate_route_maps_nsga3.sh RC101
```

## 📁 Estrutura de Diretórios

```
TCC/
├── resultsNSGA3/                      # Resultados brutos
│   ├── evo_c101_exec01.txt
│   ├── evo_c101_exec02.txt
│   └── ...
│
├── results_validation_NSGA3_C1/       # Validação básica C1
├── results_validation_NSGA3_R1/       # Validação básica R1
├── results_validation_NSGA3_RC1/      # Validação básica RC1
│
├── results_validation_NSGA3_C1_detailed/   # Relatórios detalhados C1
├── results_validation_NSGA3_R1_detailed/   # Relatórios detalhados R1
├── results_validation_NSGA3_RC1_detailed/  # Relatórios detalhados RC1
│
├── best_results_summary_nsga3.txt     # Tabela de melhores resultados
├── best_results_summary_nsga3.csv     # Tabela em CSV
│
└── scripts/
    ├── summarize_best_results_nsga3.py
    ├── generate_detailed_validation_nsga3.py
    ├── validate_nsga3_solution.py
    ├── validate_all_nsga3.py
    └── generate_maps_nsga3.py
```

## 🔧 Comparação com AEMMT

### Scripts Equivalentes

| AEMMT | NSGA-III |
|-------|----------|
| `scripts/summarize_best_results.py` | `scripts/summarize_best_results_nsga3.py` |
| `scripts/generate_detailed_validation.py` | `scripts/generate_detailed_validation_nsga3.py` |
| `validate_all_rigorous.sh` | `validate_all_rigorous_nsga3.sh` |
| `scripts/validate_solution_rigorous.py` | `scripts/validate_nsga3_solution.py` |

### Principais Diferenças

1. **Formato de Arquivos**
   - AEMMT: "Distância total: 1245,20"
   - NSGA-III: "NSGA3_DistânciaMin 1429,09"

2. **Formato de Rotas**
   - AEMMT: "Veículo 1: Depósito(0) -> Cliente(13) -> ..."
   - NSGA-III: "=== ROTA DO VEÍCULO 1 ===" com coordenadas explícitas

3. **Multi-objetivo**
   - AEMMT: Ponderação de objetivos
   - NSGA-III: Frente de Pareto + arquivo específico de ponderação

## 💡 Dicas

- **Para validação rápida**: Use `validate_all_nsga3.py`
- **Para análise completa**: Use `validate_all_rigorous_nsga3.sh`
- **Para comparar resultados**: Use `summarize_best_results_nsga3.py`
- **Para visualização**: Use `generate_route_maps_nsga3.sh`

## ⚠️ Notas Importantes

1. Os scripts assumem que os resultados estão em `resultsNSGA3/`
2. As instâncias Solomon devem estar em `app/src/main/java/instances/solomon/`
3. A validação rigorosa pode demorar vários minutos
4. A geração de mapas requer matplotlib
