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


def compare_results():
    """Compara resultados mono e multi-objetivo e gera gráficos"""
    # Criar diretório para os gráficos comparativos
    output_dir = 'compareMultiMono'
    os.makedirs(output_dir, exist_ok=True)
    
    # Obter listas de arquivos de resultados
    mono_files = sorted(glob.glob('resultsMono/mono_results_*.txt'))
    multi_files = sorted(glob.glob('resultsMulti/evolution_results_*.txt'))
    
    if not mono_files:
        print("Nenhum arquivo encontrado em resultsMono/")
        return
    
    if not multi_files:
        print("Nenhum arquivo encontrado em resultsMulti/")
        return
    
    print(f"Encontrados {len(mono_files)} arquivos mono e {len(multi_files)} arquivos multi.")
    
    # Agrupar arquivos por timestamp se possível
    timestamp_mono = {extract_timestamp(f): f for f in mono_files}
    timestamp_multi = {extract_timestamp(f): f for f in multi_files}
    
    # Mapear comparações
    comparisons = []
    
    # Primeiro, tenta combinar por timestamp
    for ts in timestamp_mono:
        if ts in timestamp_multi:
            comparisons.append((timestamp_mono[ts], timestamp_multi[ts], f"timestamp_{ts}"))
    
    # Para arquivos restantes, combina na ordem
    remaining_mono = [f for f in mono_files if extract_timestamp(f) not in [c[2] for c in comparisons]]
    remaining_multi = [f for f in multi_files if extract_timestamp(f) not in [c[2] for c in comparisons]]
    
    for i in range(min(len(remaining_mono), len(remaining_multi))):
        comparisons.append((remaining_mono[i], remaining_multi[i], f"comparison_{i+1}"))
    
    # Se não houver correspondências, compara os arquivos na ordem
    if not comparisons:
        for i in range(min(len(mono_files), len(multi_files))):
            comparisons.append((mono_files[i], multi_files[i], f"comparison_{i+1}"))
    
    # Gerar gráficos comparativos
    for mono_file, multi_file, comp_id in comparisons:
        print(f"Comparando: {os.path.basename(mono_file)} vs {os.path.basename(multi_file)}")
        
        # Ler dados
        mono_generations, mono_values = read_mono_results(mono_file)
        multi_generations, multi_values = read_multi_results(multi_file)
        
        # Verificar se temos dados válidos
        if not mono_values or not multi_values:
            print(f"  Erro: Dados insuficientes em um dos arquivos.")
            continue
            
        # Criar gráfico
        plt.figure(figsize=(14, 8))
        
        # Verificar se as gerações são as mesmas, se não, usar índices
        use_generations = (len(mono_generations) == len(multi_generations) and 
                          all(mg == sg for mg, sg in zip(mono_generations, multi_generations)))
        
        if use_generations:
            plt.plot(mono_generations, mono_values, 'o-', color='blue', label='Mono-Objetivo')
            plt.plot(multi_generations, multi_values, 'o-', color='red', label='Multi-Objetivo (Ponderação)')
            plt.xlabel('Geração')
        else:
            # Se as gerações forem diferentes, normalizar para índices percentuais
            mono_indices = [i/(len(mono_values)-1) * 100 for i in range(len(mono_values))]
            multi_indices = [i/(len(multi_values)-1) * 100 for i in range(len(multi_values))]
            
            plt.plot(mono_indices, mono_values, 'o-', color='blue', label='Mono-Objetivo')
            plt.plot(multi_indices, multi_values, 'o-', color='red', label='Multi-Objetivo (Ponderação)')
            plt.xlabel('Progresso (%)') 
        
        # Configurar gráfico
        plt.title('Comparação Mono vs Multi-Objetivo (Ponderação)')
        plt.ylabel('Fitness (menor é melhor)')
        plt.grid(True, linestyle='--', alpha=0.7)
        plt.legend()
        
        # Adicionar informações sobre os arquivos
        plt.figtext(0.01, 0.01, f'Mono: {os.path.basename(mono_file)}\nMulti: {os.path.basename(multi_file)}', 
                   fontsize=8, wrap=True)
        
        # Salvar gráfico
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_file = os.path.join(output_dir, f'compare_{comp_id}_{timestamp}.png')
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        plt.close()
        
        print(f"  Gráfico comparativo salvo em: {output_file}")


if __name__ == "__main__":
    compare_results()
    print("\nProcessamento concluído! Verifique a pasta 'compareMultiMono' para ver os gráficos gerados.")