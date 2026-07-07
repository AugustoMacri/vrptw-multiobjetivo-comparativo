#!/bin/bash
# =============================================================================
# GERA TABELAS RESUMO DOS MELHORES RESULTADOS POR CATEGORIA (AEMMT)
# =============================================================================
# Para cada categoria (C1, R1, RC1), encontra a MELHOR execucao (menor
# distancia total) de cada instancia e escreve em results/summary/.
#
# Uso:
#   bash pipeline/summarize.sh
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

source "$SCRIPT_DIR/config.sh"

mkdir -p "$SUMMARY_DIR"

# Chama o script Python que percorre results/ e gera as tabelas
SUMMARIZER="$SCRIPT_DIR/summarize.py"
if [ ! -f "$SUMMARIZER" ]; then
    log_error "Sumarizador nao encontrado: $SUMMARIZER"
    exit 1
fi

log_info "Gerando tabelas resumo em $SUMMARY_DIR/..."
$PYTHON_CMD "$SUMMARIZER" --algo "$ALGO_NAME" --results-dir "$RESULTS_DIR" --output-dir "$SUMMARY_DIR"
log_ok "Resumo concluido"
