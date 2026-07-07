import os
import glob
import re
import pandas as pd
from datetime import datetime

def extract_timestamp(filename):
    """Extrai o timestamp do nome do arquivo"""
    match = re.search(r'(\d{8}_\d{6})', filename)
    if match:
        return match.group(1)
    return None

def extract_mono_stats(file_path):
    """Extrai estatísticas do arquivo de estatísticas mono-objetivo"""
    try:
        with open(file_path, 'r') as f:
            lines = f.readlines()
        
        # Procura as linhas com as métricas específicas
        best_fitness = None
        avg_fitness = None
        std_deviation = None
        
        for line in lines:
            if "Melhor Fitness:" in line:
                best_fitness = float(line.split(":")[1].strip().replace(',', '.'))
            elif "Fitness Médio:" in line:
                avg_fitness = float(line.split(":")[1].strip().replace(',', '.'))
            elif "Desvio Padrão:" in line:
                std_deviation = float(line.split(":")[1].strip().replace(',', '.'))
        
        timestamp = extract_timestamp(file_path)
        return timestamp, best_fitness, avg_fitness, std_deviation
    
    except Exception as e:
        print(f"Erro ao processar {file_path}: {e}")
        return None, None, None, None

def extract_multi_stats(file_path):
    """Extrai estatísticas do arquivo de estatísticas multi-objetivo (apenas ponderação)"""
    try:
        with open(file_path, 'r') as f:
            lines = f.readlines()
        
        # Procura as linhas com as métricas específicas
        best_fitness = None
        avg_fitness = None
        std_deviation = None
        
        # No formato simplificado (apenas subpopulação de ponderação)
        for line in lines:
            if "Melhor Fitness:" in line:
                best_fitness = float(line.split(":")[1].strip().replace(',', '.'))
            elif "Fitness Médio:" in line:
                avg_fitness = float(line.split(":")[1].strip().replace(',', '.'))
            elif "Desvio Padrão:" in line:
                std_deviation = float(line.split(":")[1].strip().replace(',', '.'))
        
        timestamp = extract_timestamp(file_path)
        return timestamp, best_fitness, avg_fitness, std_deviation
    
    except Exception as e:
        print(f"Erro ao processar {file_path}: {e}")
        return None, None, None, None

def compile_mono_stats():
    """Compila estatísticas dos arquivos mono-objetivo"""
    stats_files = glob.glob('resultsMono/stats/mono_stats_*.txt')
    
    if not stats_files:
        print("Nenhum arquivo de estatísticas mono-objetivo encontrado.")
        return
    
    print(f"Encontrados {len(stats_files)} arquivos de estatísticas mono-objetivo.")
    
    # Lista para armazenar os dados
    data = []
    
    # Processa cada arquivo
    for i, file_path in enumerate(sorted(stats_files)):
        timestamp, best_fitness, avg_fitness, std_deviation = extract_mono_stats(file_path)
        
        if best_fitness is not None:
            data.append({
                'Execução': i+1,
                'Timestamp': timestamp,
                'Melhor Fitness': best_fitness,
                'Fitness Médio': avg_fitness,
                'Desvio Padrão': std_deviation,
                'Arquivo': os.path.basename(file_path)
            })
    
    # Cria DataFrame
    df = pd.DataFrame(data)
    
    # Cria diretório final_stats se não existir
    os.makedirs('resultsMono/final_stats', exist_ok=True)
    
    # Salva como CSV
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    csv_path = f'resultsMono/final_stats/mono_stats_table_{timestamp}.csv'
    df.to_csv(csv_path, index=False)
    
    # Salva como TXT formatado
    txt_path = f'resultsMono/final_stats/mono_stats_table_{timestamp}.txt'
    
    with open(txt_path, 'w') as f:
        f.write("Estatísticas das Execuções Mono-Objetivo\n")
        f.write("=======================================\n\n")
        f.write(f"Total de execuções analisadas: {len(data)}\n\n")
        
        # Escreve cabeçalho da tabela
        f.write(f"{'Execução':<10} | {'Melhor Fitness':<15} | {'Fitness Médio':<15} | {'Desvio Padrão':<15} | {'Arquivo':<30}\n")
        f.write("-" * 95 + "\n")
        
        # Escreve dados
        for row in data:
            f.write(f"{row['Execução']:<10} | {row['Melhor Fitness']:<15.2f} | {row['Fitness Médio']:<15.2f} | {row['Desvio Padrão']:<15.2f} | {row['Arquivo']:<30}\n")
        
        # Adiciona estatísticas gerais
        if data:
            f.write("\nEstatísticas Gerais:\n")
            f.write(f"Melhor fitness encontrado: {min(row['Melhor Fitness'] for row in data):.2f}\n")
            f.write(f"Pior fitness encontrado: {max(row['Melhor Fitness'] for row in data):.2f}\n")
            f.write(f"Fitness médio geral: {sum(row['Melhor Fitness'] for row in data) / len(data):.2f}\n")
            f.write(f"Média dos fitness médios: {sum(row['Fitness Médio'] for row in data) / len(data):.2f}\n")
            f.write(f"Média dos desvios padrão: {sum(row['Desvio Padrão'] for row in data) / len(data):.2f}\n")
    
    print(f"Tabela de estatísticas mono-objetivo salva em: {csv_path}")
    print(f"Relatório de estatísticas mono-objetivo salvo em: {txt_path}")

