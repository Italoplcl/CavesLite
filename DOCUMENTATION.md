# CavesLite — documentación completa

CavesLite es un plugin independiente para **Purpur/Paper 26.3** y **Java 25**. Su objetivo es ampliar Minecraft vanilla con encuentros raros, criaturas especiales, Herobrine y ambientación subterránea, manteniendo las funciones opcionales y evitando trabajo costoso cuando están desactivadas.

## Filosofía y límites

- Los encuentros especiales vienen **OFF por defecto**.
- `enabled` decide si un encuentro puede aparecer naturalmente; `priority` sólo define su peso frente a otros encuentros habilitados.
- El summon administrativo funciona aunque el spawn natural esté desactivado.
- BossBars individuales vienen OFF por defecto.
- No se generan estructuras, túneles, derrumbes, vines ni terreno nuevo.
- Los artefactos temporales deben estar identificados como propiedad de CavesLite antes de que el plugin los elimine.
- El núcleo no requiere resource pack.

## Requisitos y dependencias

CavesLite requiere Java 25 y está dirigido a Purpur/Paper 26.3. Folia no está soportado. Vault y PlaceholderAPI son `softdepend`: su ausencia no impide iniciar CavesLite.

Vault se usa sólo para recompensas económicas. PlaceholderAPI registra el identificador `caveslite` y los placeholders `%caveslite_*%`.

## Comandos

Comando principal: `/clite`. Alias: `/clite`. Permiso: `caveslite.command` (OP por defecto).

| Comando | Función |
|---|---|
| `/clite` | Abre el Dialog principal de encuentros. |
| `/clite options` | Abre las opciones globales. |
| `/clite list` | Lista los IDs registrados. |
| `/clite summon <mob> [x y z [mundo]]` | Invoca manualmente un encuentro para pruebas. |
| `/clite summon herobrine [jugador] [tipo]` | Invoca un encuentro de Herobrine. |
| `/clite herobrine debug [jugador]` | Muestra información de depuración de Herobrine. |
| `/clite kill [mob]` | Limpia entidades/artefactos propiedad de CavesLite. |
| `/clite kills [jugador]` | Consulta el contador de eliminaciones. |
| `/clite reload` | Recarga config, idiomas, módulos y Dialogs. |

## Dialogs

`dialogs.yml` controla la presentación del panel administrativo: títulos, descripciones, etiquetas, iconos/materiales, prefijos y textos de botones. La lógica y las acciones permanecen en Java.

Cada encuentro dispone de controles comunes cuando corresponden: spawn natural, prioridad, nombre, mostrar nombre, BossBar, altura, cooldown, máximo activo, summon y limpieza.

### `/clite options`

El panel global contiene checks para los módulos que existen actualmente:

1. Ambient Sounds.
2. Footsteps.
3. Tension.
4. BossBars.
5. Spawn Titles.
6. Warnings / ActionBar.
7. Trails / partículas.
8. Achievements.
9. Leaderboards.

Guardar actualiza `config.yml` y aplica los cambios sin reiniciar el servidor.

## Idiomas

`language` en `config.yml` selecciona un archivo dentro de `plugins/CavesLite/lang/`. Se incluyen `es.yml` y `en.yml`. Pueden añadirse idiomas personalizados siguiendo las mismas claves. El sistema usa inglés como fallback cuando falta una traducción.

## Ambientación

### `caverns.ambient`

Controla sonidos ambientales subterráneos. `worlds`, `chance`, `y-max`, `near`, `server-wise` y `server-wise-distance` determinan dónde y cómo se reproducen. Cada entrada de `sounds` define `volume`, `pitch` y `weight`.

`depth-scaling` puede aumentar la frecuencia y bajar el tono a mayor profundidad mediante `y-full`, `y-none`, `max-chance-multiplier` y `pitch-drop`.

### `caverns.ambient.footsteps`

Genera pasos fantasma sin crear una entidad. Permite configurar mundos, probabilidad, altura máxima, requisito de estar solo, distancia, dirección, sonido, volumen y tono.

