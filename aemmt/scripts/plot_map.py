#!/usr/bin/env python3
import os
import sys
import glob
import matplotlib.pyplot as plt
import numpy as np
import argparse


def list_instances():
    """Lista as instâncias disponíveis e permite escolher uma"""
    instance_dir = "src/instances/solomon/"
    # Verifica se o diretório existe
    if not os.path.exists(instance_dir):
        instance_dir = "bin/instances/solomon/"  # Tenta outra localização comum
        if not os.path.exists(instance_dir):
            print("Diretório de instâncias não encontrado.")
            return None

    # Lista todos os arquivos .txt no diretório
    instance_files = sorted(glob.glob(os.path.join(instance_dir, "*.txt")))

    if not instance_files:
        print("Nenhuma instância encontrada.")
        return None

    # Mostra as instâncias disponíveis
    print("\nInstâncias disponíveis:")
    for i, file_path in enumerate(instance_files):
        file_name = os.path.basename(file_path)
        print(f"{i+1}. {file_name}")

    # Solicita a escolha do usuário
    choice = -1
    while choice < 1 or choice > len(instance_files):
        try:
            choice = int(
                input(f"\nEscolha uma instância (1-{len(instance_files)}): "))
            if choice < 1 or choice > len(instance_files):
                print(
                    f"Por favor, escolha um número entre 1 e {len(instance_files)}.")
        except ValueError:
            print("Por favor, digite um número válido.")

    return instance_files[choice-1]


def read_instance_coordinates(file_path):
    """Lê as coordenadas dos clientes de um arquivo de instância"""
    coordinates = []

    try:
        with open(file_path, 'r') as f:
            lines = f.readlines()

        # Procura a seção CUSTOMER
        customer_section = False
        header_passed = False

        for line in lines:
            line = line.strip()

            # Identifica o início da seção CUSTOMER
            if "CUSTOMER" in line:
                customer_section = True
                continue

            # Pula linhas de cabeçalho
            if customer_section and not header_passed:
                if "CUST NO." in line:
                    header_passed = True
                continue

            # Processa linhas de dados
            if customer_section and header_passed and line:
                # Divide a linha em partes, ignorando espaços extras
                parts = line.split()
                if len(parts) >= 3:  # Verifica se há pelo menos cust_no, x, y
                    try:
                        # O formato esperado é: CUST_NO X_COORD Y_COORD ...
                        cust_no = int(parts[0])
                        x_coord = float(parts[1])
                        y_coord = float(parts[2])
                        coordinates.append((cust_no, x_coord, y_coord))
                    except (ValueError, IndexError):
                        # Ignora linhas que não podem ser convertidas
                        pass

    except Exception as e:
        print(f"Erro ao ler o arquivo {file_path}: {e}")

    return coordinates


def plot_coordinates(coordinates, instance_name, output_dir="mapping_results"):
    """Plota as coordenadas em um plano cartesiano e salva na pasta especificada"""
    if not coordinates:
        print(f"⚠️  Nenhuma coordenada encontrada para {instance_name}")
        return False

    # Extrair coordenadas para plotagem
    cust_nos = [c[0] for c in coordinates]
    x_coords = [c[1] for c in coordinates]
    y_coords = [c[2] for c in coordinates]

    # Destacar o depósito (cliente 0)
    depot_index = cust_nos.index(0) if 0 in cust_nos else None

    # Configurar o plot
    plt.figure(figsize=(10, 8))

    # Plotar todos os clientes
    plt.scatter(x_coords, y_coords, c='blue', marker='o',
                s=50, alpha=0.7, label='Clientes')

    # Destacar o depósito
    if depot_index is not None:
        plt.scatter(x_coords[depot_index], y_coords[depot_index],
                    c='red', marker='s', s=100, label='Depósito')

    # Configurações do gráfico
    # plt.title(
    #     f'Mapa de Clientes - Instância {instance_name}', fontsize=16, fontweight='bold')
    # plt.xlabel('Coordenada X', fontsize=12)
    # plt.ylabel('Coordenada Y', fontsize=12)
    plt.grid(True, linestyle='--', alpha=0.7)
    # plt.legend(fontsize=10)

    # Ajustar limites para visualização adequada (margem de 10%)
    x_min, x_max = min(x_coords), max(x_coords)
    y_min, y_max = min(y_coords), max(y_coords)
    x_margin = (x_max - x_min) * 0.1 if x_max != x_min else 10
    y_margin = (y_max - y_min) * 0.1 if y_max != y_min else 10

    plt.xlim(x_min - x_margin, x_max + x_margin)
    plt.ylim(y_min - y_margin, y_max + y_margin)

    # Criar diretório para salvar o mapa se não existir
    os.makedirs(output_dir, exist_ok=True)

    # Gerar nome de arquivo
    save_path = os.path.join(output_dir, f"map_{instance_name}.png")

    # Salvar a figura
    plt.tight_layout()
    plt.savefig(save_path, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"  ✓ Mapa salvo: {save_path}")
    return True


