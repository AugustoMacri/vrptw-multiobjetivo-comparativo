#!/usr/bin/env python3
"""
Script para comparar resultados AEMMT vs NSGA-III
Gera uma tabela comparativa das estatísticas de ambos os algoritmos
"""

import re
from pathlib import Path
import statistics


def extract_stats_from_file(file_path):
    """Extrai estatísticas de um arquivo de resultados"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extrair distâncias
    distances = []
    for match in re.finditer(r'\d+\s+\|\s+([\d,.]+)', content):
        dist_str = match.group(1).replace(',', '.')
        distances.append(float(dist_str))

    if not distances:
        return None

    return {
        'best': min(distances),
        'worst': max(distances),
        'mean': statistics.mean(distances),
        'std': statistics.stdev(distances) if len(distances) > 1 else 0.0,
        'count': len(distances)
    }


def main():
    print("=" * 80)
    print("COMPARAÇÃO: AEMMT vs NSGA-III")
    print("Instâncias Solomon C1 (C101 - C109)")
    print("=" * 80)
    print()

    # Diretórios
    aemmt_dir = Path("../results_validation_C1")
    nsga3_dir = Path("results_validation_NSGA3_C1")

    instances = [f"C{i:02d}" for i in range(101, 110)]

    # Tabela comparativa
    print(f"{'Inst.':<8} {'Algoritmo':<10} {'Melhor':>10} {'Pior':>10} {'Média':>10} {'Desvio':>10} {'Δ Média':>10}")
    print("-" * 80)

    for instance in instances:
        # AEMMT
        aemmt_files = list(
            (aemmt_dir / instance).glob("resultados_aemmt_*.txt"))
        if aemmt_files:
            aemmt_stats = extract_stats_from_file(aemmt_files[0])
            if aemmt_stats:
                print(f"{instance:<8} {'AEMMT':<10} {aemmt_stats['best']:>10.2f} {aemmt_stats['worst']:>10.2f} "
                      f"{aemmt_stats['mean']:>10.2f} {aemmt_stats['std']:>10.2f} {'-':>10}")

        # NSGA-III
        nsga3_files = list(
            (nsga3_dir / instance).glob("resultados_nsga3_*.txt"))
        if nsga3_files:
            nsga3_stats = extract_stats_from_file(nsga3_files[0])
            if nsga3_stats and aemmt_stats:
                delta = nsga3_stats['mean'] - aemmt_stats['mean']
                delta_str = f"{delta:+.2f}"
                print(f"{'':<8} {'NSGA-III':<10} {nsga3_stats['best']:>10.2f} {nsga3_stats['worst']:>10.2f} "
                      f"{nsga3_stats['mean']:>10.2f} {nsga3_stats['std']:>10.2f} {delta_str:>10}")
                print("-" * 80)

    print()
    print("Legenda:")
    print("  Δ Média: Diferença entre média NSGA-III e AEMMT (negativo = NSGA-III melhor)")
    print()


if __name__ == '__main__':
    main()
