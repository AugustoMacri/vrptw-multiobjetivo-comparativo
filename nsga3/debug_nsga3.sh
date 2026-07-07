#!/bin/bash

# =============================================================
# Script de Depuracao do NSGA-III
# Executa uma unica instancia e analisa os logs de debug
# =============================================================
# Uso: ./debug_nsga3.sh [INSTANCE_NUM]
#   INSTANCE_NUM: 1=C101, 2=C102, ..., 18=R101, etc. (default: 1)
# =============================================================

INSTANCE_NUM=${1:-1}
INSTANCE_NAMES=(
    "" "C101" "C102" "C103" "C104" "C105" "C106" "C107" "C108" "C109"
    "" "" "" "" "" "" "" ""
    "R101" "R102" "R103" "R104" "R105" "R106" "R107" "R108" "R109"
)
INSTANCE_NAME=${INSTANCE_NAMES[$INSTANCE_NUM]:-"inst$INSTANCE_NUM"}

echo "=========================================="
echo " DEBUG NSGA-III - Instancia: $INSTANCE_NAME"
echo "=========================================="
echo ""

# Compilar
echo ">>> Compilando..."
./gradlew build -x test --console=plain -q 2>&1
if [ $? -ne 0 ]; then
    echo "ERRO: Compilacao falhou!"
    echo "Verifique os erros acima e corrija antes de continuar."
    exit 1
fi
echo "OK: Compilacao bem-sucedida"
echo ""

# Preparar classpath
CLASSPATH="app/build/classes/java/main"
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    for jar in "$GRADLE_CACHE"/*/*/*/*/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

# Criar diretorio de debug
DEBUG_DIR="debug_output"
mkdir -p "$DEBUG_DIR"
OUTPUT_FILE="$DEBUG_DIR/debug_${INSTANCE_NAME}_$(date +%Y%m%d_%H%M%S).txt"

echo ">>> Executando NSGA-III com debug..."
echo ">>> Output salvo em: $OUTPUT_FILE"
echo ""

# Executar com captura de output
java -cp "$CLASSPATH" main.App $INSTANCE_NUM 4 1 "_debug" 2>&1 | tee "$OUTPUT_FILE"

echo ""
echo "=========================================="
echo " ANALISE DE DEBUG"
echo "=========================================="

# Extrair metricas do output
echo ""
echo "--- Populacao ---"
grep -i "solucoes criadas" "$OUTPUT_FILE" | tail -1
grep -i "populacao efetiva" "$OUTPUT_FILE" | tail -1

echo ""
echo "--- Evolucao ---"
echo "Primeiras e ultimas entradas de evolucao:"
grep "^\[Eval" "$OUTPUT_FILE" | head -3
echo "..."
grep "^\[Eval" "$OUTPUT_FILE" | tail -3

echo ""
echo "--- Diversidade ---"
grep -i "unique\|unicas" "$OUTPUT_FILE" | tail -3

echo ""
echo "--- Objetivos Finais ---"
grep -i "DistanciaMin\|TempoMin\|CombustivelMin" "$OUTPUT_FILE"

echo ""
echo "--- Frente de Pareto ---"
grep -i "tamanho.*frente\|solucoes.*pareto\|solucoes encontradas" "$OUTPUT_FILE"

echo ""
echo "=========================================="
echo " FIM DO DEBUG"
echo "=========================================="
echo "Output completo em: $OUTPUT_FILE"
