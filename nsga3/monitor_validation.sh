#!/bin/bash

# Script para monitorar a validação NSGA-III em execução

LOG_FILE="validation_full.log"
RESULT_DIR="results_validation_NSGA3_C1"

echo "=========================================="
echo "Monitor de Validação NSGA-III"
echo "=========================================="
echo ""

# Verificar se a validação está rodando
if pgrep -f "run_validation_nsga3_c1.sh" > /dev/null; then
    echo "✓ Validação em execução"
else
    echo "✗ Validação não está rodando"
fi

echo ""
echo "=== PROGRESSO ==="
tail -5 "$LOG_FILE" 2>/dev/null | grep -E "Execução|Progresso|Instância" || echo "Aguardando início..."

echo ""
echo "=== ESTATÍSTICAS ==="

# Contar execuções completadas
if [ -d "$RESULT_DIR" ]; then
    COMPLETED=$(find "$RESULT_DIR" -name "stats_*.txt" 2>/dev/null | wc -l)
    echo "Execuções completadas: $COMPLETED/90"
    
    # Calcular progresso percentual
    PERCENT=$(awk "BEGIN {printf \"%.1f\", ($COMPLETED/90)*100}")
    echo "Progresso: $PERCENT%"
    
    # Estimar tempo restante (assumindo ~60s por execução)
    REMAINING=$((90 - COMPLETED))
    TIME_LEFT=$((REMAINING * 60))
    MINUTES=$((TIME_LEFT / 60))
    echo "Tempo estimado restante: ~$MINUTES minutos"
else
    echo "Nenhuma execução completada ainda"
fi

echo ""
echo "=== ÚLTIMAS LINHAS DO LOG ==="
tail -10 "$LOG_FILE" 2>/dev/null || echo "Log ainda vazio"

echo ""
echo "=========================================="
echo "Para monitorar continuamente, use:"
echo "  watch -n 10 ./monitor_validation.sh"
echo "=========================================="
