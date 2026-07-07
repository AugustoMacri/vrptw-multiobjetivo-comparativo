#!/bin/bash
# =============================================================================
# VALIDACAO RIGOROSA DOS RESULTADOS (AEMMT)
# =============================================================================
# Itera por todos os arquivos evo_*.txt em results/ e valida cada um contra
# a instancia correspondente (capacidade, janelas de tempo, cobertura).
#
# Uso:
#   bash pipeline/validate.sh
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

source "$SCRIPT_DIR/config.sh"

VALIDATOR="scripts/validate_solution_rigorous.py"
if [ ! -f "$VALIDATOR" ]; then
    log_error "Validador nao encontrado: $VALIDATOR"
    exit 1
fi

TOTAL=0
VALID=0
INVALID=0
INVALID_LIST=()

log_info "Iniciando validacao rigorosa de todos os resultados..."

for cat_dir in "$RESULTS_C1_DIR" "$RESULTS_R1_DIR" "$RESULTS_RC1_DIR"; do
    [ -d "$cat_dir" ] || continue
    for inst_dir in "$cat_dir"/*/; do
        [ -d "$inst_dir" ] || continue
        inst_name=$(basename "$inst_dir")
        instance_file="$INSTANCES_DIR/${inst_name}.txt"

        if [ ! -f "$instance_file" ]; then
            log_warn "Instancia nao encontrada: $instance_file"
            continue
        fi

        for evo_file in "$inst_dir"evo_*.txt; do
            [ -f "$evo_file" ] || continue
            TOTAL=$((TOTAL + 1))
            if $PYTHON_CMD "$VALIDATOR" "$instance_file" "$evo_file" > /dev/null 2>&1; then
                VALID=$((VALID + 1))
            else
                INVALID=$((INVALID + 1))
                INVALID_LIST+=("$evo_file")
            fi
        done
    done
done

echo ""
log_info "==== RESUMO DA VALIDACAO ===="
log_info "Total:      $TOTAL"
log_info "Validas:    $VALID"
log_info "Invalidas:  $INVALID"

if [ $TOTAL -gt 0 ]; then
    pct=$((VALID * 100 / TOTAL))
    log_info "Taxa:       ${pct}%"
fi

if [ $INVALID -gt 0 ]; then
    log_error "Solucoes invalidas:"
    for f in "${INVALID_LIST[@]}"; do
        echo "  - $f"
    done
    exit 1
fi

log_ok "Todas as solucoes sao validas!"
