#!/usr/bin/env python3
"""
Analisa todas as execuções de cada instância NSGA-III (C, R, RC) e gera uma tabela
com os melhores resultados encontrados.

Para cada instância: pega o melhor das 10 execuções baseado na distância total.
"""

import os
import re
import sys
from pathlib import Path
from collections import defaultdict


def extract_distance_from_evo(file_path):
    """
    Extrai a distância total de um arquivo de solução NSGA-III.
    Procura por "NSGA3_DistânciaMin" na primeira linha
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Procura por "NSGA3_DistanciaMin" (com ou sem acento)
        match = re.search(r'NSGA3_Dist[aâ]nciaMin\s+([\d,]+)', content)
        if match:
            distance_str = match.group(1).replace(',', '.')
            return float(distance_str)

        # Alternativa: procura na secao MELHOR SOLUCAO
        match = re.search(r'Dist[aâ]ncia:\s*([\d,]+)', content)
        if match:
            distance_str = match.group(1).replace(',', '.')
            return float(distance_str)

        return None
    except Exception as e:
        print(f"Erro ao ler {file_path}: {e}", file=sys.stderr)
        return None


def extract_num_vehicles(file_path):
    """
    Extrai o numero de veiculos usados.
    Conta linhas 'Veiculo N:' na secao MELHOR SOLUCAO.
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Conta linhas "Veiculo N:" na secao MELHOR SOLUCAO
        vehicles = len(re.findall(r'Veiculo\s+\d+:', content))
        if vehicles > 0:
            return vehicles

        # Fallback: formato antigo
        vehicles = len(re.findall(r'=== ROTA DO VE', content))
        return vehicles if vehicles > 0 else None
    except Exception:
        return None


def analyze_instance(instance_name, results_dir):
    """
    Analisa todas as execuções de uma instância e retorna o melhor resultado.

    Returns:
        dict com: {
            'instance': nome da instância,
            'best_exec': número da melhor execução,
            'distance': melhor distância encontrada,
            'vehicles': número de veículos,
            'file': caminho do arquivo
        }
    """

    best_result = {
        'instance': instance_name,
        'best_exec': None,
        'distance': float('inf'),
        'vehicles': None,
        'file': None
    }

    # Procura por arquivos de solução (exec01 até exec10)
    for exec_num in range(1, 11):
        # Formato: evo_c101_exec01.txt
        pattern = f"evo_{instance_name.lower()}_exec{exec_num:02d}.txt"
        file_path = results_dir / pattern

        if not file_path.exists():
            continue

        distance = extract_distance_from_evo(file_path)

        if distance is not None and distance < best_result['distance']:
            best_result['distance'] = distance
            best_result['best_exec'] = exec_num
            best_result['vehicles'] = extract_num_vehicles(file_path)
            best_result['file'] = str(file_path)

    if best_result['best_exec'] is None:
        return None

    return best_result


def generate_summary_table():
    """
    Gera tabela resumo com os melhores resultados de todas as instâncias.
    """
    project_root = Path(__file__).resolve().parent.parent
    results_dir = project_root / 'resultsNSGA3'

    if not results_dir.exists():
        print(f"[ERRO] Diretório não encontrado: {results_dir}")
        sys.exit(1)

    # Dicionário para armazenar resultados por categoria
    results_by_category = {
        'C1': [],
        'R1': [],
        'RC1': []
    }

    # Instâncias a analisar
    instances_config = {
        'C1': [f'C{i}' for i in range(101, 110)],  # C101-C109
        'R1': [f'R{i}' for i in range(101, 110)],  # R101-R109
        'RC1': [f'RC{i}' for i in range(101, 109)]  # RC101-RC108
    }

    print("="*80)
    print("ANALISANDO RESULTADOS DAS INSTÂNCIAS NSGA-III")
    print("="*80)
    print()

    # Analisa cada categoria
    for category, instances in instances_config.items():
        print(f"Analisando categoria {category}...")

        for instance in instances:
            result = analyze_instance(instance, results_dir)
            if result:
                results_by_category[category].append(result)
                vehicles_str = f"{result['vehicles']} veículos" if result['vehicles'] else "N/A"
                print(
                    f"  [OK] {instance}: melhor = exec{result['best_exec']:02d} ({vehicles_str}, distância: {result['distance']:.2f})")
            else:
                print(f"  [--] {instance}: sem resultados")

        print()

    # Gera tabela formatada
    print("="*80)
    print("TABELA DE MELHORES RESULTADOS NSGA-III")
    print("="*80)
    print()

    output_lines = []
    output_lines.append("="*80)
    output_lines.append("MELHORES RESULTADOS POR INSTÂNCIA - NSGA-III")
    output_lines.append("="*80)
    output_lines.append("")

    for category in ['C1', 'R1', 'RC1']:
        results = results_by_category[category]

        if not results:
            continue

        output_lines.append(f"--- INSTÂNCIAS {category} ---")
        output_lines.append("")
        output_lines.append(
            f"{'Instância':<12} | {'Melhor Exec':<12} | {'Veículos':<10} | {'Distância':<15}")
        output_lines.append("-" * 80)

        for result in sorted(results, key=lambda x: x['instance']):
            vehicles_str = str(result['vehicles']
                               ) if result['vehicles'] else 'N/A'
            output_lines.append(
                f"{result['instance']:<12} | "
                f"exec{result['best_exec']:02d}       | "
                f"{vehicles_str:<10} | "
                f"{result['distance']:<15.2f}"
            )

        output_lines.append("")

        # Estatísticas da categoria
        if results:
            distances = [r['distance'] for r in results]
            avg_distance = sum(distances) / len(distances)
            min_distance = min(distances)
            max_distance = max(distances)

            # Calcula desvio padrão
            if len(distances) > 1:
                variance = sum((d - avg_distance) **
                               2 for d in distances) / len(distances)
                std_dev = variance ** 0.5
            else:
                std_dev = 0.0

            output_lines.append(f"Estatísticas {category}:")
            output_lines.append(f"  Melhor distância: {min_distance:.2f}")
            output_lines.append(f"  Pior distância: {max_distance:.2f}")
            output_lines.append(f"  Distância média: {avg_distance:.2f}")
            output_lines.append(f"  Desvio padrão: {std_dev:.2f}")
            output_lines.append("")

    output_lines.append("="*80)

    # Exibe na tela
    for line in output_lines:
        print(line)

    # Salva em arquivo
    output_file = project_root / 'best_results_summary_nsga3.txt'
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(output_lines))

    print()
    print(f"[OK] Tabela salva em: {output_file}")

    # Gera também em formato CSV
    csv_file = project_root / 'best_results_summary_nsga3.csv'
    with open(csv_file, 'w', encoding='utf-8') as f:
        f.write("Categoria,Instancia,MelhorExec,Veiculos,Distancia\n")
        for category in ['C1', 'R1', 'RC1']:
            for result in results_by_category[category]:
                vehicles = result['vehicles'] if result['vehicles'] else ''
                f.write(
                    f"{category},{result['instance']},{result['best_exec']},{vehicles},{result['distance']:.2f}\n")

    print(f"[OK] CSV salvo em: {csv_file}")
    print()


def main():
    generate_summary_table()


if __name__ == '__main__':
    main()
