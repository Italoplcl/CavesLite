# CavesLite

CavesLite es un plugin independiente para **Minecraft Java 26.3**, orientado a **Purpur/Paper** y **Java 25**. Amplía la exploración vanilla con encuentros poco comunes, criaturas especiales, Herobrine y ambientación subterránea sin convertir el servidor en un sistema RPG pesado ni depender de un resource pack.

El proyecto nació inspirado por *Dangerous Caves 2*, pero CavesLite tiene identidad, arquitectura, administración y mecánicas propias.

## Características

- 18 criaturas/encuentros configurables más Herobrine.
- Todos los encuentros especiales vienen **desactivados por defecto**.
- Prioridad/peso independiente del estado activado/desactivado.
- BossBars desactivadas por defecto y configurables por encuentro.
- Nombre visible editable y activable/desactivable por mob.
- Mimic con control de proximidad, límite activo, expiración y limpieza persistente.
- Sonidos ambientales, pasos fantasma y sistema de tensión.
- Títulos de aparición, avisos ActionBar y trails opcionales.
- Logros, recompensas Vault y rankings opcionales.
- PlaceholderAPI opcional con identificador `%caveslite_*%`.
- Paneles nativos de Dialogs de Minecraft; apariencia editable en `dialogs.yml`.
- Sin estructuras, derrumbes, vines ni generación de terreno propia.

## Requisitos

- Purpur/Paper 26.3.
- Java 25.
- Vault y PlaceholderAPI son opcionales.
- Folia no está soportado actualmente.

## Instalación

1. Compila o descarga `CavesLite-1.12.0.jar`.
2. Colócalo en `plugins/`.
3. Inicia el servidor para crear `plugins/CavesLite/`.
4. Revisa `config.yml`, `dialogs.yml` y `lang/`.
5. Activa explícitamente sólo los encuentros que quieras utilizar.

> **Migración desde builds anteriores:** CavesLite usa nueva identidad de plugin, permisos, placeholders y etiquetas persistentes. Para probar 1.12.0 de forma limpia, retira el JAR anterior y usa una carpeta `plugins/CavesLite/` nueva. Conserva una copia de tu configuración anterior sólo como referencia al trasladar ajustes manualmente.

## Comandos

Comando principal: `/clite`. Único alias: `/caveslite`. Permiso administrativo: `caveslite.command`.

- `/clite` — abre el panel principal.
- `/clite options` — opciones globales mediante checks.
- `/clite list` — IDs registrados.
- `/clite summon <mob> [x y z [mundo]]` — invocación manual para pruebas.
- `/clite summon herobrine [jugador] [tipo]` — prueba de Herobrine.
- `/clite kill [mob]` — limpia entidades/artefactos propiedad de CavesLite.
- `/clite kills [jugador]` — contador de eliminaciones.
- `/clite debug mobs [mob]` — cantidad, tiempo activo y ubicación de mobs cargados.
- `/clite reload` — recarga configuración, idiomas y Dialogs.

## Administración

`/clite options` controla mediante checks los módulos globales existentes: sonidos ambientales, footsteps, tensión, BossBars, títulos, warnings, trails, achievements y leaderboards.

El Dialog principal permite administrar encuentros individuales. `dialogs.yml` controla la presentación visual sin recompilar el plugin.

## Compilación

El proyecto incluye GitHub Actions. También puede compilarse con JDK 25 y Gradle usando:

```bash
gradle build --no-daemon
```

El JAR se genera en `build/libs/` con nombre `CavesLite-1.12.0.jar`.

## Documentación

La referencia completa está en [`DOCUMENTATION.md`](DOCUMENTATION.md). La pauta vigente de validación está en [`TESTING.md`](TESTING.md).

## Origen y licencia

CavesLite comenzó como una reimplementación inspirada en **Dangerous Caves 2**, de imDaniX / Evil-Lootlye. Parte del diseño histórico heredado está cubierto por la licencia MIT incluida en `LICENSE`. El proyecto actual ha evolucionado como plugin independiente. Consulta `LICENSE` para los avisos correspondientes.