def compile_multi_stats():
    """Compila estatísticas dos arquivos multi-objetivo"""
    stats_files = glob.glob('resultsMulti/stats/multi_stats_*.txt')
    
    if not stats_files:
        print("Nenhum arquivo de estatísticas multi-objetivo encontrado.")
        return
    
    print(f"Encontrados {len(stats_files)} arquivos de estatísticas multi-objetivo.")
    
    # Lista para armazenar os dados
    data = []
    
    # Processa cada arquivo
    for i, file_path in enumerate(sorted(stats_files)):
        timestamp, best_fitness, avg_fitness, std_deviation = extract_multi_stats(file_path)
        
        if best_fitness is not None:
            data.append({
                'Execução': i+1,
                'Timestamp': timestamp,
                'Melhor Fitness': best_fitness,
                'Fitness Médio': avg_fitness,
                'Desvio Padrão': std_deviation,
                'Arquivo': os.path.basename(file_path)
            })
    
    # Verifica se temos dados
    if not data:
        print("Não foi possível extrair dados dos arquivos multi-objetivo.")
        return
    
    # Cria DataFrame
    df = pd.DataFrame(data)
    
    # Cria diretório final_stats se não existir
    os.makedirs('resultsMulti/final_stats', exist_ok=True)
    
    # Salva como CSV
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    csv_path = f'resultsMulti/final_stats/multi_stats_table_{timestamp}.csv'
    df.to_csv(csv_path, index=False)
    
    # Salva como TXT formatado
    txt_path = f'resultsMulti/final_stats/multi_stats_table_{timestamp}.txt'
    
    with open(txt_path, 'w') as f:
        f.write("Estatísticas das Execuções Multi-Objetivo (Subpopulação de Ponderação)\n")
        f.write("==================================================================\n\n")
        f.write(f"Total de execuções analisadas: {len(data)}\n\n")
        
        # Escreve cabeçalho da tabela
        f.write(f"{'Execução':<10} | {'Melhor Fitness':<15} | {'Fitness Médio':<15} | {'Desvio Padrão':<15} | {'Arquivo':<30}\n")
        f.write("-" * 95 + "\n")
        
        # Escreve dados
        for row in data:
            f.write(f"{row['Execução']:<10} | {row['Melhor Fitness']:<15.2f} | {row['Fitness Médio']:<15.2f} | {row['Desvio Padrão']:<15.2f} | {row['Arquivo']:<30}\n")
        
        # Adiciona estatísticas gerais
        if data:
            f.write("\nEstatísticas Gerais:\n")
            f.write(f"Melhor fitness encontrado: {min(row['Melhor Fitness'] for row in data):.2f}\n")
            f.write(f"Pior fitness encontrado: {max(row['Melhor Fitness'] for row in data):.2f}\n")
            f.write(f"Fitness médio geral: {sum(row['Melhor Fitness'] for row in data) / len(data):.2f}\n")
            f.write(f"Média dos fitness médios: {sum(row['Fitness Médio'] for row in data) / len(data):.2f}\n")
            f.write(f"Média dos desvios padrão: {sum(row['Desvio Padrão'] for row in data) / len(data):.2f}\n")
    
    print(f"Tabela de estatísticas multi-objetivo salva em: {csv_path}")
    print(f"Relatório de estatísticas multi-objetivo salvo em: {txt_path}")

def main():
    print("=== Compilando Estatísticas de Execuções ===")
    
    # Compila estatísticas mono-objetivo
    print("\nProcessando arquivos mono-objetivo...")
    compile_mono_stats()
    
    # Compila estatísticas multi-objetivo
    print("\nProcessando arquivos multi-objetivo...")
    compile_multi_stats()
    
    print("\nProcessamento concluído!")

if __name__ == "__main__":
    main()