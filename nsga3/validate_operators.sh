#!/bin/bash

# =============================================================
# Validacao dos Operadores Geneticos
# Verifica se crossover e mutacao estao funcionando corretamente
# analisando os logs detalhados do NSGA-III
# =============================================================

echo "=========================================="
echo " VALIDACAO DE OPERADORES GENETICOS"
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

echo ""
echo ">>> Executando C101..."
OUTPUT=$(java -cp "$CLASSPATH" main.App 1 4 1 "_validate" 2>&1)

echo ""
echo "=== VERIFICACAO 1: Populacao Inicial ==="
CREATED=$(echo "$OUTPUT" | grep "Solucoes criadas na inicializacao" | grep -oP '[\d]+')
echo "Solucoes criadas: ${CREATED:-?}"
if [ -n "$CREATED" ] && [ "$CREATED" -gt 0 ]; then
    echo "OK: Populacao inicializada"
else
    echo "FALHA: Populacao nao foi criada corretamente"
fi

echo ""
echo "=== VERIFICACAO 2: Diversidade ==="
UNIQUE=$(echo "$OUTPUT" | grep "Solucoes unicas observadas" | grep -oP '[\d]+')
echo "Solucoes unicas: ${UNIQUE:-?}"
if [ -n "$UNIQUE" ] && [ "$UNIQUE" -gt 10 ]; then
    echo "OK: Boa diversidade ($UNIQUE solucoes unicas)"
elif [ -n "$UNIQUE" ] && [ "$UNIQUE" -gt 1 ]; then
    echo "PARCIAL: Alguma diversidade ($UNIQUE), mas pode melhorar"
else
    echo "FALHA: Sem diversidade significativa"
fi

echo ""
echo "=== VERIFICACAO 3: Evolucao dos Objetivos ==="
FIRST_LOG=$(echo "$OUTPUT" | grep "^\[Eval" | head -1)
LAST_LOG=$(echo "$OUTPUT" | grep "^\[Eval" | tail -1)

if [ -n "$FIRST_LOG" ] && [ -n "$LAST_LOG" ]; then
    echo "Inicio: $FIRST_LOG"
    echo "Fim:    $LAST_LOG"

    # Extrair distancias
    FIRST_DIST=$(echo "$FIRST_LOG" | grep -oP 'Dist=[\d.]+' | grep -oP '[\d.]+')
    LAST_DIST=$(echo "$LAST_LOG" | grep -oP 'Dist=[\d.]+' | grep -oP '[\d.]+')

    if [ -n "$FIRST_DIST" ] && [ -n "$LAST_DIST" ]; then
        # Comparar usando bc ou awk
        IMPROVED=$(awk "BEGIN { print ($LAST_DIST < $FIRST_DIST) ? 1 : 0 }")
        if [ "$IMPROVED" = "1" ]; then
            echo "OK: Distancia melhorou de $FIRST_DIST para $LAST_DIST"
        else
            echo "AVISO: Distancia nao melhorou ($FIRST_DIST -> $LAST_DIST)"
        fi
    fi
else
    echo "AVISO: Nenhum log de evolucao encontrado"
fi

echo ""
echo "=== VERIFICACAO 4: Frente de Pareto ==="
PARETO=$(echo "$OUTPUT" | grep "Solucoes na frente de Pareto" | grep -oP '[\d]+')
echo "Tamanho da frente: ${PARETO:-?}"
if [ -n "$PARETO" ] && [ "$PARETO" -gt 5 ]; then
    echo "OK: Frente diversificada ($PARETO solucoes)"
elif [ -n "$PARETO" ] && [ "$PARETO" -gt 1 ]; then
    echo "PARCIAL: Frente pequena ($PARETO solucoes)"
else
    echo "FALHA: Frente com 0-1 solucao"
fi

echo ""
echo "=== VERIFICACAO 5: Restricoes ==="
# Verificar se ha penalidades pesadas (indicaria violacoes)
HAS_PENALTY=$(echo "$OUTPUT" | grep -oP 'Dist=[\d.]+' | tail -1 | grep -oP '[\d.]+')
if [ -n "$HAS_PENALTY" ]; then
    PENALTY_CHECK=$(awk "BEGIN { print ($HAS_PENALTY > 100000) ? 1 : 0 }")
    if [ "$PENALTY_CHECK" = "1" ]; then
        echo "AVISO: Objetivos muito altos ($HAS_PENALTY) - possiveis violacoes"
    else
        echo "OK: Objetivos em faixa normal ($HAS_PENALTY)"
    fi
fi

echo ""
echo "=========================================="
echo " RESUMO"
echo "=========================================="
echo ""
echo "Se todos os testes passaram (OK), os operadores estao funcionais."
echo "Se algum teste falhou, verifique o output completo para detalhes."
echo ""
echo "Para ver o output completo, execute:"
echo "  ./debug_nsga3.sh 1"
echo "=========================================="

# Limpar
rm -f "resultsNSGA3/evo_c101_validate.txt" "resultsNSGA3/pareto_c101_validate.txt"