### `tension`

Produce latidos cuando alguno de los IDs configurados está dentro del radio. El intervalo se acorta a medida que la amenaza se acerca.

### `boss-bar`

BossBar global para encuentros configurados. El progreso puede representar distancia o salud. Además del switch global, cada mob tiene su propia habilitación de BossBar.

### `spawn-titles`, `warnings` y `trails`

Son módulos visuales independientes para títulos de aparición, avisos ActionBar y partículas. Cada uno puede apagarse desde `/clite options` y configurarse en `config.yml`.

## Achievements, Vault y leaderboards

`achievements` cuenta eliminaciones y puede otorgar hitos, sonidos, anuncios y recompensas mediante Vault. No crea advancements vanilla.

`leaderboards` mantiene rankings globales y opcionalmente por mob. El ordenamiento/guardado se realiza periódicamente en vez de escribir a disco en cada muerte.

Placeholders disponibles:

- `%caveslite_kills%`
- `%caveslite_kills_<mob>%`
- `%caveslite_next_threshold%`
- `%caveslite_last_achievement%`
- `%caveslite_top_kills_<rank>%`
- `%caveslite_top_kills_<rank>_name%`
- `%caveslite_top_kills_<mob>_<rank>%`
- `%caveslite_top_kills_<mob>_<rank>_name%`

## Sistema de spawn

La sección `mobs` controla el reemplazo natural: mundos, rango Y, luz máxima, chance global y tipos vanilla elegibles. Cada encuentro tiene además `enabled` y `priority`.

`enabled: false` siempre lo excluye del spawn natural. Entre los encuentros habilitados, `priority` actúa como peso relativo. `priority: 0` equivale a peso nulo y tampoco participa. El summon manual no depende de estas condiciones.

## Encuentros incluidos

| ID | Característica principal |
|---|---|
| `alpha-spider` | Minions y telarañas en combate. |
| `cave-golem` | Golem mineral resistente; interacción especial con picotas. |
| `crying-bat` | Encuentro ambiental basado en gritos. |
| `dead-miner` | Minero subterráneo con antorchas y drops. |
| `hexed-armor` | Entidad invisible asociada a armadura maldita. |
| `hungering-darkness` | Amenaza ligada a oscuridad extrema. |
| `lava-creeper` | Creeper con efecto volcánico al explotar. |
| `magma-monster` | Deja fuego/magma durante su movimiento. |
| `mimic` | Se disfraza como cofre y reacciona al ser descubierto. |
| `smoke-demon` | Amenaza invisible con humo/wither en oscuridad. |
| `tnt-creeper` | Creeper con TNT adicional. |
| `watcher` | Encuentro de observación/jumpscare. |
| `giant-zombie` | Giant vanilla convertido en encuentro raro. |
| `chicken-jockey` | Jockey especialmente rápido/móvil. |
| `killer-bunny` | Variante pequeña, rápida y agresiva. |
| `illusioner` | Usa el Illusioner vanilla como encuentro especial. |
| `spider-jockey` | Encuentro compuesto araña + jinete. |
| `skeleton-horseman` | Encuentro montado de esqueleto y caballo. |

Todos vienen desactivados para spawn natural en una instalación nueva.

## Mimic

Mimic usa identificación persistente para distinguir sus cofres de cofres normales. Sus controles específicos incluyen:

- `max-active`: máximo de Mimics vivos.
- `min-distance-from-chest`: separación respecto de cofres existentes.
- `max-active-chests`: máximo de cofres Mimic activos por mundo.
- `lifetime-seconds`: expiración de un cofre no descubierto.
- `remove-on-unload`: limpieza al descargar el chunk.

`/clite kill` y el apagado normal sólo deben retirar artefactos que CavesLite pueda identificar como propios.

## Herobrine

Herobrine es un sistema independiente de acecho y viene con `enabled: false`. Usa un Mannequin y no depende de un fake player persistente ni de un resource pack.

