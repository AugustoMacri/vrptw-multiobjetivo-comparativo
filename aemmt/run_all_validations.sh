#!/bin/bash

echo "========================================="
echo "Iniciando validação C1..."
echo "========================================="
python3 scripts/run_validation_c1.py

echo ""
echo "========================================="
echo "Iniciando validação R1..."
echo "========================================="
python3 scripts/run_validation_r1.py

echo ""
echo "========================================="
echo "Iniciando validação RC1..."
echo "========================================="
python3 scripts/run_validation_rc1.py

echo ""
echo "========================================="
echo "Todas as validações concluídas!"
echo "========================================="
