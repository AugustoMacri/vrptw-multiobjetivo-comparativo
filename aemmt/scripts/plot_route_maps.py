#!/usr/bin/env python3
"""
Script para gerar mapas de rotas dos veículos a partir dos arquivos de resultados.
Lê as rotas iniciais e finais dos arquivos evo_*.txt e cria visualizações coloridas.
"""

import os
import re
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
from pathlib import Path
import numpy as np


def read_instance_file(instance_path):
    """Lê o arquivo da instância e retorna as coordenadas dos clientes."""
    clients = {}

    with open(instance_path, 'r') as f:
        lines = f.readlines()

    # Encontrar a seção de coordenadas dos clientes
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


def parse_routes_from_evo_file(evo_file_path):
    """Extrai as rotas iniciais e finais do arquivo evo_*.txt."""
    with open(evo_file_path, 'r') as f:
        content = f.read()

    initial_routes = {}
    final_routes = {}

    # Padrão para encontrar rotas: "Veículo X: Depósito(0) -> Cliente(Y) -> ... -> Depósito(0)"
    route_pattern = r'Veículo (\d+): (Depósito\(\d+\).*?Depósito\(\d+\))'

    # Separar seções inicial e final
    initial_section_match = re.search(
        r'ROTAS INICIAIS.*?={80}(.*?)={80}', content, re.DOTALL)
    final_section_match = re.search(
        r'ROTAS FINAIS.*?={80}(.*?)={80}', content, re.DOTALL)

    if initial_section_match:
        initial_section = initial_section_match.group(1)
        for match in re.finditer(route_pattern, initial_section):
            vehicle_id = int(match.group(1))
            route_str = match.group(2)

            # Extrair IDs dos clientes
            client_ids = []
            for client_match in re.finditer(r'Cliente\((\d+)\)', route_str):
                client_ids.append(int(client_match.group(1)))

            if client_ids:
                initial_routes[vehicle_id] = [0] + client_ids + [0]

    if final_section_match:
        final_section = final_section_match.group(1)
        for match in re.finditer(route_pattern, final_section):
            vehicle_id = int(match.group(1))
            route_str = match.group(2)

            # Extrair IDs dos clientes
            client_ids = []
            for client_match in re.finditer(r'Cliente\((\d+)\)', route_str):
                client_ids.append(int(client_match.group(1)))

            if client_ids:
                final_routes[vehicle_id] = [0] + client_ids + [0]

    return initial_routes, final_routes


def plot_routes(clients, routes, title, output_path):
    """Plota as rotas em um mapa com cores diferentes para cada veículo."""
    fig, ax = plt.subplots(figsize=(12, 10))

    # Plotar todos os clientes como pontos
    for client_id, (x, y) in clients.items():
        if client_id == 0:
            # Depósito em vermelho e maior
            ax.plot(x, y, 'rs', markersize=12,
                    label='Depósito' if client_id == 0 else '')
        else:
            ax.plot(x, y, 'ko', markersize=6)

        # Adicionar número do cliente
        ax.text(x, y + 1.5, str(client_id), fontsize=8, ha='center')

    # Gerar cores para cada veículo
    num_vehicles = len(routes)
    colors = list(mcolors.TABLEAU_COLORS.values())
    if num_vehicles > len(colors):
        colors = plt.cm.rainbow(np.linspace(0, 1, num_vehicles))

    # Plotar rotas
    for idx, (vehicle_id, route) in enumerate(sorted(routes.items())):
        if len(route) < 2:
            continue

        color = colors[idx % len(colors)]

        # Extrair coordenadas da rota
        route_x = []
        route_y = []
        for client_id in route:
            if client_id in clients:
                x, y = clients[client_id]
                route_x.append(x)
                route_y.append(y)

        # Plotar a rota
        ax.plot(route_x, route_y, color=color, linewidth=2,
                label=f'Veículo {vehicle_id}', alpha=0.7)

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
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"Mapa salvo em: {output_path}")


