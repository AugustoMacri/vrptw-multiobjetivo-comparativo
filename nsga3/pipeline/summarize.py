#!/usr/bin/env python3
"""
Sumarizador padronizado de resultados - AEMMT e NSGA-III.

Le results/<categoria>/<instancia>/evo_*.txt e gera tabelas com:
  - melhor distancia de cada instancia entre as 10 execucoes
  - quantos veiculos usou
  - qual execucao foi a melhor
  - estatisticas por categoria (min, max, media, desvio padrao)

Saidas em <output-dir>/:
  - summary_c1.txt
  - summary_r1.txt
  - summary_rc1.txt
  - summary_all.txt
  - summary_all.csv

Uso:
    python3 pipeline/summarize.py \
        --algo AEMMT \
        --results-dir results \
        --output-dir results/summary
"""

import argparse
import csv
import re
import statistics
import sys
from pathlib import Path


# ----------------------------------------------------------------------------
# PARSERS
# ----------------------------------------------------------------------------

def _read_text(path: Path):
    """Le o arquivo inteiro tentando UTF-8 e caindo para latin-1 (compativel
    com saidas em Windows-1252 do AEMMT)."""
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin-1")


def parse_best_distance_aemmt(evo_file: Path):
    """Extrai distancia total e #veiculos da secao 'ROTAS FINAIS' do AEMMT.

    Tolerante a encoding (UTF-8 ou CP1252/latin-1) e a palavras com qualquer
    caractere no lugar dos acentos.
    """
    total_dist = None
    num_vehicles = None
    in_final = False

    # Regex sao deliberadamente "frouxas": qualquer char no lugar dos acentos.
    re_dist = re.compile(r"Dist.ncia\s+total:\s*([\d.,]+)")
    re_veh = re.compile(r"Total de ve.culos usados:\s*(\d+)")

    for raw in _read_text(evo_file).splitlines():
        line = raw.strip()
        if "ROTAS FINAIS" in line:
            in_final = True
            continue
        if in_final and "FRENTE DE PARETO" in line:
            break
        if in_final:
            m = re_dist.search(line)
            if m:
                total_dist = float(m.group(1).replace(",", "."))
            m = re_veh.search(line)
            if m:
                num_vehicles = int(m.group(1))
    return total_dist, num_vehicles


def parse_best_distance_nsga3(evo_file: Path):
    """Extrai distancia minima e #veiculos do NSGA-III."""
    total_dist = None
    num_vehicles = None

    content = _read_text(evo_file)

    m = re.search(r"NSGA3_DistanciaMin\s+([\d.,]+)", content)
    if m:
        total_dist = float(m.group(1).replace(",", "."))

    # Conta linhas "Veiculo X:" na secao MELHOR SOLUCAO
    in_solution = False
    veh_count = 0
    for line in content.split("\n"):
        if "=== MELHOR SOLUCAO" in line:
            in_solution = True
            continue
        if in_solution and re.match(r"Veiculo\s+\d+:.*rota:", line):
            veh_count += 1
    if veh_count > 0:
        num_vehicles = veh_count
    return total_dist, num_vehicles


def parse_evo(evo_file: Path, algo: str):
    if algo.upper().startswith("NSGA"):
        return parse_best_distance_nsga3(evo_file)
    return parse_best_distance_aemmt(evo_file)


# ----------------------------------------------------------------------------
# SUMARIZACAO
# ----------------------------------------------------------------------------

def collect_executions(inst_dir: Path, algo: str):
    """Para uma pasta de instancia, coleta (exec, distancia, veiculos) de cada
    execucao. Retorna lista de tuplas ordenadas por nome de arquivo."""
    items = []
    for evo in sorted(inst_dir.glob("evo_*.txt")):
        m = re.search(r"exec(\d+)", evo.name)
        exec_id = m.group(1) if m else "?"
        dist, veh = parse_evo(evo, algo)
        if dist is not None:
            items.append((exec_id, dist, veh, evo.name))
    return items


def pick_best(executions):
    """Retorna a tupla com menor distancia."""
    if not executions:
        return None
    return min(executions, key=lambda x: x[1])


def category_dirs(results_dir: Path):
    """Retorna (categoria, pasta) para C1, R1, RC1 que existirem."""
    out = []
    for cat in ["C1", "R1", "RC1"]:
        p = results_dir / cat
        if p.is_dir():
            out.append((cat, p))
    return out


def summarize_category(cat: str, cat_dir: Path, algo: str):
    """Gera dict de resumo da categoria."""
    rows = []
    for inst_dir in sorted(cat_dir.iterdir()):
        if not inst_dir.is_dir():
            continue
        inst = inst_dir.name
        execs = collect_executions(inst_dir, algo)
        if not execs:
            continue
        best = pick_best(execs)
        all_dists = [e[1] for e in execs]
        rows.append({
            "instance": inst,
            "best_exec": best[0],
            "best_dist": best[1],
            "best_veh": best[2] if best[2] else "-",
            "best_file": best[3],
            "n_execs": len(execs),
            "min_dist": min(all_dists),
            "max_dist": max(all_dists),
            "mean_dist": statistics.mean(all_dists),
            "std_dist": statistics.stdev(all_dists) if len(all_dists) > 1 else 0.0,
        })
    return rows


