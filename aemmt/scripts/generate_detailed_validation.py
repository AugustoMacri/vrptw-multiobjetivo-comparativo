#!/usr/bin/env python3
"""
Gera relatório DETALHADO de validação com visualização passo a passo
"""

import sys
import os
import re
import math
from pathlib import Path


class Customer:
    def __init__(self, id, x, y, demand, ready_time, due_time, service_time):
        self.id = id
        self.x = x
        self.y = y
        self.demand = demand
        self.ready_time = ready_time
        self.due_time = due_time
        self.service_time = service_time


class Instance:
    def __init__(self, filename):
        self.filename = filename
        self.customers = []
        self.vehicle_capacity = 0
        self.num_vehicles = 0
        self.load_instance(filename)

    def load_instance(self, filename):
        """Carrega a instância Solomon"""
        with open(filename, 'r') as f:
            lines = f.readlines()

        vehicle_line = lines[4].split()
        self.num_vehicles = int(vehicle_line[0])
        self.vehicle_capacity = int(vehicle_line[1])

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
            self.customers.append(customer)

    def get_customer(self, id):
        """Retorna o cliente com o ID especificado"""
        for c in self.customers:
            if c.id == id:
                return c
        return None

    def distance(self, c1, c2):
        """Calcula distância euclidiana entre dois clientes"""
        dx = c1.x - c2.x
        dy = c1.y - c2.y
        return math.sqrt(dx * dx + dy * dy)


