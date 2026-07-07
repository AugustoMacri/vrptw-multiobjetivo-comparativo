#!/usr/bin/env python3
"""
Compara resultados entre algoritmo Multi-Objetivo (AEMMT) e NSGA-III
"""

import os
import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path


def parse_evolution_file(filepath):
    """Extrai métricas de um arquivo de evolução"""
    metrics = {}

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

        # Padrões para extrair valores
        patterns = {
            'dist_min': r'DistânciaMin\s+([\d.]+)',
            'time_min': r'TempoMin\s+([\d.]+)',
            'fuel_min': r'CombustívelMin\s+([\d.]+)',
            'fitness_min': r'FitnessPonderadoMin\s+([\d.]+)',
            'exec_time': r'TempoExecução\(ms\)\s+(\d+)'
        }

        for key, pattern in patterns.items():
            match = re.search(pattern, content)
            if match:
                metrics[key] = float(match.group(1))

    return metrics


def compare_algorithms(instance_name):
    """Compara resultados de uma instância específica"""

    # Caminhos dos diretórios
    multi_dir = Path(f"results_validation_C1/{instance_name}")
    nsga3_dir = Path(f"results_validation_NSGA3_C1/{instance_name}")

    if not multi_dir.exists() or not nsga3_dir.exists():
        print(f"⚠ Diretórios não encontrados para {instance_name}")
        return None

    # Coletar resultados Multi-Objetivo
    multi_results = []
    for exec_num in range(1, 11):
        evo_file = multi_dir / \
            f"evo_{instance_name.lower()}_exec{exec_num:02d}.txt"
        if evo_file.exists():
            metrics = parse_evolution_file(evo_file)
            if metrics:
                metrics['algorithm'] = 'Multi-Obj'
                metrics['execution'] = exec_num
                multi_results.append(metrics)

    # Coletar resultados NSGA-III
    nsga3_results = []
    for exec_num in range(1, 11):
        evo_file = nsga3_dir / \
            f"evo_{instance_name.lower()}_exec{exec_num:02d}.txt"
        if evo_file.exists():
            metrics = parse_evolution_file(evo_file)
            if metrics:
                metrics['algorithm'] = 'NSGA-III'
                metrics['execution'] = exec_num
                nsga3_results.append(metrics)

    # Criar DataFrame
    df_multi = pd.DataFrame(multi_results)
    df_nsga3 = pd.DataFrame(nsga3_results)
    df_all = pd.concat([df_multi, df_nsga3], ignore_index=True)

    return df_all


def generate_comparison_report():
    """Gera relatório completo de comparação"""

    instances = [f"C10{i}" for i in range(1, 10)]

    print("=" * 60)
    print("COMPARAÇÃO: Multi-Objetivo AEMMT vs NSGA-III")
    print("=" * 60)
    print()

    all_results = []

    for instance in instances:
        df = compare_algorithms(instance)
        if df is not None and not df.empty:
            all_results.append(df)

            print(f"\n{instance}:")
            print("-" * 60)

            # Estatísticas por algoritmo
            summary = df.groupby('algorithm').agg({
                'fitness_min': ['mean', 'std', 'min', 'max'],
                'dist_min': ['mean', 'std'],
                'time_min': ['mean', 'std'],
                'exec_time': ['mean', 'std']
            }).round(2)

            print(summary)

            # Teste de significância (Mann-Whitney)
            if len(df[df['algorithm'] == 'Multi-Obj']) >= 3 and \
               len(df[df['algorithm'] == 'NSGA-III']) >= 3:
                try:
                    from scipy.stats import mannwhitneyu

                    multi_fitness = df[df['algorithm']
                                       == 'Multi-Obj']['fitness_min']
                    nsga3_fitness = df[df['algorithm']
                                       == 'NSGA-III']['fitness_min']

                    statistic, p_value = mannwhitneyu(
                        multi_fitness, nsga3_fitness)

                    print(f"\nTeste Mann-Whitney U: p-value = {p_value:.4f}")
                    if p_value < 0.05:
                        winner = 'Multi-Obj' if multi_fitness.mean() < nsga3_fitness.mean() else 'NSGA-III'
                        print(
                            f"✓ Diferença significativa (α=0.05): {winner} é melhor")
                    else:
                        print("→ Sem diferença significativa (α=0.05)")
                except ImportError:
                    print("\n⚠ scipy não instalado. Instale com: pip install scipy")

    # Salvar resultados consolidados
    if all_results:
        df_final = pd.concat(all_results, ignore_index=True)
        df_final.to_csv("comparison_multi_nsga3.csv", index=False)
        print("\n" + "=" * 60)
        print("✓ Relatório salvo em: comparison_multi_nsga3.csv")
        print("=" * 60)
    else:
        print("\n⚠ Nenhum resultado encontrado para comparação")


if __name__ == "__main__":
    generate_comparison_report()
