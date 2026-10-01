# DangerousCaves (Lite) — Documentación completa

Versión reducida y ampliada de **Dangerous Caves 2** (imDaniX / Evil-Lootlye, licencia MIT), reescrita para **Purpur/Paper 26.3**. Conserva los sonidos ambientales y los 12 mobs personalizados del plugin original, y suma un set de funciones nuevas de ambiente, progresión y rankings.

Este documento explica **cada opción de `config.yml`**, qué hace, y por qué se decidió cada valor por defecto. Si solo buscas instrucciones de instalación, el `README.md` es más corto y va directo a eso.

---

## Requisitos

- **Purpur 26.3** o superior (también funciona en Paper puro, sin las opciones extra de Purpur).
- **Java 25.**
- No compatible con Folia.

### Dependencias opcionales (softdepend)

El plugin nunca deja de funcionar si estas no están instaladas — simplemente esa parte de la función queda apagada:

| Plugin | Para qué se usa | Si no está instalado |
|---|---|---|
| **Vault** | Pagar dinero por matar mobs y por logros. Funciona con cualquier plugin de economía que Vault tenga registrado (CMI, EssentialsX, etc.) — el plugin nunca habla directo con CMI ni Essentials. | No se reparte dinero. Aviso único en consola, no en cada muerte. |
| **PlaceholderAPI** | Expone los placeholders `%dangerouscaves_*%` para tops, scoreboards, tab list, etc. | Los placeholders simplemente no se registran. |

---

## Comandos

Todos bajo `/dcaves` (alias de `/dangerouscaves`), permiso `dangerouscaves.command` (por defecto, solo op):

| Comando | Qué hace |
|---|---|
| `/dcaves list` | Lista los IDs de los 12 mobs personalizados. |
| `/dcaves summon <mob> [x y z [mundo]]` | Invoca un mob. Sin coordenadas, aparece donde estás parado. |
| `/dcaves kill [mob]` | Elimina todos los mobs personalizados vivos, o solo los de un tipo si lo especificas. |
| `/dcaves kills [jugador]` | Muestra el total de muertes de mobs personalizados de ese jugador (o las tuyas si no pones nombre). |
| `/dcaves reload` | Vuelve a leer `config.yml` sin reiniciar el servidor. |

---

## `caverns.ambient` — Sonidos ambientales

Sonidos que se reproducen al azar cerca de un jugador que está en una cueva (poca luz de cielo, bajo `y-max`).

```yaml
caverns:
  ambient:
    enabled: true
    worlds: [world]
    chance: 6.35          # % de probabilidad, revisado en cada tick de jugador
    y-max: 64              # por sobre esta altura, nunca suena
    near: 7                 # el sonido sale de un punto al azar hasta 7 bloques del jugador (0 = en el jugador)
    server-wise: true      # true = lo escucha cualquiera cerca del punto, no solo quien lo disparó
    server-wise-distance: 0 # distancia mínima entre dos sonidos server-wise a la vez (0 = sin límite)
```

### `sounds` — la lista de sonidos

Cada sonido tiene `volume`, `pitch` y `weight` (peso relativo: por defecto 10, entre más bajo, más raro suena en comparación con los demás). Por defecto trae 9 sonidos, desde música de disco hasta el gruñido de aviso del Warden, con los más intensos (`ENTITY_WARDEN_NEARBY_CLOSE`, `BLOCK_SCULK_SHRIEKER_SHRIEK`) con peso bajo para que sean escasos.

### `depth-scaling` — más tenebroso mientras más bajas

```yaml
    depth-scaling:
      enabled: true
      y-full: -32              # a esta altura o más abajo, el efecto está al máximo
      y-none: 60                # a esta altura o más arriba, no hay efecto
      max-chance-multiplier: 3.0 # la "chance" de arriba se multiplica hasta por esto
      pitch-drop: 0.3            # cuánto baja el tono de cada sonido en el punto más profundo
```

### `footsteps` — pasos fantasma

Un sonido de pasos aparece detrás de un jugador que está **solo** (nadie a menos de 20 bloques) y bajo tierra. No hay ningún mob ahí — es puro ambiente.

