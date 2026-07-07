#!/usr/bin/env python3
"""
Script para validar TODAS as soluções geradas pelo NSGA-III
Verifica capacidade, time windows e cobertura de clientes em todas as 260 execuções

Uso: python3 scripts/validate_all_nsga3.py
"""

import os
import sys
import subprocess
from pathlib import Path

# Cores ANSI
GREEN = '\033[0;32m'
RED = '\033[0;31m'
YELLOW = '\033[1;33m'
BLUE = '\033[0;34m'
NC = '\033[0m'  # No Color


def print_header(text):
    print()
    print(f"{BLUE}{'='*70}{NC}")
    print(f"{BLUE}   {text}{NC}")
    print(f"{BLUE}{'='*70}{NC}")
    print()


def validate_solution(instance_file, solution_file):
    """
    Valida uma solução NSGA-III usando o validador específico

    Returns:
        tuple: (success, capacity_violations, time_violations, missing_clients)
    """
    try:
        # Usar validador específico para formato NSGA-III
        result = subprocess.run(
            ['python3', 'scripts/validate_nsga3_solution.py',
                instance_file, solution_file],
            capture_output=True,
            text=True,
            timeout=30
        )

        output = result.stdout + result.stderr

        # Verificar se solução é válida
        is_valid = "✅ SOLUÇÃO VÁLIDA" in output

        # Extrair informações do output
        missing_clients = 0
        duplicates = 0
        capacity_violations = 0
        time_violations = 0

        for line in output.split('\n'):
            # Ex: "❌ Clientes faltando (13): [1, 2, ...]"
            if 'Clientes faltando' in line:
                import re
                match = re.search(r'faltando\s*\((\d+)\)', line)
                if match:
                    missing_clients = int(match.group(1))

            # Ex: "❌ Clientes duplicados (3): [5, 10, ...]"
            if 'Clientes duplicados' in line:
                import re
                match = re.search(r'duplicados\s*\((\d+)\)', line)
                if match:
                    duplicates = int(match.group(1))

            # Ex: "❌ VIOLAÇÕES DE CAPACIDADE (2):"
            if 'VIOLAÇÕES DE CAPACIDADE' in line:
                import re
                match = re.search(r'CAPACIDADE\s*\((\d+)\)', line)
                if match:
                    capacity_violations = int(match.group(1))

            # Ex: "❌ VIOLAÇÕES DE JANELAS DE TEMPO (5):"
            if 'VIOLAÇÕES DE JANELAS DE TEMPO' in line:
                import re
                match = re.search(r'TEMPO\s*\((\d+)\)', line)
                if match:
                    time_violations = int(match.group(1))

        return (is_valid, capacity_violations, time_violations, missing_clients + duplicates)

    except subprocess.TimeoutExpired:
        print(f"{RED}  ⏱️  Timeout ao validar{NC}")
        return (False, -1, -1, -1)
    except Exception as e:
        print(f"{RED}  ⚠️  Erro: {e}{NC}")
        return (False, -1, -1, -1)


