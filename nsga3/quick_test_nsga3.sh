#!/bin/bash

# =============================================================
# Teste Rapido do NSGA-III
# Compila e executa C101, verificando se os operadores funcionam
# =============================================================

echo "=========================================="
echo " TESTE RAPIDO NSGA-III"
echo "=========================================="
echo ""

# Compilar
echo ">>> [1/3] Compilando projeto..."
./gradlew build -x test --console=plain -q 2>&1
if [ $? -ne 0 ]; then
    echo "ERRO: Compilacao falhou!"
    exit 1
fi
echo "OK: Compilacao bem-sucedida"

# Preparar classpath
CLASSPATH="app/build/classes/java/main"
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    for jar in "$GRADLE_CACHE"/*/*/*/*/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

# Executar C101
echo ""
echo ">>> [2/3] Executando C101 com NSGA-III..."
START=$(date +%s)
OUTPUT=$(java -cp "$CLASSPATH" main.App 1 4 1 "_quicktest" 2>&1)
EXIT_CODE=$?
END=$(date +%s)
DURATION=$((END - START))

if [ $EXIT_CODE -ne 0 ]; then
    echo "ERRO: Execucao falhou (exit code: $EXIT_CODE)"
    echo "$OUTPUT"
    exit 1
fi
echo "OK: Executou em ${DURATION}s"

# Analisar resultados
echo ""
echo ">>> [3/3] Analisando resultados..."
echo ""

# Populacao
POP=$(echo "$OUTPUT" | grep "Solucoes criadas na inicializacao" | grep -o '[0-9]*')
EVALS=$(echo "$OUTPUT" | grep "Total de avaliacoes" | grep -o '[0-9]*')
UNIQUE=$(echo "$OUTPUT" | grep "Solucoes unicas observadas" | grep -o '[0-9]*')
PARETO=$(echo "$OUTPUT" | grep "Solucoes na frente de Pareto" | grep -o '[0-9]*')

echo "Populacao real:      ${POP:-?}"
echo "Total avaliacoes:    ${EVALS:-?}"
echo "Solucoes unicas:     ${UNIQUE:-?}"
echo "Frente de Pareto:    ${PARETO:-?}"
echo "Tempo execucao:      ${DURATION}s"

echo ""

# Verificar evolucao
FIRST_EVAL=$(echo "$OUTPUT" | grep "^\[Eval" | head -1)
LAST_EVAL=$(echo "$OUTPUT" | grep "^\[Eval" | tail -1)

if [ -n "$FIRST_EVAL" ]; then
    echo "Primeira avaliacao registrada:"
    echo "  $FIRST_EVAL"
    echo "Ultima avaliacao registrada:"
    echo "  $LAST_EVAL"
else
    echo "AVISO: Nenhum log de evolucao encontrado!"
fi

echo ""

# Veredicto
echo "=========================================="
echo " VEREDICTO"
echo "=========================================="

PASS=true

if [ -n "$UNIQUE" ] && [ "$UNIQUE" -gt 1 ]; then
    echo "OK: Diversidade detectada ($UNIQUE solucoes unicas)"
else
    echo "PROBLEMA: Sem diversidade (solucoes unicas: ${UNIQUE:-0})"
    PASS=false
fi

if [ -n "$PARETO" ] && [ "$PARETO" -gt 1 ]; then
    echo "OK: Frente de Pareto com $PARETO solucoes"
else
    echo "AVISO: Frente de Pareto com apenas ${PARETO:-0} solucao(oes)"
fi

if [ -n "$EVALS" ] && [ "$EVALS" -gt 1000 ]; then
    echo "OK: Algoritmo executou $EVALS avaliacoes"
else
    echo "AVISO: Poucas avaliacoes (${EVALS:-0})"
fi

echo ""
if [ "$PASS" = true ]; then
    echo "RESULTADO: PASSOU - Operadores parecem funcionais"
else
    echo "RESULTADO: VERIFICAR - Pode haver problemas com evolucao"
fi
echo "=========================================="

# Limpar arquivo temporario
rm -f "resultsNSGA3/evo_c101_quicktest.txt" "resultsNSGA3/pareto_c101_quicktest.txt"
