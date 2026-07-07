#!/usr/bin/env python3
"""
Validador DETALHADO com saída passo a passo
Mostra tempo de chegada em cada cliente e validação das janelas
"""

import sys
import math
import re


class Customer:
    def __init__(self, id, x, y, demand, ready_time, due_time, service_time):
        self.id = id
        self.x = x
        self.y = y
        self.demand = demand
        self.ready_time = ready_time
        self.due_time = due_time
        self.service_time = service_time


def load_instance(filename):
    """Carrega a instância Solomon"""
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
            id=int(parts[0]),
            x=float(parts[1]),
            y=float(parts[2]),
            demand=int(parts[3]),
            ready_time=int(parts[4]),
            due_time=int(parts[5]),
            service_time=int(parts[6])
        )
        customers.append(customer)

    return customers, vehicle_capacity


def get_customer(customers, id):
    """Retorna o cliente com o ID especificado"""
    for c in customers:
        if c.id == id:
            return c
    return None


def distance(c1, c2):
    """Calcula distância euclidiana entre dois clientes"""
    dx = c1.x - c2.x
    dy = c1.y - c2.y
    return math.sqrt(dx * dx + dy * dy)


def load_solution(solution_file):
    """Carrega a solução do arquivo"""
    routes = []
    with open(solution_file, 'r') as f:
        content = f.read()

    if 'ROTAS FINAIS' in content:
        final_section = content.split('ROTAS FINAIS')[1]
    else:
        final_section = content

    lines = final_section.split('\n')

    for line in lines:
        line = line.strip()
        if line.startswith('Veículo'):
            matches = re.findall(r'Cliente\((\d+)\)', line)
            if matches:
                route = [int(n) for n in matches]
                routes.append(route)

    return routes


def validate_route_detailed(route_idx, route, customers):
    """Valida uma rota mostrando todos os detalhes"""
    depot = get_customer(customers, 0)

    print(f"\n{'='*90}")
    print(f"ROTA {route_idx + 1}: {len(route)} clientes")
    print(f"{'='*90}")

    current_time = depot.ready_time
    current_customer = depot
    total_demand = 0
    total_distance = 0
    violations = []

    print(f"\n{'Passo':<6} {'De → Para':<25} {'Dist':<8} {'Viagem':<8} {'Chegada':<10} {'Janela':<20} {'Espera':<8} {'Serviço':<8} {'Saída':<10} {'Status':<15}")
    print("-" * 140)

    # Início no depot
    print(
        f"{'START':<6} {'Depot(0)':<25} {'-':<8} {'-':<8} {current_time:<10.2f} {'[0, 230]':<20} {'-':<8} {'-':<8} {current_time:<10.2f} {'🏁 Início':<15}")

    for i, customer_id in enumerate(route, 1):
        customer = get_customer(customers, customer_id)
        if not customer:
            print(f"ERRO: Cliente {customer_id} não encontrado!")
            continue

        # Calcula distância e tempo de viagem
        dist = distance(current_customer, customer)
        travel_time = dist
        arrival_time = current_time + travel_time

        # Verifica janela de tempo
        start_service = max(arrival_time, customer.ready_time)
        wait_time = start_service - arrival_time

        # Status da validação
        status = ""
        if arrival_time < customer.ready_time:
            status = f"⏰ Espera {wait_time:.2f}"
        elif arrival_time <= customer.due_time:
            status = "✅ OK"
        else:
            status = f"❌ VIOLAÇÃO!"
            violations.append({
                'route': route_idx + 1,
                'customer': customer_id,
                'arrival': arrival_time,
                'deadline': customer.due_time,
                'delay': arrival_time - customer.due_time
            })

        # Tempo de saída
        departure_time = start_service + customer.service_time

        # Acumula demanda e distância
        total_demand += customer.demand
        total_distance += dist

        # Informações para impressão
        from_to = f"{current_customer.id} → {customer_id}"
        window = f"[{customer.ready_time}, {customer.due_time}]"
        wait_str = f"{wait_time:.2f}" if wait_time > 0 else "-"

        print(f"{i:<6} {from_to:<25} {dist:<8.2f} {travel_time:<8.2f} {arrival_time:<10.2f} {window:<20} {wait_str:<8} {customer.service_time:<8} {departure_time:<10.2f} {status:<15}")

        # Atualiza para próximo cliente
        current_time = departure_time
        current_customer = customer

    # Retorno ao depot
    dist = distance(current_customer, depot)
    travel_time = dist
    arrival_time = current_time + travel_time
    total_distance += dist

    status_return = "✅ OK" if arrival_time <= depot.due_time else f"⚠️ Tarde ({arrival_time:.2f} > {depot.due_time})"

    print("-" * 140)
    print(
        f"{'END':<6} {f'{current_customer.id} → Depot(0)':<25} {dist:<8.2f} {travel_time:<8.2f} {arrival_time:<10.2f} {'[0, 230]':<20} {'-':<8} {'-':<8} {arrival_time:<10.2f} {status_return:<15}")

    print(f"\n{'─'*90}")
    print(f"📊 RESUMO DA ROTA {route_idx + 1}:")
    print(f"   🚚 Demanda total: {total_demand}")
    print(f"   📏 Distância total: {total_distance:.2f}")
    print(f"   ⏱️  Tempo total: {arrival_time:.2f}")
    print(f"   {'✅ Sem violações de janelas de tempo' if not violations else f'❌ {len(violations)} violação(ões) encontrada(s)'}")

    return violations, total_distance, total_demand


