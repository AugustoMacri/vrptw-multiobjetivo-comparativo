#!/usr/bin/env python3
"""
Validador de solucoes NSGA-III para VRPTW.

Le arquivos evo_*.txt no formato:
  Veiculo 0: 12 clientes, demanda: 150/200, rota: [42, 41, 40, ...]

Valida:
- Cobertura completa (todos clientes visitados)
- Sem duplicatas
- Capacidade dos veiculos
- Janelas de tempo
"""

import sys
import re
import math


class Customer:
    def __init__(self, cid, x, y, demand, ready_time, due_time, service_time):
        self.id = cid
        self.x = x
        self.y = y
        self.demand = demand
        self.ready_time = ready_time
        self.due_time = due_time
        self.service_time = service_time


def load_instance(filename):
    """Carrega a instancia Solomon"""
    customers = []
    with open(filename, 'r') as f:
        lines = f.readlines()

    vehicle_line = lines[4].split()
    vehicle_capacity = int(vehicle_line[1])

    customer_start = 9
    for line in lines[customer_start:]:
        parts = line.split()
        if len(parts) < 7:
            continue
        customer = Customer(
            cid=int(parts[0]),
            x=float(parts[1]),
            y=float(parts[2]),
            demand=int(parts[3]),
            ready_time=int(parts[4]),
            due_time=int(parts[5]),
            service_time=int(parts[6])
        )
        customers.append(customer)

    return {c.id: c for c in customers}, vehicle_capacity


def distance(c1, c2):
    dx = c1.x - c2.x
    dy = c1.y - c2.y
    return math.sqrt(dx * dx + dy * dy)


def parse_routes(solution_file):
    """Parse rotas do formato: Veiculo 0: 12 clientes, demanda: 150/200, rota: [42, 41, ...]"""
    routes = []
    pattern = re.compile(r'Veiculo\s+\d+:.*rota:\s*\[([^\]]*)\]')

    with open(solution_file, 'r', encoding='utf-8') as f:
        # .strip() remove \r\n (CRLF do Windows) para funcionar tambem no Linux
        lines = [l.strip() for l in f.readlines()]

    in_solution_section = False
    for line in lines:
        if '=== MELHOR SOLUCAO' in line:
            in_solution_section = True
            continue

        if not in_solution_section:
            continue

        match = pattern.search(line)
        if match:
            clients_str = match.group(1).strip()
            if clients_str:
                clients = [int(c.strip()) for c in clients_str.split(',')]
                routes.append(clients)

    return routes


def validate_route_time_windows(route, customers, depot):
    """Valida janelas de tempo de uma rota, retorna lista de violacoes"""
    violations = []
    current_time = 0.0
    current = depot

    for client_id in route:
        client = customers.get(client_id)
        if not client:
            violations.append(f"Cliente {client_id} nao encontrado na instancia")
            continue

        travel = distance(current, client)
        arrival = current_time + travel

        if arrival > client.due_time:
            violations.append(
                f"Cliente {client_id}: chegada {arrival:.2f} > deadline {client.due_time} "
                f"(atraso: {arrival - client.due_time:.2f})"
            )

        start_service = max(arrival, client.ready_time)
        current_time = start_service + client.service_time
        current = client

    # Retorno ao deposito
    travel_back = distance(current, depot)
    arrival_depot = current_time + travel_back
    if arrival_depot > depot.due_time:
        violations.append(
            f"Retorno deposito: chegada {arrival_depot:.2f} > deadline {depot.due_time} "
            f"(atraso: {arrival_depot - depot.due_time:.2f})"
        )

    return violations


