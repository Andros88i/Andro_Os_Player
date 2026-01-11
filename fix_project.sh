#!/bin/bash

echo "Corrigiendo estructura del proyecto Andro_Os Player..."

# Crear directorios necesarios
mkdir -p app/src/main/res/{layout,drawable,values,mipmap-xxxhdpi}
mkdir -p app/src/main/java/com/androos/player
mkdir -p .github/workflows

# Crear archivos básicos si no existen
if [ ! -f "gradlew" ]; then
    echo "gradlew no encontrado, creando..."
    cat > gradlew << 'EOF'
#!/bin/bash
# Gradle wrapper stub
echo "Please sync project with Gradle files in Android Studio"
EOF
    chmod +x gradlew
fi

if [ ! -f "gradle/wrapper/gradle-wrapper.properties" ]; then
    echo "Creando gradle-wrapper.properties..."
    mkdir -p gradle/wrapper
    cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
fi

echo ""
echo "Estructura corregida. Para construir el proyecto:"
echo "1. Abre el proyecto en Android Studio"
echo "2. File -> Sync Project with Gradle Files"
echo "3. Build -> Make Project"
echo ""
echo "Para GitHub Actions, asegúrate de que todos los archivos estén commitados."
