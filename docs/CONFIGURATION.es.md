# Configuración de CavesLite 2.0 preview

Esta guía describe `2.0.0-preview.1`. La configuración inicial es deliberadamente escasa y editable. Sustituye la política antigua de entregar todos los encuentros desactivados.

## Instalación y actualización

Requiere Paper/Purpur 26.3 y Java 25. Vault y PlaceholderAPI son opcionales. No requiere resource pack ni mod de cliente. Folia no está soportado.

Detén el servidor, conserva una copia de `plugins/CavesLite/`, cambia el JAR y arranca. No uses el `/reload` global de Bukkit. CavesLite no sobrescribe configuraciones ni idiomas existentes. El preset nuevo solo se aplica automáticamente en una instalación nueva. En una actualización, guarda tu config anterior y copia `plugins/CavesLite/presets/basic.yml` a `plugins/CavesLite/config.yml` para adoptarlo; después ejecuta `/clite reload`. Los presets también están en `src/main/resources/presets/` del proyecto.

## Configuración básica aprobada

| Criatura / ID | Estado inicial | Peso |
|---|---|---|
| Araña alfa / `alpha-spider` | Desactivada | 1 |
| Gólem de las cuevas / `cave-golem` | Activo | 2 |
| Murciélago llorón / `crying-bat` | Desactivado | 1 |
| Minero muerto / `dead-miner` | Activo | 3 |
| Armadura embrujada / `hexed-armor` | Desactivada | 1 |
| Oscuridad hambrienta / `hungering-darkness` | Activa | 1 |
| Creeper de lava / `lava-creeper` | Desactivado | 1 |
| Monstruo de magma / `magma-monster` | Activo | 1 |
| Mimic / `mimic` | Activo | 1 |
| Demonio de humo / `smoke-demon` | Activo | 1 |
| Creeper de TNT / `tnt-creeper` | Activo | 3 |
| Vigilante / `watcher` | Desactivado | 1 |
| Zombie gigante / `giant-zombie` | Activo | 1 |
| Jinete de gallina / `chicken-jockey` | Activo | 1 |
| Conejo asesino / `killer-bunny` | Activo | 1 |
| Ilusionista / `illusioner` | Desactivado | 1 |
| Jinete de araña / `spider-jockey` | Activo | 2 |
| Jinete esqueleto / `skeleton-horseman` | Activo | 2 |

Herobrine está activo e independiente del pool: intento cada 15–25 minutos por jugador y máximo un encuentro activo por jugador. No existe un peso de Herobrine compartido con los otros mobs.

- `mobs.try-chance: 10`: 10% de probabilidad de intentar reemplazar una aparición natural compatible.
- Mobs ordinarios: cooldown de 120 segundos y máximo de dos entidades principales cargadas por tipo, sumadas entre los mundos. Los pasajeros no deberían interpretarse como otro encuentro.
- Oscuridad hambrienta, Mimic y gigante: cooldown de 600 segundos y máximo uno por tipo. Mimic tiene además un límite de cofres por mundo.
- Nether desactivado por criatura y para Herobrine. El End queda excluido de apariciones naturales.
- Mundos iniciales: `world`. Adapta el nombre si tu mundo se llama distinto. Una lista vacía permite mundos de entorno Overworld; no significa todas las dimensiones.

## Cómo activar y personalizar

Con OP o `caveslite.command`, abre `/clite`, selecciona el mob, activa Spawn natural, asigna un peso mayor que cero y pulsa Guardar. El guardado escribe la configuración y la recarga inmediatamente. Blanco significa activo; gris tachado significa desactivado. El estado no garantiza una aparición: aún se aplican sus condiciones y límites. El panel del gigante incluye persecución, ataques, refuerzos, loot y varios valores de combate. Los ajustes avanzados se editan en YAML.

Ejemplo para habilitar una criatura inicialmente desactivada:

```yaml
mobs:
  alpha-spider:
    enabled: true
    nether-enabled: false
    priority: 1
    cooldown-seconds: 120
    max-active: 2
```