def process_instance(instance_path, output_dir="mapping_results"):
    """Processa uma única instância e gera o mapa"""
    instance_name = os.path.basename(instance_path).replace('.txt', '').upper()

    coordinates = read_instance_coordinates(instance_path)

    if coordinates:
        return plot_coordinates(coordinates, instance_name, output_dir)
    else:
        print(f"  ✗ Não foi possível extrair coordenadas de {instance_name}")
        return False


def process_multiple_instances(pattern, instances_dir, output_dir):
    """Processa múltiplas instâncias baseado em um padrão"""
    instance_files = sorted(
        glob.glob(os.path.join(instances_dir, f"{pattern}.txt")))

    if not instance_files:
        print(f"✗ Nenhuma instância encontrada com padrão: {pattern}")
        return 0

    print(f"\n{'='*60}")
    print(f"Processando {len(instance_files)} instâncias ({pattern})")
    print(f"{'='*60}\n")

    success_count = 0
    for instance_path in instance_files:
        instance_name = os.path.basename(
            instance_path).replace('.txt', '').upper()
        print(f"[{success_count+1}/{len(instance_files)}] {instance_name}...")

        if process_instance(instance_path, output_dir):
            success_count += 1

    print(f"\n{'='*60}")
    print(f"Resumo: ✓ {success_count}/{len(instance_files)} mapas gerados")
    print(f"Diretório de saída: {output_dir}")
    print(f"{'='*60}\n")

    return success_count


def main():
    parser = argparse.ArgumentParser(
        description='Gera mapas de coordenadas de instâncias VRP',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemplos de uso:
  %(prog)s --instance C101                    # Gera mapa para C101
  %(prog)s --all-c1                           # Gera mapas para todas C1 (C101-C109)
  %(prog)s --all-r1                           # Gera mapas para todas R1 (R101-R112)
  %(prog)s --all-rc1                          # Gera mapas para todas RC1 (RC101-RC108)
  %(prog)s --all                              # Gera mapas para TODAS as instâncias
  %(prog)s --instance C101 --output-dir maps  # Especifica diretório de saída
        """
    )

    parser.add_argument('--instance', type=str,
                        help='Nome da instância específica (ex: C101, R101, RC101)')
    parser.add_argument('--all-c1', action='store_true',
                        help='Processar todas as instâncias C1 (C101-C109)')
    parser.add_argument('--all-r1', action='store_true',
                        help='Processar todas as instâncias R1 (R101-R112)')
    parser.add_argument('--all-rc1', action='store_true',
                        help='Processar todas as instâncias RC1 (RC101-RC108)')
    parser.add_argument('--all', action='store_true',
                        help='Processar TODAS as instâncias (C1, R1, RC1)')
    parser.add_argument('--instances-dir', type=str,
                        default='src/instances/solomon',
                        help='Diretório com os arquivos das instâncias')
    parser.add_argument('--output-dir', type=str,
                        default='mapping_results',
                        help='Diretório para salvar os mapas gerados')

    args = parser.parse_args()

    # Verificar se o diretório de instâncias existe
    if not os.path.exists(args.instances_dir):
        alt_dir = "bin/instances/solomon"
        if os.path.exists(alt_dir):
            args.instances_dir = alt_dir
            print(f"ℹ️  Usando diretório alternativo: {alt_dir}\n")
        else:
            print(
                f"✗ Diretório de instâncias não encontrado: {args.instances_dir}")
            return 1

    # Processar baseado nos argumentos
    if args.all:
        print("\n" + "="*60)
        print("Gerando mapas para TODAS as instâncias")
        print("="*60)
        total = 0
        total += process_multiple_instances("C1*",
                                            args.instances_dir, args.output_dir)
        total += process_multiple_instances("R1*",
                                            args.instances_dir, args.output_dir)
        total += process_multiple_instances("RC1*",
                                            args.instances_dir, args.output_dir)
        print(f"\n✓ Total de {total} mapas gerados com sucesso!")

    elif args.all_c1:
        process_multiple_instances("C1*", args.instances_dir, args.output_dir)

    elif args.all_r1:
        process_multiple_instances("R1*", args.instances_dir, args.output_dir)

    elif args.all_rc1:
        process_multiple_instances("RC1*", args.instances_dir, args.output_dir)

    elif args.instance:
        instance_path = os.path.join(
            args.instances_dir, f"{args.instance.upper()}.txt")

        if not os.path.exists(instance_path):
            print(f"✗ Instância não encontrada: {instance_path}")
            return 1

        print(f"\nProcessando instância: {args.instance.upper()}")
        if process_instance(instance_path, args.output_dir):
            print("✓ Mapa gerado com sucesso!")
        else:
            print("✗ Falha ao gerar mapa")
            return 1

    else:
        # Modo interativo (comportamento original)
        print("=== Visualizador de Instâncias VRP ===")
        instance_path = list_instances()

        if not instance_path:
            return 1

        instance_name = os.path.basename(instance_path).replace('.txt', '')
        print(f"\nLendo coordenadas da instância {instance_name}...")

        if process_instance(instance_path, args.output_dir):
            print("✓ Mapa gerado com sucesso!")
        else:
            print("✗ Falha ao gerar mapa")
            return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
