import os
import matplotlib.pyplot as plt
import glob


def read_results_file(file_path):
    """Lê um arquivo de resultados mono-objetivo e retorna os dados estruturados."""
    with open(file_path, 'r') as f:
        lines = f.readlines()

    # Extrair gerações (números) do cabeçalho
    generations = [int(g.replace('g', ''))
                   for g in lines[0].strip().split('\t')[1:] if g.strip()]

    # Extrair dados da população mono-objetivo (segunda linha)
    data = {}
    if len(lines) > 1:
        parts = lines[1].strip().split('\t')
        if len(parts) > 1:
            pop_name = parts[0]
            # Converter string para float, substituindo vírgula por ponto
            values = [float(v.replace(',', '.'))
                      for v in parts[1:] if v.strip()]
            data[pop_name] = values

    return generations, data


def plot_results(file_path):
    """Plota os resultados de um arquivo mono-objetivo e salva o gráfico."""
    try:
        generations, data = read_results_file(file_path)

        # Extrai o nome do arquivo sem caminho e extensão
        file_name = os.path.basename(file_path).replace('.txt', '')

        # Cria um novo gráfico
        fig, ax = plt.subplots(figsize=(12, 8))
        fig.suptitle(f'Evolução do Fitness ao Longo das Gerações (Mono-Objetivo)\n{file_name}', fontsize=16)
        
        # Define um layout mais compacto
        plt.tight_layout(rect=[0, 0.03, 1, 0.95])

        # Cor para a população mono-objetivo
        color = 'purple'

        # Obtém o nome da população (geralmente será "Mono-Objetivo")
        pop_name = list(data.keys())[0] if data else "Mono-Objetivo"
        values = data.get(pop_name, [])

        # Plota os dados se existirem
        if len(generations) == len(values):
            ax.plot(generations, values, marker='o', linestyle='-',
                   color=color, label=pop_name)
            
            # Configurações do gráfico
            ax.set_title(f'Evolução do Fitness {pop_name}')
            ax.set_xlabel('Geração')
            ax.set_ylabel('Fitness (menor é melhor)')
            ax.grid(True, linestyle='--', alpha=0.7)
            
            # Ajusta os limites do eixo Y para começar um pouco abaixo do menor valor
            if values:
                y_min = min(values) * 0.95
                y_max = max(values) * 1.05
                ax.set_ylim(y_min, y_max)
        
        # Cria diretório para salvar os gráficos
        plots_dir = os.path.join('resultsMono', 'plots')
        os.makedirs(plots_dir, exist_ok=True)

        # Salva o gráfico
        output_path = os.path.join(plots_dir, f'{file_name}_plot.png')
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f'Gráfico salvo em: {output_path}')

        plt.close()

    except Exception as e:
        print(f"Erro ao processar o arquivo {file_path}: {e}")
        import traceback
        traceback.print_exc()  # Mostra o rastreamento completo do erro


def main():
    # Diretório onde os resultados mono-objetivo estão armazenados
    results_dir = 'resultsMono'

    # Verifica se o diretório de resultados existe
    if not os.path.exists(results_dir):
        print(f"Diretório {results_dir} não encontrado!")
        return

    # Encontra todos os arquivos .txt no diretório de resultados
    result_files = glob.glob(os.path.join(
        results_dir, 'mono_results_*.txt'))

    if not result_files:
        print(f"Nenhum arquivo de resultados encontrado em {results_dir}")
        return

    print(f"Encontrados {len(result_files)} arquivos de resultados.")

    # Processa cada arquivo
    for file_path in result_files:
        print(f"Processando: {file_path}")
        plot_results(file_path)

    print("\nProcessamento concluído! Verifique a pasta 'resultsMono/plots' para ver os gráficos gerados.")


if __name__ == "__main__":
    main()