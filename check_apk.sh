#!/bin/bash

echo "=== Diagnóstico de construcción de APK ==="
echo ""

# Verificar estructura del proyecto
echo "1. Verificando estructura del proyecto..."
if [ -d "app/src/main" ]; then
    echo "✓ Directorio app/src/main existe"
else
    echo "✗ Directorio app/src/main NO existe"
fi

if [ -f "app/build.gradle" ]; then
    echo "✓ Archivo app/build.gradle existe"
else
    echo "✗ Archivo app/build.gradle NO existe"
fi

echo ""
echo "2. Limpiando y construyendo proyecto..."
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease

echo ""
echo "3. Buscando archivos APK..."
find app/build -name "*.apk" -type f 2>/dev/null | while read file; do
    echo "   Encontrado: $file"
    ls -lh "$file"
done

echo ""
echo "4. Listando directorios de salida..."
echo "Debug directory:"
ls -la app/build/outputs/apk/debug/ 2>/dev/null || echo "   No existe"
echo ""
echo "Release directory:"
ls -la app/build/outputs/apk/release/ 2>/dev/null || echo "   No existe"

echo ""
echo "=== Diagnóstico completo ==="
