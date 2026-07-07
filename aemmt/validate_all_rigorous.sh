#!/bin/bash
# Script para validar TODOS os resultados usando validate_solution_rigorous.py
# E gerar relatórios detalhados em results_validation_*_detailed/

echo "========================================="
echo "VALIDAÇÃO RIGOROSA DE TODOS OS RESULTADOS"
echo "========================================="
echo ""

INSTANCE_DIR="src/instances/solomon"
TOTAL=0
VALID=0
INVALID=0

# Função para validar uma solução
validate_solution() {
    local instance_file=$1
    local solution_file=$2
    local instance_name=$3
    local output_file=$4
    
    if [ ! -f "$solution_file" ]; then
        return
    fi
    
    TOTAL=$((TOTAL + 1))
    
    echo "Validando: $instance_name - $(basename $solution_file)"
    
    if python3 scripts/validate_solution_rigorous.py "$instance_file" "$solution_file" > /dev/null 2>&1; then
        VALID=$((VALID + 1))
        echo "  ✓ VÁLIDA"
    else
        INVALID=$((INVALID + 1))
        echo "  ❌ INVÁLIDA"
        # Mostra os erros
        python3 scripts/validate_solution_rigorous.py "$instance_file" "$solution_file" 2>&1 | grep -E "ERRO|❌"
    fi
    
    # Gera relatório detalhado
    python3 scripts/generate_detailed_validation.py "$instance_file" "$solution_file" "$output_file" > /dev/null 2>&1
    
    echo ""
}

# Validar instâncias C1 (C101-C109)
echo "=== VALIDANDO INSTÂNCIAS C1 ==="
for i in {101..109}; do
    instance="C$i"
    instance_file="$INSTANCE_DIR/$instance.txt"
    
    if [ -f "$instance_file" ]; then
        for exec_num in {01..10}; do
            solution_file="results_validation_C1/$instance/evo_c${i}_exec${exec_num}.txt"
            output_file="results_validation_C1_detailed/$instance/evo_c${i}_exec${exec_num}.txt"
            validate_solution "$instance_file" "$solution_file" "$instance" "$output_file"
        done
    fi
done

# Validar instâncias R1 (R101-R112)
echo "=== VALIDANDO INSTÂNCIAS R1 ==="
for i in {101..112}; do
    instance="R$i"
    instance_file="$INSTANCE_DIR/$instance.txt"
    
    if [ -f "$instance_file" ]; then
        for exec_num in {01..10}; do
            solution_file="results_validation_R1/$instance/evo_r${i}_exec${exec_num}.txt"
            output_file="results_validation_R1_detailed/$instance/evo_r${i}_exec${exec_num}.txt"
            validate_solution "$instance_file" "$solution_file" "$instance" "$output_file"
        done
    fi
done

# Validar instâncias RC1 (RC101-RC108)
echo "=== VALIDANDO INSTÂNCIAS RC1 ==="
for i in {101..108}; do
    instance="RC$i"
    instance_file="$INSTANCE_DIR/$instance.txt"
    
    if [ -f "$instance_file" ]; then
        for exec_num in {01..10}; do
            solution_file="results_validation_RC1/$instance/evo_rc${i}_exec${exec_num}.txt"
            output_file="results_validation_RC1_detailed/$instance/evo_rc${i}_exec${exec_num}.txt"
            validate_solution "$instance_file" "$solution_file" "$instance" "$output_file"
        done
    fi
done

# Resumo final
echo "========================================="
echo "RESUMO DA VALIDAÇÃO"
echo "========================================="
echo "Total de soluções validadas: $TOTAL"
echo "Soluções VÁLIDAS: $VALID"
echo "Soluções INVÁLIDAS: $INVALID"
echo ""
echo "📄 Relatórios detalhados salvos em:"
echo "   - results_validation_C1_detailed/"
echo "   - results_validation_R1_detailed/"
echo "   - results_validation_RC1_detailed/"
echo "========================================="

if [ $INVALID -eq 0 ]; then
    echo "✓ TODAS AS SOLUÇÕES SÃO VÁLIDAS!"
    exit 0
else
    echo "❌ EXISTEM SOLUÇÕES INVÁLIDAS!"
    exit 1
fi