def write_category_table(rows, cat: str, algo: str, output_file: Path):
    """Escreve tabela formatada para uma categoria."""
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(f"# Resumo {cat} - {algo}\n")
        f.write(f"# Melhor execucao por instancia (10 execucoes por instancia)\n")
        f.write("# " + "-" * 80 + "\n\n")
        f.write(f"{'Instancia':<10} {'Best Exec':<10} {'Veiculos':<10} {'Dist Min':>12} "
                f"{'Dist Med':>12} {'Dist Max':>12} {'Std':>10}\n")
        f.write("-" * 90 + "\n")
        for r in rows:
            f.write(f"{r['instance']:<10} exec{r['best_exec']:<6} "
                    f"{str(r['best_veh']):<10} "
                    f"{r['best_dist']:>12.2f} "
                    f"{r['mean_dist']:>12.2f} "
                    f"{r['max_dist']:>12.2f} "
                    f"{r['std_dist']:>10.2f}\n")

        if rows:
            f.write("\n")
            f.write(f"Estatisticas da categoria {cat}:\n")
            all_best = [r["best_dist"] for r in rows]
            f.write(f"  Melhor distancia:    {min(all_best):.2f}\n")
            f.write(f"  Pior distancia:      {max(all_best):.2f}\n")
            f.write(f"  Media das melhores:  {statistics.mean(all_best):.2f}\n")
            if len(all_best) > 1:
                f.write(f"  Desvio padrao:       {statistics.stdev(all_best):.2f}\n")


def write_consolidated_csv(all_rows, algo: str, output_file: Path):
    """Tabela CSV completa."""
    with open(output_file, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["category", "instance", "best_exec", "best_veh",
                    "best_dist", "mean_dist", "max_dist", "std_dist",
                    "n_execs", "algo"])
        for cat, rows in all_rows:
            for r in rows:
                w.writerow([cat, r["instance"], r["best_exec"], r["best_veh"],
                            r["best_dist"], r["mean_dist"], r["max_dist"],
                            r["std_dist"], r["n_execs"], algo])


def write_consolidated_txt(all_rows, algo: str, output_file: Path):
    """Tabela txt consolidada."""
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(f"# Resumo Consolidado - {algo}\n")
        f.write(f"# Todas as categorias\n")
        f.write("# " + "=" * 80 + "\n\n")
        for cat, rows in all_rows:
            f.write(f"\n--- Categoria {cat} ---\n\n")
            f.write(f"{'Instancia':<10} {'Best Exec':<10} {'Veiculos':<10} "
                    f"{'Dist Min':>12} {'Dist Med':>12} {'Std':>10}\n")
            f.write("-" * 80 + "\n")
            for r in rows:
                f.write(f"{r['instance']:<10} exec{r['best_exec']:<6} "
                        f"{str(r['best_veh']):<10} "
                        f"{r['best_dist']:>12.2f} "
                        f"{r['mean_dist']:>12.2f} "
                        f"{r['std_dist']:>10.2f}\n")
            if rows:
                all_best = [r["best_dist"] for r in rows]
                f.write(f"\n  Min: {min(all_best):.2f}  Med: {statistics.mean(all_best):.2f}  "
                        f"Max: {max(all_best):.2f}\n")


# ----------------------------------------------------------------------------
# MAIN
# ----------------------------------------------------------------------------

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--algo", required=True, choices=["AEMMT", "NSGA-III"])
    p.add_argument("--results-dir", type=Path, required=True)
    p.add_argument("--output-dir", type=Path, required=True)
    args = p.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)

    all_rows = []
    for cat, cat_dir in category_dirs(args.results_dir):
        rows = summarize_category(cat, cat_dir, args.algo)
        all_rows.append((cat, rows))
        out_file = args.output_dir / f"summary_{cat.lower()}.txt"
        write_category_table(rows, cat, args.algo, out_file)
        print(f"[OK] {out_file}  ({len(rows)} instancias)")

    if all_rows:
        write_consolidated_txt(all_rows, args.algo, args.output_dir / "summary_all.txt")
        write_consolidated_csv(all_rows, args.algo, args.output_dir / "summary_all.csv")
        print(f"[OK] {args.output_dir / 'summary_all.txt'}")
        print(f"[OK] {args.output_dir / 'summary_all.csv'}")
    else:
        print("AVISO: nenhuma categoria encontrada em " + str(args.results_dir))


if __name__ == "__main__":
    main()
