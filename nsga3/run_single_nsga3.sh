#!/bin/bash

# Script para executar uma única instância com NSGA-III
# Uso: ./run_single_nsga3.sh <numero_instancia> [tempo_limite]
# Exemplo: ./run_single_nsga3.sh 1 300  (executa C101 por 300 segundos)

if [ $# -eq 0 ]; then
    echo "Erro: Nenhuma instância especificada!"
    echo ""
    echo "Uso: ./run_single_nsga3.sh <numero_instancia> [tempo_limite]"
    echo ""
    echo "Instâncias disponíveis:"
    echo "  1-9   : C101 a C109"
    echo "  10-17 : R101 a R109"
    echo "  18-25 : RC101 a RC108"
    echo ""
    echo "Exemplo: ./run_single_nsga3.sh 1 300  (executa C101 por 300s)"
    exit 1
fi

INSTANCE_NUM=$1
TIMEOUT=${2:-300}  # Default: 300 segundos (5 minutos)

echo "=========================================="
echo "Executor NSGA-III - Instância Única"
echo "=========================================="
echo ""

# Compilar se necessário
if [ ! -d "app/build/classes/java/main" ] || [ ! -f "app/build/classes/java/main/main/App.class" ]; then
    echo "Compilando o projeto..."
    ./gradlew build -x test --console=plain -q
    
    if [ $? -ne 0 ]; then
        echo "Erro na compilação! Abortando execução."
        exit 1
    fi
    
    echo "Compilação concluída!"
    echo ""
fi

# Preparar classpath
CLASSPATH="app/build/classes/java/main"

# Adicionar JARs das dependências (JMetal, etc)
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    for jar in "$GRADLE_CACHE"/*/*/*/*/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

echo "Executando instância número: $INSTANCE_NUM"
echo "Algoritmo: NSGA-III (opção 4)"
echo "Tipo: Solomon (opção 1)"
echo "Timeout: ${TIMEOUT}s"
echo ""

# Executar NSGA-III com Java direto
java -cp "$CLASSPATH" main.App $INSTANCE_NUM 4 1

if [ $? -eq 124 ]; then
    echo ""
    echo "⚠ Execução interrompida por timeout (${TIMEOUT}s)"
elif [ $? -ne 0 ]; then
    echo ""
    echo "✗ ERRO: Falha ao executar a instância"
    exit 1
else
    echo ""
    echo "✓ Execução concluída com sucesso!"
fi
