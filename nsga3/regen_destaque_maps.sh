#!/bin/bash
# =============================================================================
# Regera APENAS os 3 mapas de destaque do NSGA-III usados no TCC,
# com fontes maiores e legenda fora do mapa (pipeline/plot_map_destaque.py).
#
# Mapas gerados em destaque_maps/ com os nomes esperados pelo template.tex:
#   - NSGA3_map_c106_exec04.png   (C106 exec04 - melhor execucao do NSGA-III)
#   - NSGA3_map_r108_exec01.png   (R108 exec01 - melhor execucao do NSGA-III)
#   - NSGA3_map_rc106_exec10.png  (RC106 exec10 - melhor execucao do NSGA-III)
#
# Uso:
#   bash regen_destaque_maps.sh
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PLOT="pipeline/plot_map_destaque.py"
INSTANCES_DIR="app/src/main/java/instances/solomon"
OUTPUT_DIR="destaque_maps"

mkdir -p "$OUTPUT_DIR"

PYTHON_CMD=""
if python3 --version &> /dev/null 2>&1; then
    PYTHON_CMD=python3
elif python --version &> /dev/null 2>&1; then
    PYTHON_CMD=python
else
    echo "[ERRO] python3/python nao encontrado"
    exit 1
fi
export PYTHONIOENCODING=utf-8

# Mapeamento: instancia -> melhor execucao do NSGA-III (do summary)
declare -A BEST=(
    ["C106"]="04:c1"
    ["R108"]="01:r1"
    ["RC106"]="10:rc1"
)

# Nome de saida exatamente como esperado pelo template
declare -A OUTNAME=(
    ["C106"]="NSGA3_map_c106_exec04.png"
    ["R108"]="NSGA3_map_r108_exec01.png"
    ["RC106"]="NSGA3_map_rc106_exec10.png"
)

for inst in C106 R108 RC106; do
    info="${BEST[$inst]}"
    exec_num="${info%%:*}"
    cat_lower="${info##*:}"
    cat_upper=$(echo "$cat_lower" | tr "[:lower:]" "[:upper:]")
    inst_lower=$(echo "$inst" | tr "[:upper:]" "[:lower:]")

    EVO_FILE="results/$cat_upper/$inst/evo_${inst_lower}_exec${exec_num}.txt"
    INSTANCE_FILE="$INSTANCES_DIR/${inst}.txt"
    OUT="$OUTPUT_DIR/${OUTNAME[$inst]}"

    if [ ! -f "$EVO_FILE" ]; then
        echo "[ERRO] evo nao encontrado: $EVO_FILE"
        continue
    fi
    if [ ! -f "$INSTANCE_FILE" ]; then
        echo "[ERRO] instancia nao encontrada: $INSTANCE_FILE"
        continue
    fi

    echo ">>> Gerando $OUT"
    $PYTHON_CMD "$PLOT" \
        --algo "NSGA-III" \
        --instance "$inst" \
        --exec "$exec_num" \
        --evo-file "$EVO_FILE" \
        --instance-file "$INSTANCE_FILE" \
        --output "$OUT"
done

echo ""
echo "[OK] Mapas gerados em $OUTPUT_DIR/:"
ls -la "$OUTPUT_DIR/"
echo ""
echo "Para usar no TCC, copie esses 3 arquivos para Latex_tcc/figuras/"
