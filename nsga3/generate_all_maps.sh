#!/bin/bash

# Script para gerar todos os mapas das rotas NSGA-III
# Gera um mapa para cada execução de cada instância C1

echo "=========================================="
echo "Gerador de Mapas - NSGA-III"
echo "=========================================="

RESULTS_DIR="resultsNSGA3"
MAPS_DIR="$RESULTS_DIR/maps"
INSTANCES_DIR="app/src/main/java/instances/solomon"

# Criar diretório de mapas
mkdir -p "$MAPS_DIR"

# Contador de mapas gerados
total_maps=0
total_files=0

# Para cada instância C1
for instance in C101 C102 C103 C104 C105 C106 C107 C108 C109; do
    echo ""
    echo "Processando $instance..."
    instance_maps=0
    
    # Criar subpasta para esta instância
    mkdir -p "$MAPS_DIR/$instance"
    
    # Para cada execução (01 a 10)
    for exec_num in $(seq -f "%02g" 1 10); do
        evo_file="$RESULTS_DIR/evo_${instance,,}_exec${exec_num}.txt"
        
        if [ -f "$evo_file" ]; then
            output_file="$MAPS_DIR/$instance/map_${instance,,}_exec${exec_num}.png"
            
            # Gerar mapa
            python3 scripts/generate_maps_nsga3.py \
                --instance "$instance" \
                --evo-file "$evo_file" \
                --output "$output_file" 2>&1 | grep -v "^Lendo"
            
            if [ $? -eq 0 ] && [ -f "$output_file" ]; then
                instance_maps=$((instance_maps + 1))
                total_maps=$((total_maps + 1))
            fi
            
            total_files=$((total_files + 1))
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
echo "Total de arquivos processados: $total_files"
echo "Total de mapas gerados: $total_maps"
echo "Mapas salvos em: $MAPS_DIR"
echo "=========================================="
