#!/bin/bash

# Helper script para facilitar uso do projeto NSGA-III
# Uso: ./nsga3-helper.sh [comando]

set -e

PROJECT_DIR="/home/augusto/Desktop/VRP_NSGA_TCC"

show_help() {
    cat << EOF
========================================
VRP NSGA-III - Helper Script
========================================

Uso: $0 [comando] [opções]

COMANDOS:

  build                 Compila o projeto
  test <instancia>      Testa uma instância (padrão: 1 = C101)
  validate-all          Executa todas as instâncias C1 (10x cada)
  validate-capacity     Valida capacidades dos resultados NSGA-III
  generate-maps <inst>  Gera mapas de rotas (inst = C101 ou all_c1)
  compare               Compara Multi-Objetivo vs NSGA-III
  clean                 Limpa arquivos temporários
  help                  Mostra esta mensagem

EXEMPLOS:

  $0 build
      Compila o projeto

  $0 test 1
      Testa C101 rapidamente (60 segundos)

  $0 validate-all
      Executa validação completa (90 execuções, ~8-15 horas)

  $0 validate-capacity
      Verifica violações de capacidade nos resultados

  $0 generate-maps C101
      Gera mapas apenas para C101

  $0 generate-maps all_c1
      Gera mapas para todas instâncias C1

  $0 compare
      Compara resultados Multi-Objetivo vs NSGA-III

  $0 clean
      Remove arquivos temporários e builds

========================================
EOF
}

build_project() {
    echo "🔨 Compilando o projeto..."
    ./gradlew build -x test --console=plain -q
    echo "✓ Compilação concluída!"
}

test_instance() {
    local instance=${1:-1}
    echo "🧪 Testando instância $instance (60 segundos)..."
    ./run_single_nsga3.sh "$instance" 60
}

validate_all() {
    echo "🔄 Iniciando validação completa..."
    echo "⚠️  ATENÇÃO: Isso pode levar de 8 a 15 horas!"
    read -p "Deseja continuar? (s/N): " confirm
    
    if [[ "$confirm" =~ ^[Ss]$ ]]; then
        ./run_validation_nsga3_c1.sh
    else
        echo "Operação cancelada."
    fi
}

validate_capacity() {
    echo "🔍 Validando capacidades dos resultados NSGA-III..."
    
    if [ ! -d "results_validation_NSGA3_C1" ]; then
        echo "✗ Erro: Diretório de resultados não encontrado!"
        echo "Execute primeiro: $0 validate-all"
        exit 1
    fi
    
    python3 scripts/validate_capacity.py --nsga3
}

generate_maps() {
    local target=${1:-all_c1}
    echo "🗺️  Gerando mapas de rotas para: $target"
    
    if [ ! -d "results_validation_NSGA3_C1" ]; then
        echo "✗ Erro: Diretório de resultados não encontrado!"
        echo "Execute primeiro: $0 validate-all"
        exit 1
    fi
    
    ./generate_route_maps_nsga3.sh "$target"
}

compare_results() {
    echo "📊 Comparando Multi-Objetivo vs NSGA-III..."
    
    if [ ! -d "results_validation_C1" ] || [ ! -d "results_validation_NSGA3_C1" ]; then
        echo "✗ Erro: Faltam diretórios de resultados!"
        echo "Execute as validações primeiro."
        exit 1
    fi
    
    python3 scripts/compare_multi_nsga3.py
}

clean_project() {
    echo "🧹 Limpando arquivos temporários..."
    
    ./gradlew clean
    rm -rf app/resultsNSGA3/*.txt
    
    echo "✓ Limpeza concluída!"
}

# Main
case "${1:-help}" in
    build)
        build_project
        ;;
    test)
        build_project
        test_instance "${2:-1}"
        ;;
    validate-all)
        build_project
        validate_all
        ;;
    validate-capacity)
        validate_capacity
        ;;
    generate-maps)
        generate_maps "${2:-all_c1}"
        ;;
    compare)
        compare_results
        ;;
    clean)
        clean_project
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo "✗ Comando desconhecido: $1"
        echo ""
        show_help
        exit 1
        ;;
esac

exit 0
