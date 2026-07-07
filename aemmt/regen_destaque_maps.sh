#!/bin/bash
# =============================================================================
# Regera APENAS os 3 mapas de destaque do AEMMT usados no TCC,
# com fontes maiores e legenda fora do mapa (pipeline/plot_map_destaque.py).
#
# Mapas gerados em destaque_maps/ com os nomes esperados pelo template.tex:
#   - AEMMT_map_c106_exec9.png    (C106 exec09 - melhor execucao do AEMMT)
#   - AEMMT_map_r108_exec08.png   (R108 exec08 - melhor execucao do AEMMT)
#   - AEMMT_map_rc106_exec02.png  (RC106 exec02 - melhor execucao do AEMMT)
#
# Uso:
#   bash regen_destaque_maps.sh
# =============================================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PLOT="pipeline/plot_map_destaque.py"
INSTANCES_DIR="src/instances/solomon"
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

# Mapeamento: instancia -> melhor execucao do AEMMT (do summary)
declare -A BEST=(
    ["C106"]="09:c1"
    ["R108"]="08:r1"
    ["RC106"]="02:rc1"
)

# Nome de saida (sem zero a esquerda no exec do C106 conforme template antigo)
declare -A OUTNAME=(
    ["C106"]="AEMMT_map_c106_exec9.png"
    ["R108"]="AEMMT_map_r108_exec08.png"
    ["RC106"]="AEMMT_map_rc106_exec02.png"
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
        --algo "AEMMT" \
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
