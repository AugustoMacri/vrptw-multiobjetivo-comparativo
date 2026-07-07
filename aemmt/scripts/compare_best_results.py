import os
import matplotlib.pyplot as plt
import glob
import re
from datetime import datetime


def extract_timestamp(filename):
    """Extrai o timestamp do nome do arquivo"""
    match = re.search(r'(\d{8}_\d{6})', filename)
    if match:
        return match.group(1)
    return None


def read_mono_results(file_path):
    """Lê um arquivo de resultados mono-objetivo"""
    with open(file_path, 'r') as f:
        lines = f.readlines()

    generations = [int(g.replace('g', ''))
                   for g in lines[0].strip().split('\t')[1:] if g.strip()]

    mono_values = []
    if len(lines) > 1:
        parts = lines[1].strip().split('\t')
        if len(parts) > 1:
            mono_values = [float(v.replace(',', '.'))
                           for v in parts[1:] if v.strip()]

    return generations, mono_values


def read_multi_results(file_path):
    """Lê um arquivo de resultados multi-objetivo e extrai apenas os valores de ponderação"""
    with open(file_path, 'r') as f:
        lines = f.readlines()

    generations = [int(g.replace('g', ''))
                   for g in lines[0].strip().split('\t')[1:] if g.strip()]

    ponderation_values = []
    for line in lines[1:]:
        parts = line.strip().split('\t')
        if len(parts) > 1 and 'subPopPonderation' in parts[0]:
            ponderation_values = [float(v.replace(',', '.'))
                                  for v in parts[1:] if v.strip()]
            break

    return generations, ponderation_values


def find_best_result(files, read_function):
    """Encontra o arquivo com o melhor resultado final (menor fitness)"""
    best_file = None
    best_fitness = float('inf')
    best_generations = []
    best_values = []

    for file_path in files:
        generations, values = read_function(file_path)

        if values and values[-1] < best_fitness:
            best_fitness = values[-1]
            best_file = file_path
            best_generations = generations
            best_values = values

    return best_file, best_generations, best_values, best_fitness


