#!/bin/bash
# =============================================================================
# GERA MAPAS PADRONIZADOS DAS ROTAS (AEMMT)
# =============================================================================
# Itera por todos os evo_*.txt em results/ e gera 1 PNG por execucao em:
#   results/<categoria>/<instancia>/maps/map_aemmt_<inst>_exec<N>.png
#
# Uso:
#   bash pipeline/maps.sh               # gera para todos
#   bash pipeline/maps.sh C1            # apenas C1
#   bash pipeline/maps.sh C1 R1         # C1 e R1
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

source "$SCRIPT_DIR/config.sh"

CATEGORIES=("$@")
if [ ${#CATEGORIES[@]} -eq 0 ]; then
    CATEGORIES=(C1 R1 RC1)
fi

PLOTTER="$SCRIPT_DIR/plot_map.py"
if [ ! -f "$PLOTTER" ]; then
    log_error "plot_map.py nao encontrado em $PLOTTER"
    exit 1
fi

generate_for_category() {
    local cat="$1"
    local cat_dir
    case "$cat" in
        C1)  cat_dir="$RESULTS_C1_DIR"  ;;
        R1)  cat_dir="$RESULTS_R1_DIR"  ;;
        RC1) cat_dir="$RESULTS_RC1_DIR" ;;
        *)   log_error "Categoria desconhecida: $cat"; return 1 ;;
    esac

    [ -d "$cat_dir" ] || { log_warn "Sem resultados em $cat_dir"; return 0; }

    log_info "===== Gerando mapas: categoria $cat ====="

    for inst_dir in "$cat_dir"/*/; do
        [ -d "$inst_dir" ] || continue
        local inst_name=$(basename "$inst_dir")
        local inst_lower=$(echo "$inst_name" | tr "[:upper:]" "[:lower:]")
        local instance_file="$INSTANCES_DIR/${inst_name}.txt"
        local maps_dir="$inst_dir/maps"
        mkdir -p "$maps_dir"

        if [ ! -f "$instance_file" ]; then
            log_warn "Instancia nao encontrada: $instance_file"
            continue
        fi

        for evo_file in "$inst_dir"evo_*.txt; do
            [ -f "$evo_file" ] || continue
            local fname=$(basename "$evo_file")
            local exec_num=$(echo "$fname" | sed -E 's/.*exec([0-9]+)\.txt/\1/')
            local out_png="$maps_dir/map_aemmt_${inst_lower}_exec${exec_num}.png"

            $PYTHON_CMD "$PLOTTER" \
                --algo "AEMMT" \
                --instance "$inst_name" \
                --exec "$exec_num" \
                --evo-file "$evo_file" \
                --instance-file "$instance_file" \
                --output "$out_png" > /dev/null 2>&1 \
                && log_ok "  $out_png" \
                || log_warn "  falhou: $evo_file"
        done
    done
}

for cat in "${CATEGORIES[@]}"; do
    generate_for_category "$cat"
done

log_ok "Mapas gerados"
