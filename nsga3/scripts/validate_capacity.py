#!/usr/bin/env python3
"""
Valida restrições de capacidade nos resultados de VRP
Suporta tanto a versão Multi-Objetivo quanto NSGA-III
"""

import os
import re
import argparse
from pathlib import Path

# Diretórios de resultados
MULTI_OBJ_RESULTS_DIR = "results_validation_C1"
NSGA3_RESULTS_DIR = "results_validation_NSGA3_C1"


def parse_route_file(filepath, vehicle_capacity=200):
    """Analisa um arquivo de resultados e verifica violações de capacidade"""

    violations = []
    total_vehicles = 0

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

            # Padrão para encontrar informações de demanda total
            # Formato: "Veículo X: Y clientes, demanda total: Z/200"
            pattern = r'Veículo (\d+): (\d+) clientes, demanda total: (\d+)/(\d+)'
            matches = re.findall(pattern, content)

            for match in matches:
                vehicle_id = int(match[0])
                num_clients = int(match[1])
                total_demand = int(match[2])
                capacity = int(match[3])
                total_vehicles += 1

                if total_demand > capacity:
                    violations.append({
                        'vehicle': vehicle_id,
                        'demand': total_demand,
                        'capacity': capacity,
                        'excess': total_demand - capacity
                    })

    except FileNotFoundError:
        print(f"⚠ Arquivo não encontrado: {filepath}")
        return None, None
    except Exception as e:
        print(f"⚠ Erro ao processar {filepath}: {e}")
        return None, None

    return violations, total_vehicles


def validate_instance(instance_name, results_dir, vehicle_capacity=200):
    """Valida todas as execuções de uma instância"""

    instance_dir = Path(results_dir) / instance_name

    if not instance_dir.exists():
        print(f"⚠ Diretório não encontrado: {instance_dir}")
        return None

    print(f"\n{'='*60}")
    print(f"Instância: {instance_name}")
    print(f"{'='*60}")

    total_executions = 0
    executions_with_violations = 0
    total_violations = 0
    max_violation = 0

    # Validar cada execução
    for exec_num in range(1, 11):
        evo_file = instance_dir / \
            f"evo_{instance_name.lower()}_exec{exec_num:02d}.txt"

        if not evo_file.exists():
            continue

        total_executions += 1
        violations, total_vehicles = parse_route_file(
            evo_file, vehicle_capacity)

        if violations is None:
            continue

        if violations:
            executions_with_violations += 1
            total_violations += len(violations)

            print(
                f"\n  ✗ Execução {exec_num:02d}: {len(violations)} violação(ões)")

            for v in violations:
                excess = v['excess']
                if excess > max_violation:
                    max_violation = excess

                print(f"    - Veículo {v['vehicle']}: demanda {v['demand']}/{v['capacity']} "
                      f"(excesso: {excess})")
        else:
            print(f"  ✓ Execução {exec_num:02d}: Sem violações")

    # Resumo da instância
    print(f"\n{'─'*60}")
    print(f"Resumo:")
    print(f"  Execuções analisadas: {total_executions}/10")
    print(f"  Execuções com violações: {executions_with_violations}")
    print(f"  Total de violações: {total_violations}")
    if max_violation > 0:
        print(f"  Maior excesso de capacidade: {max_violation}")

    if executions_with_violations == 0:
        print(f"  ✓ 100% de conformidade!")
    else:
        conformity = (
            (total_executions - executions_with_violations) / total_executions) * 100
        print(f"  Taxa de conformidade: {conformity:.1f}%")

    return {
        'instance': instance_name,
        'total_executions': total_executions,
        'violations_count': executions_with_violations,
        'total_violations': total_violations,
        'max_violation': max_violation
    }


def main():
    parser = argparse.ArgumentParser(
        description='Valida restrições de capacidade em resultados VRP')
    parser.add_argument('--nsga3', action='store_true',
                        help='Validar resultados NSGA-III')
    parser.add_argument('--capacity', type=int, default=200,
                        help='Capacidade do veículo (padrão: 200)')
    parser.add_argument('--instance', type=str,
                        help='Validar apenas uma instância específica (ex: C101)')

    args = parser.parse_args()

    # Selecionar diretório baseado no argumento
    if args.nsga3:
        results_dir = NSGA3_RESULTS_DIR
        algorithm_name = "NSGA-III"
    else:
        results_dir = MULTI_OBJ_RESULTS_DIR
        algorithm_name = "Multi-Objetivo"

    print("=" * 60)
    print(f"VALIDAÇÃO DE CAPACIDADE - {algorithm_name}")
    print("=" * 60)
    print(f"Diretório: {results_dir}")
    print(f"Capacidade do veículo: {args.capacity}")

    # Verificar se o diretório existe
    if not Path(results_dir).exists():
        print(f"\n✗ Erro: Diretório não encontrado: {results_dir}")
        print("\nCertifique-se de executar o script de validação antes:")
        if args.nsga3:
            print("  ./run_validation_nsga3_c1.sh")
        else:
            print("  ./run_validation_c1.sh")
        return

    # Determinar quais instâncias validar
    if args.instance:
        instances = [args.instance]
    else:
        instances = [f"C10{i}" for i in range(1, 10)]

    # Validar cada instância
    results = []
    for instance in instances:
        result = validate_instance(instance, results_dir, args.capacity)
        if result:
            results.append(result)

    # Resumo geral
    if results:
        print(f"\n{'='*60}")
        print("RESUMO GERAL")
        print(f"{'='*60}")

        total_executions = sum(r['total_executions'] for r in results)
        total_with_violations = sum(r['violations_count'] for r in results)
        total_violations = sum(r['total_violations'] for r in results)
        max_violation = max(r['max_violation'] for r in results)

        print(f"Instâncias analisadas: {len(results)}")
        print(f"Total de execuções: {total_executions}")
        print(f"Execuções com violações: {total_with_violations}")
        print(f"Total de violações encontradas: {total_violations}")

        if total_with_violations == 0:
            print("\n✓✓✓ SUCESSO: 100% de conformidade em todas as instâncias! ✓✓✓")
        else:
            conformity = (
                (total_executions - total_with_violations) / total_executions) * 100
            print(f"\nTaxa de conformidade global: {conformity:.1f}%")
            print(f"Maior excesso encontrado: {max_violation}")
            print("\n⚠ Algumas violações de capacidade foram encontradas.")
            print("Verifique os detalhes acima para mais informações.")


if __name__ == "__main__":
    main()