def process_instance(instance_name, results_dir, instances_dir, output_dir):
    """Processa uma instância e gera os mapas de rotas."""
    # Encontrar arquivo evo_*.txt mais recente
    evo_files = list(Path(results_dir).glob(
        f"evo_{instance_name.lower()}_exec*.txt"))

    if not evo_files:
        # Tentar sem o padrão _exec
        evo_files = list(Path(results_dir).glob(
            f"evo_{instance_name.lower()}.txt"))

    if not evo_files:
        print(
            f"Aviso: Nenhum arquivo evo_*.txt encontrado para {instance_name}")
        return

    # Usar o arquivo mais recente
    evo_file = max(evo_files, key=os.path.getmtime)
    print(f"\nProcessando: {evo_file.name}")

    # Ler coordenadas da instância
    instance_file = Path(instances_dir) / f"{instance_name}.txt"
    if not instance_file.exists():
        print(f"Aviso: Arquivo de instância não encontrado: {instance_file}")
        return

    clients = read_instance_file(instance_file)
    if not clients:
        print(f"Aviso: Não foi possível ler coordenadas de {instance_file}")
        return

    # Extrair rotas do arquivo evo
    initial_routes, final_routes = parse_routes_from_evo_file(evo_file)

    if not initial_routes and not final_routes:
        print(f"Aviso: Nenhuma rota encontrada em {evo_file.name}")
        return

    # Criar diretório de saída
    output_dir_path = Path(output_dir)
    output_dir_path.mkdir(parents=True, exist_ok=True)

    # Gerar mapas
    if initial_routes:
        initial_output = output_dir_path / \
            f"route_map_{instance_name.lower()}_initial.png"
        plot_routes(clients, initial_routes,
                    f"Rotas Iniciais - {instance_name}",
                    initial_output)

    if final_routes:
        final_output = output_dir_path / \
            f"route_map_{instance_name.lower()}_final.png"
        plot_routes(clients, final_routes,
                    f"Rotas Finais - {instance_name}",
                    final_output)


def main():
    """Função principal."""
    import argparse

    parser = argparse.ArgumentParser(
        description='Gera mapas de rotas a partir dos resultados')
    parser.add_argument('--instance', type=str,
                        help='Nome da instância (ex: C101)')
    parser.add_argument('--results-dir', type=str,
                        default='results_validation_C1_previous',
                        help='Diretório com os resultados')
    parser.add_argument('--results-file', type=str,
                        help='Arquivo de resultado específico (ex: evo_c101_exec01.txt)')
    parser.add_argument('--instances-dir', type=str,
                        default='src/instances/solomon',
                        help='Diretório com os arquivos das instâncias')
    parser.add_argument('--output-dir', type=str,
                        help='Diretório para salvar os mapas')

    args = parser.parse_args()

    if args.results_file:
        # Processar um único arquivo de resultado
        if not args.instance:
            print("Erro: --instance é obrigatório quando usar --results-file")
            return

        instance_name = args.instance.upper()
        results_file_path = Path(args.results_file)

        if not results_file_path.exists():
            print(f"Erro: Arquivo não encontrado: {results_file_path}")
            return

        # Buscar arquivo da instância
        instance_file = Path(args.instances_dir) / f"{instance_name}.txt"
        if not instance_file.exists():
            print(
                f"Erro: Arquivo da instância não encontrado: {instance_file}")
            return

        # Ler coordenadas
        clients = read_instance_file(instance_file)

        # Extrair rotas
        initial_routes, final_routes = parse_routes_from_evo_file(
            results_file_path)

        # Criar diretório de saída
        output_dir = Path(args.output_dir or results_file_path.parent)
        output_dir.mkdir(parents=True, exist_ok=True)

        # Nome base do arquivo
        file_basename = results_file_path.stem  # ex: evo_c101_exec01

        # Gerar mapas
        initial_output = output_dir / f"route_map_{file_basename}_initial.png"
        final_output = output_dir / f"route_map_{file_basename}_final.png"

        if initial_routes:
            plot_routes(clients, initial_routes,
                        f"{instance_name} - Rotas Iniciais", initial_output)

        if final_routes:
            plot_routes(clients, final_routes,
                        f"{instance_name} - Rotas Finais", final_output)

        return

    if args.instance:
        # Processar uma única instância
        instance_name = args.instance.upper()
        results_subdir = Path(args.results_dir) / instance_name

        if not results_subdir.exists():
            results_subdir = Path(args.results_dir)

        output_subdir = Path(
            args.output_dir or args.results_dir) / instance_name / 'route_maps'

        process_instance(instance_name, results_subdir,
                         args.instances_dir, output_subdir)
    else:
        # Processar todas as instâncias C1
        instances = [f'C{i:02d}' for i in range(101, 110)]  # C101 a C109

        for instance_name in instances:
            results_subdir = Path(args.results_dir) / instance_name

            if not results_subdir.exists():
                print(f"Aviso: Diretório não encontrado: {results_subdir}")
                continue

            output_subdir = Path(
                args.output_dir or args.results_dir) / instance_name / 'route_maps'

            try:
                process_instance(instance_name, results_subdir,
                                 args.instances_dir, output_subdir)
            except Exception as e:
                print(f"Erro ao processar {instance_name}: {e}")
                continue


if __name__ == '__main__':
    main()