```yaml
    footsteps:
      enabled: true
      worlds: [world]
      chance: 8              # % revisado cada 3 segundos (60 ticks)
      y-max: 40                # no hay y-min: suena igual de profundo que sea
      require-alone: true
      distance: 4              # a cuántos bloques detrás del jugador aparece
      behind-only: true       # false = en cualquier dirección alrededor
      sound: minecraft:block.stone.step
      volume: 0.6
      pitch: 0.75
```

---

## `tension` — Latido que se acelera

Cuando un mob "que da miedo" está cerca, suena un latido cuyo intervalo baja de `max-interval-ticks` a `min-interval-ticks` mientras el mob se acerca dentro de `radius`.

```yaml
tension:
  enabled: true
  mobs: [watcher, hungering-darkness, smoke-demon]
  radius: 16
  max-interval-ticks: 60   # intervalo del latido en el borde del radio
  min-interval-ticks: 10    # intervalo del latido con el mob encima
  sound: minecraft:block.sculk_sensor.clicking
  volume: 1
  pitch: 1
```

---

## `boss-bar` — Barra de jefe con el mob más cercano

Muestra una bossbar con el nombre del mob personalizado habilitado más cercano a cada jugador.

```yaml
boss-bar:
  enabled: true
  worlds: [world]
  radius: 24                          # distancia máxima para aparecer en la barra
  format: "&4{mob} &7cerca..."        # {mob} se reemplaza por el nombre del mob
  progress: distance                    # "distance" = se llena al acercarse | "health" = vida restante del mob
  color: RED                            # RED, BLUE, GREEN, PINK, PURPLE, WHITE, YELLOW
  style: PROGRESS                       # PROGRESS (sólida) | NOTCHED_6/10/12/20 (segmentada)
  mobs:                                 # true/false por mob — crying-bat apagado por defecto
    ...
```

---

## `spawn-titles` — Título dramático al aparecer un mob

Un flash de texto grande cuando un mob habilitado aparece (por spawn natural, comando, o al abrir un `mimic`).

```yaml
spawn-titles:
  enabled: true
  radius: 20                # solo lo ven jugadores dentro de este radio
  cooldown-ticks: 100        # un mismo jugador no ve otro título antes de 5s, para no saturar en spawns en ráfaga
  fade-in-ticks: 10
  stay-ticks: 40
  fade-out-ticks: 10
  title: "&4¡Cuidado!"
  subtitle: "&7{mob} ha aparecido cerca"
  mobs: { ... }
```

---

## `warnings` — Aviso en Action Bar

Mensaje corto y discreto en la barra de acción mientras un mob habilitado está cerca (revisado 1 vez por segundo).

```yaml
warnings:
  enabled: true
  radius: 10
  message: "&7Sientes que algo te observa..."
  mobs:                     # por defecto solo los mobs "de sigilo": hexed-armor,
    ...                     # hungering-darkness, mimic, smoke-demon, watcher
```

---

## `trails` — Partículas de rastro

Deja un rastro de partículas detrás de mobs específicos, mientras estén dentro del `radius` de algún jugador conectado.

```yaml
trails:
  enabled: true
  radius: 32
  mobs:
    watcher:
      enabled: true
      particle: SOUL
      count: 2
      offset: 0.25
    smoke-demon:
      enabled: true
      particle: SMOKE
      count: 2
      offset: 0.3
    hungering-darkness:
      enabled: false        # ejemplo desactivado: al ser invisible, quizás no quieras delatarlo
      particle: SQUID_INK
      count: 1
      offset: 0.2
```

`particle` acepta cualquier nombre válido del enum `Particle` de Bukkit.

---

## `achievements` — Logros propios del plugin

**Importante:** esto **no es** un logro/avance real de Minecraft (no aparece en el menú de Avances ni como toast oficial arriba a la derecha) — eso requeriría un *datapack* aparte. Es un sistema propio: cuenta las muertes de **cualquier** mob personalizado por jugador y, al llegar a un umbral, muestra un título grande, un sonido y (opcional) un anuncio en el chat de todo el servidor.

