#!/bin/bash

# Script para executar todas as instâncias RC1 com NSGA-III (10 vezes cada)
# Uso: ./run_validation_nsga3_rc1.sh

echo "=========================================="
echo "Executor de Validação NSGA-III - RC1"
echo "=========================================="
echo ""

# Criar diretório de resultados
RESULT_DIR="results_validation_NSGA3_RC1"
mkdir -p "$RESULT_DIR"

# Instâncias RC1: de 41 a 48 (RC101 a RC108)
INSTANCES=(41 42 43 44 45 46 47 48)
INSTANCE_NAMES=("RC101" "RC102" "RC103" "RC104" "RC105" "RC106" "RC107" "RC108")

# Compilar o projeto se necessário
echo "📦 Verificando compilação..."
if [ ! -d "app/build/classes/java/main" ] || [ ! -f "app/build/classes/java/main/main/App.class" ]; then
    echo "Compilando o projeto NSGA-III..."
    ./gradlew build -x test --console=plain -q

    if [ $? -ne 0 ]; then
        echo "Erro na compilação! Abortando execução."
        exit 1
    fi

    echo "✓ Compilação concluída!"
else
    echo "✓ Projeto já compilado"
fi

# Preparar classpath para execução direta
CLASSPATH="app/build/classes/java/main"

# Adicionar JARs das dependências (JMetal, etc)
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    for jar in "$GRADLE_CACHE"/*/*/*/*/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

echo ""

# Total de execuções
TOTAL_EXECUTIONS=$((${#INSTANCES[@]} * 10))
CURRENT_EXECUTION=0

# Para cada instância RC1
for idx in "${!INSTANCES[@]}"; do
    INSTANCE_NUM=${INSTANCES[$idx]}
    INSTANCE_NAME=${INSTANCE_NAMES[$idx]}

    echo "=========================================="
    echo "Instância: $INSTANCE_NAME (Número: $INSTANCE_NUM)"
    echo "=========================================="

    # Criar diretório para a instância
    INSTANCE_DIR="$RESULT_DIR/$INSTANCE_NAME"
    mkdir -p "$INSTANCE_DIR"

    # Executar 10 vezes
    for exec_num in $(seq -f "%02g" 1 10); do
        CURRENT_EXECUTION=$((CURRENT_EXECUTION + 1))

        echo ""
        echo ">>> Execução $exec_num/10 - $INSTANCE_NAME"
        echo ">>> Progresso global: $CURRENT_EXECUTION/$TOTAL_EXECUTIONS"

        # Arquivos de saída
        EVO_FILE="$INSTANCE_DIR/evo_${INSTANCE_NAME,,}_exec${exec_num}.txt"
        STATS_FILE="$INSTANCE_DIR/stats_${INSTANCE_NAME,,}_exec${exec_num}.txt"
        PARETO_FILE="$INSTANCE_DIR/pareto_${INSTANCE_NAME,,}_exec${exec_num}.txt"

        # Sufixo único para esta execução
        EXEC_SUFFIX="_exec${exec_num}"

        # Executar NSGA-III com Java direto (muito mais rápido que gradlew run)
        # Argumentos: <instancia> <algoritmo=4> <tipo=1> <sufixo_execucao>
        START_TIME=$(date +%s)
        java -cp "$CLASSPATH" main.App $INSTANCE_NUM 4 1 "$EXEC_SUFFIX" > "$STATS_FILE" 2>&1
        EXIT_CODE=$?
        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))

        if [ $EXIT_CODE -eq 124 ]; then
            echo "⏱️  Timeout após 300s"
        elif [ $EXIT_CODE -ne 0 ]; then
            echo "⚠️  Erro na execução (código: $EXIT_CODE)"
        else
            echo "⏱️  Tempo: ${DURATION}s"
        fi

        # Mover arquivos gerados (agora já vêm com sufixo único)
        if [ -f "app/resultsNSGA3/evo_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" ]; then
            mv "app/resultsNSGA3/evo_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" "$EVO_FILE"
            echo "✓ Evolução salva em: $EVO_FILE"
        else
            echo "⚠ Arquivo de evolução não encontrado!"
        fi

        if [ -f "app/resultsNSGA3/pareto_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" ]; then
            mv "app/resultsNSGA3/pareto_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" "$PARETO_FILE"
            echo "✓ Pareto salvo em: $PARETO_FILE"
        else
            echo "⚠ Arquivo Pareto não encontrado!"
        fi

        # Verificar se stats foi criado
        if [ -f "$STATS_FILE" ]; then
            echo "✓ Estatísticas salvas em: $STATS_FILE"
        fi

        echo "✓ Execução $exec_num concluída"
    done

    echo ""
    echo "✓ Todas as 10 execuções de $INSTANCE_NAME concluídas!"
    echo ""
done

echo ""
echo "=========================================="
echo "VALIDAÇÃO NSGA-III CONCLUÍDA!"
echo "=========================================="
echo "Total de execuções: $TOTAL_EXECUTIONS"
echo "Resultados salvos em: $RESULT_DIR/"
echo ""
echo "Próximos passos:"
echo "  1. Validar capacidades: python3 scripts/validate_capacity.py --nsga3"
echo "  2. Gerar mapas: ./generate_route_maps_nsga3.sh all_rc1"
echo "  3. Comparar resultados com versão original"
echo ""