class DetailedValidator:
    def __init__(self, instance, solution_file, output_file):
        self.instance = instance
        self.solution_file = solution_file
        self.output_file = output_file
        self.routes = []
        self.output_lines = []

    def write_line(self, line=""):
        """Adiciona linha ao output"""
        self.output_lines.append(line)

    def load_solution(self):
        """Carrega a solução do arquivo"""
        with open(self.solution_file, 'r') as f:
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
                    self.routes.append(route)

        return len(self.routes) > 0

    def generate_detailed_report(self):
        """Gera relatório detalhado da solução"""

        # Header
        self.write_line("="*80)
        self.write_line("RELATÓRIO DETALHADO DE VALIDAÇÃO")
        self.write_line("="*80)
        self.write_line()
        self.write_line(
            f"Instância: {os.path.basename(self.instance.filename)}")
        self.write_line(f"Solução: {os.path.basename(self.solution_file)}")
        self.write_line(f"Número de veículos: {len(self.routes)}")
        self.write_line(
            f"Capacidade dos veículos: {self.instance.vehicle_capacity}")
        self.write_line()

        # Carrega solução
        if not self.load_solution():
            self.write_line("❌ ERRO: Nenhuma rota encontrada!")
            return False

        depot = self.instance.get_customer(0)
        total_distance = 0
        all_valid = True

        # Para cada rota
        for route_idx, route in enumerate(self.routes):
            self.write_line("="*80)
            self.write_line(f"VEÍCULO {route_idx + 1}")
            self.write_line("="*80)
            self.write_line()

            # Validação de capacidade
            total_demand = 0
            for cid in route:
                customer = self.instance.get_customer(cid)
                if customer:
                    total_demand += customer.demand

            capacity_ok = total_demand <= self.instance.vehicle_capacity
            self.write_line(
                f"📦 Carga total: {total_demand}/{self.instance.vehicle_capacity}")
            if capacity_ok:
                self.write_line(f"   ✓ Capacidade respeitada")
            else:
                self.write_line(f"   ❌ VIOLAÇÃO DE CAPACIDADE!")
                all_valid = False
            self.write_line()

            # Simulação passo a passo
            self.write_line("⏱️  EXECUÇÃO DA ROTA:")
            self.write_line()

            current_time = depot.ready_time
            current_customer = depot
            route_distance = 0
            time_violations = []

            # Partida do depósito
            self.write_line(f"🏢 Depósito (ID: {depot.id})")
            self.write_line(f"   Posição: ({depot.x:.1f}, {depot.y:.1f})")
            self.write_line(f"   Tempo de partida: {current_time:.2f}")
            self.write_line()

            for step, customer_id in enumerate(route, 1):
                customer = self.instance.get_customer(customer_id)
                if not customer:
                    self.write_line(
                        f"❌ ERRO: Cliente {customer_id} não encontrado!")
                    all_valid = False
                    continue

                # Calcula tempo de viagem
                travel_distance = self.instance.distance(
                    current_customer, customer)
                travel_time = travel_distance
                arrival_time = current_time + travel_time

                self.write_line(
                    f"   ↓ viaja {travel_distance:.2f} unidades (tempo: {travel_time:.2f})")
                self.write_line()
                self.write_line(f"👤 Cliente {customer_id} (Passo {step})")
                self.write_line(
                    f"   Posição: ({customer.x:.1f}, {customer.y:.1f})")
                self.write_line(f"   Demanda: {customer.demand}")
                self.write_line(
                    f"   Janela de tempo: [{customer.ready_time:.0f}, {customer.due_time:.0f}]")
                self.write_line(
                    f"   Tempo de serviço: {customer.service_time:.0f}")
                self.write_line()
                self.write_line(f"   • Chegada: {arrival_time:.2f}")

                # Verifica janela de tempo
                if arrival_time < customer.ready_time:
                    wait_time = customer.ready_time - arrival_time
                    self.write_line(
                        f"   • Espera até: {customer.ready_time:.2f} (aguarda {wait_time:.2f} unidades)")
                    start_service = customer.ready_time
                elif arrival_time > customer.due_time:
                    self.write_line(
                        f"   • ❌ VIOLAÇÃO! Chegada após deadline ({customer.due_time:.0f})")
                    self.write_line(
                        f"   • Atraso: {arrival_time - customer.due_time:.2f} unidades")
                    time_violations.append(customer_id)
                    start_service = arrival_time
                    all_valid = False
                else:
                    self.write_line(f"   • ✓ Dentro da janela!")
                    start_service = arrival_time

                self.write_line(f"   • Inicia serviço: {start_service:.2f}")
                self.write_line(
                    f"   • Termina serviço: {start_service + customer.service_time:.2f}")
                self.write_line()

                # Atualiza estado
                route_distance += travel_distance
                current_time = start_service + customer.service_time
                current_customer = customer

            # Retorno ao depósito
            return_distance = self.instance.distance(current_customer, depot)
            return_time = return_distance
            arrival_depot = current_time + return_time

            self.write_line(
                f"   ↓ viaja {return_distance:.2f} unidades (tempo: {return_time:.2f})")
            self.write_line()
            self.write_line(f"🏢 Retorno ao Depósito")
            self.write_line(f"   • Chegada: {arrival_depot:.2f}")

            if arrival_depot > depot.due_time:
                self.write_line(
                    f"   • ⚠️  AVISO: Chegada após horário do depósito ({depot.due_time:.0f})")
            else:
                self.write_line(f"   • ✓ Retorno dentro do horário")

            route_distance += return_distance
            total_distance += route_distance

            self.write_line()
            self.write_line(f"📏 Distância da rota: {route_distance:.2f}")

            if time_violations:
                self.write_line(
                    f"⚠️  Violações de tempo em {len(time_violations)} cliente(s): {time_violations}")

            self.write_line()

        # Resumo final
        self.write_line("="*80)
        self.write_line("RESUMO GERAL")
        self.write_line("="*80)
        self.write_line()
        self.write_line(f"📏 Distância total: {total_distance:.2f}")
        self.write_line(f"🚚 Veículos utilizados: {len(self.routes)}")
        self.write_line()

        if all_valid:
            self.write_line("✅ SOLUÇÃO VÁLIDA!")
            self.write_line("   Todas as restrições foram respeitadas.")
        else:
            self.write_line("❌ SOLUÇÃO INVÁLIDA!")
            self.write_line("   Foram detectadas violações de restrições.")

        self.write_line()
        self.write_line("="*80)

        return True

    def save_report(self):
        """Salva o relatório em arquivo"""
        output_path = Path(self.output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(self.output_lines))

        return True


def main():
    if len(sys.argv) < 4:
        print("Uso: python3 generate_detailed_validation.py <instance_file> <solution_file> <output_file>")
        print()
        print("Exemplo:")
        print("  python3 generate_detailed_validation.py src/instances/solomon/C101.txt \\")
        print("          results_validation_C1/C101/evo_c101_exec01.txt \\")
        print("          results_validation_C1_detailed/C101/evo_c101_exec01.txt")
        sys.exit(1)

    instance_file = sys.argv[1]
    solution_file = sys.argv[2]
    output_file = sys.argv[3]

    if not os.path.exists(instance_file):
        print(f"❌ Arquivo de instância não encontrado: {instance_file}")
        sys.exit(1)

    if not os.path.exists(solution_file):
        print(f"❌ Arquivo de solução não encontrado: {solution_file}")
        sys.exit(1)

    # Carrega instância
    instance = Instance(instance_file)

    # Gera relatório detalhado
    validator = DetailedValidator(instance, solution_file, output_file)
    validator.generate_detailed_report()
    validator.save_report()

    print(f"✓ Relatório gerado: {output_file}")


if __name__ == '__main__':
    main()
