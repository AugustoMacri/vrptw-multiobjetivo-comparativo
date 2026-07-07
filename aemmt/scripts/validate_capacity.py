#!/usr/bin/env python3
"""
Script para validar as capacidades dos veículos nas rotas geradas.
Verifica se algum veículo está ultrapassando a capacidade máxima permitida.
"""

import re
import sys
from pathlib import Path


def load_instance_data(instance_file):
    """Carrega os dados de demanda dos clientes da instância."""
    demands = {}
    with open(instance_file, 'r') as f:
        lines = f.readlines()

    # Encontra a seção de clientes
    in_customer_section = False
    for line in lines:
        if 'CUSTOMER' in line:
            in_customer_section = True
            continue
        if 'CUST NO.' in line:  # Pula o cabeçalho
            continue
        if in_customer_section and line.strip():
            parts = line.split()
            if len(parts) >= 7:
                try:
                    client_id = int(parts[0])
                    demand = int(parts[3])
                    demands[client_id] = demand
                except ValueError:
                    # Pula linhas que não são dados de clientes
                    continue

    return demands


def validate_route_file(route_file, demands, capacity=200):
    """Valida as capacidades dos veículos em um arquivo de resultados."""
    violations = []

    with open(route_file, 'r') as f:
        content = f.read()

    # Padrão para extrair informações dos veículos
    vehicle_pattern = r'Veículo (\d+): .*?\n\s+Clientes: (\d+) \| Demanda: (\d+)/(\d+)'

    matches = re.findall(vehicle_pattern, content)

    for match in matches:
        vehicle_id, num_clients, demand, max_capacity = match
        demand = int(demand)
        max_capacity = int(max_capacity)

        if demand > max_capacity:
            violations.append({
                'vehicle': int(vehicle_id),
                'demand': demand,
                'capacity': max_capacity,
                'overflow': demand - max_capacity
            })

    return violations


def main():
    # Diretório com os resultados de validação
    results_dir = Path("results_validation_C1")

    # Capacidade padrão das instâncias Solomon C1
    capacity = 200

    # Instâncias C1
    instances = [f"C10{i}" for i in range(1, 10)]

    total_violations = 0
    total_routes = 0

    print("=" * 80)
    print("VALIDAÇÃO DE CAPACIDADE DOS VEÍCULOS")
    print("=" * 80)
    print()

    for instance in instances:
        instance_dir = results_dir / instance
        if not instance_dir.exists():
            continue

        print(f"\n{instance}:")
        print("-" * 40)

        # Carrega as demandas dos clientes
        instance_file = Path(f"src/instances/solomon/{instance}.txt")
        if not instance_file.exists():
            print(f"  ⚠ Arquivo da instância não encontrado")
            continue

        demands = load_instance_data(instance_file)

        # Verifica todas as execuções
        instance_violations = 0
        instance_routes = 0

        for exec_num in range(1, 11):
            result_file = instance_dir / \
                f"evo_{instance.lower()}_exec{exec_num:02d}.txt"

            if not result_file.exists():
                continue

            violations = validate_route_file(result_file, demands, capacity)
            instance_routes += 1

            if violations:
                instance_violations += len(violations)
                print(
                    f"  Exec {exec_num:02d}: {len(violations)} violações encontradas")
                for v in violations:
                    print(f"    - Veículo {v['vehicle']}: {v['demand']}/{v['capacity']} "
                          f"(+{v['overflow']} acima da capacidade)")
            else:
                print(f"  Exec {exec_num:02d}: ✓ Todas as capacidades OK")

        if instance_violations == 0:
            print(
                f"  ✓ Instância OK: {instance_routes} execuções sem violações")
        else:
            print(
                f"  ✗ Total: {instance_violations} violações em {instance_routes} execuções")

        total_violations += instance_violations
        total_routes += instance_routes

    print()
    print("=" * 80)
    print("RESUMO FINAL")
    print("=" * 80)
    if total_violations == 0:
        print(
            f"✓ SUCESSO: Todas as {total_routes} execuções respeitam a capacidade máxima!")
    else:
        print(
            f"✗ FALHA: {total_violations} violações encontradas em {total_routes} execuções")
    print()


if __name__ == "__main__":
    main()
