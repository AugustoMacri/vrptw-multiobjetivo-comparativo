#!/bin/bash

# =============================================================
# Comparacao Antes vs Depois das Correcoes
# Executa 3 instancias (C101, C103, R101) e mostra metricas
# =============================================================

echo "=========================================="
echo " COMPARACAO: Novas Correcoes NSGA-III"
echo "=========================================="
echo ""

# Compilar
echo ">>> Compilando..."
./gradlew build -x test --console=plain -q 2>&1
if [ $? -ne 0 ]; then
    echo "ERRO: Compilacao falhou!"; exit 1
fi
echo "OK"

# Preparar classpath
CLASSPATH="app/build/classes/java/main"
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    for jar in "$GRADLE_CACHE"/*/*/*/*/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

RESULTS_DIR="comparison_results"
mkdir -p "$RESULTS_DIR"

# Instancias para testar
INSTANCES=("1:C101" "3:C103" "18:R101")

echo ""
printf "%-10s | %-8s | %-12s | %-12s | %-12s | %-8s | %-8s\n" \
    "Instancia" "Tempo(s)" "BestDist" "BestTime" "BestFuel" "Unique" "Pareto"
echo "---------- | -------- | ------------ | ------------ | ------------ | -------- | --------"

for entry in "${INSTANCES[@]}"; do
    IFS=':' read -r NUM NAME <<< "$entry"

    START=$(date +%s)
    OUTPUT=$(java -cp "$CLASSPATH" main.App $NUM 4 1 "_compare" 2>&1)
    END=$(date +%s)
    DURATION=$((END - START))

    # Salvar output completo
    echo "$OUTPUT" > "$RESULTS_DIR/${NAME}_output.txt"

    # Extrair metricas
    BEST_DIST=$(echo "$OUTPUT" | grep "Dist:" | grep "Melhores" -A 1 | grep "Dist:" | head -1 | grep -oP '[\d.]+' | head -1)
    if [ -z "$BEST_DIST" ]; then
        BEST_DIST=$(echo "$OUTPUT" | grep "DistanciaMin" | grep -oP '[\d.]+' | head -1)
    fi

    BEST_TIME=$(echo "$OUTPUT" | grep "Tempo:" | grep "Melhores" -A 2 | grep "Tempo:" | head -1 | grep -oP '[\d.]+' | head -1)
    if [ -z "$BEST_TIME" ]; then
        BEST_TIME=$(echo "$OUTPUT" | grep "TempoMin" | grep -oP '[\d.]+' | head -1)
    fi

    BEST_FUEL=$(echo "$OUTPUT" | grep "Comb:" | grep "Melhores" -A 3 | grep "Comb:" | head -1 | grep -oP '[\d.]+' | head -1)
    if [ -z "$BEST_FUEL" ]; then
        BEST_FUEL=$(echo "$OUTPUT" | grep "CombustivelMin" | grep -oP '[\d.]+' | head -1)
    fi

    UNIQUE=$(echo "$OUTPUT" | grep "Solucoes unicas" | grep -oP '[\d]+' | tail -1)
    PARETO=$(echo "$OUTPUT" | grep "Solucoes na frente" | grep -oP '[\d]+' | head -1)

    printf "%-10s | %-8s | %-12s | %-12s | %-12s | %-8s | %-8s\n" \
        "$NAME" "${DURATION}" "${BEST_DIST:-N/A}" "${BEST_TIME:-N/A}" "${BEST_FUEL:-N/A}" \
        "${UNIQUE:-N/A}" "${PARETO:-N/A}"

    # Limpar arquivos temporarios
    rm -f "resultsNSGA3/evo_${NAME,,}_compare.txt" "resultsNSGA3/pareto_${NAME,,}_compare.txt"
done

echo ""
echo "=========================================="
echo " O QUE VERIFICAR"
echo "=========================================="
echo ""
echo "1. DIVERSIDADE: 'Unique' deve ser > 1."
echo "   Se = 1, os operadores nao estao gerando diversidade."
echo ""
echo "2. PARETO: Deve ter > 1 solucao na frente."
echo "   Se = 1, nao ha trade-off entre objetivos."
echo ""
echo "3. EVOLUCAO: Verifique os logs em $RESULTS_DIR/"
echo "   Procure por linhas '[Eval N]' mostrando melhoria progressiva."
echo ""
echo "4. Outputs completos em: $RESULTS_DIR/"
echo "=========================================="
