#!/bin/bash
# =============================================================================
# GERA TABELAS RESUMO DOS MELHORES RESULTADOS POR CATEGORIA (NSGA-III)
# =============================================================================
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

source "$SCRIPT_DIR/config.sh"
mkdir -p "$SUMMARY_DIR"

SUMMARIZER="$SCRIPT_DIR/summarize.py"
if [ ! -f "$SUMMARIZER" ]; then
    log_error "Sumarizador nao encontrado: $SUMMARIZER"
    exit 1
fi

log_info "Gerando tabelas resumo em $SUMMARY_DIR/..."
$PYTHON_CMD "$SUMMARIZER" --algo "$ALGO_NAME" --results-dir "$RESULTS_DIR" --output-dir "$SUMMARY_DIR"
log_ok "Resumo concluido"