def main():
    if len(sys.argv) != 3:
        print("Uso: python3 validate_detailed.py <instancia.txt> <solucao.txt>")
        sys.exit(1)

    instance_file = sys.argv[1]
    solution_file = sys.argv[2]

    print("="*90)
    print("VALIDAÇÃO DETALHADA DE SOLUÇÃO VRPTW")
    print("="*90)
    print(f"Instância: {instance_file}")
    print(f"Solução: {solution_file}")

    # Carrega dados
    customers, vehicle_capacity = load_instance(instance_file)
    routes = load_solution(solution_file)

    print(f"\n📋 Capacidade do veículo: {vehicle_capacity}")
    print(f"🚚 Número de rotas: {len(routes)}")

    # Valida cada rota
    all_violations = []
    total_distance = 0
    total_demand = 0

    for route_idx, route in enumerate(routes):
        violations, dist, demand = validate_route_detailed(
            route_idx, route, customers)
        all_violations.extend(violations)
        total_distance += dist
        total_demand += demand

        if demand > vehicle_capacity:
            print(
                f"   ⚠️  ATENÇÃO: Capacidade excedida! ({demand} > {vehicle_capacity})")

    # Resumo final
    print(f"\n\n{'='*90}")
    print("RESUMO GERAL")
    print(f"{'='*90}")
    print(f"📏 Distância total: {total_distance:.2f}")
    print(f"🚚 Veículos utilizados: {len(routes)}")
    print(f"📦 Demanda total: {total_demand}")

    if all_violations:
        print(
            f"\n❌ SOLUÇÃO INVÁLIDA - {len(all_violations)} VIOLAÇÃO(ÕES) DE JANELAS DE TEMPO:")
        print(
            f"\n{'Rota':<8} {'Cliente':<10} {'Chegada':<12} {'Deadline':<12} {'Atraso':<12}")
        print("-" * 54)
        for v in all_violations:
            print(
                f"{v['route']:<8} {v['customer']:<10} {v['arrival']:<12.2f} {v['deadline']:<12} {v['delay']:<12.2f}")
    else:
        print(f"\n✅ SOLUÇÃO VÁLIDA - Todas as janelas de tempo foram respeitadas!")

    print(f"\n{'='*90}\n")


if __name__ == "__main__":
    main()
