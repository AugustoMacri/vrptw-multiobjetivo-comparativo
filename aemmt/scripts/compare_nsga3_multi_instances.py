#!/usr/bin/env python3
"""
Compare NSGA3 `results.txt` with multi `multi_stats_table_*.txt` for instances C101, R101, RC101.

For each instance folder under `results_NSGA3/` the script:
- reads `results.txt` (NSGA3) — one fitness per line (10 lines expected)
- reads `multi_stats_table_*.txt` and extracts the per-execution "Melhor Fitness" (Execução 1..10)
- writes a comparison text file into the instance folder with a table and bests
- saves a side-by-side bar chart (10 pairs) into the instance folder

Usage: run from project root:
  python3 scripts/compare_nsga3_multi_instances.py
"""

import os
import re
import math
from pathlib import Path
from datetime import datetime
import matplotlib.pyplot as plt


BASE = Path(__file__).resolve().parent.parent / 'results_NSGA3'
INSTANCES = ['C101', 'R101', 'RC101']


def read_nsga3_results(path: Path):
    """Read results.txt and return list of floats in order."""
    vals = []
    with path.open('r', encoding='utf-8') as f:
        for line in f:
            s = line.strip()
            if not s:
                continue
            try:
                vals.append(float(s.replace(',', '.')))
            except ValueError:
                # ignore non-numeric lines
                pass
    return vals


def read_multi_stats(path: Path):
    """Parse multi_stats_table file and return list of per-execution best fitnesses (ordered by execução number).

    Expects lines like:
    1          | 11021.30
    """
    vals = []
    pattern = re.compile(r"^\s*(\d+)\s*\|\s*([0-9.,]+)")
    with path.open('r', encoding='utf-8') as f:
        for line in f:
            m = pattern.match(line)
            if m:
                idx = int(m.group(1))
                v = float(m.group(2).replace(',', '.'))
                vals.append((idx, v))
    # sort by index and return only values
    vals_sorted = [v for i, v in sorted(vals, key=lambda x: x[0])]
    return vals_sorted


def find_multi_stats_file(dirpath: Path):
    # pick the first matching multi_stats_table_*.txt
    candidates = sorted(dirpath.glob('multi_stats_table_*.txt'))
    if candidates:
        return candidates[0]
    # also try other common names
    candidates = sorted(dirpath.glob('*/multi_stats_table_*.txt'))
    return candidates[0] if candidates else None


def compute_stats(values):
    """Compute best, average, and standard deviation from a list of values."""
    if not values:
        return None, None, None
    best = min(values)
    avg = sum(values) / len(values)
    variance = sum((v - avg) ** 2 for v in values) / len(values)
    std_dev = math.sqrt(variance)
    return best, avg, std_dev


def write_nsga3_txt(instance_dir: Path, nsga_vals):
    """Write NSGA3 statistics to file."""
    ts = datetime.now().strftime('%Y%m%d_%H%M%S')
    out_path = instance_dir / f'stats_nsga3_{ts}.txt'
    best, avg, std = compute_stats(nsga_vals)

    with out_path.open('w', encoding='utf-8') as f:
        f.write(f'Estatísticas NSGA3 - {instance_dir.name}\n')
        f.write('=' * 60 + '\n\n')
        f.write(f"Execução\tFitness\n")
        f.write('-' * 40 + '\n')
        for i, val in enumerate(nsga_vals, 1):
            f.write(f"{i}\t{val:.2f}\n")
        f.write('\n')
        f.write(f"Melhor Fitness: {best:.2f}\n")
        f.write(f"Fitness Médio: {avg:.2f}\n")
        f.write(f"Desvio Padrão: {std:.2f}\n")

    return out_path


def write_aemmt_txt(instance_dir: Path, multi_vals):
    """Write AEMMT (Multi) statistics to file."""
    ts = datetime.now().strftime('%Y%m%d_%H%M%S')
    out_path = instance_dir / f'stats_aemmt_{ts}.txt'
    best, avg, std = compute_stats(multi_vals)

    with out_path.open('w', encoding='utf-8') as f:
        f.write(f'Estatísticas AEMMT (Multi) - {instance_dir.name}\n')
        f.write('=' * 60 + '\n\n')
        f.write(f"Execução\tFitness\n")
        f.write('-' * 40 + '\n')
        for i, val in enumerate(multi_vals, 1):
            f.write(f"{i}\t{val:.2f}\n")
        f.write('\n')
        f.write(f"Melhor Fitness: {best:.2f}\n")
        f.write(f"Fitness Médio: {avg:.2f}\n")
        f.write(f"Desvio Padrão: {std:.2f}\n")

    return out_path


def plot_averages(instance_dir: Path, nsga_vals, multi_vals):
    """Plot a bar chart with only 2 bars: average NSGA3 and average AEMMT."""
    _, avg_nsga, _ = compute_stats(nsga_vals)
    _, avg_multi, _ = compute_stats(multi_vals)

    fig, ax = plt.subplots(figsize=(8, 6))

    labels = ['NSGA3', 'AEMMT (Multi)']
    values = [avg_nsga, avg_multi]
    colors = ['tab:blue', 'tab:red']

    ax.bar(labels, values, color=colors, width=0.5)

    ax.set_ylabel('Fitness')
    ax.set_title(f'Comparação NSGA3 vs Multi - {instance_dir.name}')
    ax.grid(axis='y', linestyle='--', alpha=0.5)
    ax.set_ylim(top=15000)

    out_png = instance_dir / f'compare_nsga3_multi_{instance_dir.name}.png'
    plt.tight_layout()
    plt.savefig(out_png, dpi=300)
    plt.close()
    return out_png


def process_instance(instance_name: str):
    inst_dir = BASE / instance_name
    if not inst_dir.exists():
        print(f'Instance dir not found: {inst_dir}')
        return

    nsga_file = inst_dir / 'results.txt'
    if not nsga_file.exists():
        print(f'NSGA3 results.txt not found for {instance_name}')
        return
    nsga_vals = read_nsga3_results(nsga_file)

    multi_file = find_multi_stats_file(inst_dir)
    if not multi_file:
        print(f'multi_stats_table file not found for {instance_name}')
        multi_vals = []
    else:
        multi_vals = read_multi_stats(multi_file)

    txt_nsga3 = write_nsga3_txt(inst_dir, nsga_vals)
    txt_aemmt = write_aemmt_txt(inst_dir, multi_vals)
    png = plot_averages(inst_dir, nsga_vals, multi_vals)

    print(f'Wrote {txt_nsga3}, {txt_aemmt}, and {png}')


def main():
    for inst in INSTANCES:
        print('Processing', inst)
        process_instance(inst)


if __name__ == '__main__':
    main()