```yaml
achievements:
  enabled: true
  broadcast: true                                  # anunciar en el chat del servidor al desbloquear
  sound: minecraft:ui.toast.challenge_complete
  economy:
    enabled: true
    per-kill: 3          # dinero pagado en CADA muerte de mob personalizado (vía Vault)
  thresholds:              # cada umbral tiene su propia etiqueta y su propio pago
    5:
      label: "&6Cazador Novato"
      reward: 25
    15:
      label: "&6Cazador de las Profundidades"
      reward: 75
    30:
      label: "&4Terror de las Cuevas"
      reward: 150
    50:
      label: "&4Leyenda de las Cavernas"
      reward: 300
```

Puedes agregar más umbrales (por ejemplo `100:`) o cambiar los montos libremente. El pago solo ocurre si `economy.enabled` es `true` **y** hay un plugin de economía detrás de Vault.

---

## `leaderboards` — Rankings de muertes

Alimenta los placeholders de "top" para usar en scoreboards, NPCs o tablas de clasificación.

**Por qué es distinto a simplemente sumar en cada muerte:** guardar a disco y reordenar la lista completa en cada muerte (como hacen algunos plugins) puede causar micro-tirones en un servidor con varios jugadores matando mobs seguido. Este sistema actualiza el conteo en memoria al instante, pero **solo reordena y guarda a disco cada `update-interval-ticks` (y solo si hubo cambios)**, y el guardado corre en un hilo aparte del principal. El archivo (`leaderboards.yml`, dentro de la carpeta del plugin) es propio, no toca los datos del jugador salvo el contador por-mob individual.

```yaml
leaderboards:
  enabled: true
  update-interval-ticks: 100   # cada cuánto se reordena/guarda (100 ticks = 5s)
  top-size: 10                  # cuántos puestos se guardan; pedir el puesto 11 devuelve vacío
  total: true                    # ranking combinando TODOS los mobs
  mobs:                          # ranking POR mob — todos apagados por defecto
    watcher: false                # actívalos solo para los mobs que te interesen:
    ...                           # cada uno activado cuesta un poco más de memoria y disco
```

Si solo te importa el ranking del `watcher`, deja todo lo demás en `false` — es justamente para eso que existe el toggle por mob.

---

## `mobs` — Los 12 mobs personalizados

Configuración general de spawn:

```yaml
mobs:
  enabled: true
  try-chance: 25            # % de reemplazar un spawn vanilla por uno personalizado
  worlds: [world]
  y-min: -64
  y-max: 64
  restrict-rename: false     # true = no se pueden renombrar con name tag
  max-light-level: 16        # nivel de luz máximo para spawnear (16+ = deshabilitado)
  replace-mobs:               # qué mobs vanilla se reemplazan
    - ZOMBIE
    - HUSK
    - SKELETON
    - STRAY
    - CREEPER
    - SPIDER
    - WITCH
    - ENDERMAN
```

`priority` en cada mob controla su probabilidad relativa de spawn (fórmula: `prioridad_del_mob / suma_de_todas_las_prioridades`). `priority: 0` lo desactiva del todo (así viene `mimic` por defecto).

### Los 12 mobs

| Mob (`id`) | Comportamiento | Detalle clave de su config |
|---|---|---|
| **alpha-spider** | Genera cave spiders como minions y cubre al jugador en telaraña al atacar. | `cobweb-chance`, `minion-chance` |
| **cave-golem** | Golpea fuerte, solo recibe daño normal con picota, deja caer su "cabeza" (un mineral) al morir. Puede nacer al minar cierto tipo de bloque. | `variants` (los minerales que puede llevar), `spawn-from-block`, `damage-modifier`, `nonpickaxe-modifier` |
| **crying-bat** | Grita y a veces muere sola. Puramente ambiental. | `cry-chance`, `death-chance` |
| **dead-miner** | Coloca antorchas en la oscuridad y suelta objetos al recibir daño. | `place-torches`, `torches-cooldown`, `drop-items` |
| **hexed-armor** | Zombie invisible con armadura maldita; al golpear, intercambia su armadura por la tuya. | `binding-curse`, `apply-chance` |
| **hungering-darkness** | Solo nace en oscuridad total; mata si no hay luz cerca. | `damage`, `remove-on-light`, `night-vision` |
| **lava-creeper** | Al explotar, deja un cráter de fuego, magma y lava. | `block-chances` (fire/magma_block/obsidian/lava), `radius` |
| **magma-monster** | Deja un rastro de fuego y bloques de magma a su paso. | `fire-chance`, `magma-chance`, `requires-target` |
| **mimic** | Se convierte en cofre cuando no tiene objetivo; ataca al abrirlo. **Desactivado por defecto** (`priority: 0`). | `drop-items` |
| **smoke-demon** | Invisible, ciega y aplica wither a quien se acerque, mientras esté en oscuridad. | `harm-radius`, `max-light` |
| **tnt-creeper** | Al explotar, genera TNT extra; también puede detonar una mini-explosión al recibir daño. | `tnt-amount`, `explosion-chance` |
| **watcher** | Se teletransporta detrás tuyo apenas dejas de mirarlo. Puro jumpscare. | usa `head-value` para su textura de cabeza |

