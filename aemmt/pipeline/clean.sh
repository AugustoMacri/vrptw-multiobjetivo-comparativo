#!/bin/bash
# =============================================================================
# LIMPEZA AUTOMATICA DE RESULTADOS ANTERIORES (AEMMT)
# =============================================================================
# Remove apenas pastas/arquivos GERADOS automaticamente. Nao toca em codigo
# fonte, instancias ou scripts. Recria as pastas necessarias.
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# Source config
source "$SCRIPT_DIR/config.sh"

log_info "Limpando resultados antigos em $REPO_ROOT..."

# Diretorios padronizados (novo formato)
rm -rf "$RESULTS_DIR" "$LOGS_DIR"

# Diretorios legados (gerados por scripts antigos)
rm -rf results_validation_C1 results_validation_R1 results_validation_RC1
rm -rf results_validation_C1_detailed results_validation_R1_detailed results_validation_RC1_detailed
rm -rf "$JAVA_OUTPUT_DIR"
rm -rf mapping_results

# Arquivos avulsos legados na raiz
rm -f best_results_summary.txt best_results_summary.csv

# Recria a estrutura padronizada
mkdir -p "$RESULTS_C1_DIR" "$RESULTS_R1_DIR" "$RESULTS_RC1_DIR"
mkdir -p "$SUMMARY_DIR" "$LOGS_DIR"
mkdir -p "$JAVA_OUTPUT_DIR" "$JAVA_OUTPUT_DIR/stats"

log_ok "Limpeza concluida. Estrutura recriada em $RESULTS_DIR/"
