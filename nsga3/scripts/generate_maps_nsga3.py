#!/usr/bin/env python3
"""
Script para gerar mapas das rotas NSGA-III a partir dos arquivos evo_*.txt
Formato esperado: === ROTA DO VEÍCULO X === seguido por Cliente ID [X: coord, Y: coord]
"""

import os
import re
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
from pathlib import Path
import numpy as np
import argparse


def read_instance_coordinates(instance_file):
    """Lê as coordenadas dos clientes do arquivo da instância."""
    clients = {}

    with open(instance_file, 'r') as f:
        lines = f.readlines()

    # Encontrar a seção de coordenadas
    coord_section = False
    for line in lines:
        if 'CUST NO.' in line or 'CUSTOMER' in line:
            coord_section = True
            continue

        if coord_section and line.strip():
            parts = line.strip().split()
            if len(parts) >= 3:
                try:
                    client_id = int(parts[0])
                    x = float(parts[1])
                    y = float(parts[2])
                    clients[client_id] = (x, y)
                except ValueError:
                    continue

    return clients


def parse_routes_from_evo(evo_file):
    """Extrai as rotas do arquivo evo_*.txt.
    Formato: Veiculo 0: 12 clientes, demanda: 150/200, rota: [42, 41, 40, ...]
    """
    routes = {}
    pattern = re.compile(r'Veiculo\s+(\d+):.*rota:\s*\[([^\]]*)\]')

    with open(evo_file, 'r', encoding='utf-8') as f:
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
            vehicle_id = int(match.group(1))
            clients_str = match.group(2).strip()
            if clients_str:
                clients = [int(c.strip()) for c in clients_str.split(',')]
                routes[vehicle_id] = clients

    return routes


def plot_route_map(clients, routes, title, output_file):
    """Gera o mapa das rotas."""
    fig, ax = plt.subplots(figsize=(12, 10))

    # Plotar depósito (quadrado vermelho)
    if 0 in clients:
        depot_x, depot_y = clients[0]
        ax.plot(depot_x, depot_y, 'rs', markersize=12,
                label='Depósito', zorder=5)
        ax.text(depot_x, depot_y + 1.5, '0', fontsize=8,
                ha='center')

    # Plotar todos os clientes
    for client_id, (x, y) in clients.items():
        if client_id != 0:
            ax.plot(x, y, 'ko', markersize=6, zorder=3)
            ax.text(x, y + 1.5, str(client_id), fontsize=8, ha='center')

    # Gerar cores para cada veículo (mesmas do AEMMT)
    num_vehicles = len(routes)
    colors = list(mcolors.TABLEAU_COLORS.values())
    if num_vehicles > len(colors):
        colors = plt.cm.rainbow(np.linspace(0, 1, num_vehicles))

    # Plotar rotas
    for idx, (vehicle_id, route) in enumerate(sorted(routes.items())):
        if not route:
            continue

        color = colors[idx % len(colors)]

        # Adicionar depósito no início e fim
        full_route = [0] + route + [0]

        # Extrair coordenadas
        route_x = []
        route_y = []
        for client_id in full_route:
            if client_id in clients:
                x, y = clients[client_id]
                route_x.append(x)
                route_y.append(y)

        # Plotar linha da rota
        ax.plot(route_x, route_y, color=color, linewidth=2,
                label=f'Veículo {vehicle_id}',
                alpha=0.7, zorder=2)

        # Adicionar setas para indicar direção
        for i in range(len(route_x) - 1):
            dx = route_x[i+1] - route_x[i]
            dy = route_y[i+1] - route_y[i]
            ax.annotate('', xy=(route_x[i+1], route_y[i+1]),
                        xytext=(route_x[i], route_y[i]),
                        arrowprops=dict(arrowstyle='->', color=color,
                                        lw=1.5, alpha=0.6))

    ax.set_xlabel('Coordenada X', fontsize=12)
    ax.set_ylabel('Coordenada Y', fontsize=12)
    ax.set_title(title, fontsize=14, fontweight='bold')
    ax.grid(True, alpha=0.3)
    ax.legend(loc='upper right', fontsize=9, ncol=2)

    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"✓ Mapa salvo: {output_file}")


def main():
    parser = argparse.ArgumentParser(
        description='Gera mapas de rotas NSGA-III')
    parser.add_argument('--instance', type=str, required=True,
                        help='Nome da instância (ex: C101)')
    parser.add_argument('--evo-file', type=str, required=True,
                        help='Arquivo evo_*.txt com as rotas')
    parser.add_argument('--instances-dir', type=str,
                        default='app/src/main/java/instances/solomon',
                        help='Diretório com arquivos das instâncias')
    parser.add_argument('--output', type=str,
                        help='Arquivo de saída (PNG)')

    args = parser.parse_args()

    # Validar arquivos
    evo_file = Path(args.evo_file)
    if not evo_file.exists():
        print(f"Erro: Arquivo não encontrado: {evo_file}")
        return

    instance_file = Path(args.instances_dir) / f"{args.instance.upper()}.txt"
    if not instance_file.exists():
        print(f"Erro: Arquivo da instância não encontrado: {instance_file}")
        return

    # Ler dados
    print(f"Lendo instância: {instance_file}")
    clients = read_instance_coordinates(instance_file)

    print(f"Lendo rotas: {evo_file}")
    routes = parse_routes_from_evo(evo_file)

    if not routes:
        print("Erro: Nenhuma rota encontrada no arquivo")
        return

    print(f"Encontradas {len(routes)} rotas")

    # Definir arquivo de saída com subpasta por instância
    if args.output:
        output_file = Path(args.output)
    else:
        # Criar subpasta para a instância
        instance_folder = evo_file.parent / 'maps' / args.instance.upper()
        instance_folder.mkdir(parents=True, exist_ok=True)
        output_file = instance_folder / f"map_{evo_file.stem}.png"

    output_file.parent.mkdir(parents=True, exist_ok=True)

    # Gerar mapa
    title = f"{args.instance.upper()} - NSGA-III ({evo_file.stem})"
    plot_route_map(clients, routes, title, output_file)


if __name__ == '__main__':
    main()
