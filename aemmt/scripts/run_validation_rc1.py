#!/usr/bin/env python3
"""
Script para validação do algoritmo AEMMT nas instâncias RC101-RC108.
Executa 10 vezes cada instância e armazena resultados em results_validation_RC1/.

Uso:
    python3 scripts/run_validation_rc1.py
"""

import subprocess
import os
import re
import math
from pathlib import Path
from datetime import datetime

# Configurações
PROJECT_ROOT = Path(__file__).resolve().parent.parent
RESULTS_BASE_DIR = PROJECT_ROOT / 'results_validation_RC1'
BIN_DIR = PROJECT_ROOT / 'bin'
RESULTS_MULTI_DIR = PROJECT_ROOT / 'resultsMulti'

# Instâncias RC1 (RC101 até RC108)
INSTANCES = {
    'RC101': 41,
    'RC102': 42,
    'RC103': 43,
    'RC104': 44,
    'RC105': 45,
    'RC106': 46,
    'RC107': 47,
    'RC108': 48,
}

NUM_EXECUTIONS = 10


def compile_project():
    """Compila o projeto Java."""
    print("=" * 60)
    print("Compilando o projeto...")
    print("=" * 60)

    src_dir = PROJECT_ROOT / 'src'

    # Compilar todos os arquivos Java
    result = subprocess.run(
        ['javac', '-d', str(BIN_DIR), '-sourcepath', str(src_dir),
         str(src_dir / 'main' / 'App.java')],
        capture_output=True,
        text=True
    )

    if result.returncode != 0:
        print("ERRO na compilação:")
        print(result.stderr)
        return False

    print("✓ Compilação concluída com sucesso!\n")
    return True


def run_instance(instance_number):
    """Executa uma instância específica do algoritmo."""
    result = subprocess.run(
        ['java', '-cp', str(BIN_DIR), 'main.App', str(instance_number)],
        capture_output=True,
        text=True,
        cwd=str(PROJECT_ROOT)
    )

    return result.returncode == 0, result.stdout, result.stderr