Tipos disponibles: `observe`, `flee-on-sight`, `behind`, `false-chase`, `outside-observer`, `watcher`, `lurking`, `stalking` y `creeping`. El sistema usa intervalos aleatorios por jugador, límites de distancia, línea de visión y posiciones seguras cargadas.

`watcher` se congela al ser observado y se aproxima cuando el jugador deja de mirarlo. `creeping` puede ejecutar un Sneaky Strike configurado. Herobrine puede colocar raramente una antorcha de redstone como pista ambiental. Su combate sólo comienza al ser atacado y dispone de duración, huida, daño, knockback y BossBar opcional.

## Nombres y encuentros compuestos

Cada mob puede configurar `name` y `show-name`. El nombre admite el formato de texto utilizado por el plugin. En Chicken Jockey, Spider Jockey y Skeleton Horseman sólo la entidad principal muestra el nombre para evitar etiquetas duplicadas.

## Identificación y limpieza

CavesLite usa su propio namespace persistente `caveslite` y tags `caveslite-mob*`. `/clite kill` valida IDs registrados antes de retirar entidades y artefactos. No debe usarse como un comando genérico para matar mobs vanilla.

## Rendimiento

Cuando no hay encuentros naturales habilitados, `MobManager` mantiene vacío su pool natural y el listener de spawn retorna sin hacer búsquedas costosas. Las tareas periódicas deben salir rápidamente cuando su módulo no tiene trabajo. Mimic evita escanear chunks que no indiquen contener artefactos suyos.

Para validar rendimiento utiliza Spark y la pauta de `TESTING.md`. No se fija un porcentaje universal: la comparación debe hacerse bajo el mismo entorno y carga.

## Archivos del proyecto

- `README.md`: presentación, instalación y uso rápido.
- `DOCUMENTATION.md`: referencia técnica vigente.
- `TESTING.md`: única pauta de validación vigente.
- `config.yml`: mecánicas y comportamiento.
- `dialogs.yml`: presentación de Dialogs.
- `lang/es.yml` y `lang/en.yml`: textos traducibles.

No se mantienen documentos históricos por versión dentro del source principal; Git conserva ese historial.

## Origen y licencia

CavesLite nació inspirado por **Dangerous Caves 2**, de imDaniX / Evil-Lootlye. La atribución MIT correspondiente al material histórico se conserva en `LICENSE`. CavesLite es actualmente un proyecto independiente con identidad y evolución propias.

---

## Administración y configuración (1.12.0)

### Comandos
- `/clite` abre el panel principal. `/clite` es su único alias.
- `/clite options` abre los interruptores globales.
- `/clite debug mobs` muestra los mobs CavesLite actualmente cargados, su ubicación y cuánto tiempo llevan activos.
- `/clite debug mobs <id>` limita el diagnóstico a un mob.
- `/clite reload` relee los YAML cuando fueron editados manualmente. **No es necesario después de Guardar en un Dialog**: Guardar escribe `config.yml` y aplica `reloadAll()` inmediatamente.

### Spawn común de mobs
`enabled` decide si el mob participa del spawn natural. Invocarlo manualmente sigue siendo posible aunque esté desactivado.

`priority` es un **peso relativo**, no un porcentaje ni una frecuencia. La interfaz usa 0–10 porque diez niveles son suficientes para expresar relaciones simples. Si Giant tiene 10 e Illusioner 5, Giant tiene el doble de peso cuando CavesLite debe escoger entre ambos; esto no significa 10% y 5%. Prioridad 0 equivale a no tener peso en la selección natural.

`cooldown-seconds` es el tiempo mínimo entre dos apariciones naturales del mismo tipo. Es **global para ese tipo de mob**, no por chunk. Un cooldown de 300 no crea un mob cada cinco minutos: sólo impide otro spawn natural de ese tipo durante esos cinco minutos después de uno exitoso.