def main():
    print_header("VALIDAÇÃO COMPLETA - NSGA-III")

    # Diretórios base
    base_dir = Path('.')
    instances_dir = base_dir / 'app' / 'src' / \
        'main' / 'java' / 'instances' / 'solomon'

    # Verificar se os arquivos foram gerados em resultsNSGA3/
    results_dir = base_dir / 'resultsNSGA3'
    if not results_dir.exists():
        print(f"{RED}✗ Diretório resultsNSGA3/ não encontrado!{NC}")
        print(f"{YELLOW}  Execute primeiro: bash run_all_validations_nsga3.sh{NC}")
        print()
        return 1

    print(f"{GREEN}📁 Diretório de resultados: {results_dir}{NC}")
    print()

    # Classes de instâncias
    classes = [
        {
            'name': 'C1',
            'instances': [f'C1{i:02d}' for i in range(1, 10)]  # C101-C109
        },
        {
            'name': 'R1',
            'instances': [f'R1{i:02d}' for i in range(1, 10)]  # R101-R109
        },
        {
            'name': 'RC1',
            'instances': [f'RC1{i:02d}' for i in range(1, 9)]  # RC101-RC108
        }
    ]

    # Estatísticas globais
    total_executions = 0
    total_valid = 0
    total_invalid = 0
    total_missing = 0

    class_stats = {}

    # Para cada classe
    for class_info in classes:
        class_name = class_info['name']
        instances = class_info['instances']

        print_header(f"VALIDANDO CLASSE {class_name}")

        class_valid = 0
        class_invalid = 0
        class_missing = 0
        class_capacity_violations = 0
        class_time_violations = 0

        # Para cada instância
        for instance_name in instances:
            instance_file = instances_dir / f'{instance_name}.txt'

            if not instance_file.exists():
                print(f"{RED}✗ Instância não encontrada: {instance_file}{NC}")
                continue

            print(f"\n{YELLOW}📋 Validando {instance_name}...{NC}")

            instance_valid = 0
            instance_invalid = 0

            # Para cada execução (01-10)
            for exec_num in range(1, 11):
                exec_suffix = f"{exec_num:02d}"
                # Todos os arquivos estão diretamente em resultsNSGA3/
                solution_file = results_dir / \
                    f"evo_{instance_name.lower()}_exec{exec_suffix}.txt"

                if not solution_file.exists():
                    print(f"{RED}  ✗ exec{exec_suffix}: Arquivo não encontrado{NC}")
                    class_missing += 1
                    continue

                # Validar solução
                is_valid, cap_viol, time_viol, missing = validate_solution(
                    str(instance_file),
                    str(solution_file)
                )

                total_executions += 1

                if is_valid:
                    print(f"{GREEN}  ✓ exec{exec_suffix}: VÁLIDA{NC}")
                    instance_valid += 1
                    class_valid += 1
                    total_valid += 1
                else:
                    print(f"{RED}  ✗ exec{exec_suffix}: INVÁLIDA "
                          f"(cap:{cap_viol}, time:{time_viol}, missing:{missing}){NC}")
                    instance_invalid += 1
                    class_invalid += 1
                    total_invalid += 1

                    if cap_viol > 0:
                        class_capacity_violations += cap_viol
                    if time_viol > 0:
                        class_time_violations += time_viol

            # Sumário da instância
            if instance_valid == 10:
                print(f"{GREEN}  ✓ {instance_name}: 10/10 válidas{NC}")
            else:
                print(f"{RED}  ✗ {instance_name}: {instance_valid}/10 válidas{NC}")

        # Sumário da classe
        class_stats[class_name] = {
            'valid': class_valid,
            'invalid': class_invalid,
            'missing': class_missing,
            'capacity_violations': class_capacity_violations,
            'time_violations': class_time_violations
        }

        expected = len(instances) * 10
        print()
        print(f"{BLUE}{'─'*70}{NC}")
        print(f"{BLUE}SUMÁRIO {class_name}:{NC}")
        print(f"  Válidas:   {class_valid}/{expected}")
        print(f"  Inválidas: {class_invalid}/{expected}")
        print(f"  Faltando:  {class_missing}/{expected}")
        if class_capacity_violations > 0:
            print(f"  Violações de capacidade: {class_capacity_violations}")
        if class_time_violations > 0:
            print(f"  Violações de time window: {class_time_violations}")
        print(f"{BLUE}{'─'*70}{NC}")

    # Sumário final
    print_header("SUMÁRIO FINAL - VALIDAÇÃO NSGA-III")

    print(f"{BLUE}ESTATÍSTICAS GLOBAIS:{NC}")
    print()
    print(f"  Total de execuções: {total_executions}")
    print(f"  {GREEN}Soluções válidas:   {total_valid}{NC}")
    print(f"  {RED}Soluções inválidas: {total_invalid}{NC}")
    print(f"  {YELLOW}Arquivos faltando:  {total_missing}{NC}")
    print()

    if total_executions > 0:
        success_rate = (total_valid / total_executions) * 100
        print(f"  {BLUE}Taxa de sucesso: {success_rate:.1f}%{NC}")

    print()
    print(f"{BLUE}DETALHAMENTO POR CLASSE:{NC}")
    print()

    for class_name in ['C1', 'R1', 'RC1']:
        if class_name in class_stats:
            stats = class_stats[class_name]
            total_class = stats['valid'] + stats['invalid'] + stats['missing']

            if total_class > 0:
                rate = (stats['valid'] / total_class) * \
                    100 if total_class > 0 else 0

                print(f"  {class_name}:")
                print(
                    f"    Válidas:   {stats['valid']}/{total_class} ({rate:.1f}%)")
                print(f"    Inválidas: {stats['invalid']}")
                print(f"    Faltando:  {stats['missing']}")

                if stats['capacity_violations'] > 0 or stats['time_violations'] > 0:
                    print(
                        f"    Violações capacidade: {stats['capacity_violations']}")
                    print(
                        f"    Violações time window: {stats['time_violations']}")
                print()

    # Resultado final
    print(f"{BLUE}{'='*70}{NC}")

    if total_invalid == 0 and total_missing == 0:
        print(f"{GREEN}✅ TODAS AS {total_valid} SOLUÇÕES SÃO VÁLIDAS!{NC}")
        print(f"{GREEN}   O algoritmo NSGA-III está funcionando perfeitamente! 🎉{NC}")
        return 0
    else:
        print(f"{YELLOW}⚠️  VALIDAÇÃO INCOMPLETA{NC}")
        if total_invalid > 0:
            print(f"{RED}   {total_invalid} soluções inválidas encontradas{NC}")
        if total_missing > 0:
            print(f"{YELLOW}   {total_missing} arquivos faltando{NC}")
        print()
        print(
            f"{YELLOW}   Revise os resultados e execute novamente as instâncias com problemas.{NC}")
        return 1

    print(f"{BLUE}{'='*70}{NC}")
    print()


if __name__ == '__main__':
    sys.exit(main())
