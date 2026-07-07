#!/bin/bash
# =============================================================================
# PIPELINE COMPLETO AEMMT
# =============================================================================
# Executa as 5 etapas em sequencia:
#   1) clean.sh        - limpa resultados anteriores
#   2) run_all.sh      - executa 10 rodadas de cada instancia
#   3) validate.sh     - valida solucoes
#   4) summarize.sh    - gera tabelas de melhores resultados
#   5) maps.sh         - gera mapas PNG
#
# Falha cedo se qualquer etapa retornar erro.
#
# Uso:
#   bash pipeline/pipeline.sh                 # tudo (C1 + R1 + RC1)
#   bash pipeline/pipeline.sh C1              # apenas C1
#   bash pipeline/pipeline.sh C1 R1           # C1 e R1
#   bash pipeline/pipeline.sh --skip-clean    # pula limpeza
#   bash pipeline/pipeline.sh --skip-run      # pula execucao (so reprocessa)
#   bash pipeline/pipeline.sh --skip-maps     # pula mapas (mais rapido)
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

source "$SCRIPT_DIR/config.sh"

# --- Flags ---
SKIP_CLEAN=0
SKIP_RUN=0
SKIP_VALIDATE=0
SKIP_SUMMARIZE=0
SKIP_MAPS=0
CATEGORIES=()

while [ $# -gt 0 ]; do
    case "$1" in
        --skip-clean)     SKIP_CLEAN=1 ;;
        --skip-run)       SKIP_RUN=1 ;;
        --skip-validate)  SKIP_VALIDATE=1 ;;
        --skip-summarize) SKIP_SUMMARIZE=1 ;;
        --skip-maps)      SKIP_MAPS=1 ;;
        -h|--help)
            head -n 30 "$0" | grep "^#" | sed 's/^# //'
            exit 0
            ;;
        *) CATEGORIES+=("$1") ;;
    esac
    shift
done

# Cria logs e timestamp
mkdir -p "$LOGS_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
PIPELINE_LOG="$LOGS_DIR/pipeline_${TIMESTAMP}.log"
exec > >(tee -a "$PIPELINE_LOG") 2>&1

START_TOTAL=$(date +%s)

log_info "==============================================="
log_info "PIPELINE COMPLETO - $ALGO_NAME"
log_info "Inicio: $(date)"
log_info "Categorias: ${CATEGORIES[*]:-C1 R1 RC1}"
log_info "Log:    $PIPELINE_LOG"
log_info "==============================================="

# Verifica dependencias
if ! check_dependencies; then
    exit 1
fi

# Etapa 1: limpeza
if [ "$SKIP_CLEAN" -eq 0 ]; then
    log_info "[1/5] Limpando resultados antigos..."
    bash "$SCRIPT_DIR/clean.sh"
else
    log_warn "[1/5] Limpeza pulada (--skip-clean)"
fi

# Etapa 2: execucao
if [ "$SKIP_RUN" -eq 0 ]; then
    log_info "[2/5] Executando $NUM_EXECUTIONS rodadas por instancia..."
    bash "$SCRIPT_DIR/run_all.sh" "${CATEGORIES[@]}"
else
    log_warn "[2/5] Execucao pulada (--skip-run)"
fi

# Etapa 3: validacao
if [ "$SKIP_VALIDATE" -eq 0 ]; then
    log_info "[3/5] Validando solucoes..."
    bash "$SCRIPT_DIR/validate.sh" || log_warn "Validacao retornou avisos"
else
    log_warn "[3/5] Validacao pulada (--skip-validate)"
fi

# Etapa 4: resumo
if [ "$SKIP_SUMMARIZE" -eq 0 ]; then
    log_info "[4/5] Gerando tabelas resumo..."
    bash "$SCRIPT_DIR/summarize.sh"
else
    log_warn "[4/5] Sumarizacao pulada (--skip-summarize)"
fi

# Etapa 5: mapas
if [ "$SKIP_MAPS" -eq 0 ]; then
    log_info "[5/5] Gerando mapas PNG..."
    bash "$SCRIPT_DIR/maps.sh" "${CATEGORIES[@]}"
else
    log_warn "[5/5] Mapas pulados (--skip-maps)"
fi

END_TOTAL=$(date +%s)
TOTAL_DUR=$((END_TOTAL - START_TOTAL))

log_info "==============================================="
log_ok "PIPELINE CONCLUIDO em $((TOTAL_DUR / 60))min$((TOTAL_DUR % 60))s"
log_info "Resultados: $RESULTS_DIR/"
log_info "Resumo:     $SUMMARY_DIR/"
log_info "Log:        $PIPELINE_LOG"
log_info "==============================================="
