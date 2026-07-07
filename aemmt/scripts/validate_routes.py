#!/usr/bin/env python3
"""
Script para validar se todas as rotas atendem todos os clientes.
Verifica se há clientes faltando ou duplicados.
"""

import re
import sys
from pathlib import Path


def validate_route_file(file_path):
    """Valida um arquivo evo_*.txt para verificar se todos os clientes são atendidos."""

    with open(file_path, 'r') as f:
        content = f.read()

    # Determinar número de clientes pela instância
    instance_name = Path(file_path).stem.replace('evo_', '').upper()

    # C e RC instances têm 100 clientes, R instances também
    num_clients = 100

    # Extrair rotas iniciais e finais
    sections = {
        'INICIAL': re.search(r'ROTAS INICIAIS.*?={80}(.*?)={80}', content, re.DOTALL),
        'FINAL': re.search(r'ROTAS FINAIS.*?={80}(.*?)={80}', content, re.DOTALL)
    }

    results = {}

    for section_name, section_match in sections.items():
        if not section_match:
            results[section_name] = {'error': 'Seção não encontrada'}
            continue

        section_text = section_match.group(1)

        # Extrair todos os clientes mencionados
        clients = []
        for match in re.finditer(r'Cliente\((\d+)\)', section_text):
            client_id = int(match.group(1))
            clients.append(client_id)

        # Analisar
        unique_clients = set(clients)
        expected_clients = set(range(1, num_clients + 1))

        missing = sorted(expected_clients - unique_clients)
        duplicates = []

        # Verificar duplicados
        seen = set()
        for client in clients:
            if client in seen:
                if client not in duplicates:
                    duplicates.append(client)
            seen.add(client)

        duplicates = sorted(duplicates)

        results[section_name] = {
            'total_clients': len(clients),
            'unique_clients': len(unique_clients),
            'expected_clients': num_clients,
            'missing': missing,
            'duplicates': duplicates,
            'valid': len(missing) == 0 and len(duplicates) == 0
        }

    return results, instance_name


def print_validation_results(file_path):
    """Imprime os resultados da validação."""

    print(f"\n{'='*80}")
    print(f"VALIDAÇÃO DE ROTAS: {Path(file_path).name}")
    print(f"{'='*80}\n")

    try:
        results, instance_name = validate_route_file(file_path)

        for section_name, data in results.items():
            print(f"--- ROTAS {section_name} ---")

            if 'error' in data:
                print(f"  ❌ ERRO: {data['error']}\n")
                continue

            if data['valid']:
                print(
                    f"  ✅ VÁLIDO - Todos os {data['expected_clients']} clientes atendidos")
                print(f"     Total de menções: {data['total_clients']}")
                print(f"     Clientes únicos: {data['unique_clients']}\n")
            else:
                print(f"  ❌ INVÁLIDO")
                print(f"     Total de menções: {data['total_clients']}")
                print(f"     Clientes únicos: {data['unique_clients']}")
                print(f"     Esperado: {data['expected_clients']} clientes")

                if data['missing']:
                    print(
                        f"     ⚠️  FALTANDO ({len(data['missing'])}): {', '.join(map(str, data['missing'][:20]))}")
                    if len(data['missing']) > 20:
                        print(
                            f"         ... e mais {len(data['missing']) - 20} clientes")

                if data['duplicates']:
                    print(
                        f"     ⚠️  DUPLICADOS ({len(data['duplicates'])}): {', '.join(map(str, data['duplicates'][:20]))}")
                    if len(data['duplicates']) > 20:
                        print(
                            f"         ... e mais {len(data['duplicates']) - 20} clientes")

                print()

        # Resumo final
        print(f"{'='*80}")
        initial_valid = results.get('INICIAL', {}).get('valid', False)
        final_valid = results.get('FINAL', {}).get('valid', False)

        if initial_valid and final_valid:
            print("✅ RESULTADO: Rotas inicial e final são VÁLIDAS")
        elif initial_valid and not final_valid:
            print("⚠️  RESULTADO: Rota inicial OK, mas rota final INVÁLIDA")
        elif not initial_valid and final_valid:
            print("⚠️  RESULTADO: Rota inicial INVÁLIDA, mas rota final OK")
        else:
            print("❌ RESULTADO: Ambas as rotas são INVÁLIDAS")

        print(f"{'='*80}\n")

        return initial_valid and final_valid

    except FileNotFoundError:
        print(f"❌ ERRO: Arquivo não encontrado: {file_path}\n")
        return False
    except Exception as e:
        print(f"❌ ERRO ao validar: {e}\n")
        return False


def main():
    """Função principal."""
    import argparse

    parser = argparse.ArgumentParser(
        description='Valida rotas em arquivos evo_*.txt')
    parser.add_argument('files', nargs='+',
                        help='Arquivos evo_*.txt para validar')

    args = parser.parse_args()

    all_valid = True

    for file_path in args.files:
        valid = print_validation_results(file_path)
        all_valid = all_valid and valid

    # Exit code
    sys.exit(0 if all_valid else 1)


if __name__ == '__main__':
    main()