`max-active` también es **global para ese tipo de mob**. Los UUID conocidos se conservan mientras el chunk está descargado y se reactivan al cargarlo, por lo que volar generando/cargando chunks no debe saltarse el límite. Los mobs no fuerzan chunks a permanecer cargados. CavesLite no usa chunks forzados para estas entidades.

Los mobs usan `setRemoveWhenFarAway(true)`, por lo que siguen la política normal de despawn de entidades del servidor. Un chunk descargado deja de ser procesado por los ticks de CavesLite; al cargarse de nuevo, las entidades persistentes etiquetadas se vuelven a registrar.

### Nombres configurables
`name` es la única fuente del nombre visible. Se usa para la entidad y para las superficies que anuncian el mob (Dialog, tooltip, Spawn Title y BossBar). El identificador interno (`cave-golem`, `mimic`, etc.) se conserva sólo para configuración/comandos y lógica interna.

### Drops y comando al morir
Todos los mobs tienen `extra-drops`, una lista de materiales adicionales. No reemplaza los drops vanilla ni las mecánicas especiales existentes. Dead Miner y Mimic conservan `drop-items` como su lista propia editable desde el Dialog; Cave Golem conserva `variants`, porque su bloque de cabeza es también su drop.

Todos los mobs tienen además `death-command.enabled: false` y `death-command.command`. El comando se ejecuta desde consola sólo cuando el mob muere a manos de un jugador. Placeholders disponibles: `%player%`, `%mob%`, `%world%`, `%x%`, `%y%`, `%z%`.

Herobrine conserva su sistema `reward.command`, desactivado por defecto, porque su muerte pertenece a su controlador de encuentro propio.

### Opciones especiales en Dialogs
Además de los controles comunes, los Dialogs exponen las opciones propias que más afectan a cada mecánica: probabilidades de Alpha Spider, grito/muerte de Crying Bat, cabeza/drop/antorchas de Dead Miner, cabeza de Watcher, intercambio de Hexed Armor, luz de Hungering Darkness, fuego/magma de Magma Monster, TNT Creeper, velocidad del Chicken Jockey, límites del Mimic y variantes del Cave Golem. Las texturas Base64 de Dead Miner y Watcher pueden pegarse directamente en su panel.

### Herobrine
El panel de Herobrine incluye estado, alturas, cooldown, intervalo de encuentros, distancias, BossBar, vida/daño, Sneaky Strike, antorchas de redstone, skin Base64 y su comando de recompensa. Las opciones de comportamiento más avanzadas siguen documentadas y editables en `config.yml`.

## Sonidos ambientales
Se configuran en `config.yml` bajo `caverns.ambient`. `/clite options` activa o desactiva el sistema completo; la lista y sus parámetros se editan en YAML.

`chance` es la probabilidad base evaluada por el ciclo de sonidos ambientales para un jugador que cumple las condiciones (mundo, cueva y altura). `depth-scaling` puede multiplicar esa probabilidad cuanto más profundo está el jugador.

Cada entrada de `sounds` tiene `volume`, `pitch` y `weight`. **Weight es peso relativo, no porcentaje.** Si sólo existen dos sonidos con pesos 10 y 2, el primero tiene cinco veces más peso de selección que el segundo. Cambiar el peso no cambia la probabilidad de que ocurra un evento ambiental; sólo cambia cuál sonido es escogido una vez que el evento ya fue aceptado.

Valores actuales incluidos por defecto:
- `MUSIC_DISC_11` (peso 10)
- `ENCHANT_THORNS_HIT` (10)
- `ENTITY_GHAST_SCREAM` (8)
- `MUSIC_DISC_13` (10)
- `AMBIENT_CAVE` (10)
- `BLOCK_SCULK_SHRIEKER_SHRIEK` (4)
- `ENTITY_WARDEN_NEARBY_CLOSE` (2)
- `ENTITY_ENDERMAN_STARE` (6)
- `ENTITY_WITHER_AMBIENT` (3)

Se pueden agregar o quitar sonidos válidos de Minecraft siguiendo la misma estructura. Después de editar manualmente el YAML usa `/clite reload`.
