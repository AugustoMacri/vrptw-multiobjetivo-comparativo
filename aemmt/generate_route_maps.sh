#!/bin/bash

# Script para gerar mapas de rotas após executar instâncias
# Uso: ./generate_route_maps.sh [instance_name]

INSTANCES_DIR="src/instances/solomon"

if [ $# -eq 0 ]; then
    echo "=========================================="
    echo "Gerador de Mapas de Rotas VRP"
    echo "=========================================="
    echo ""
    echo "Uso: ./generate_route_maps.sh <instance_name>"
    echo ""
    echo "Exemplos:"
    echo "  ./generate_route_maps.sh c101              # Gera mapas para C101 (resultsMulti)"
    echo "  ./generate_route_maps.sh r101              # Gera mapas para R101 (resultsMulti)"
    echo "  ./generate_route_maps.sh all_c1            # Gera mapas para todas as C1 (C101-C109) - 10 execuções cada"
    echo "  ./generate_route_maps.sh all_r1            # Gera mapas para todas as R1 (R101-R109) - 10 execuções cada"
    echo "  ./generate_route_maps.sh all_rc1           # Gera mapas para todas as RC1 (RC101-RC108) - 10 execuções cada"
    echo ""
    exit 1
fi

INSTANCE_PARAM=$1

# Diretórios de validação
RESULTS_VALIDATION_C1_DIR="results_validation_C1"
RESULTS_VALIDATION_DIR="results_validation_R1"
RESULTS_VALIDATION_RC1_DIR="results_validation_RC1"
PLOT_SCRIPT="scripts/plot_route_maps.py"

generate_single_map() {
    local instance_name=$1
    local instance_upper=$(echo "$instance_name" | tr '[:lower:]' '[:upper:]')
    
    echo ""
    echo "Gerando mapas para instância: $instance_upper"
    echo "----------------------------------------"
    
    if [ -f "resultsMulti/evo_${instance_name}.txt" ]; then
        python3 scripts/plot_route_maps.py \
            --instance "$instance_upper" \
            --results-dir resultsMulti \
            --instances-dir "$INSTANCES_DIR" \
            --output-dir resultsMulti/route_maps
        
        if [ $? -eq 0 ]; then
            echo "✓ Mapas gerados com sucesso!"
            echo "  - Inicial: resultsMulti/route_maps/${instance_upper}/route_maps/route_map_${instance_name}_initial.png"
            echo "  - Final:   resultsMulti/route_maps/${instance_upper}/route_maps/route_map_${instance_name}_final.png"
        else
            echo "✗ Erro ao gerar mapas para $instance_upper"
        fi
    else
        echo "✗ Arquivo resultsMulti/evo_${instance_name}.txt não encontrado!"
        echo "  Execute primeiro: ./run_single_instance.sh <numero_instancia>"
    fi
}

generate_validation_maps() {
    local instance_name=$1
    local instance_upper=$(echo "$instance_name" | tr '[:lower:]' '[:upper:]')
    local validation_dir="results_validation_C1/${instance_upper}"
    
    if [ ! -d "$validation_dir" ]; then
        echo "✗ Diretório $validation_dir não encontrado!"
        return
    fi
    
    echo ""
    echo "=========================================="
    echo "Gerando mapas para: $instance_upper"
    echo "=========================================="
    
    local maps_generated=0
    local maps_failed=0
    
    for exec_num in {01..10}; do
        local evo_file="$validation_dir/evo_${instance_name}_exec${exec_num}.txt"
        
        if [ -f "$evo_file" ]; then
            echo ""
            echo "Execução ${exec_num}/10..."
            
            python3 scripts/plot_route_maps.py \
                --instance "$instance_upper" \
                --results-file "$evo_file" \
                --instances-dir "$INSTANCES_DIR" \
                --output-dir "$validation_dir/route_maps_exec${exec_num}" \
                2>/dev/null
            
            if [ $? -eq 0 ]; then
                echo "  ✓ Mapas gerados: $validation_dir/route_maps_exec${exec_num}/"
                ((maps_generated++))
            else
                echo "  ✗ Erro ao gerar mapas"
                ((maps_failed++))
            fi
        else
            echo "  ✗ Arquivo não encontrado: $evo_file"
            ((maps_failed++))
        fi
    done
    
    echo ""
    echo "Resumo $instance_upper: ✓ $maps_generated gerados | ✗ $maps_failed falhas"
}

# Função para gerar mapas de validação para R1 (results_validation_R1)
generate_validation_maps_r1() {
    echo "Gerando mapas para validação R1 (results_validation_R1)..."
    
    # Lista de instâncias R1 (R101 até R109)
    INSTANCES=("r101" "r102" "r103" "r104" "r105" "r106" "r107" "r108" "r109")
    
    for instance_lower in "${INSTANCES[@]}"; do
        instance_upper=$(echo "$instance_lower" | tr '[:lower:]' '[:upper:]')
        
        echo ""
        echo "============================================"
        echo "Processando instância: $instance_upper"
        echo "============================================"
        
        # Diretório base dos resultados desta instância
        base_dir="${RESULTS_VALIDATION_DIR}/${instance_upper}"
        
        if [ ! -d "$base_dir" ]; then
            echo "⚠️  Aviso: Diretório $base_dir não encontrado. Pulando..."
            continue
        fi
        
        # Gera mapas para cada execução (exec01 até exec10)
        for exec_num in $(seq -w 1 10); do
            exec_id="exec${exec_num}"
            results_file="${base_dir}/evo_${instance_lower}_${exec_id}.txt"
            output_dir="${base_dir}/route_maps_${exec_id}"
            
            if [ ! -f "$results_file" ]; then
                echo "⚠️  Arquivo não encontrado: $results_file. Pulando..."
                continue
            fi
            
            echo ""
            echo "Gerando mapas para $instance_upper $exec_id..."
            
            # Cria o diretório de saída se não existir
            mkdir -p "$output_dir"
            
            # Gera os mapas usando o script Python
            python3 "$PLOT_SCRIPT" \
                --instance "$instance_lower" \
                --results-file "$results_file" \
                --instances-dir "$INSTANCES_DIR" \
                --output-dir "$output_dir"
            
            if [ $? -eq 0 ]; then
                echo "✓ Mapas gerados em: $output_dir"
            else
                echo "✗ Erro ao gerar mapas para $instance_upper $exec_id"
            fi
        done
    done
    
    echo ""
    echo "============================================"
    echo "Todos os mapas gerados com sucesso!"
}

# Função para gerar mapas de validação para RC1 (results_validation_RC1)
generate_validation_maps_rc1() {
    echo "Gerando mapas para validação RC1 (results_validation_RC1)..."
    
    # Lista de instâncias RC1 (RC101 até RC108)
    INSTANCES=("rc101" "rc102" "rc103" "rc104" "rc105" "rc106" "rc107" "rc108")
    
    for instance_lower in "${INSTANCES[@]}"; do
        instance_upper=$(echo "$instance_lower" | tr '[:lower:]' '[:upper:]')
        
        echo ""
        echo "============================================"
        echo "Processando instância: $instance_upper"
        echo "============================================"
        
        # Diretório base dos resultados desta instância
        base_dir="${RESULTS_VALIDATION_RC1_DIR}/${instance_upper}"
        
        if [ ! -d "$base_dir" ]; then
            echo "⚠️  Aviso: Diretório $base_dir não encontrado. Pulando..."
            continue
        fi
        
        # Gera mapas para cada execução (exec01 até exec10)
        for exec_num in $(seq -w 1 10); do
            exec_id="exec${exec_num}"
            results_file="${base_dir}/evo_${instance_lower}_${exec_id}.txt"
            output_dir="${base_dir}/route_maps_${exec_id}"
            
            if [ ! -f "$results_file" ]; then
                echo "⚠️  Arquivo não encontrado: $results_file. Pulando..."
                continue
            fi
            
            echo ""
            echo "Gerando mapas para $instance_upper $exec_id..."
            
            # Cria o diretório de saída se não existir
            mkdir -p "$output_dir"
            
            # Gera os mapas usando o script Python
            python3 "$PLOT_SCRIPT" \
                --instance "$instance_lower" \
                --results-file "$results_file" \
                --instances-dir "$INSTANCES_DIR" \
                --output-dir "$output_dir"
            
            if [ $? -eq 0 ]; then
                echo "✓ Mapas gerados em: $output_dir"
            else
                echo "✗ Erro ao gerar mapas para $instance_upper $exec_id"
            fi
        done
    done
    
    echo ""
    echo "============================================"
    echo "Todos os mapas gerados com sucesso!"
}

# Processar comando
case "$INSTANCE_PARAM" in
    all_c1)
        echo "=========================================="
        echo "Gerando mapas para TODAS as instâncias C1"
        echo "Validação: 10 execuções por instância"
        echo "=========================================="
        
        for i in {101..109}; do
            generate_validation_maps "c${i}"
        done
        
        echo ""
        echo "=========================================="
        echo "Geração de mapas concluída!"
        echo "Mapas salvos em: results_validation_C1/CXXX/route_maps_execYY/"
        echo "=========================================="
        ;;
    
    all_c1_validation)
        echo "=========================================="
        echo "Gerando mapas de VALIDAÇÃO C1"
        echo "10 execuções por instância"
        echo "=========================================="
        
        for i in {101..109}; do
            generate_validation_maps "c${i}"
        done
        
        echo ""
        echo "=========================================="
        echo "Geração de mapas concluída!"
        echo "Mapas salvos em: results_validation_C1/CXXX/route_maps_execYY/"
        echo "=========================================="
        ;;
    
    all_r1)
        echo "=========================================="
        echo "Gerando mapas para TODAS as instâncias R1"
        echo "Validação: 10 execuções por instância"
        echo "=========================================="
        
        generate_validation_maps_r1
        
        echo ""
        echo "=========================================="
        echo "Geração de mapas concluída!"
        echo "Mapas salvos em: results_validation_R1/R1XX/route_maps_execYY/"
        echo "=========================================="
        ;;
    
    all_rc1)
        echo "=========================================="
        echo "Gerando mapas para TODAS as instâncias RC1"
        echo "Validação: 10 execuções por instância"
        echo "=========================================="
        
        generate_validation_maps_rc1
        
        echo ""
        echo "=========================================="
        echo "Geração de mapas concluída!"
        echo "Mapas salvos em: results_validation_RC1/RC1XX/route_maps_execYY/"
        echo "=========================================="
        ;;
    
    c[0-9]*)
        # Instância C específica - verificar se é de validação
        instance_upper=$(echo "$INSTANCE_PARAM" | tr '[:lower:]' '[:upper:]')
        if [ -d "results_validation_C1/${instance_upper}" ]; then
            echo "Detectado diretório de validação. Gerando mapas para 10 execuções..."
            generate_validation_maps "$INSTANCE_PARAM"
        else
            generate_single_map "$INSTANCE_PARAM"
        fi
        ;;
    
    *)
        # Instância individual
        generate_single_map "$INSTANCE_PARAM"
        ;;
esac