def compare_best_results():
    """Compara apenas os melhores resultados mono e multi-objetivo"""
    # Determinar diretório base (projeto) a partir da localização do script
    script_dir = os.path.dirname(os.path.realpath(__file__))
    base_dir = os.path.abspath(os.path.join(script_dir, '..'))

    # Criar diretório para os gráficos comparativos (no diretório do projeto)
    output_dir = os.path.join(base_dir, 'compareBestResults')
    os.makedirs(output_dir, exist_ok=True)

    # Obter listas de arquivos de resultados (caminhos absolutos, a partir do projeto)
    mono_files = sorted(glob.glob(os.path.join(
        base_dir, 'resultsMono', 'mono_results_*.txt')))
    multi_files = sorted(glob.glob(os.path.join(
        base_dir, 'resultsMulti', 'evolution_results_*.txt')))

    if not mono_files:
        print("Nenhum arquivo encontrado em resultsMono/")
        return

    if not multi_files:
        print("Nenhum arquivo encontrado em resultsMulti/")
        return

    print(
        f"Analisando {len(mono_files)} arquivos mono e {len(multi_files)} arquivos multi...")

    # Encontrar o melhor resultado para cada tipo
    best_mono_file, best_mono_generations, best_mono_values, best_mono_fitness = find_best_result(
        mono_files, read_mono_results)

    best_multi_file, best_multi_generations, best_multi_values, best_multi_fitness = find_best_result(
        multi_files, read_multi_results)

    print("\n=== MELHORES RESULTADOS IDENTIFICADOS ===")
    print(f"Melhor Mono-Objetivo: {os.path.basename(best_mono_file)}")
    print(f"Fitness final: {best_mono_fitness:.2f}")
    print(
        f"\nMelhor Multi-Objetivo (Ponderação): {os.path.basename(best_multi_file)}")
    print(f"Fitness final: {best_multi_fitness:.2f}")

    # Determinar qual abordagem teve melhor desempenho
    if best_mono_fitness < best_multi_fitness:
        winner = "Mono-Objetivo"
        improvement = (best_multi_fitness - best_mono_fitness) / \
            best_multi_fitness * 100
    else:
        winner = "Multi-Objetivo"
        improvement = (best_mono_fitness - best_multi_fitness) / \
            best_mono_fitness * 100

    print(f"\nMelhor desempenho: {winner}")
    print(f"Melhoria: {improvement:.2f}%")

    # Criar gráfico comparativo dos melhores resultados
    plt.figure(figsize=(14, 8))

    # Verificar se as gerações são as mesmas, se não, usar índices
    use_generations = (len(best_mono_generations) == len(best_multi_generations) and
                       all(mg == sg for mg, sg in zip(best_mono_generations, best_multi_generations)))

    if use_generations:
        plt.plot(best_mono_generations, best_mono_values, 'o-', color='blue', linewidth=2,
                 label=f'Melhor Mono-Objetivo (Final: {best_mono_fitness:.2f})')
        plt.plot(best_multi_generations, best_multi_values, 'o-', color='red', linewidth=2,
                 label=f'Melhor Multi-Objetivo (Final: {best_multi_fitness:.2f})')
        plt.xlabel('Geração')
    else:
        # Se as gerações forem diferentes, normalizar para índices percentuais
        mono_indices = [i/(len(best_mono_values)-1) *
                        100 for i in range(len(best_mono_values))]
        multi_indices = [i/(len(best_multi_values)-1) *
                         100 for i in range(len(best_multi_values))]

        plt.plot(mono_indices, best_mono_values, 'o-', color='blue', linewidth=2,
                 label=f'Melhor Mono-Objetivo (Final: {best_mono_fitness:.2f})')
        plt.plot(multi_indices, best_multi_values, 'o-', color='red', linewidth=2,
                 label=f'Melhor Multi-Objetivo (Final: {best_multi_fitness:.2f})')
        plt.xlabel('Progresso (%)')

    # Configurar gráfico
    plt.title(
        'Comparação dos Melhores Resultados: Mono vs Multi-Objetivo', fontsize=16)
    # Renomeado de "Fitness (menor é melhor)" para "Fitness"
    plt.ylabel('Fitness', fontsize=14)
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.legend(fontsize=12)

    # A anotação sobre o vencedor foi removida aqui

    # Adicionar informações sobre os arquivos
    plt.figtext(0.02, 0.92,
                f'Mono: {os.path.basename(best_mono_file)}\nMulti: {os.path.basename(best_multi_file)}',
                fontsize=10, bbox=dict(boxstyle="round,pad=0.3", fc="lightgrey", alpha=0.3))

    # Salvar gráfico dos melhores
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = os.path.join(
        output_dir, f'best_results_comparison_{timestamp}.png')
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    plt.close()

    print(
        f"\nGráfico comparativo dos melhores resultados salvo em: {output_file}")

    # Salvar os dados em um arquivo de texto
    summary_file = os.path.join(
        output_dir, f'best_results_summary_{timestamp}.txt')
    with open(summary_file, 'w') as f:
        f.write("=== COMPARAÇÃO DOS MELHORES RESULTADOS ===\n\n")
        f.write(
            f"Data da análise: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")

        f.write("MELHOR RESULTADO MONO-OBJETIVO\n")
        f.write(f"Arquivo: {os.path.basename(best_mono_file)}\n")
        f.write(f"Fitness final: {best_mono_fitness:.2f}\n\n")

        f.write("MELHOR RESULTADO MULTI-OBJETIVO (PONDERAÇÃO)\n")
        f.write(f"Arquivo: {os.path.basename(best_multi_file)}\n")
        f.write(f"Fitness final: {best_multi_fitness:.2f}\n\n")

        f.write(f"COMPARAÇÃO\n")
        f.write(f"Melhor desempenho: {winner}\n")
        f.write(f"Melhoria percentual: {improvement:.2f}%\n")

    print(f"Resumo detalhado salvo em: {summary_file}")


if __name__ == "__main__":
    compare_best_results()
    print("\nProcessamento concluído! Verifique a pasta 'compareBestResults' para ver os resultados.")
