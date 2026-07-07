# Pipeline AEMMT — Guia de Execucao

Pipeline unificado, automatizado e padronizado para o algoritmo **AEMMT**
aplicado ao VRPTW. Um comando executa **toda** a sequencia:
limpeza → execucao → validacao → tabelas → mapas.

## TL;DR — Maquina nova

```bash
# 1. Dependencias
# Java 17, Python 3 com matplotlib e numpy
# (em distros Debian/Ubuntu)
sudo apt install -y openjdk-17-jdk python3 python3-pip
pip install matplotlib numpy

# 2. Clone e entre na pasta
git clone <repo-url> Vehicle_Routing_Problem_Java
cd Vehicle_Routing_Problem_Java

# 3. Rode tudo
bash run_full_pipeline_aemmt.sh
```

Tempo estimado: **~6h** (260 execucoes × ~80s cada + 15min de pos-processamento)
em maquina Linux com Java 17.

## Estrutura do pipeline

```
pipeline/
├── config.sh          Variaveis centralizadas (instancias, diretorios, etc)
├── clean.sh           Limpa resultados antigos
├── run_all.sh         Compila e executa 10 rodadas por instancia
├── validate.sh        Valida solucoes (capacidade, janelas, cobertura)
├── summarize.sh       Gera tabelas .txt/.csv com melhores resultados
├── summarize.py       (utilizado por summarize.sh)
├── maps.sh            Gera mapas PNG padronizados
├── plot_map.py        (utilizado por maps.sh)
└── pipeline.sh        Master que orquestra tudo

run_full_pipeline_aemmt.sh    Atalho na raiz do projeto
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
│   │       ├── map_aemmt_c101_exec01.png
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
bash run_full_pipeline_aemmt.sh
```

### Apenas uma categoria

```bash
bash run_full_pipeline_aemmt.sh C1
bash run_full_pipeline_aemmt.sh C1 R1
bash run_full_pipeline_aemmt.sh RC1
```

### Pulando etapas (uso avancado)

```bash
# Re-processar apos algumas mudancas, sem re-executar 260x
bash run_full_pipeline_aemmt.sh --skip-clean --skip-run

# Executar mas pular mapas (mais rapido)
bash run_full_pipeline_aemmt.sh --skip-maps

# Apenas regenerar mapas a partir de resultados existentes
bash run_full_pipeline_aemmt.sh --skip-clean --skip-run --skip-validate --skip-summarize
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
- `resultsMulti/` (intermediario da JVM)
- `results_validation_C1/`, `results_validation_R1/`, `results_validation_RC1/` (legado)
- `results_validation_*_detailed/` (legado)
- `mapping_results/` (legado)
- `logs/`
- `best_results_summary.{txt,csv}` (legado)

**NUNCA** remove: codigo-fonte, instancias, scripts, configuracoes.

## O que mudou no AEMMT

- `App.java` agora aceita um segundo argumento opcional `<exec_suffix>` e
  anexa esse sufixo aos arquivos gerados (`evo_c101_exec01.txt` em vez de
  `evo_c101.txt`).
- O arquivo `evo_<instancia>_exec<N>.txt` agora inclui uma secao final
  **FRENTE DE PARETO (TABELA DE NAO-DOMINANCIA)** com:
  - tamanho da Pareto
  - identificacao da execucao
  - lista enumerada das solucoes nao-dominadas (idx, distancia, tempo,
    combustivel, num_veiculos)
  - contadores de diagnostico (tentativas, insercoes, rejeicoes)

## Configuracao

Edite `pipeline/config.sh` para alterar parametros globais:

| Variavel             | Default                              | Descricao                              |
|----------------------|--------------------------------------|----------------------------------------|
| `NUM_EXECUTIONS`     | 10                                   | Quantas vezes cada instancia eh rodada |
| `INSTANCES_C1`       | C101..C109                           | Lista de instancias C1 (nome:numero)   |
| `INSTANCES_R1`       | R101..R109                           | Lista de instancias R1                  |
| `INSTANCES_RC1`      | RC101..RC108                         | Lista de instancias RC1                 |
| `RESULTS_DIR`        | `results`                            | Pasta-raiz dos resultados              |
| `INSTANCES_DIR`      | `src/instances/solomon`              | Onde ficam os .txt das instancias      |

## Padronizacao com NSGA-III

Os dois projetos agora seguem a MESMA estrutura:

| Item                    | AEMMT                          | NSGA-III                       |
|-------------------------|--------------------------------|--------------------------------|
| Pasta de pipeline       | `pipeline/`                    | `pipeline/`                    |
| Atalho raiz             | `run_full_pipeline_aemmt.sh`   | `run_full_pipeline_nsga3.sh`   |
| Resultados              | `results/<cat>/<inst>/`        | `results/<cat>/<inst>/`        |
| Tabelas resumo          | `results/summary/`             | `results/summary/`             |
| Logs                    | `logs/pipeline_<ts>.log`       | `logs/pipeline_<ts>.log`       |
| Nome de evo             | `evo_<inst>_exec<NN>.txt`      | `evo_<inst>_exec<NN>.txt`      |
| Nome de pareto          | `pareto_<inst>_exec<NN>.txt`   | `pareto_<inst>_exec<NN>.txt`   |
| Nome de stats           | `stats_<inst>_exec<NN>.txt`    | `stats_<inst>_exec<NN>.txt`    |
| Nome de mapa            | `map_aemmt_<inst>_exec<NN>.png`| `map_nsga3_<inst>_exec<NN>.png`|
| Titulo de mapa          | "C101 - AEMMT (Exec 01)"       | "C101 - NSGA-III (Exec 01)"    |

Resultado: copiar a pasta `results/` de uma maquina para outra so para
comparacao manual eh trivial; os scripts de plot/sumarizacao funcionam em
ambos os projetos.

## Riscos e cuidados

- **Tempo de execucao**: ~6h. Use `screen` ou `tmux` para nao perder se a
  conexao SSH cair.
- **Espaco em disco**: ~500 MB para os 260 evos + paretos + mapas.
- **Re-execucao**: `clean.sh` apaga tudo. Para retomar de onde parou, use
  `--skip-clean --skip-run` e reprocesse so o que precisa.
- **Compilacao Java**: o pipeline recompila do zero a cada execucao. Se ja
  estiver compilado e nao mudou o codigo, use `--skip-run` apos rodar.

## Sugestoes para futuras execucoes

- Para rodar apenas 3 execucoes em vez de 10 (debug rapido), edite
  `NUM_EXECUTIONS=3` em `pipeline/config.sh`.
- Para adicionar novas instancias (ex: C2), adicione `INSTANCES_C2` em
  `config.sh` e estenda `run_all.sh` e `maps.sh` com a nova categoria.
- Para gerar mapas em alta qualidade (300dpi+), edite o `dpi=200` em
  `pipeline/plot_map.py:plot_routes`.
