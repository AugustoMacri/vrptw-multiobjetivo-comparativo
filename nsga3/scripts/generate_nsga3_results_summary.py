#!/usr/bin/env python3
"""
Script para gerar arquivos de resultados NSGA-III no formato AEMMT
Extrai as distâncias mínimas de cada execução e gera estatísticas
"""

import re
import sys
from pathlib import Path
from datetime import datetime
import statistics


def extract_distance_from_evo(evo_file):
    """Extrai a distância mínima do arquivo evo_*.txt"""
    with open(evo_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Procurar por "NSGA3_DistânciaMin" na primeira linha de dados
    match = re.search(r'NSGA3_DistânciaMin\s+([\d,]+)', content)
    if match:
        distance_str = match.group(1).replace(',', '.')
        return float(distance_str)

    return None


def generate_results_file(instance, results_dir, output_file):
    """Gera arquivo de resultados para uma instância"""

    # Coletar distâncias de todas as execuções
    distances = []
    for exec_num in range(1, 11):
        evo_file = results_dir / \
            f"evo_{instance.lower()}_exec{exec_num:02d}.txt"

        if evo_file.exists():
            distance = extract_distance_from_evo(evo_file)
            if distance:
                distances.append((exec_num, distance))

    if not distances:
        print(f"  ⚠ Nenhuma distância encontrada para {instance}")
        return False

    # Calcular estatísticas
    dist_values = [d[1] for d in distances]
    best_dist = min(dist_values)
    worst_dist = max(dist_values)
    avg_dist = statistics.mean(dist_values)
    std_dist = statistics.stdev(dist_values) if len(dist_values) > 1 else 0.0

    # Gerar arquivo de resultados
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(f"Resultados NSGA-III - {instance}\n")
        f.write("=" * 60 + "\n")
        f.write("\n")
        f.write(f"Total de execuções: {len(distances)}\n")
        f.write("\n")
        f.write("Execução | Melhor Distância Encontrada\n")
        f.write("-" * 40 + "\n")

        for exec_num, distance in distances:
            f.write(f"{exec_num:8d} | {distance:8.2f}\n")

        f.write("\n")
        f.write("Estatísticas:\n")
        f.write("-" * 40 + "\n")
        f.write(f"Melhor distância encontrada: {best_dist:.2f}\n")
        f.write(f"Pior distância encontrada: {worst_dist:.2f}\n")
        f.write(f"Distância média geral: {avg_dist:.2f}\n")
        f.write(f"Desvio padrão das distâncias: {std_dist:.2f}\n")
        f.write("\n")

    print(
        f"  ✓ {instance}: Média={avg_dist:.2f}, Melhor={best_dist:.2f}, σ={std_dist:.2f}")
    return True


def main():
    results_dir = Path("app/resultsNSGA3")
    output_base_dir = Path("results_validation_NSGA3_C1")

    if not results_dir.exists():
        print(f"Erro: Diretório não encontrado: {results_dir}")
        sys.exit(1)

    print("=" * 60)
    print("Gerador de Resultados NSGA-III")
    print("=" * 60)
    print()

    # Processar todas as instâncias C1
    instances = [f"C{i:02d}" for i in range(101, 110)]  # C101 a C109

    total_generated = 0
    for instance in instances:
        # Criar diretório de saída para a instância
        output_dir = output_base_dir / instance
        output_dir.mkdir(parents=True, exist_ok=True)

        # Nome do arquivo de saída com timestamp
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_file = output_dir / f"resultados_nsga3_{timestamp}.txt"

        print(f"Processando {instance}...")
        if generate_results_file(instance, results_dir, output_file):
            total_generated += 1

    print()
    print("=" * 60)
    print(f"Concluído! {total_generated} arquivos gerados")
    print(f"Resultados salvos em: {output_base_dir}")
    print("=" * 60)


if __name__ == '__main__':
    main()