def validate_solution(instance_file, solution_file):
    customers, vehicle_capacity = load_instance(instance_file)
    depot = customers[0]
    num_clients = len([c for c in customers.values() if c.id != 0])

    routes = parse_routes(solution_file)

    if not routes:
        return {
            'valid': False,
            'error': 'Nenhuma rota encontrada no arquivo. Formato esperado: "Veiculo N: ... rota: [...]"',
            'num_clients': num_clients,
            'routes_found': 0,
            'visited': 0,
            'missing': list(range(1, num_clients + 1)),
            'duplicates': [],
            'capacity_violations': [],
            'time_violations': [],
        }

    # Cobertura e duplicatas
    all_clients = []
    for route in routes:
        all_clients.extend(route)

    visited = set(all_clients)
    expected = set(range(1, num_clients + 1))
    missing = sorted(expected - visited)

    seen = set()
    duplicates = []
    for c in all_clients:
        if c in seen:
            duplicates.append(c)
        seen.add(c)

    # Capacidade
    capacity_violations = []
    for i, route in enumerate(routes):
        total_demand = sum(customers[c].demand for c in route if c in customers)
        if total_demand > vehicle_capacity:
            capacity_violations.append({
                'vehicle': i,
                'demand': total_demand,
                'capacity': vehicle_capacity,
                'excess': total_demand - vehicle_capacity
            })

    # Janelas de tempo
    time_violations = []
    for i, route in enumerate(routes):
        route_violations = validate_route_time_windows(route, customers, depot)
        for v in route_violations:
            time_violations.append(f"Veiculo {i}: {v}")

    is_valid = (len(missing) == 0 and
                len(duplicates) == 0 and
                len(capacity_violations) == 0 and
                len(time_violations) == 0)

    return {
        'valid': is_valid,
        'error': None,
        'num_clients': num_clients,
        'routes_found': len(routes),
        'visited': len(visited),
        'missing': missing,
        'duplicates': duplicates,
        'capacity_violations': capacity_violations,
        'time_violations': time_violations,
    }


def print_result(result, verbose=True):
    print(f"{'=' * 70}")
    print(f"VALIDACAO NSGA-III")
    print(f"{'=' * 70}")

    if result.get('error'):
        print(f"ERRO: {result['error']}")
        print(f"{'=' * 70}")
        return

    print(f"Clientes esperados: {result['num_clients']}")
    print(f"Clientes visitados: {result['visited']}")
    print(f"Numero de rotas:    {result['routes_found']}")
    print()

    # Cobertura
    if result['missing']:
        print(f"[FALHA] Clientes faltando ({len(result['missing'])}): {result['missing']}")
    else:
        print(f"[OK] Cobertura completa - todos os {result['num_clients']} clientes visitados")

    # Duplicatas
    if result['duplicates']:
        print(f"[FALHA] Clientes duplicados ({len(result['duplicates'])}): {result['duplicates']}")
    else:
        print(f"[OK] Sem duplicatas")

    # Capacidade
    if result['capacity_violations']:
        print(f"[FALHA] Violacoes de capacidade ({len(result['capacity_violations'])}):")
        for v in result['capacity_violations']:
            print(f"   Veiculo {v['vehicle']}: demanda {v['demand']} > capacidade {v['capacity']} (excesso: {v['excess']})")
    else:
        print(f"[OK] Capacidade respeitada em todas as rotas")

    # Janelas de tempo
    if result['time_violations']:
        print(f"[FALHA] Violacoes de janelas de tempo ({len(result['time_violations'])}):")
        if verbose:
            for v in result['time_violations']:
                print(f"   {v}")
        else:
            # Mostra so as 5 primeiras
            for v in result['time_violations'][:5]:
                print(f"   {v}")
            if len(result['time_violations']) > 5:
                print(f"   ... e mais {len(result['time_violations']) - 5} violacoes")
    else:
        print(f"[OK] Janelas de tempo respeitadas")

    print()
    if result['valid']:
        print("[OK] SOLUCAO VALIDA")
    else:
        print("[FALHA] SOLUCAO INVALIDA")


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Uso: python3 validate_nsga3_solution.py <instance.txt> <evo_file.txt> [--verbose]")
        sys.exit(1)

    instance_file = sys.argv[1]
    solution_file = sys.argv[2]
    verbose = '--verbose' in sys.argv

    result = validate_solution(instance_file, solution_file)
    print_result(result, verbose=verbose)

    sys.exit(0 if result['valid'] else 1)
