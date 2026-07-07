#!/usr/bin/env python3
"""
Verificação do cálculo de tempo conforme literatura Solomon (1987)
"""

import math

print("=" * 80)
print("VERIFICAÇÃO DO CÁLCULO DE TEMPO - Literatura Solomon (1987)")
print("=" * 80)
print()

# Dados reais da instância C101
print("📚 LITERATURA SOLOMON (1987):")
print("-" * 80)
print("Referência: Solomon, M. M. (1987)")
print("'Algorithms for the vehicle routing and scheduling problems")
print(" with time window constraints'")
print()
print("ESPECIFICAÇÕES:")
print("  • Coordenadas: Unidades Euclidianas arbitrárias")
print("  • Distância: Euclidiana d = √[(x₂-x₁)² + (y₂-y₁)²]")
print("  • Tempo de viagem: t = d (tempo = distância)")
print("  • Velocidade: Implicitamente = 1 (não especificada)")
print("  • Janelas de tempo: Em unidades de tempo")
print()

print("=" * 80)
print("EXEMPLO PRÁTICO - Instância C101")
print("=" * 80)
print()

# Dados do C101
depot = {'x': 40, 'y': 50, 'ready': 0, 'due': 1236, 'service': 0}
client1 = {'x': 45, 'y': 68, 'ready': 912, 'due': 967, 'service': 90}
client2 = {'x': 45, 'y': 70, 'ready': 825, 'due': 870, 'service': 90}

# Calcular distâncias
d_depot_c1 = math.sqrt((client1['x'] - depot['x'])
                       ** 2 + (client1['y'] - depot['y'])**2)
d_c1_c2 = math.sqrt((client2['x'] - client1['x'])
                    ** 2 + (client2['y'] - client1['y'])**2)
d_c2_depot = math.sqrt((depot['x'] - client2['x'])
                       ** 2 + (depot['y'] - client2['y'])**2)

print("Rota: Depósito(0) → Cliente(1) → Cliente(2) → Depósito(0)")
print("-" * 80)
print()

print("COORDENADAS:")
print(
    f"  Depósito: ({depot['x']}, {depot['y']}) - Janela: [{depot['ready']}, {depot['due']}]")
print(
    f"  Cliente 1: ({client1['x']}, {client1['y']}) - Janela: [{client1['ready']}, {client1['due']}], Service: {client1['service']}")
print(
    f"  Cliente 2: ({client2['x']}, {client2['y']}) - Janela: [{client2['ready']}, {client2['due']}], Service: {client2['service']}")
print()

print("DISTÂNCIAS EUCLIDIANAS:")
print(f"  Depósito → Cliente 1: {d_depot_c1:.2f} unidades")
print(f"  Cliente 1 → Cliente 2: {d_c1_c2:.2f} unidades")
print(f"  Cliente 2 → Depósito: {d_c2_depot:.2f} unidades")
print(f"  Distância total: {d_depot_c1 + d_c1_c2 + d_c2_depot:.2f} unidades")
print()

print("=" * 80)
print("CÁLCULO DE TEMPO (Padrão Solomon - Velocidade = 1):")
print("=" * 80)
print()

# Simulação da rota
current_time = 0

print("PASSO 1: Depósito → Cliente 1")
print(f"  Tempo de viagem: {d_depot_c1:.2f} (= distância)")
current_time += d_depot_c1
print(f"  Chegada ao Cliente 1: t = {current_time:.2f}")
print(f"  Janela do Cliente 1: [{client1['ready']}, {client1['due']}]")
if current_time < client1['ready']:
    print(f"  ⏰ Chegou cedo! Aguarda até t = {client1['ready']}")
    current_time = client1['ready']
elif current_time > client1['due']:
    print(f"  ❌ VIOLAÇÃO! Chegou tarde (after {client1['due']})")
else:
    print(f"  ✅ Dentro da janela!")
print(f"  Tempo de serviço: {client1['service']}")
current_time += client1['service']
print(f"  Saída do Cliente 1: t = {current_time:.2f}")
print()

print("PASSO 2: Cliente 1 → Cliente 2")
print(f"  Tempo de viagem: {d_c1_c2:.2f} (= distância)")
current_time += d_c1_c2
print(f"  Chegada ao Cliente 2: t = {current_time:.2f}")
print(f"  Janela do Cliente 2: [{client2['ready']}, {client2['due']}]")
if current_time < client2['ready']:
    print(f"  ⏰ Chegou cedo! Aguarda até t = {client2['ready']}")
    current_time = client2['ready']
elif current_time > client2['due']:
    print(f"  ❌ VIOLAÇÃO! Chegou tarde (after {client2['due']})")
else:
    print(f"  ✅ Dentro da janela!")
print(f"  Tempo de serviço: {client2['service']}")
current_time += client2['service']
print(f"  Saída do Cliente 2: t = {current_time:.2f}")
print()

print("PASSO 3: Cliente 2 → Depósito")
print(f"  Tempo de viagem: {d_c2_depot:.2f} (= distância)")
current_time += d_c2_depot
print(f"  Retorno ao Depósito: t = {current_time:.2f}")
print(f"  Janela do Depósito: [{depot['ready']}, {depot['due']}]")
if current_time > depot['due']:
    print(f"  ❌ VIOLAÇÃO! Retornou tarde")
else:
    print(f"  ✅ Retornou dentro do prazo!")
print()

print("=" * 80)
print("RESUMO:")
print("=" * 80)
print(f"✅ Tempo total da rota: {current_time:.2f} unidades")
print(f"✅ Distância total: {d_depot_c1 + d_c1_c2 + d_c2_depot:.2f} unidades")
print()

print("=" * 80)
print("VALIDAÇÃO DA IMPLEMENTAÇÃO:")
print("=" * 80)
print()
print("Código ANTES da correção (INCORRETO):")
print("  VEHICLE_SPEED = 50")
print("  tempo = (distância / 50) × 60")
print(
    f"  Exemplo: tempo = ({d_depot_c1:.2f} / 50) × 60 = {(d_depot_c1/50)*60:.2f} ❌")
print(
    f"  Erro: {((d_depot_c1/50)*60 / d_depot_c1):.2f}x mais lento (20% a mais)")
print()

print("Código APÓS a correção (CORRETO):")
print("  VEHICLE_SPEED = 1")
print("  tempo = distância / 1 = distância")
print(f"  Exemplo: tempo = {d_depot_c1:.2f} / 1 = {d_depot_c1:.2f} ✅")
print(f"  ✅ Conforme Solomon (1987)")
print()

print("=" * 80)
print("CONCLUSÃO:")
print("=" * 80)
print("✅ A correção está CORRETA e de acordo com a literatura!")
print("✅ VEHICLE_SPEED = 1 é o padrão Solomon")
print("✅ Tempo de viagem = Distância Euclidiana")
print("✅ Resultados agora são comparáveis com benchmarks")
print()
print("⚠️  IMPORTANTE: Todas as execuções anteriores (com velocidade=50)")
print("   devem ser refeitas para obter resultados válidos!")
print("=" * 80)