Para las cabezas personalizadas (`dead-miner`, `watcher`), `head-value` es el "Value" (con `=` al final) que se copia de [minecraft-heads.com](https://minecraft-heads.com/).

---

## Placeholders (PlaceholderAPI)

Identificador del plugin: `dangerouscaves`. Solo se registran si PlaceholderAPI está instalado.

| Placeholder | Qué devuelve |
|---|---|
| `%dangerouscaves_kills%` | Total de muertes de mobs personalizados del jugador (siempre contado, sin importar `leaderboards`). |
| `%dangerouscaves_kills_<mob>%` | Muertes de ese mob específico. `0` si ese mob no está activado en `leaderboards.mobs`. |
| `%dangerouscaves_next_threshold%` | Muertes que faltan para el próximo logro, o `-` si ya alcanzó el último. |
| `%dangerouscaves_last_achievement%` | Etiqueta del logro más alto alcanzado, o vacío si ninguno. |
| `%dangerouscaves_top_kills_<puesto>%` | Cantidad de muertes de quien ocupa ese puesto en el ranking total. |
| `%dangerouscaves_top_kills_<puesto>_name%` | Nombre de quien ocupa ese puesto en el ranking total. |
| `%dangerouscaves_top_kills_<mob>_<puesto>%` | Cantidad de muertes de quien ocupa ese puesto en el ranking de ese mob. |
| `%dangerouscaves_top_kills_<mob>_<puesto>_name%` | Nombre de quien ocupa ese puesto en el ranking de ese mob. |

Un ranking "por mob" solo tiene datos si ese mob está en `true` dentro de `leaderboards.mobs`. Un puesto fuera de `leaderboards.top-size` devuelve texto vacío.

---

## Créditos

- **Dangerous Caves 2** © imDaniX y Evil-Lootlye, licencia MIT — diseño original de los mobs y los sonidos ambientales, reescrito para la API actual.
- **PaperMC/Paper** y **PurpurMC/Purpur** — API sobre la que corre el plugin.
- **Vault** y **PlaceholderAPI** — puentes opcionales usados para economía y placeholders; ver `LICENSE` para el detalle de licencias.


## Herobrine: sistema de acecho (1.7.1)

Se agregaron tres encuentros inspirados en la mecánica de observación de From The Fog, implementados de forma nativa para Paper/Purpur:

- **LURKING:** Herobrine aparece muy lejos (50-80 bloques por defecto), con desplazamiento lateral, y mira al jugador.
- **STALKING:** aparece a distancia media (25-46 bloques) y mantiene la mirada sobre su objetivo.
- **CREEPING:** aparece muy cerca (3-5 bloques), normalmente detrás del jugador. Puede ejecutar un único Sneaky Strike si no es descubierto.

Los tres encuentros buscan suelo válido alrededor de la altura del jugador, no fuerzan la carga de chunks y desaparecen al superar `stalking.max-lifetime-seconds`. La detección requiere dirección de mirada y línea de visión real. Todas las distancias, duración y parámetros del Sneaky Strike se pueden ajustar en `herobrine.stalking` dentro de `config.yml`.
