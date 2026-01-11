#!/bin/bash

# Script para crear la estructura completa del proyecto Andro_Os Player

echo "Creando estructura del proyecto Andro_Os Player..."

# Crear directorios principales
mkdir -p app/src/main/java/com/androos/player/{viewmodel,repository,adapter,model}
mkdir -p app/src/main/res/{layout,drawable,values,mipmap-xxxhdpi}
mkdir -p .github/workflows
mkdir -p gradle/wrapper

echo "Estructura creada exitosamente."
echo ""
echo "Para completar el proyecto:"
echo "1. Coloca las imágenes del icono en app/src/main/res/mipmap-xxxhdpi/"
echo "2. Ejecuta: chmod +x gradlew"
echo "3. Abre el proyecto en Android Studio y sincroniza Gradle"