def extract_best_distance(instance_name):
    """Extrai a melhor distância do arquivo de resultados."""
    result_file = RESULTS_MULTI_DIR / f'evo_{instance_name.lower()}.txt'

    if not result_file.exists():
        return None

    try:
        with open(result_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        # Procurar linha subPopDistance
        for line in lines:
            if line.startswith('subPopDistance'):
                # Pegar o último valor (geração final)
                values = line.strip().split('\t')[1:]  # Remove o cabeçalho
                if values:
                    # Último valor não-vazio
                    last_value = [v for v in values if v.strip()][-1]
                    # Converter vírgula para ponto e remover espaços
                    distance = float(last_value.replace(',', '.').strip())
                    return distance
    except Exception as e:
        print(f"Erro ao extrair distância: {e}")
        return None

    return None


def compute_statistics(distances):
    """Calcula estatísticas das distâncias."""
    if not distances:
        return None, None, None, None

    best = min(distances)
    worst = max(distances)
    mean = sum(distances) / len(distances)

    # Desvio padrão
    variance = sum((d - mean) ** 2 for d in distances) / len(distances)
    std_dev = math.sqrt(variance)

    return best, worst, mean, std_dev


def save_results_table(instance_name, distances):
    """Salva tabela de resultados para uma instância."""
    instance_dir = RESULTS_BASE_DIR / instance_name
    instance_dir.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    output_file = instance_dir / f'resultados_aemmt_{timestamp}.txt'

    best, worst, mean, std_dev = compute_statistics(distances)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(f"Resultados AEMMT - {instance_name}\n")
        f.write("=" * 60 + "\n\n")
        f.write(f"Total de execuções: {len(distances)}\n\n")

        f.write("Execução | Melhor Distância Encontrada\n")
        f.write("-" * 40 + "\n")

        for i, distance in enumerate(distances, 1):
            f.write(f"{i:8d} | {distance:8.2f}\n")

        f.write("\n")
        f.write("Estatísticas:\n")
        f.write("-" * 40 + "\n")
        f.write(f"Melhor distância encontrada: {best:.2f}\n")
        f.write(f"Pior distância encontrada: {worst:.2f}\n")
        f.write(f"Distância média geral: {mean:.2f}\n")
        f.write(f"Desvio padrão das distâncias: {std_dev:.2f}\n")

    print(f"  → Resultados salvos em: {output_file}")
    return output_file


def move_result_files(instance_name, execution_num):
    """Move arquivos de resultado para o diretório da instância."""
    instance_dir = RESULTS_BASE_DIR / instance_name
    instance_dir.mkdir(parents=True, exist_ok=True)

    # Mover arquivo de evolução
    evo_file = RESULTS_MULTI_DIR / f'evo_{instance_name.lower()}.txt'
    if evo_file.exists():
        dest = instance_dir / \
            f'evo_{instance_name.lower()}_exec{execution_num:02d}.txt'
        evo_file.rename(dest)

    # Mover arquivo de estatísticas
    stats_file = RESULTS_MULTI_DIR / 'stats' / \
        f'stats_{instance_name.lower()}.txt'
    if stats_file.exists():
        dest = instance_dir / \
            f'stats_{instance_name.lower()}_exec{execution_num:02d}.txt'
        stats_file.rename(dest)


def run_validation():
    """Executa validação completa para todas as instâncias RC1."""
    print("\n" + "=" * 60)
    print("VALIDAÇÃO AEMMT - INSTÂNCIAS RC1 (RC101-RC108)")
    print("=" * 60 + "\n")

    # Compilar projeto
    if not compile_project():
        print("Abortando execução devido a erro de compilação.")
        return

    # Criar diretório base de resultados
    RESULTS_BASE_DIR.mkdir(parents=True, exist_ok=True)

    total_instances = len(INSTANCES)
    current_instance = 0

    # Executar cada instância
    for instance_name, instance_number in INSTANCES.items():
        current_instance += 1
        distances = []

        print("\n" + "=" * 60)
        print(f"[{current_instance}/{total_instances}] Instância: {instance_name}")
        print("=" * 60)

        # Executar 10 vezes
        for execution in range(1, NUM_EXECUTIONS + 1):
            print(
                f"\n  Execução {execution}/{NUM_EXECUTIONS}...", end=" ", flush=True)

            success, stdout, stderr = run_instance(instance_number)

            if not success:
                print("❌ FALHOU")
                print(f"    Erro: {stderr}")
                continue

            # Extrair melhor distância
            distance = extract_best_distance(instance_name)

            if distance is not None:
                distances.append(distance)
                print(f"✓ Distância: {distance:.2f}")

                # Mover arquivos de resultado
                move_result_files(instance_name, execution)
            else:
                print("⚠️  Não foi possível extrair distância")

        # Salvar tabela de resultados
        if distances:
            print(f"\n  Processando resultados de {instance_name}...")
            save_results_table(instance_name, distances)

            # Mostrar estatísticas resumidas
            best, worst, mean, std_dev = compute_statistics(distances)
            print(f"\n  📊 Resumo {instance_name}:")
            print(f"     Melhor: {best:.2f}")
            print(f"     Média: {mean:.2f}")
            print(f"     Desvio: {std_dev:.2f}")
        else:
            print(f"\n  ⚠️  Nenhum resultado válido para {instance_name}")

    print("\n" + "=" * 60)
    print("VALIDAÇÃO CONCLUÍDA!")
    print("=" * 60)
    print(f"\nResultados salvos em: {RESULTS_BASE_DIR}")
    print("\nEstrutura de diretórios:")
    for instance_name in INSTANCES.keys():
        instance_dir = RESULTS_BASE_DIR / instance_name
        if instance_dir.exists():
            files = list(instance_dir.glob('*'))
            print(f"  {instance_name}/: {len(files)} arquivos")


if __name__ == '__main__':
    try:
        run_validation()
    except KeyboardInterrupt:
        print("\n\n⚠️  Execução interrompida pelo usuário.")
    except Exception as e:
        print(f"\n\n❌ Erro durante execução: {e}")
        import traceback
        traceback.print_exc()