Este bloque es un ejemplo para combinar con tu archivo, no un reemplazo de la configuración completa. Para desactivarla usa `enabled: false`; el peso puede conservarse. `priority: 0` también la excluye del pool, aunque figure activa en el panel. Tras editar YAML, usa `/clite reload`.

## Probabilidad general, pesos y límites

Solo se consideran eventos `NATURAL` de los tipos enumerados en `mobs.replace-mobs`. Se comprueban mundo, luz y probabilidad general; se selecciona un mob según su peso y luego se aplican su contexto, alturas, espacio, cooldown y cap. Si la selección falla esas condiciones, se conserva la aparición vanilla: no se vuelve a elegir otro tipo.

El peso 3 ofrece tres veces el peso de selección del peso 1 dentro del mismo pool. **No es 3% ni garantiza tres veces las apariciones observadas**: hay condiciones distintas y límites. El 10% es un techo de intentos para eventos compatibles, no el porcentaje de todos los mobs del servidor. No se reemplazan spawners, huevos ni apariciones de otros plugins por este motor. Golem al romper mineral y refuerzos del gigante son disparadores adicionales limitados, no reemplazos NATURAL.

`mobs.y-min/y-max` y `spawn-y-min/spawn-y-max` se aplican al Overworld. El gigante usa sus propios límites (80–320) y requiere noche (13000–23000) y luz de cielo. Los mobs de cueva usan el contexto subterráneo existente. `max-light-level: 16` desactiva la restricción global adicional de luz; no desactiva las restricciones específicas de oscuridad de ciertos mobs.

Los caps comunes cuentan las entidades principales que el plugin conoce y que están cargadas; no son un censo de todos los chunks descargados ni un nuevo mob cap vanilla. `/clite kill` y las descargas eliminan IDs obsoletos; al cargar entidades etiquetadas se vuelven a registrar. Los cooldowns comunes se comparten por tipo entre mundos y se reinician al reiniciar el plugin.

## Perfil vanilla y perfil de rendimiento

El preset básico es el recomendado para empezar con una experiencia vanilla ampliada: 10%, 120 segundos y cap 2, manteniendo las tres excepciones de 10 minutos/cap 1. No cambia ajustes de Paper/Purpur.

`presets/performance.yml` conserva la misma selección y pesos, pero usa 5%, 240 segundos/cap 1 para los ordinarios y Herobrine cada 30–45 minutos; las excepciones siguen en 600 segundos/cap 1. Para usarlo, guarda tu configuración, copia ese preset a `config.yml`, adapta los mundos y recarga.

En servidores con guías de rendimiento, límites de monstruos, intervalos de spawn, distancia de simulación/activación y otras opciones pueden reducir las apariciones naturales disponibles o la actividad de entidades. CavesLite no compensa eso creando monstruos ilimitados. El perfil propuesto reduce los encuentros personalizados; no promete una ganancia fija de TPS y debe medirse bajo carga comparable.

