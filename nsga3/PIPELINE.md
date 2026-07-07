# Pipeline NSGA-III — Guia de Execucao

Pipeline unificado, automatizado e padronizado para o algoritmo **NSGA-III**
aplicado ao VRPTW. Um comando executa **toda** a sequencia:
limpeza → execucao → validacao → tabelas → mapas.

## TL;DR — Maquina nova

```bash
# 1. Dependencias
# Java 17, Python 3 com matplotlib e numpy
sudo apt install -y openjdk-17-jdk python3 python3-pip
pip install matplotlib numpy

# 2. Clone e entre na pasta
git clone <repo-url> TCC
cd TCC

# 3. Rode tudo
bash run_full_pipeline_nsga3.sh
```

Tempo estimado: **~7h** (260 execucoes × ~90s cada + 15min de pos-processamento).
O Gradle compila automaticamente na primeira execucao.

## Estrutura do pipeline

```
pipeline/
├── config.sh          Variaveis centralizadas
├── clean.sh           Limpa resultados antigos
├── run_all.sh         Compila (via Gradle) e executa 10 rodadas por instancia
├── validate.sh        Valida solucoes (capacidade, janelas, cobertura)
├── summarize.sh       Gera tabelas .txt/.csv com melhores resultados
├── summarize.py       (utilizado por summarize.sh)
├── maps.sh            Gera mapas PNG padronizados
├── plot_map.py        (utilizado por maps.sh)
└── pipeline.sh        Master que orquestra tudo

run_full_pipeline_nsga3.sh    Atalho na raiz do projeto
```

## Diretorios de saida

```
results/
├── C1/
│   ├── C101/
│   │   ├── evo_c101_exec01.txt       Rotas finais + frente de Pareto
│   │   ├── pareto_c101_exec01.txt    Pareto detalhada
│   │   ├── stats_c101_exec01.txt     Log da JVM
│   │   ├── ...exec02.txt ... exec10.txt
│   │   └── maps/
│   │       ├── map_nsga3_c101_exec01.png
│   │       └── ...
│   ├── C102/ ... C109/
├── R1/   R101/ ... R109/
├── RC1/  RC101/ ... RC108/
└── summary/
    ├── summary_c1.txt           Tabela formatada por categoria
    ├── summary_r1.txt
    ├── summary_rc1.txt
    ├── summary_all.txt          Consolidado das 3 categorias
    └── summary_all.csv          Mesmo dado em CSV

logs/
└── pipeline_<timestamp>.log     Log completo de cada execucao do pipeline
```

## Comandos detalhados

### Pipeline completo (uso normal)

```bash
bash run_full_pipeline_nsga3.sh
```

### Apenas uma categoria

```bash
bash run_full_pipeline_nsga3.sh C1
bash run_full_pipeline_nsga3.sh C1 R1
bash run_full_pipeline_nsga3.sh RC1
```

### Pulando etapas (uso avancado)

```bash
# Re-processar apos algumas mudancas, sem re-executar 260x
bash run_full_pipeline_nsga3.sh --skip-clean --skip-run

# Executar mas pular mapas (mais rapido)
bash run_full_pipeline_nsga3.sh --skip-maps

# Apenas regenerar mapas a partir de resultados existentes
bash run_full_pipeline_nsga3.sh --skip-clean --skip-run --skip-validate --skip-summarize
```

### Etapas individuais

```bash
bash pipeline/clean.sh
bash pipeline/run_all.sh C1
bash pipeline/validate.sh
bash pipeline/summarize.sh
bash pipeline/maps.sh C1
```

## Limpeza automatica

`pipeline/clean.sh` remove **apenas** o que e gerado automaticamente:

- `results/` (estrutura padronizada nova)
- `resultsNSGA3/` e `app/resultsNSGA3/` (intermediarios)
- `results_validation_NSGA3_C1/`, `R1/`, `RC1/` (legado)
- `debug_output/`
- `logs/`
- `best_results_summary_nsga3.{txt,csv}`, `validation_results.txt` (legado)

**NUNCA** remove: codigo-fonte, instancias, scripts, configuracoes, classes
compiladas (`app/build/`).

## Configuracao

Edite `pipeline/config.sh` para alterar parametros globais:

| Variavel             | Default                                       | Descricao                              |
|----------------------|-----------------------------------------------|----------------------------------------|
| `NUM_EXECUTIONS`     | 10                                            | Quantas vezes cada instancia eh rodada |
| `INSTANCES_C1`       | C101..C109                                    | Lista de instancias C1 (nome:numero)   |
| `INSTANCES_R1`       | R101..R109                                    | Lista de instancias R1                  |
| `INSTANCES_RC1`      | RC101..RC108                                  | Lista de instancias RC1                 |
| `RESULTS_DIR`        | `results`                                     | Pasta-raiz dos resultados              |
| `INSTANCES_DIR`      | `app/src/main/java/instances/solomon`         | Onde ficam os .txt das instancias      |

## Padronizacao com AEMMT

Os dois projetos agora seguem a MESMA estrutura:

| Item                    | NSGA-III                       | AEMMT                          |
|-------------------------|--------------------------------|--------------------------------|
| Pasta de pipeline       | `pipeline/`                    | `pipeline/`                    |
| Atalho raiz             | `run_full_pipeline_nsga3.sh`   | `run_full_pipeline_aemmt.sh`   |
| Resultados              | `results/<cat>/<inst>/`        | `results/<cat>/<inst>/`        |
| Tabelas resumo          | `results/summary/`             | `results/summary/`             |
| Logs                    | `logs/pipeline_<ts>.log`       | `logs/pipeline_<ts>.log`       |
| Nome de evo             | `evo_<inst>_exec<NN>.txt`      | `evo_<inst>_exec<NN>.txt`      |
| Nome de pareto          | `pareto_<inst>_exec<NN>.txt`   | `pareto_<inst>_exec<NN>.txt`   |
| Nome de stats           | `stats_<inst>_exec<NN>.txt`    | `stats_<inst>_exec<NN>.txt`    |
| Nome de mapa            | `map_nsga3_<inst>_exec<NN>.png`| `map_aemmt_<inst>_exec<NN>.png`|
| Titulo de mapa          | "C101 - NSGA-III (Exec 01)"    | "C101 - AEMMT (Exec 01)"       |

## Riscos e cuidados

- **Tempo de execucao**: ~7h. Use `screen` ou `tmux` para nao perder se a
  conexao SSH cair.
- **Espaco em disco**: ~500 MB para os 260 evos + paretos + mapas.
- **Re-execucao**: `clean.sh` apaga tudo. Para retomar de onde parou, use
  `--skip-clean --skip-run`.
- **Compilacao Gradle**: a primeira execucao baixa as dependencias do JMetal
  (~50MB). Garantir que o cache `~/.gradle/caches/` exista apos isso.
- **Classpath**: `pipeline/run_all.sh` constroi o classpath manualmente a
  partir do cache Gradle. Se Gradle estiver em outro local, ajustar
  `build_classpath()` em `config.sh`.

## Sugestoes para futuras execucoes

- Para rodar apenas 3 execucoes em vez de 10 (debug rapido), edite
  `NUM_EXECUTIONS=3` em `pipeline/config.sh`.
- Para alterar o numero de divisoes do NSGA-III (que determina a populacao
  efetiva), edite `NUMBER_OF_DIVISIONS` em
  `app/src/main/java/genetic/nsga/VRPNSGAIIIRunner.java`.
- Para gerar mapas em alta qualidade (300dpi+), edite o `dpi=200` em
  `pipeline/plot_map.py:plot_routes`.
