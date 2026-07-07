#!/usr/bin/env python3
"""
Variante do plot_map.py com fontes maiores e legenda fora do mapa,
otimizada para as figuras de destaque do TCC (C106, R108, RC106).

Diferencas em relacao ao plot_map.py original:
- Fonte da legenda 8 -> 14 (legivel quando a figura aparece a 0.48\\textwidth)
- Legenda em uma coluna a direita (fora do plot) com fundo solido
- Fonte dos IDs dos clientes 7 -> 9 (legivel mas nao polui)
- Marcador do deposito 14 -> 16
- Marcadores dos clientes 6 -> 7
- Titulo 15 -> 17
- DPI 200 -> 250 (mais nitido)

Uso identico ao plot_map.py:
    python3 pipeline/plot_map_destaque.py \\
        --algo AEMMT --instance C106 --exec 09 \\
        --evo-file results/C1/C106/evo_c106_exec09.txt \\
        --instance-file src/instances/solomon/C106.txt \\
        --output destaque_maps/AEMMT_map_c106_exec9.png
"""

import argparse
import re
import sys
from pathlib import Path

try:
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    import matplotlib.colors as mcolors
    import numpy as np
except ImportError:
    print("ERRO: matplotlib/numpy nao instalado. pip install matplotlib numpy")
    sys.exit(1)


def read_instance_coordinates(instance_file: Path):
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
                    clients[int(parts[0])] = (float(parts[1]), float(parts[2]))
                except ValueError:
                    continue
    return clients


def parse_routes_aemmt(evo_file: Path):
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
                    clients = [int(c) for c in cli_re.findall(m.group(2)) if int(c) != 0]
                    if clients:
                        routes[vehicle] = clients
    return routes


def parse_routes_nsga3(evo_file: Path):
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
                s = m.group(2).strip()
                if s:
                    routes[vehicle] = [int(c.strip()) for c in s.split(",")]
    return routes


def parse_routes_auto(evo_file: Path, algo: str):
    if algo.upper().startswith("NSGA"):
        return parse_routes_nsga3(evo_file)
    return parse_routes_aemmt(evo_file)


def plot_routes(clients, routes, algo, instance, exec_num, output_file: Path):
    # Figura mais larga para acomodar legenda externa
    fig, ax = plt.subplots(figsize=(13, 10))

    # Deposito
    if 0 in clients:
        dx, dy = clients[0]
        ax.plot(dx, dy, "rs", markersize=16, label="Deposito", zorder=5)
        ax.text(dx, dy + 1.8, "0", fontsize=11, ha="center", fontweight="bold")

    # Clientes
    for cid, (x, y) in clients.items():
        if cid != 0:
            ax.plot(x, y, "ko", markersize=7, zorder=3)
            ax.text(x, y + 1.5, str(cid), fontsize=9, ha="center", alpha=0.8)

    # Cores: tab10 para ate 10, arco-iris para mais
    num_vehicles = len(routes)
    if num_vehicles <= 10:
        colors = list(mcolors.TABLEAU_COLORS.values())
    else:
        colors = list(plt.cm.rainbow(np.linspace(0, 1, num_vehicles)))

    # Rotas
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

        ax.plot(rx, ry, color=color, linewidth=2.2,
                label=f"Veiculo {vehicle_id}", alpha=0.8, zorder=2)

        for i in range(len(rx) - 1):
            ax.annotate("", xy=(rx[i + 1], ry[i + 1]), xytext=(rx[i], ry[i]),
                        arrowprops=dict(arrowstyle="->", color=color,
                                        lw=1.4, alpha=0.7))

    # Titulo padronizado
    if exec_num:
        title = f"{instance} - {algo} (Exec {exec_num})"
    else:
        title = f"{instance} - {algo}"
    ax.set_title(title, fontsize=17, fontweight="bold")
    ax.set_xlabel("Coordenada X", fontsize=13)
    ax.set_ylabel("Coordenada Y", fontsize=13)
    ax.tick_params(axis="both", labelsize=11)
    ax.grid(True, alpha=0.3)

    # Legenda FORA do plot, a direita, em coluna unica, fonte grande
    ax.legend(loc="center left", bbox_to_anchor=(1.01, 0.5),
              fontsize=13, ncol=1, framealpha=0.95, borderaxespad=0.5)

    plt.tight_layout()
    output_file.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(output_file, dpi=250, bbox_inches="tight")
    plt.close()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--algo", required=True, choices=["AEMMT", "NSGA-III"])
    p.add_argument("--instance", required=True)
    p.add_argument("--exec", dest="exec_num", default="")
    p.add_argument("--evo-file", required=True, type=Path)
    p.add_argument("--instance-file", required=True, type=Path)
    p.add_argument("--output", required=True, type=Path)
    args = p.parse_args()

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
