#!/bin/bash
# Script para validar TODAS as solucoes NSGA-III usando validacao rigorosa

echo "========================================="
echo "VALIDACAO RIGOROSA DE TODOS OS RESULTADOS NSGA-III"
echo "========================================="
echo ""

INSTANCE_DIR="app/src/main/java/instances/solomon"
RESULTS_DIR="resultsNSGA3"
TOTAL=0
VALID=0
INVALID=0

# Detecta python: testa se responde com versao real (nao alias Microsoft Store)
PYTHON=""
for cmd in python3 python; do
    if $cmd --version &> /dev/null; then
        PYTHON=$cmd
        break
    fi
done
if [ -z "$PYTHON" ]; then
    echo "ERRO: python nao encontrado!"
    exit 1
fi
echo "Usando: $PYTHON ($($PYTHON --version 2>&1))"
echo ""

# Funcao para validar uma solucao
validate_solution() {
    local instance_file=$1
    local solution_file=$2
    local instance_name=$3

    if [ ! -f "$solution_file" ]; then
        return
    fi

    TOTAL=$((TOTAL + 1))

    # Captura saida do validador
    output=$($PYTHON scripts/validate_nsga3_solution.py "$instance_file" "$solution_file" 2>&1)
    exit_code=$?

    if [ $exit_code -eq 0 ]; then
        VALID=$((VALID + 1))
        echo "  [OK] $instance_name - $(basename $solution_file)"
    else
        INVALID=$((INVALID + 1))
        echo "  [FALHA] $instance_name - $(basename $solution_file)"
        # Mostra linhas com [FALHA] ou ERRO para identificar o problema
        echo "$output" | grep -E "\[FALHA\]|ERRO|Nenhuma" | while read line; do
            echo "         $line"
        done
        # Se nao mostrou nada, mostra saida completa (debug)
        if ! echo "$output" | grep -qE "\[FALHA\]|ERRO|Nenhuma"; then
            echo "         [DEBUG] Saida completa:"
            echo "$output" | head -5 | while read line; do
                echo "           $line"
            done
        fi
    fi
}

# Validar instancias C1 (C101-C109)
echo "=== VALIDANDO INSTANCIAS C1 ==="
for i in $(seq 101 109); do
    instance="C${i}"
    instance_file="$INSTANCE_DIR/$instance.txt"

    if [ -f "$instance_file" ]; then
        for exec_num in $(seq -w 1 10); do
            solution_file="$RESULTS_DIR/evo_${instance,,}_exec${exec_num}.txt"
            validate_solution "$instance_file" "$solution_file" "$instance"
        done
    fi
done
echo ""

# Validar instancias R1 (R101-R112)
echo "=== VALIDANDO INSTANCIAS R1 ==="
for i in $(seq 101 112); do
    instance="R${i}"
    instance_file="$INSTANCE_DIR/$instance.txt"

    if [ -f "$instance_file" ]; then
        for exec_num in $(seq -w 1 10); do
            solution_file="$RESULTS_DIR/evo_${instance,,}_exec${exec_num}.txt"
            validate_solution "$instance_file" "$solution_file" "$instance"
        done
    fi
done
echo ""

# Validar instancias RC1 (RC101-RC108)
echo "=== VALIDANDO INSTANCIAS RC1 ==="
for i in $(seq 101 108); do
    instance="RC${i}"
    instance_file="$INSTANCE_DIR/$instance.txt"

    if [ -f "$instance_file" ]; then
        for exec_num in $(seq -w 1 10); do
            solution_file="$RESULTS_DIR/evo_${instance,,}_exec${exec_num}.txt"
            validate_solution "$instance_file" "$solution_file" "$instance"
        done
    fi
done
echo ""

# Resumo final
echo "========================================="
echo "RESUMO DA VALIDACAO"
echo "========================================="
echo "Total de solucoes validadas: $TOTAL"
echo "Solucoes VALIDAS:   $VALID"
echo "Solucoes INVALIDAS: $INVALID"

if [ $TOTAL -gt 0 ]; then
    pct=$((VALID * 100 / TOTAL))
    echo "Taxa de validade:   ${pct}%"
fi
echo "========================================="

if [ $INVALID -eq 0 ]; then
    echo "[OK] TODAS AS SOLUCOES SAO VALIDAS!"
    exit 0
else
    echo "[FALHA] EXISTEM SOLUCOES INVALIDAS!"
    exit 1
fi
