#!/bin/bash

# ═══════════════════════════════════════════════════════════════════
#   SCRIPT DE VALIDAÇÃO COMPLETA - NSGA-III
# ═══════════════════════════════════════════════════════════════════
#
#   Executa todas as instâncias Solomon (C1, R1, RC1) com NSGA-III
#   10 execuções por instância = 260 execuções totais
#
#   Uso: ./run_all_validations_nsga3.sh
#
# ═══════════════════════════════════════════════════════════════════

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Função para imprimir cabeçalho
print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}   $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo ""
}

# Função para executar validação de uma classe de instâncias
run_validation_class() {
    local CLASS_NAME=$1
    local INSTANCE_START=$2
    local INSTANCE_END=$3
    local RESULT_DIR=$4
    
    print_header "VALIDAÇÃO: $CLASS_NAME (Instâncias $INSTANCE_START-$INSTANCE_END)"
    
    # Criar diretório de resultados
    mkdir -p "$RESULT_DIR"
    
    # Array de nomes das instâncias
    declare -a INSTANCE_NAMES
    for i in $(seq -f "%02g" 1 9); do
        INSTANCE_NAMES+=("${CLASS_NAME}${i}")
    done
    
    # Calcular números das instâncias
    declare -a INSTANCE_NUMS
    for i in $(seq $INSTANCE_START $INSTANCE_END); do
        INSTANCE_NUMS+=($i)
    done
    
    # Total de execuções para esta classe
    TOTAL_EXECUTIONS=$((${#INSTANCE_NUMS[@]} * 10))
    CURRENT_EXECUTION=0
    
    echo -e "${GREEN}📦 Classe: $CLASS_NAME${NC}"
    echo -e "${GREEN}📊 Total de execuções: $TOTAL_EXECUTIONS${NC}"
    echo ""
    
    # Para cada instância
    for idx in "${!INSTANCE_NUMS[@]}"; do
        INSTANCE_NUM=${INSTANCE_NUMS[$idx]}
        
        # Determinar nome da instância (C101, R101, etc)
        if [ "$CLASS_NAME" = "C1" ]; then
            INSTANCE_NAME="C1$(printf "%02d" $((idx + 1)))"
        elif [ "$CLASS_NAME" = "R1" ]; then
            INSTANCE_NAME="R1$(printf "%02d" $((idx + 1)))"
        else
            INSTANCE_NAME="RC1$(printf "%02d" $((idx + 1)))"
        fi
        
        echo "────────────────────────────────────────────────────────────────────"
        echo -e "${YELLOW}Instância: $INSTANCE_NAME (Número: $INSTANCE_NUM)${NC}"
        echo "────────────────────────────────────────────────────────────────────"
        
        # Criar diretório para a instância
        INSTANCE_DIR="$RESULT_DIR/$INSTANCE_NAME"
        mkdir -p "$INSTANCE_DIR"
        
        # Executar 10 vezes
        for exec_num in $(seq -f "%02g" 1 10); do
            CURRENT_EXECUTION=$((CURRENT_EXECUTION + 1))
            
            echo ""
            echo -e "${BLUE}>>> Execução $exec_num/10 - $INSTANCE_NAME${NC}"
            echo -e "${BLUE}>>> Progresso: $CURRENT_EXECUTION/$TOTAL_EXECUTIONS execuções${NC}"
            
            # Arquivos de saída
            EVO_FILE="$INSTANCE_DIR/evo_${INSTANCE_NAME,,}_exec${exec_num}.txt"
            STATS_FILE="$INSTANCE_DIR/stats_${INSTANCE_NAME,,}_exec${exec_num}.txt"
            PARETO_FILE="$INSTANCE_DIR/pareto_${INSTANCE_NAME,,}_exec${exec_num}.txt"
            
            # Sufixo único para esta execução
            EXEC_SUFFIX="_exec${exec_num}"
            
            # Executar NSGA-III
            # Argumentos: <instancia> <algoritmo=4> <tipo=1> <sufixo_execucao>
            START_TIME=$(date +%s)
            java -cp "$CLASSPATH" main.App $INSTANCE_NUM 4 1 "$EXEC_SUFFIX" > "$STATS_FILE" 2>&1
            EXIT_CODE=$?
            END_TIME=$(date +%s)
            DURATION=$((END_TIME - START_TIME))
            
            if [ $EXIT_CODE -eq 124 ]; then
                echo -e "${RED}⏱️  Timeout após 300s${NC}"
            elif [ $EXIT_CODE -ne 0 ]; then
                echo -e "${RED}⚠️  Erro na execução (código: $EXIT_CODE)${NC}"
            else
                echo -e "${GREEN}⏱️  Tempo: ${DURATION}s${NC}"
            fi
            
            # Mover arquivos gerados
            if [ -f "app/resultsNSGA3/evo_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" ]; then
                mv "app/resultsNSGA3/evo_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" "$EVO_FILE"
                echo -e "${GREEN}✓ Evolução salva${NC}"
            else
                echo -e "${RED}⚠ Arquivo de evolução não encontrado!${NC}"
            fi
            
            if [ -f "app/resultsNSGA3/pareto_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" ]; then
                mv "app/resultsNSGA3/pareto_${INSTANCE_NAME,,}${EXEC_SUFFIX}.txt" "$PARETO_FILE"
                echo -e "${GREEN}✓ Pareto salvo${NC}"
            fi
            
            echo -e "${GREEN}✓ Execução $exec_num concluída${NC}"
        done
        
        echo ""
        echo -e "${GREEN}✓ Todas as 10 execuções de $INSTANCE_NAME concluídas!${NC}"
        echo ""
    done
    
    echo ""
    echo -e "${GREEN}════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ VALIDAÇÃO $CLASS_NAME CONCLUÍDA!${NC}"
    echo -e "${GREEN}   Total: $TOTAL_EXECUTIONS execuções${NC}"
    echo -e "${GREEN}   Resultados em: $RESULT_DIR/${NC}"
    echo -e "${GREEN}════════════════════════════════════════════════════════════════════${NC}"
    echo ""
}

# ═══════════════════════════════════════════════════════════════════
#   INÍCIO DA EXECUÇÃO
# ═══════════════════════════════════════════════════════════════════

print_header "VALIDAÇÃO COMPLETA NSGA-III - SOLOMON BENCHMARK"

echo -e "${YELLOW}Este script executará:${NC}"
echo "  • 9 instâncias C1 (C101-C109) × 10 execuções = 90 execuções"
echo "  • 9 instâncias R1 (R101-R109) × 10 execuções = 90 execuções"
echo "  • 8 instâncias RC1 (RC101-RC108) × 10 execuções = 80 execuções"
echo ""
echo -e "${YELLOW}Total: 260 execuções${NC}"
echo ""
echo -e "${YELLOW}Tempo estimado: ~21 horas (300s/execução)${NC}"
echo ""

# Confirmar execução
read -p "Deseja continuar? (s/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    echo "Execução cancelada pelo usuário."
    exit 0
fi

# Registrar início
SCRIPT_START=$(date +%s)
echo ""
echo -e "${GREEN}✓ Iniciando validação em: $(date)${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════════
#   COMPILAÇÃO
# ═══════════════════════════════════════════════════════════════════

print_header "COMPILAÇÃO DO PROJETO"

if [ ! -d "app/build/classes/java/main" ] || [ ! -f "app/build/classes/java/main/main/App.class" ]; then
    echo -e "${YELLOW}Compilando o projeto NSGA-III...${NC}"
    ./gradlew build -x test --console=plain -q
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Erro na compilação! Abortando execução.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Compilação concluída!${NC}"
else
    echo -e "${GREEN}✓ Projeto já compilado${NC}"
fi

# Preparar classpath
CLASSPATH="app/build/classes/java/main"

# Adicionar JARs das dependências (JMetal, etc)
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    for jar in "$GRADLE_CACHE"/*/*/*/*/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi

echo -e "${GREEN}✓ Classpath configurado${NC}"

# ═══════════════════════════════════════════════════════════════════
#   EXECUTAR VALIDAÇÕES
# ═══════════════════════════════════════════════════════════════════

# C1: Instâncias 1-9 (C101-C109)
run_validation_class "C1" 1 9 "results_validation_NSGA3_C1"

# R1: Instâncias 18-26 (R101-R109)
run_validation_class "R1" 18 26 "results_validation_NSGA3_R1"

# RC1: Instâncias 41-48 (RC101-RC108)
run_validation_class "RC1" 41 48 "results_validation_NSGA3_RC1"

# ═══════════════════════════════════════════════════════════════════
#   SUMÁRIO FINAL
# ═══════════════════════════════════════════════════════════════════

SCRIPT_END=$(date +%s)
TOTAL_DURATION=$((SCRIPT_END - SCRIPT_START))
HOURS=$((TOTAL_DURATION / 3600))
MINUTES=$(((TOTAL_DURATION % 3600) / 60))
SECONDS=$((TOTAL_DURATION % 60))

print_header "VALIDAÇÃO COMPLETA NSGA-III FINALIZADA! ✅"

echo -e "${GREEN}📊 SUMÁRIO:${NC}"
echo ""
echo "  ✓ Instâncias C1:  90 execuções → results_validation_NSGA3_C1/"
echo "  ✓ Instâncias R1:  90 execuções → results_validation_NSGA3_R1/"
echo "  ✓ Instâncias RC1: 80 execuções → results_validation_NSGA3_RC1/"
echo ""
echo -e "${GREEN}  TOTAL: 260 execuções concluídas!${NC}"
echo ""
echo -e "${YELLOW}⏱️  Tempo total: ${HOURS}h ${MINUTES}m ${SECONDS}s${NC}"
echo ""
echo -e "${BLUE}📁 Resultados salvos em:${NC}"
echo "  • results_validation_NSGA3_C1/"
echo "  • results_validation_NSGA3_R1/"
echo "  • results_validation_NSGA3_RC1/"
echo ""
echo -e "${YELLOW}🔍 PRÓXIMOS PASSOS:${NC}"
echo ""
echo "  1. Validar soluções geradas:"
echo "     ${GREEN}python3 scripts/validate_solution_rigorous.py <instancia> <resultado>${NC}"
echo ""
echo "  2. Validar todas as capacidades:"
echo "     ${GREEN}python3 scripts/validate_all_nsga3.py${NC}"
echo ""
echo "  3. Gerar mapas das rotas:"
echo "     ${GREEN}./generate_route_maps_nsga3.sh all${NC}"
echo ""
echo "  4. Comparar com versão AEMMT:"
echo "     ${GREEN}python3 scripts/compare_aemmt_nsga3.py${NC}"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}   VALIDAÇÃO NSGA-III CONCLUÍDA COM SUCESSO! 🎉${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════════${NC}"
echo ""
