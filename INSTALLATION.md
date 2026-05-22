# NaptX - Guía de Instalación

Guía rápida para configurar y ejecutar la aplicación.

---

## Requisitos

- **Android Studio** Ladybug 2024.2.1+
- **JDK 17+** (incluido con Android Studio)
- **Git**
- **Dispositivo Android 9.0+** con sensor biométrico

---

## Instalación Rápida

### 1. Clonar el Repositorio

```bash
git clone https://github.com/WalterDeRacagua/NaptX.git
cd OfflinePaymentSystem
```

O desde Android Studio: **File --> New --> Project from Version Control**

---

### 2. Abrir y Sincronizar

1. Abre el proyecto en Android Studio
2. Espera a que Gradle sincronice automáticamente (3-5 min)
3. Si no se inicia: **File --> Sync Project with Gradle Files**

---

### 3. Build del Proyecto

Build --> Clean Project
Build --> Rebuild Project

Espera a ver: `BUILD SUCCESSFUL`

---

## Instalar en Dispositivo

### Preparar el Dispositivo

1. **Ajustes → Acerca del teléfono** → Toca 7 veces "Número de compilación"
2. **Ajustes → Opciones de desarrollador** → Activa "Depuración USB"
3. Conecta por USB → Acepta "Permitir depuración USB"

---

### Opción A: Instalación Directa (Recomendado)

1. Conecta tu dispositivo por USB al ordenador (este reconocerá tu dispositivo)
2. En Android Studio, click en **RUN**
3. Espera a que instale (1-3 min)

---

### Opción B: Generar APK Manual

**Desde Terminal:**
```bash
# Mac/Linux
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**APK generado en:** `app/build/outputs/apk/debug/app-debug.apk`

Envía el APK a tu móvil (email, Drive, AirDrop) e instálalo.

---

**Versión:** 1.0.0 | **Android Mínimo:** API 28
