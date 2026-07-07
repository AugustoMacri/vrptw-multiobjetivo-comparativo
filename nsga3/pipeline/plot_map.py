#!/usr/bin/env python3
"""
Gerador padronizado de mapas de rotas - AEMMT e NSGA-III.

Le um arquivo evo_<instancia>_exec<N>.txt (formato com secao "ROTAS FINAIS"
do AEMMT ou "MELHOR SOLUCAO" do NSGA-III) e plota as rotas em um mapa
PNG com layout uniforme em ambos os projetos.

Uso:
    python3 pipeline/plot_map.py \
        --algo AEMMT \
        --instance C101 \
        --exec 01 \
        --evo-file results/C1/C101/evo_c101_exec01.txt \
        --instance-file src/instances/solomon/C101.txt \
        --output results/C1/C101/maps/map_aemmt_c101_exec01.png

Convencoes visuais:
    - Deposito: quadrado vermelho grande
    - Clientes: circulos pretos pequenos com ID
    - Veiculos: cores distintas (tab10 + arco-iris para >10)
    - Setas: indicam direcao do deslocamento
    - Titulo: "<INSTANCIA> - <ALGO> (Exec <N>)"
"""

import argparse
import re
import sys
from pathlib import Path

try:
    import matplotlib

    matplotlib.use("Agg")  # nao precisa de display
    import matplotlib.pyplot as plt
    import matplotlib.colors as mcolors
    import numpy as np
except ImportError as e:
    print("ERRO: matplotlib/numpy nao instalado.")
    print("      Instale com: pip install matplotlib numpy")
    sys.exit(1)


def read_instance_coordinates(instance_file: Path):
    """Le coordenadas dos clientes do arquivo de instancia Solomon."""
    clients = {}
    with open(instance_file, "r", encoding="utf-8") as f:
        lines = f.readlines()

    coord_section = False
    for line in lines:
        if "CUST NO." in line or "CUSTOMER" in line:
            coord_section = True
            continue
        if coord_section and line.strip():
            parts = line.strip().split()
            if len(parts) >= 3:
                try:
                    cid = int(parts[0])
                    x = float(parts[1])
                    y = float(parts[2])
                    clients[cid] = (x, y)
                except ValueError:
                    continue
    return clients


def parse_routes_aemmt(evo_file: Path):
    """Le rotas do formato AEMMT.

    Procura pela secao 'ROTAS FINAIS' e extrai cada linha:
      Veiculo X: Deposito(0) -> Cliente(Y) -> ... -> Deposito(0)
    """
    routes = {}
    in_final = False
    pattern = re.compile(r"Ve[ií]culo\s+(\d+):\s+(.*)")
    cli_re = re.compile(r"Cliente\((\d+)\)")

    with open(evo_file, "r", encoding="utf-8", errors="replace") as f:
        for raw in f:
            line = raw.strip()
            if "ROTAS FINAIS" in line:
                in_final = True
                continue
            if in_final and "FRENTE DE PARETO" in line:
                break
            if in_final and "===" not in line:
                m = pattern.match(line)
                if m:
                    vehicle = int(m.group(1))
                    clients = [int(c) for c in cli_re.findall(m.group(2))]
                    # Remove deposito (0) - sera redesenhado
                    clients = [c for c in clients if c != 0]
                    if clients:
                        routes[vehicle] = clients
    return routes


def parse_routes_nsga3(evo_file: Path):
    """Le rotas do formato NSGA-III.

    Procura pela secao 'MELHOR SOLUCAO' e extrai cada linha:
      Veiculo X: ... rota: [c1, c2, c3, ...]
    """
    routes = {}
    pattern = re.compile(r"Veiculo\s+(\d+):.*rota:\s*\[([^\]]*)\]")
    in_solution = False

    with open(evo_file, "r", encoding="utf-8", errors="replace") as f:
        for raw in f:
            line = raw.strip()
            if "=== MELHOR SOLUCAO" in line:
                in_solution = True
                continue
            if not in_solution:
                continue
            m = pattern.search(line)
            if m:
                vehicle = int(m.group(1))
                clients_str = m.group(2).strip()
                if clients_str:
                    clients = [int(c.strip()) for c in clients_str.split(",")]
                    routes[vehicle] = clients
    return routes


