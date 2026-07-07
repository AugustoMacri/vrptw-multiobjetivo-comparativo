#!/usr/bin/env python3
"""
Simple manager to run analysis/visualization scripts in sequence.

Runs (by default) in this order:
 - visualize_results_multi.py
 - final_stats.py
 - compare_results.py
 - compare_best_results.py

Usage:
  python3 scripts_manager.py [--continue-on-error]

Logs are written to ./scripts/logs/<script>_YYYYmmdd_HHMMSS.log
"""

import argparse
import os
import subprocess
import sys
from datetime import datetime


DEFAULT_SCRIPTS = [
    "visualize_results_multi.py",
    "final_stats.py",
    "compare_results.py",
    "compare_best_results.py",
]


def timestamp():
    return datetime.now().strftime("%Y%m%d_%H%M%S")


def run_script(script_path, cwd, log_path):
    """Run a Python script and capture stdout/stderr to log_path.

    Returns the subprocess return code and the captured output as text.
    """
    print(f"Running {os.path.basename(script_path)} -> logging to {log_path}")
    proc = subprocess.run([sys.executable, script_path], cwd=cwd,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out = proc.stdout.decode(errors="replace")
    with open(log_path, "w", encoding="utf-8") as f:
        f.write(out)
    return proc.returncode, out


def main():
    parser = argparse.ArgumentParser(
        description="Run analysis scripts in sequence and log output.")
    parser.add_argument("--continue-on-error", action="store_true",
                        help="continue running remaining scripts if one fails")
    parser.add_argument("--scripts", nargs="*",
                        help="override the default ordered script list (paths relative to scripts/)")
    args = parser.parse_args()

    scripts_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(scripts_dir)
    logs_dir = os.path.join(scripts_dir, "logs")
    os.makedirs(logs_dir, exist_ok=True)

    scripts = args.scripts if args.scripts else DEFAULT_SCRIPTS

    summary = []

    for script in scripts:
        script_path = os.path.join(scripts_dir, script)
        if not os.path.exists(script_path):
            msg = f"MISSING: {script_path}"
            print(msg)
            summary.append((script, -1, msg))
            if not args.continue_on_error:
                print(
                    "Stopping due to missing script. Use --continue-on-error to skip missing scripts.")
                break
            else:
                continue

        log_file = f"{os.path.splitext(os.path.basename(script))[0]}_{timestamp()}.log"
        log_path = os.path.join(logs_dir, log_file)

        # Run child scripts from project root so relative paths like 'resultsMulti' resolve correctly
        rc, out = run_script(script_path, project_root, log_path)
        summary.append((script, rc, log_path))
        if rc != 0:
            print(f"Script {script} exited with code {rc}. Log: {log_path}")
            if not args.continue_on_error:
                print("Stopping further execution.")
                break

    print("\nRun summary:")
    for s, rc, info in summary:
        status = "OK" if rc == 0 else (
            "MISSING" if rc == -1 else f"FAIL({rc})")
        print(f" - {s}: {status} -> {info}")


if __name__ == "__main__":
    main()
