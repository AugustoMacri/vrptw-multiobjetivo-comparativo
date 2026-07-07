#!/bin/bash
# =============================================================================
# CONFIGURACAO CENTRALIZADA DO PIPELINE NSGA-III
# =============================================================================
# Variaveis usadas por todos os scripts do pipeline. Edite aqui para ajustar
# o comportamento de uma execucao completa sem precisar mexer em multiplos
# scripts.
# =============================================================================

# --- Identificacao do algoritmo (usado em titulos de mapas, relatorios, etc) ---
export ALGO_NAME="NSGA-III"
export ALGO_DESC="Non-dominated Sorting Genetic Algorithm III"

# --- Parametros de execucao ---
export NUM_EXECUTIONS=10                 # Quantas vezes cada instancia eh executada
export NUM_GENERATIONS=3000              # Geracoes por execucao (definido no Java)

# --- Instancias do benchmark de Solomon ---
# Mapeamento <nome>:<numero_java>
export INSTANCES_C1=("C101:1" "C102:2" "C103:3" "C104:4" "C105:5" "C106:6" "C107:7" "C108:8" "C109:9")
export INSTANCES_R1=("R101:18" "R102:19" "R103:20" "R104:21" "R105:22" "R106:23" "R107:24" "R108:25" "R109:26")
export INSTANCES_RC1=("RC101:41" "RC102:42" "RC103:43" "RC104:44" "RC105:45" "RC106:46" "RC107:47" "RC108:48")

# --- Diretorios padronizados ---
export RESULTS_DIR="results"                       # Pasta-raiz dos resultados padronizados
export RESULTS_C1_DIR="$RESULTS_DIR/C1"
export RESULTS_R1_DIR="$RESULTS_DIR/R1"
export RESULTS_RC1_DIR="$RESULTS_DIR/RC1"
export SUMMARY_DIR="$RESULTS_DIR/summary"
export LOGS_DIR="logs"

# --- Diretorios do projeto NSGA-III ---
export INSTANCES_DIR="app/src/main/java/instances/solomon"  # Onde estao os .txt das instancias
export GRADLE_CLASSES="app/build/classes/java/main"         # Onde fica o bytecode compilado

# --- Diretorio intermediario do Java (criado pelo proprio main.App) ---
# OBS: o NSGA-III cria saidas em app/resultsNSGA3 quando rodado via Gradle,
# ou em resultsNSGA3 quando rodado via java direto. Tratamos ambos.
export JAVA_OUTPUT_DIR="resultsNSGA3"
export JAVA_OUTPUT_DIR_GRADLE="app/resultsNSGA3"

# --- Selecao de python (compatibilidade Linux/Windows) ---
# Testa EXECUTANDO --version porque no Windows o `python` pode ser apenas o
# alias do Microsoft Store que apenas redireciona o usuario.
detect_python() {
    if python3 --version &> /dev/null 2>&1; then
        echo "python3"
    elif python --version &> /dev/null 2>&1; then
        echo "python"
    else
        echo ""
    fi
}
export PYTHON_CMD=$(detect_python)

# --- Forca encoding UTF-8 nas chamadas Python (resolve emoji/acentos em Windows) ---
export PYTHONIOENCODING=utf-8

# --- Cores para output (opcional, desativar com NO_COLOR=1) ---
if [ -z "$NO_COLOR" ] && [ -t 1 ]; then
    export COLOR_RED='\033[0;31m'
    export COLOR_GREEN='\033[0;32m'
    export COLOR_YELLOW='\033[0;33m'
    export COLOR_BLUE='\033[0;34m'
    export COLOR_RESET='\033[0m'
else
    export COLOR_RED=''
    export COLOR_GREEN=''
    export COLOR_YELLOW=''
    export COLOR_BLUE=''
    export COLOR_RESET=''
fi

# --- Funcoes utilitarias compartilhadas ---
log_info()  { echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET}  $*"; }
log_ok()    { echo -e "${COLOR_GREEN}[OK]${COLOR_RESET}    $*"; }
log_warn()  { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET}  $*"; }
log_error() { echo -e "${COLOR_RED}[ERRO]${COLOR_RESET}  $*" >&2; }

# --- Constroi classpath com JARs do Gradle (para execucao direta com java) ---
build_classpath() {
    local cp="$GRADLE_CLASSES"
    local gradle_cache="$HOME/.gradle/caches/modules-2/files-2.1"

    # No Windows (Git Bash) usar ; como separador; no Linux usar :
    local sep=":"
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
        sep=";"
    fi

    if [ -d "$gradle_cache" ]; then
        for jar in "$gradle_cache"/*/*/*/*/*.jar; do
            [ -f "$jar" ] && cp="$cp$sep$jar"
        done
    fi
    echo "$cp"
}
export -f build_classpath

# --- Verifica dependencias basicas (chamada por pipeline.sh) ---
check_dependencies() {
    local missing=0

    if ! command -v java &> /dev/null; then
        log_error "java nao encontrado no PATH"
        missing=1
    fi

    if [ ! -f "./gradlew" ] && [ ! -f "gradlew" ]; then
        log_warn "gradlew nao encontrado. Pipeline depende dele para compilar."
    fi

    if [ -z "$PYTHON_CMD" ]; then
        log_error "python ou python3 nao encontrado no PATH"
        missing=1
    fi

    if [ "$missing" -ne 0 ]; then
        log_error "Dependencias faltando. Instale-as antes de continuar."
        return 1
    fi

    log_ok "Dependencias verificadas (java, $PYTHON_CMD)"
    return 0
}

# --- Marca este arquivo como ja carregado (evita re-source) ---
export PIPELINE_CONFIG_LOADED=1