def parse_routes_auto(evo_file: Path, algo: str):
    """Decide qual parser usar baseado no algoritmo."""
    if algo.upper().startswith("NSGA"):
        return parse_routes_nsga3(evo_file)
    return parse_routes_aemmt(evo_file)


def plot_routes(clients, routes, algo, instance, exec_num, output_file: Path):
    """Gera o mapa PNG com layout padronizado."""
    fig, ax = plt.subplots(figsize=(12, 10))

    # Deposito (cliente 0)
    if 0 in clients:
        dx, dy = clients[0]
        ax.plot(dx, dy, "rs", markersize=14, label="Deposito", zorder=5)
        ax.text(dx, dy + 1.8, "0", fontsize=9, ha="center", fontweight="bold")

    # Demais clientes
    for cid, (x, y) in clients.items():
        if cid != 0:
            ax.plot(x, y, "ko", markersize=6, zorder=3)
            ax.text(x, y + 1.5, str(cid), fontsize=7, ha="center", alpha=0.7)

    # Cores: tab10 para ate 10 veiculos, arco-iris a partir disso
    num_vehicles = len(routes)
    if num_vehicles <= 10:
        colors = list(mcolors.TABLEAU_COLORS.values())
    else:
        colors = list(plt.cm.rainbow(np.linspace(0, 1, num_vehicles)))

    # Plota cada rota
    for idx, (vehicle_id, route) in enumerate(sorted(routes.items())):
        if not route:
            continue
        color = colors[idx % len(colors)]
        full_route = [0] + list(route) + [0]

        rx, ry = [], []
        for cid in full_route:
            if cid in clients:
                x, y = clients[cid]
                rx.append(x)
                ry.append(y)

        ax.plot(rx, ry, color=color, linewidth=1.8,
                label=f"Veiculo {vehicle_id}", alpha=0.75, zorder=2)

        # Setas indicando direcao
        for i in range(len(rx) - 1):
            ax.annotate("", xy=(rx[i + 1], ry[i + 1]), xytext=(rx[i], ry[i]),
                        arrowprops=dict(arrowstyle="->", color=color,
                                        lw=1.2, alpha=0.6))

    # Titulo padronizado
    if exec_num:
        title = f"{instance} - {algo} (Exec {exec_num})"
    else:
        title = f"{instance} - {algo}"
    ax.set_title(title, fontsize=15, fontweight="bold")
    ax.set_xlabel("Coordenada X", fontsize=12)
    ax.set_ylabel("Coordenada Y", fontsize=12)
    ax.grid(True, alpha=0.3)
    ax.legend(loc="upper right", fontsize=8, ncol=2)

    plt.tight_layout()
    output_file.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(output_file, dpi=200, bbox_inches="tight")
    plt.close()


def main():
    parser = argparse.ArgumentParser(description="Gerador padronizado de mapas de rotas")
    parser.add_argument("--algo", required=True, choices=["AEMMT", "NSGA-III"],
                        help="Algoritmo (define parser do evo_file e titulo)")
    parser.add_argument("--instance", required=True, help="Nome da instancia (ex: C101)")
    parser.add_argument("--exec", dest="exec_num", default="", help="Numero da execucao (ex: 01)")
    parser.add_argument("--evo-file", required=True, type=Path,
                        help="Arquivo evo_<instancia>_exec<N>.txt")
    parser.add_argument("--instance-file", required=True, type=Path,
                        help="Arquivo .txt da instancia Solomon")
    parser.add_argument("--output", required=True, type=Path,
                        help="Caminho do PNG de saida")
    args = parser.parse_args()

    if not args.evo_file.exists():
        print(f"ERRO: evo file nao encontrado: {args.evo_file}")
        sys.exit(1)
    if not args.instance_file.exists():
        print(f"ERRO: instance file nao encontrado: {args.instance_file}")
        sys.exit(1)

    clients = read_instance_coordinates(args.instance_file)
    routes = parse_routes_auto(args.evo_file, args.algo)

    if not routes:
        print(f"AVISO: nenhuma rota encontrada em {args.evo_file}")
        sys.exit(2)

    plot_routes(clients, routes, args.algo, args.instance, args.exec_num, args.output)
    print(f"[OK] mapa salvo em: {args.output}  ({len(routes)} veiculos)")


if __name__ == "__main__":
    main()
