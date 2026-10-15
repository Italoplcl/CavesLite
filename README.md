# CavesLite

CavesLite es un plugin independiente para **Minecraft Java 26.3**, orientado a **Purpur/Paper** y **Java 25**. Amplía la exploración vanilla con encuentros poco comunes, criaturas especiales, Herobrine y ambientación subterránea sin convertir el servidor en un sistema RPG pesado ni depender de un resource pack.

El proyecto nació inspirado por *Dangerous Caves 2*, pero CavesLite tiene identidad, arquitectura, administración y mecánicas propias.

## Cambios de 1.12.2

- `language: es` o `language: en`: paneles, comandos, logros y valores visibles de configuración. Los dos archivos se extraen sin sobrescribir ediciones. Respaldo por clave: archivo del usuario → idioma incluido seleccionado → español del usuario → español incluido → inglés incluido.
- `@lang:<clave>` en valores visibles de `config.yml` y `dialogs.yml` permite cambiar de idioma sin reemplazar esos archivos. Los textos predeterminados de 1.12.1 se reconocen en memoria; los textos personalizados se conservan. Guardar un panel sin editar el nombre conserva su referencia de idioma. Las claves y valores técnicos de configuración (materiales, comandos, IDs) no se traducen.
- Nether desactivado por criatura mediante `mobs.<id>.nether-enabled: false`; también exige `enabled: true`, prioridad positiva y un mundo admitido en `mobs.worlds`. El checkbox está disponible en el panel individual. Añade el nombre real del mundo Nether a esa lista para habilitarlo.
- Nether usa el mismo evento `NATURAL`, lista `mobs.replace-mobs`, probabilidad `try-chance`, luz, cooldown y máximo activo que Overworld. No crea un temporizador ni añade apariciones artificiales. El disparador del gólem al romper minerales queda restringido al Overworld. Si necesitas reemplazar criaturas propias del Nether, añade sus tipos a `replace-mobs`.
- En Nether se omiten tanto los límites Y globales como los particulares del Overworld y el contexto de cueva/noche/cielo del Overworld. Se conservan las condiciones específicas de cada criatura (por ejemplo, oscuridad), y se comprueba un volumen físico conservador para el cuerpo y sus pasajeros, colisiones, líquidos, altura real del mundo y chunks cargados. El gigante necesita 3,6 bloques de ancho y 12 de alto.
- Fortalezas y bastiones se excluyen por los límites reales de estructuras generadas; los portales en ruinas están permitidos. `mobs.nether.ruined-portal-radius` (48 bloques, rango 0–128) mide distancia 3D al límite de la estructura en chunks cargados. `giant-zombie-ruined-portal-weight-multiplier` (3.0, rango 0–100) multiplica únicamente el peso del gigante en esa zona; 1 desactiva el aumento y 0 elimina su peso cerca de portales. El peso base 0 sigue siendo 0. No fuerza generación ni carga de chunks para buscar portales.
- El End queda excluido de reemplazos naturales y encuentros naturales de Herobrine. La invocación manual administrativa conserva su funcionamiento. Herobrine sigue usando su sistema independiente de encuentros, mundos y alturas configurados.
- Herobrine actualiza su rotación en cada tick de combate, aunque esté demasiado cerca del jugador o un obstáculo impida avanzar.
- Actions: checkout/setup-java/setup-gradle/upload-artifact v5, Ubuntu 24.04, Java 25 y artifact `CavesLite`.

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

1. Compila o descarga `CavesLite-1.12.2.jar`.
2. Colócalo en `plugins/`.
3. Inicia el servidor para crear `plugins/CavesLite/`.
4. Revisa `config.yml`, `dialogs.yml` y `lang/`.
5. Activa explícitamente sólo los encuentros que quieras utilizar.

> **Migración desde builds anteriores:** CavesLite usa nueva identidad de plugin, permisos, placeholders y etiquetas persistentes. Para probar 1.12.2 de forma limpia, retira el JAR anterior y usa una carpeta `plugins/CavesLite/` nueva. Conserva una copia de tu configuración anterior sólo como referencia al trasladar ajustes manualmente.

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

El JAR se genera en `build/libs/` con nombre `CavesLite-1.12.2.jar`.

## Documentación

La referencia completa está en [`DOCUMENTATION.md`](DOCUMENTATION.md). La pauta vigente de validación está en [`TESTING.md`](TESTING.md).

## Origen y licencia

CavesLite comenzó como una reimplementación inspirada en **Dangerous Caves 2**, de imDaniX / Evil-Lootlye. Parte del diseño histórico heredado está cubierto por la licencia MIT incluida en `LICENSE`. El proyecto actual ha evolucionado como plugin independiente. Consulta `LICENSE` para los avisos correspondientes.
