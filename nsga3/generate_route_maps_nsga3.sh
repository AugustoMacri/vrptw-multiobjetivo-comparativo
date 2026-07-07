#!/bin/bash

# Script para gerar mapas de rotas dos resultados NSGA-III
# Gera mapa inicial e final para cada execução de cada instância
# Uso: ./generate_route_maps_nsga3.sh [instancia|all_c1|all_r1|all_rc1]

echo "=========================================="
echo "Gerador de Mapas de Rotas - NSGA-III"
echo "=========================================="
echo ""

RESULTS_SOURCE="resultsNSGA3"
INSTANCES_DIR="app/src/main/java/instances/solomon"

# Detecta python
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

# Verificar se o diretório de resultados fonte existe
if [ ! -d "$RESULTS_SOURCE" ]; then
    echo "Erro: Diretório de resultados não encontrado: $RESULTS_SOURCE"
    exit 1
fi

# Função para gerar mapas de uma instância
generate_maps_for_instance() {
    local instance=$1
    local output_dir="$OUTPUT_BASE_DIR/$instance"
    
    # Criar diretório de saída se não existir
    mkdir -p "$output_dir"
    
    echo "Gerando mapas para $instance..."
    
    # Verificar arquivo de instância
    local instance_file="$INSTANCES_DIR/${instance}.txt"
    if [ ! -f "$instance_file" ]; then
        echo "  ⚠ Arquivo de instância não encontrado: $instance_file"
        return
    fi
    
    # Para cada execução (01 a 10)
    local total_maps=0
    for exec_num in $(seq -f "%02g" 1 10); do
        EVO_FILE="$RESULTS_SOURCE/evo_${instance,,}_exec${exec_num}.txt"
        MAP_DIR="$output_dir/maps_exec${exec_num}"
        
        if [ ! -f "$EVO_FILE" ]; then
            continue
        fi
        
        # Criar diretório para mapas desta execução
        mkdir -p "$MAP_DIR"
        
        # Gerar mapas usando o script Python NSGA3
        $PYTHON scripts/generate_maps_nsga3.py \
            --instance "$instance" \
            --evo-file "$EVO_FILE" \
            --instances-dir "$INSTANCES_DIR" \
            --output "$MAP_DIR/route_map_${instance,,}_exec${exec_num}.png" >/dev/null 2>&1
        
        if [ $? -eq 0 ] && [ -f "$MAP_DIR/route_map_${instance,,}_exec${exec_num}.png" ]; then
            echo "  ✓ Exec${exec_num}: mapa gerado"
            total_maps=$((total_maps + 1))
        fi
    done
    
    if [ $total_maps -gt 0 ]; then
        echo "✓ $instance: $total_maps mapas gerados"
    else
        echo "⚠ $instance: nenhum mapa gerado"
    fi
    echo ""
}

# Verificar argumento
if [ $# -eq 0 ]; then
    echo "Uso: $0 <instancia|all_c1|all_r1|all_rc1>"
    echo ""
    echo "Exemplos:"
    echo "  $0 C101           # Gerar mapas apenas para C101"
    echo "  $0 all_c1         # Gerar mapas para todas instâncias C1"
    echo "  $0 all_r1         # Gerar mapas para todas instâncias R1"
    echo "  $0 all_rc1        # Gerar mapas para todas instâncias RC1"
    exit 1
fi

# Processar comando
if [ "$1" = "all_c1" ]; then
    OUTPUT_BASE_DIR="results_validation_NSGA3_C1"
    echo "Gerando mapas para TODAS as instâncias C1..."
    echo ""
    
    for instance in C101 C102 C103 C104 C105 C106 C107 C108 C109; do
        generate_maps_for_instance "$instance"
    done
    
    echo "=========================================="
    echo "TODOS OS MAPAS C1 GERADOS!"
    echo "=========================================="

elif [ "$1" = "all_r1" ]; then
    OUTPUT_BASE_DIR="results_validation_NSGA3_R1"
    echo "Gerando mapas para TODAS as instâncias R1..."
    echo ""
    
    for instance in R101 R102 R103 R104 R105 R106 R107 R108 R109 R110 R111 R112; do
        generate_maps_for_instance "$instance"
    done
    
    echo "=========================================="
    echo "TODOS OS MAPAS R1 GERADOS!"
    echo "=========================================="

elif [ "$1" = "all_rc1" ]; then
    OUTPUT_BASE_DIR="results_validation_NSGA3_RC1"
    echo "Gerando mapas para TODAS as instâncias RC1..."
    echo ""
    
    for instance in RC101 RC102 RC103 RC104 RC105 RC106 RC107 RC108; do
        generate_maps_for_instance "$instance"
    done
    
    echo "=========================================="
    echo "TODOS OS MAPAS RC1 GERADOS!"
    echo "=========================================="
    
else
    # Tentar gerar para instância individual
    # Detectar categoria da instância
    if [[ "$1" =~ ^C[0-9] ]]; then
        OUTPUT_BASE_DIR="results_validation_NSGA3_C1"
    elif [[ "$1" =~ ^RC[0-9] ]]; then
        OUTPUT_BASE_DIR="results_validation_NSGA3_RC1"
    elif [[ "$1" =~ ^R[0-9] ]]; then
        OUTPUT_BASE_DIR="results_validation_NSGA3_R1"
    else
        echo "Erro: Instância não reconhecida: $1"
        echo "Use C101-C109, R101-R112, ou RC101-RC108"
        exit 1
    fi
    
    generate_maps_for_instance "$1"
fi
