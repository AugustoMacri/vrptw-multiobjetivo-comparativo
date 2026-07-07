#!/bin/bash
# =============================================================================
# EXECUCAO DAS 10 RODADAS DE TODAS AS INSTANCIAS (AEMMT)
# =============================================================================
# Compila uma vez. Para cada instancia (C1, R1, RC1) executa NUM_EXECUTIONS
# vezes via `java -cp bin main.App <numero> _exec<NN>`. Salva os 3 artefatos
# em results/<categoria>/<instancia>/:
#   - evo_<inst>_exec<NN>.txt        (rotas + Pareto)
#   - pareto_<inst>_exec<NN>.txt     (frente de Pareto detalhada)
#   - stats_<inst>_exec<NN>.txt      (log stdout/stderr da JVM)
#
# Uso:
#   bash pipeline/run_all.sh                  # roda C1 + R1 + RC1 (260 exec)
#   bash pipeline/run_all.sh C1               # apenas C1
#   bash pipeline/run_all.sh C1 R1            # C1 e R1
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

source "$SCRIPT_DIR/config.sh"

# Categorias a executar (todas se nada for passado)
CATEGORIES=("$@")
if [ ${#CATEGORIES[@]} -eq 0 ]; then
    CATEGORIES=(C1 R1 RC1)
fi

# Compilacao
log_info "Compilando o projeto Java (AEMMT)..."
rm -rf "$BIN_DIR"
mkdir -p "$BIN_DIR"
javac -d "$BIN_DIR" -sourcepath "$SRC_DIR" -encoding UTF-8 \
    $(find "$SRC_DIR" -name "*.java") 2>&1 | tee "$LOGS_DIR/compile.log"

if [ ! -f "$BIN_DIR/main/App.class" ]; then
    log_error "Compilacao falhou. Veja $LOGS_DIR/compile.log"
    exit 1
fi
log_ok "Compilacao concluida"

# Funcao para executar uma categoria
run_category() {
    local cat="$1"
    local result_dir
    local -n inst_arr

    case "$cat" in
        C1)  result_dir="$RESULTS_C1_DIR";  inst_arr=INSTANCES_C1  ;;
        R1)  result_dir="$RESULTS_R1_DIR";  inst_arr=INSTANCES_R1  ;;
        RC1) result_dir="$RESULTS_RC1_DIR"; inst_arr=INSTANCES_RC1 ;;
        *)   log_error "Categoria desconhecida: $cat"; return 1 ;;
    esac

    log_info "===== Executando categoria $cat ($(echo ${inst_arr[@]} | wc -w) instancias x $NUM_EXECUTIONS execucoes) ====="
    mkdir -p "$result_dir"

    for entry in "${inst_arr[@]}"; do
        local inst_name="${entry%%:*}"
        local inst_num="${entry##*:}"
        local inst_lower=$(echo "$inst_name" | tr "[:upper:]" "[:lower:]")
        local dest_dir="$result_dir/$inst_name"
        mkdir -p "$dest_dir"

        for n in $(seq -w 1 $NUM_EXECUTIONS); do
            local suffix="_exec${n}"
            local start=$(date +%s)
            log_info ">>> $cat $inst_name exec $n"

            # Executa o Java com sufixo. Saidas vao para resultsMulti/.
            java -cp "$BIN_DIR" main.App "$inst_num" "$suffix" \
                > "$dest_dir/stats_${inst_lower}${suffix}.txt" 2>&1

            local end=$(date +%s)
            local dur=$((end - start))

            # Move artefatos para o destino padronizado
            local evo_src="$JAVA_OUTPUT_DIR/evo_${inst_lower}${suffix}.txt"
            local pareto_src="$JAVA_OUTPUT_DIR/pareto_${inst_lower}${suffix}.txt"

            if [ -f "$evo_src" ]; then
                mv "$evo_src" "$dest_dir/"
            else
                log_warn "evo nao encontrado: $evo_src"
            fi
            if [ -f "$pareto_src" ]; then
                mv "$pareto_src" "$dest_dir/"
            fi

            log_ok "    duracao: ${dur}s"
        done
    done
}

# Execucao das categorias selecionadas
START_TOTAL=$(date +%s)
for cat in "${CATEGORIES[@]}"; do
    run_category "$cat"
done
END_TOTAL=$(date +%s)
TOTAL_DUR=$((END_TOTAL - START_TOTAL))
log_ok "Todas as execucoes concluidas em $((TOTAL_DUR / 60)) minutos"
