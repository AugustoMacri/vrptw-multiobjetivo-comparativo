# VRPTW Multiobjetivo — Comparativo AEMMT vs NSGA-III

Repositório de código-fonte, scripts de execução e dados brutos das 520 execuções experimentais do Trabalho de Conclusão de Curso:

> **Otimização Multiobjetivo do Problema de Roteamento de Veículos com Janelas de Tempo: Uma Análise Comparativa entre AEMMT e NSGA-III**
> Augusto Fernandes Macri — Faculdade de Computação, Universidade Federal de Uberlândia (FACOM-UFU), 2026.
> Orientadora: Profa. Dra. Christiane Regina Soares Brasil.

---

## Sobre o trabalho

O trabalho compara empiricamente dois Algoritmos Evolutivos Multiobjetivos (AEMOs) aplicados ao **Problema de Roteamento de Veículos com Janelas de Tempo (VRPTW)** sobre as instâncias clássicas de Solomon:

- **AEMMT** — Algoritmo Evolutivo Multiobjetivo com Muitas Tabelas (Brasil, 2013), implementado nativamente em Java 17;
- **NSGA-III** — Non-dominated Sorting Genetic Algorithm III (Deb & Jain, 2014), implementado sobre a biblioteca [jMetal](https://github.com/jMetal/jMetal).

Os dois algoritmos otimizam simultaneamente três objetivos: **distância**, **tempo de operação** e **custo de combustível** (calibrado com preços da ANP referentes ao 1º trimestre de 2026).

Foram realizadas **10 execuções independentes por instância** em cada algoritmo, totalizando **520 execuções validadas** (260 do AEMMT + 260 do NSGA-III) sobre as 26 instâncias das categorias C1, R1 e RC1 de Solomon.

---

## Estrutura do repositório

```
vrptw-multiobjetivo-comparativo/
├── README.md                    <- este arquivo
├── LICENSE                      <- licença MIT
│
├── aemmt/                       <- implementação do AEMMT (Java nativo)
│   ├── src/                     <- código-fonte Java
│   ├── pipeline/                <- scripts do pipeline de execução automatizada
│   ├── scripts/                 <- scripts auxiliares (validação, análise)
│   ├── results/                 <- resultados brutos das 260 execuções do AEMMT
│   ├── destaque_maps/           <- mapas de destaque usados no TCC
│   ├── run_full_pipeline_aemmt.sh
│   ├── validate_all_rigorous.sh
│   └── README_AEMMT.md          <- documentação técnica detalhada
│
└── nsga3/                       <- implementação do NSGA-III (jMetal + Gradle)
    ├── app/                     <- código-fonte Java + configuração Gradle
    │   ├── src/                 <- código-fonte principal
    │   ├── resultsNSGA3/        <- diretório de resultados por instância
    │   └── build.gradle.kts     <- configuração do build
    ├── pipeline/                <- scripts do pipeline de execução automatizada
    ├── results/                 <- resultados brutos das 260 execuções do NSGA-III
    ├── destaque_maps/           <- mapas de destaque usados no TCC
    ├── gradle/                  <- wrapper do Gradle
    ├── gradlew, gradlew.bat     <- scripts do Gradle wrapper (Unix/Windows)
    ├── run_full_pipeline_nsga3.sh
    ├── run_all_validations_nsga3.sh
    └── README.md                <- documentação técnica detalhada
```

---

## Pré-requisitos

### Obrigatórios

| Ferramenta | Versão | Uso |
|------------|--------|-----|
| **Java Development Kit (JDK)** | **17 ou superior** | Compilar e executar ambos os algoritmos. |
| **Bash** | 4+ | Executar os scripts de pipeline (`.sh`). No Windows, use **Git Bash** ou **WSL**. |
| **Python** | 3.8 ou superior | Gerar mapas das rotas e validar soluções (`.py`). |

### Bibliotecas Python

Instalar via `pip`:

```bash
pip install matplotlib numpy
```

### NSGA-III (adicional)

O NSGA-III usa **Gradle** para build, mas o wrapper (`./gradlew`) já vem incluído no repositório — **não é necessário instalar o Gradle separadamente**. As dependências (jMetal 6.0 e transitivas) são baixadas automaticamente no primeiro build.

### Ambiente testado

Os experimentos originais foram executados no seguinte ambiente:

- **Hardware**: AMD Ryzen 7 4800H, 16 GB RAM DDR4, SSD 256 GB
- **Sistema Operacional**: Ubuntu 22.04.5 LTS
- **Java**: OpenJDK 17
- **Python**: 3.10

O código também foi testado no Windows 10/11 via Git Bash.

---

## Como executar

### 1. Clonar o repositório

```bash
git clone https://github.com/AugustoMacri/vrptw-multiobjetivo-comparativo.git
cd vrptw-multiobjetivo-comparativo
```

### 2. Executar o AEMMT

O AEMMT é compilado e executado diretamente com `javac` e `java` (não usa Gradle/Maven).

**Executar uma única instância (modo interativo):**

```bash
cd aemmt
javac -d bin src/main/App.java src/genetic/*.java src/vrp/*.java
java -cp bin main.App
```

Ao rodar sem argumentos, um menu interativo pedirá o algoritmo, tipo de instância e número da instância.

**Executar uma instância específica (modo automático, para pipeline):**

```bash
java -cp bin main.App <numero_instancia> [sufixo_execucao]
```

Os índices de instância são:
- **C1**: 1-9 (C101 a C109)
- **R1**: 18-26 (R101 a R109)
- **RC1**: 41-48 (RC101 a RC108)

Exemplo — rodar C106 (instância 6) com sufixo `_exec09`:

```bash
java -cp bin main.App 6 _exec09
```

**Executar o pipeline completo (260 execuções — 10 por instância):**

```bash
cd aemmt
bash run_full_pipeline_aemmt.sh
```

Esse script cuida da compilação, execução das 260 rodadas e da agregação dos resultados. Tempo estimado: **algumas horas** no hardware de referência.

### 3. Executar o NSGA-III

O NSGA-III usa Gradle para gerenciar dependências (jMetal).

**Executar uma única instância:**

```bash
cd nsga3
./gradlew run --args="app/src/main/java/instances/solomon/C106.txt"
```

No Windows (cmd/PowerShell):

```powershell
cd nsga3
.\gradlew.bat run --args="app/src/main/java/instances/solomon/C106.txt"
```

Na primeira execução, o Gradle baixará todas as dependências automaticamente (~50 MB).

**Executar o pipeline completo (260 execuções):**

```bash
cd nsga3
bash run_full_pipeline_nsga3.sh
```

---

## Reprodução dos experimentos completos

Para reproduzir integralmente os 520 experimentos do TCC:

```bash
# 1. AEMMT — 260 execuções
cd aemmt
bash run_full_pipeline_aemmt.sh
cd ..

# 2. NSGA-III — 260 execuções
cd nsga3
bash run_full_pipeline_nsga3.sh
cd ..

# 3. Validação rigorosa das 520 soluções
cd aemmt
bash validate_all_rigorous.sh
cd ../nsga3
bash run_all_validations_nsga3.sh
```

A validação rigorosa verifica automaticamente, para cada solução gerada:

- **Cobertura**: exatamente 100 clientes visitados, cada um uma única vez;
- **Capacidade**: soma das demandas em cada rota ≤ 200 unidades por veículo;
- **Janelas de tempo**: instante de início do serviço respeita `[eᵢ, lᵢ]` para todos os clientes;
- **Corretude**: distâncias e tempos euclidianos calculados corretamente.

---

## Estrutura dos resultados

Os arquivos de saída seguem o padrão:

- **`results/<categoria>/<instância>/evo_<instância>_exec<NN>.txt`**: arquivo de evolução da execução `NN`, contendo as rotas finais, valores dos objetivos por geração, tempo de execução e diagnósticos.
- **`results/<categoria>/summary_<categoria>.txt`**: resumo consolidado por categoria (C1, R1, RC1) com a melhor execução por instância.

Exemplo de caminho:

```
aemmt/results/C1/C106/evo_c106_exec09.txt
nsga3/results/C1/C106/evo_c106_exec04.txt
```

---

## Regeração dos mapas do TCC

Os mapas das melhores execuções por categoria (C106, R108, RC106) são regeráveis a partir dos scripts:

```bash
# AEMMT
cd aemmt
bash regen_destaque_maps.sh

# NSGA-III
cd nsga3
bash regen_destaque_maps.sh
```

As imagens ficam disponíveis em `destaque_maps/` em cada subprojeto.

---

## Documentação técnica detalhada

Cada subprojeto possui sua própria documentação estendida:

- [`aemmt/README_AEMMT.md`](aemmt/README_AEMMT.md) — detalhes de arquitetura, parâmetros e operadores do AEMMT;
- [`aemmt/PIPELINE.md`](aemmt/PIPELINE.md) — descrição completa do pipeline de execução do AEMMT;
- [`nsga3/README.md`](nsga3/README.md) — detalhes de configuração da jMetal e do NSGA-III;
- [`nsga3/PIPELINE.md`](nsga3/PIPELINE.md) — descrição do pipeline do NSGA-III;
- [`nsga3/GUIA_SCRIPTS_NSGA3.md`](nsga3/GUIA_SCRIPTS_NSGA3.md) — guia rápido dos scripts auxiliares;
- [`nsga3/RESTRICOES_HARD_NSGA3.md`](nsga3/RESTRICOES_HARD_NSGA3.md) — tratamento das restrições no NSGA-III.

---

## Citação

Se este trabalho ou o código deste repositório for útil em sua pesquisa, cite como:

```bibtex
@mastersthesis{Macri2026,
  author  = {Augusto Fernandes Macri},
  title   = {Otimiza\c{c}\~ao Multiobjetivo do Problema de Roteamento de Ve\'iculos com Janelas de Tempo: Uma An\'alise Comparativa entre AEMMT e NSGA-III},
  school  = {Faculdade de Computa\c{c}\~ao, Universidade Federal de Uberl\^andia (FACOM-UFU)},
  year    = {2026},
  address = {Uberl\^andia, Brasil},
  type    = {Trabalho de Conclus\~ao de Curso (Bacharelado em Sistemas de Informa\c{c}\~ao)},
  url     = {https://github.com/AugustoMacri/vrptw-multiobjetivo-comparativo}
}
```

---

## Licença

Este projeto está licenciado sob a **Licença MIT** — veja o arquivo [`LICENSE`](LICENSE) para detalhes.

---

## Contato

Dúvidas, sugestões ou problemas: abra uma [issue no repositório](https://github.com/AugustoMacri/vrptw-multiobjetivo-comparativo/issues) ou entre em contato pelo e-mail institucional.
