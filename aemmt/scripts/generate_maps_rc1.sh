#!/bin/bash

# Script para gerar mapas de todas as execuções das instâncias RC1 (RC101-RC108)
# Usa o script Python plot_map.py

echo "=========================================="
echo "Gerador de Mapas - Instâncias RC1"
echo "=========================================="

RESULTS_DIR="results_validation_RC1"
INSTANCES_DIR="src/instances/solomon"

# Verificar se o diretório de resultados existe
if [ ! -d "$RESULTS_DIR" ]; then
    echo "Erro: Diretório de resultados não encontrado: $RESULTS_DIR"
    echo "Execute primeiro: python3 scripts/run_validation_rc1.py"
    exit 1
fi

# Verificar se o script de plotagem existe
if [ ! -f "scripts/plot_map.py" ]; then
    echo "Erro: Script de plotagem não encontrado: scripts/plot_map.py"
    exit 1
fi

echo ""
echo "Gerando mapas para instâncias RC1..."
echo ""

total_maps=0

# Para cada instância RC1
for instance in RC101 RC102 RC103 RC104 RC105 RC106 RC107 RC108; do
    instance_dir="$RESULTS_DIR/$instance"
    
    if [ ! -d "$instance_dir" ]; then
        echo "⚠ Diretório não encontrado: $instance_dir"
        continue
    fi
    
    echo "Processando $instance..."
    instance_maps=0
    
    # Para cada execução (01 a 10)
    for exec_num in $(seq -f "%02g" 1 10); do
        evo_file="$instance_dir/evo_${instance,,}_exec${exec_num}.txt"
        
        if [ -f "$evo_file" ]; then
            # Diretório de saída para os mapas
            map_dir="$instance_dir/maps"
            mkdir -p "$map_dir"
            
            # Nome do arquivo de saída
            output_file="$map_dir/route_map_${instance,,}_exec${exec_num}.png"
            
            # Gerar mapa usando o script Python
            python3 scripts/plot_map.py \
                --instance "$INSTANCES_DIR/${instance}.txt" \
                --results "$evo_file" \
                --output "$output_file" > /dev/null 2>&1
            
            if [ $? -eq 0 ] && [ -f "$output_file" ]; then
                instance_maps=$((instance_maps + 1))
                total_maps=$((total_maps + 1))
            fi
        fi
    done
    
    if [ $instance_maps -gt 0 ]; then
        echo "  ✓ $instance: $instance_maps mapas gerados"
    else
        echo "  ⚠ $instance: nenhum mapa gerado"
    fi
done

echo ""
echo "=========================================="
echo "CONCLUÍDO!"
echo "Total de mapas gerados: $total_maps"
echo "=========================================="
