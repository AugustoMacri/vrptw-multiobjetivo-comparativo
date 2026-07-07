#!/bin/bash
# Script para testar NSGA-III com restrições hard de janelas de tempo e capacidade

echo "=== TESTE NSGA-III COM RESTRIÇÕES HARD ==="
echo

# Compilar
echo "1. Compilando projeto..."
./gradlew build -x test > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Compilação OK"
else
    echo "   ❌ Erro na compilação"
    exit 1
fi

# Executar NSGA-III (apenas C101)
echo
echo "2. Executando NSGA-III (C101, exec_test)..."
echo "   População: 900, Gerações: 5000"
echo "   Pc=1.0, Pm=0.01"
echo

# Simular entrada: opção 4 (NSGA-III), opção 1 (Solomon), opção 1 (C101)
echo -e "4\n1\n1" | ./gradlew run --console=plain 2>&1 | tee /tmp/nsga3_test.log

# Verificar se gerou resultado
if [ -f "resultsNSGA3/evo_c101_exec_test.txt" ]; then
    echo
    echo "3. Validando resultado..."
    python3 scripts/validate_nsga3_solution.py app/src/main/java/instances/solomon/C101.txt resultsNSGA3/evo_c101_exec_test.txt
else
    echo
    echo "❌ Arquivo de resultado não encontrado"
    echo "   Procurando por: resultsNSGA3/evo_c101_exec_test.txt"
    ls -la resultsNSGA3/ | tail -10
fi