Referencias oficiales: [límites y opciones por mundo](https://docs.papermc.io/paper/reference/world-configuration/), [intervalos y límites de Bukkit](https://docs.papermc.io/paper/reference/bukkit-configuration/). No se aplica ninguna guía externa de rendimiento automáticamente.

## Nether y End

Para habilitar un mob en Nether, permite el nombre de ese mundo en `mobs.worlds`, conserva su `enabled: true` y peso positivo, y activa `nether-enabled`. Se usa el mismo motor y lista `replace-mobs`; añade tipos propios del Nether si los deseas. No usa alturas ni contexto de cueva/noche/cielo del Overworld, pero conserva condiciones particulares (por ejemplo, oscuridad).

Se comprueba espacio físico y se excluyen fortalezas/bastiones por estructuras generadas. Portales en ruinas permitidos. `mobs.nether.ruined-portal-radius` define distancia 3D a la estructura; `giant-zombie-ruined-portal-weight-multiplier` multiplica solo el peso del gigante cerca de ella. Valores 1 y 0 significan neutral y sin peso local, respectivamente; un peso base 0 o mob apagado no se activa. Las búsquedas de portales consultan solo chunks cargados.

Herobrine conserva su propio sistema de mundos/alturas y tiene `herobrine.nether-enabled: false`. El End no admite encuentros naturales. Los comandos manuales son herramientas administrativas y no demuestran las reglas naturales.

## Herobrine

Tipos: `observe`, `flee-on-sight`, `behind`, `false-chase`, `outside-observer`, `watcher`, `lurking`, `stalking`, `creeping`. La colocación requiere posiciones seguras cargadas y evita las estructuras protegidas por su comprobación local. Una colocación fallida puede reintentarse un minuto después; 15–25 minutos es un intervalo de intentos, no una garantía de presencia.

El combate empieza al ser golpeado por un jugador. Sigue mirando al objetivo, utiliza rutas terrestres para desniveles transitables y respeta alcance/línea de visión. No escala una pared vertical ni atraviesa bloques. Sus relojes son independientes del reloj diurno, así que congelar/cambiar la hora del mundo no bloquea ataques ni desaparición.

Defaults: daño base 2 (un corazón antes de reducciones), cooldown de ataque 24 ticks, máximo 35 segundos de combate y 4 segundos de huida. Puede huir antes por salud o distancia, y la expiración no exige que pueda moverse. Un golpe bloqueado o cancelado no aplica empuje adicional. Armadura, escudo, absorción, modo creativo y protecciones como god mode afectan el resultado normal.

`/clite debug herobrine [jugador]` muestra estado, segundos y `attack=[base, dealt, cancelled]`. `dealt` incluye pérdida de absorción. Es una lectura orientativa, no una forma de ignorar protecciones.

Fuera de combate, el mensaje `Stop.` solo se intenta al ser observado con línea de visión, con 0,5% por comprobación. La observación continua puede hacerle desaparecer a los 8 segundos; los tipos de acecho/watcher tienen 45 segundos de vida máxima. Antorchas de redstone, golpe furtivo y sonidos conservan sus opciones. La bossbar de terceros puede identificar la entidad como mannequin; no se ha cambiado esa compatibilidad.

## Mobs y opciones particulares

| ID | Mecánica y ajustes particulares |
|---|---|
| `alpha-spider` | Telarañas y arañas pequeñas; `cobweb-chance`, `minion-chance`. |
| `cave-golem` | Gólem con casco mineral, ataques y modificadores al recibir daño con/sin pico; `variants`, `spawn-from-block`, modificadores y efectos. Su liberación al romper minerales respeta enabled/cooldown/cap y solo ocurre en Overworld. |
| `crying-bat` | Gritos y probabilidad de morir tras gritar; `cry-chance`, `death-chance`. |
| `dead-miner` | Equipo de minero, drops al recibir daño y antorchas en oscuridad; `drop-items`, `drop-chance`, `place-torches`, `requires-target`, `torches-cooldown`, `head-value`. |
| `hexed-armor` | Intercambio de equipo al golpear y maldición opcional; `apply-chance`, `binding-curse`. |
| `hungering-darkness` | Aparición en oscuridad total y ataque peligroso; `damage` conserva 200 por defecto, luz/visión nocturna y eliminación con luz. Su baja frecuencia no implica bajo daño. |
| `lava-creeper` | Explosión y cambios de bloques/lava según sus opciones; inicialmente desactivado. |
| `magma-monster` | Zombie ardiente con efectos de fuego/magma y opciones de objetivo/daño al extinguirse. |
| `mimic` | Se oculta como cofre y reacciona al abrirlo. Distancia a otros cofres, límite de cofres, expiración y limpieza al descargar; no confundir cofre con entidad viva. |
| `smoke-demon` | Nube invisible que aplica ceguera y wither en oscuridad; luz, radio y partícula configurables. |
| `tnt-creeper` | TNT adicional al explotar y pequeña explosión al recibir daño; `tnt-amount`, `explosion-chance`. Puede afectar terreno; la garantía de no destrucción de la nueva mecánica corresponde al gigante. |
| `watcher` | Criatura con cabeza personalizada; no confundir con el tipo de encuentro watcher de Herobrine. |
| `giant-zombie` | Ver [guía completa del gigante](GIANT.es.md). |
| `chicken-jockey` | Zombie montado en gallina con amplificador de velocidad. |
| `killer-bunny` | Variante hostil de conejo con Velocidad II, vida 12 por defecto; IA/daño normales de esa variante. |
| `illusioner` | Ilusionista con su comportamiento del servidor. |
| `spider-jockey` | Esqueleto sobre araña; encuentro compuesto basado en las entidades normales. |
| `skeleton-horseman` | Esqueleto sobre caballo esqueleto. `health: 30` se aplica al caballo; no invoca la trampa vanilla ni agrega habilidades especiales. |

`extra-drops` agrega materiales a la muerte y `death-command` permite un comando de consola cuando hay un jugador asesino, con `%player%`, `%mob%`, `%world%`, `%x%`, `%y%`, `%z%`. El gigante tiene además una tabla de loot con cantidades/probabilidades. Los encuentros montados etiquetan a principal y pasajero; los diagnósticos pueden listar ambas entidades.

## Ambientación, avisos y recompensas

`caverns.ambient` controla sonidos ponderados y escalado por profundidad; `footsteps` los pasos fantasma. `tension` controla latidos. `boss-bar`, `spawn-titles`, `warnings` y `trails` tienen sus interruptores/listas/radios. `achievements` controla hitos de kills, sonido, anuncio y pagos opcionales de Vault; `leaderboards` actualiza rankings para PlaceholderAPI. Los valores se pueden editar sin activar mobs deshabilitados.

PlaceholderAPI: `%caveslite_kills%`, `%caveslite_kills_<mob>%`, `%caveslite_next_threshold%`, `%caveslite_last_achievement%`, `%caveslite_top_kills_1%`, `%caveslite_top_kills_1_name%`, `%caveslite_top_kills_<mob>_1%` y su variante `_name`. Vault sin proveedor de economía se omite; ambos plugins son opcionales.

## Idiomas y Dialogs

`language: es` o `en`; ambos archivos se extraen sin sobrescribir ediciones. Respaldo por clave: archivo seleccionado del usuario → idioma incluido seleccionado → español del usuario → español incluido → inglés incluido. Un código desconocido recurre al respaldo y los nombres de ruta inseguros se rechazan.

Los textos visibles pueden referirse a `@lang:<clave>`. Los textos predeterminados de 1.12.1 se reconocen en memoria; las personalizaciones se conservan. Los IDs, materiales, comandos y valores de juego no se traducen. Guardar un panel sin editar el nombre conserva su referencia traducible.

`dialogs.yml` permite títulos, descripciones, iconos y columnas. El render de Dialogs aplica blanco/grises también a archivos antiguos y colores RGB. Los nombres activos aparecen blancos; los inactivos, grises tachados. Las antiguas claves active-prefix/inactive-prefix están obsoletas. Los materiales y el fondo nativo conservan la apariencia del cliente.

## Comandos y alias

`/clite` y `/caveslite` comparten `caveslite.command` (OP por defecto). Consulta la tabla del README. `summon` admite alias `spawn`; `options`, `option` y `opciones`; `reload`, `r`. El debug antiguo `/clite herobrine debug [jugador]` se conserva como alias de compatibilidad, pero la ayuda y TAB usan `/clite debug herobrine [jugador]`.

Desde consola indica jugador para Herobrine/kills y coordenadas con mundo para invocar mobs. Los paneles requieren jugador. `/clite kill` elimina solo entidades/artefactos CavesLite reconocidos; `kill herobrine` elimina sus encuentros. Revisa [TESTING.md](TESTING.md) antes de aprobar la preview.
