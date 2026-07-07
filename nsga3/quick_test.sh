#!/bin/bash

# Script de teste rápido para validar se o NSGA-III está funcionando
# Usa timeout de apenas 10 segundos para teste rápido

echo "=========================================="
echo "TESTE RÁPIDO NSGA-III"
echo "=========================================="
echo ""

# Compilar se necessário
echo "📦 Compilando projeto..."
./gradlew build -x test --console=plain -q

if [ $? -ne 0 ]; then
    echo "✗ Erro na compilação!"
    exit 1
fi

echo "✓ Compilação OK"
echo ""

# Preparar classpath
CLASSPATH="app/build/classes/java/main"

# Adicionar JARs das dependências (JMetal, etc)
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    for jar in "$GRADLE_CACHE"/*/*/*/*/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

echo "🧪 Testando C101 com timeout de 10 segundos..."
echo "   (esperado: timeout, mas deve iniciar a execução)"
echo ""

# Executar com timeout curto apenas para validar
START=$(date +%s)
timeout 10s java -cp "$CLASSPATH" main.App 1 4 1 2>&1 | head -20
EXIT_CODE=$?
END=$(date +%s)
DURATION=$((END - START))

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 124 ]; then
    echo "✓ SUCESSO: Execução iniciou e foi interrompida por timeout"
    echo "   Tempo decorrido: ${DURATION}s"
    echo ""
    echo "✅ O script está funcionando corretamente!"
    echo "   Você pode executar testes completos agora."
elif [ $EXIT_CODE -eq 0 ]; then
    echo "✓ SUCESSO: Execução completou em ${DURATION}s"
    echo "   (surpreendentemente rápido, mas OK!)"
else
    echo "✗ ERRO: Falha na execução (código: $EXIT_CODE)"
    echo "   Tempo decorrido: ${DURATION}s"
    exit 1
fi
echo "=========================================="
