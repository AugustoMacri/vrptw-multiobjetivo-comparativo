#!/bin/bash

# Script para executar o algoritmo genético em múltiplas instâncias do Solomon benchmark
# Uso: ./run_all_instances.sh

echo "=========================================="
echo "Executor de Instâncias VRP"
echo "=========================================="
echo ""

# Lista de instâncias a serem executadas (número da instância)
INSTANCES=(
    1   # C101.txt
    2   # C102.txt
    3   # C103.txt
    4   # C104.txt
    5   # C105.txt
    6   # C106.txt
    7   # C107.txt
    8   # C108.txt
    9   # C109.txt
    18  # R101.txt
    19  # R102.txt
    20  # R103.txt
    21  # R104.txt
    22  # R105.txt
    23  # R106.txt
    24  # R107.txt
    25  # R108.txt
    26  # R109.txt
    41  # RC101.txt
    42  # RC102.txt
    43  # RC103.txt
    44  # RC104.txt
    45  # RC105.txt
    46  # RC106.txt
    47  # RC107.txt
    48  # RC108.txt
)

# Nomes correspondentes das instâncias (para exibição)
INSTANCE_NAMES=(
    "C101" "C102" "C103" "C104" "C105" "C106" "C107" "C108" "C109"
    "R101" "R102" "R103" "R104" "R105" "R106" "R107" "R108" "R109"
    "RC101" "RC102" "RC103" "RC104" "RC105" "RC106" "RC107" "RC108"
)

# Criar diretório de resultados se não existir
mkdir -p resultsMulti
mkdir -p resultsMulti/stats

# Compilar/construir o projeto usando Gradle
echo "Compilando o projeto com Gradle..."
./gradlew build --no-daemon

if [ $? -ne 0 ]; then
    echo "Erro na compilação! Abortando execução."
    exit 1
fi

echo "Compilação concluída com sucesso!"
echo ""

# Contador de progresso
TOTAL=${#INSTANCES[@]}
CURRENT=0

# Executar para cada instância
for i in "${!INSTANCES[@]}"; do
    INSTANCE_NUM=${INSTANCES[$i]}
    INSTANCE_NAME=${INSTANCE_NAMES[$i]}
    CURRENT=$((CURRENT + 1))
    
    echo "=========================================="
    echo "[$CURRENT/$TOTAL] Executando instância: $INSTANCE_NAME (índice $INSTANCE_NUM)"
    echo "=========================================="
    echo ""
    
    # Executar o programa Java passando o número da instância como argumento
    ./gradlew run --args="$INSTANCE_NUM" --no-daemon --console=plain
    
    if [ $? -ne 0 ]; then
        echo ""
        echo "ERRO: Falha ao executar instância $INSTANCE_NAME"
        echo "Continuando com a próxima instância..."
    else
        echo ""
        echo "✓ Instância $INSTANCE_NAME concluída com sucesso!"
    fi
    
    echo ""
    echo ""
done

echo "=========================================="
echo "Execução completa!"
echo "=========================================="
echo ""
echo "Total de instâncias processadas: $TOTAL"
echo "Resultados salvos em: resultsMulti/"
echo ""
echo "Arquivos gerados:"
ls -lh resultsMulti/evo_*.txt 2>/dev/null | awk '{print "  - " $9}'
echo ""
echo "Estatísticas geradas:"
ls -lh resultsMulti/stats/stats_*.txt 2>/dev/null | awk '{print "  - " $9}'
echo ""
