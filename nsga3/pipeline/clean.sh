#!/bin/bash
# =============================================================================
# LIMPEZA AUTOMATICA DE RESULTADOS ANTERIORES (NSGA-III)
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
rm -rf results_validation_NSGA3_C1 results_validation_NSGA3_R1 results_validation_NSGA3_RC1
rm -rf "$JAVA_OUTPUT_DIR" "$JAVA_OUTPUT_DIR_GRADLE"
rm -rf debug_output

# Arquivos avulsos legados na raiz
rm -f best_results_summary_nsga3.txt best_results_summary_nsga3.csv
rm -f validation_results.txt

# Recria a estrutura padronizada
mkdir -p "$RESULTS_C1_DIR" "$RESULTS_R1_DIR" "$RESULTS_RC1_DIR"
mkdir -p "$SUMMARY_DIR" "$LOGS_DIR"
mkdir -p "$JAVA_OUTPUT_DIR" "$JAVA_OUTPUT_DIR_GRADLE"

log_ok "Limpeza concluida. Estrutura recriada em $RESULTS_DIR/"
