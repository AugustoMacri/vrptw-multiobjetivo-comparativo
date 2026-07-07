#!/bin/bash
# Script para teste rápido do NSGA-III com restrições

echo "🧪 TESTE RÁPIDO NSGA-III COM RESTRIÇÕES HARD"
echo "============================================"
echo

# Verificar compilação
echo "1️⃣  Compilando..."
./gradlew build -x test > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Compilação OK"
else
    echo "   ❌ Erro na compilação"
    exit 1
fi

# Limpar resultados antigos
echo
echo "2️⃣  Limpando resultados antigos..."
rm -rf resultsNSGA3/
mkdir -p resultsNSGA3
echo "   ✅ Diretório limpo"

# Executar C101 com timeout de 2 minutos
echo
echo "3️⃣  Executando NSGA-III (C101)..."
echo "   ⏱️  Timeout: 2 minutos"
echo "   População: 900, Gerações: 5000"
echo

bash -c "echo -e '4\n1\n1' | ./gradlew run --console=plain" 2>&1 | \
    grep -E "(Soluções encontradas|Distância:|Tempo:|Combustível:|Melhor)" | head -20

# Verificar se gerou arquivo
echo
echo "4️⃣  Verificando arquivos gerados..."
if [ -f "resultsNSGA3/evo_c101.txt" ]; then
    echo "   ✅ Arquivo gerado: resultsNSGA3/evo_c101.txt"
    echo
    
    # Mostrar primeiras linhas
    echo "📄 Primeiras 15 linhas do arquivo:"
    head -15 resultsNSGA3/evo_c101.txt
    echo
    
    # Validar
    echo "5️⃣  Validando solução..."
    python3 scripts/validate_nsga3_solution.py \
        app/src/main/java/instances/solomon/C101.txt \
        resultsNSGA3/evo_c101.txt
else
    echo "   ❌ Arquivo não foi gerado!"
    echo "   Procurando arquivos em resultsNSGA3/:"
    ls -la resultsNSGA3/ 2>/dev/null || echo "   Diretório vazio ou não existe"
fi

echo
echo "============================================"
echo "✅ TESTE CONCLUÍDO"
