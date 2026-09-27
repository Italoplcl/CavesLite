# DangerousCaves (Lite) para Purpur 26.3

Version reducida de **Dangerous Caves 2** (imDaniX / Evil-Lootlye, licencia MIT),
adaptada a **Purpur** (fork de Paper) 26.3, con tres funciones nuevas de ambiente.

## Que incluye

- **Sonidos ambientales** en cuevas (`caverns.ambient`), con la probabilidad y el
  tono escalando segun la profundidad (`caverns.ambient.depth-scaling`).
- **Pasos fantasma** (`caverns.ambient.footsteps`): un sonido de pasos aparece
  detras de un jugador solo, bajo tierra. No hay ningun mob real ahi.
- **Latido de tension** (`tension`): cuando un mob que da miedo (por defecto
  `watcher`, `hungering-darkness`, `smoke-demon`) esta cerca, suena un latido
  que se acelera segun la distancia.
- **Los 12 mobs personalizados**, con su config de aparicion (`mobs.*`):
  alpha-spider, cave-golem, crying-bat, dead-miner, hexed-armor, hungering-darkness,
  lava-creeper, magma-monster, mimic, smoke-demon, tnt-creeper, watcher.

Se quitaron: derrumbes, envejecimiento de cuevas, hipoxia, generador de estructuras,
PlaceholderAPI, WorldGuard/GriefPrevention/Lands y bStats.

## Requisitos

- **Purpur 26.3** (o 26.1+; tambien funciona sobre Paper, ya que Purpur es
  compatible con su API) y **Java 25**.
- No es compatible con Folia.

## Compilar

**Opcion facil (GitHub):** sube esta carpeta a un repositorio tuyo. La pestana *Actions*
compila sola con Purpur API y deja el `.jar` en *Artifacts*. Si falla, el log del paso
`gradle build` muestra el error exacto.

**Local:** instala JDK 25 y Gradle 9.1+ y ejecuta `gradle build`. El jar queda en `build/libs/`.

## Instalar

1. Borra `Dangerous-Caves-2_2_13.jar` de `plugins/` (este plugin usa el mismo nombre interno
   `DangerousCaves`, asi que no pueden convivir).
2. Copia el jar nuevo. Al primer inicio crea `plugins/DangerousCaves/config.yml` si no existe.
   Si ya tenias config, conservala: los nombres de opciones antiguos son los mismos, y las
   secciones nuevas (`depth-scaling`, `footsteps`, `tension`) se agregan con sus valores por
   defecto si no existen en tu config.
3. Revisa `mobs.worlds` y `caverns.ambient.worlds` (por defecto solo `world`; lista vacia = todos los
   mundos normales) y `mobs.y-min` (ahora `-64`; antes `4`).

## Comandos (permiso `dangerouscaves.command`, por defecto op)

`/dcaves list` · `/dcaves summon <mob> [x y z [mundo]]` · `/dcaves kill [mob]` · `/dcaves reload`

## Cambios respecto al original

- Los mobs siguen usando las mismas etiquetas (`dangerouscaves:mob-type`, `dc-mob-*`): los que
  ya existen en tu mundo siguen siendo reconocidos.
- Adaptado a la API nueva: efectos (`SLOWNESS`, `STRENGTH`, `NAUSEA`), atributo `MAX_HEALTH`,
  `TNT`, sonidos por registro, cabezas con texturas via Paper/Purpur.
- Opciones que el original leia con otro nombre que el documentado se unificaron con la config:
  `red-torches` y `block-chances.*`.
- Cada sonido ambiental usa su propio `volume` / `pitch` de la config.
- `hexed-armor` ya no puede elegir piezas de armadura "legacy" internas de Material.
- `mimic` sigue desactivado por defecto (`priority: 0`).
- Migrado de la API de Paper a la de **Purpur**, para aprovechar sus opciones extra de
  rendimiento y configuracion (sigue siendo compatible con servidores Paper puros).

## Creditos

- **Dangerous Caves 2** (c) imDaniX y Evil-Lootlye, MIT License. Este proyecto reutiliza
  su diseño de mobs y sonidos ambientales, reescrito para la API actual.
- **PaperMC/Paper** y **PurpurMC/Purpur**: API sobre la que corre el plugin. Ver
  https://github.com/PaperMC/Paper y https://github.com/PurpurMC/Purpur (ambos MIT / GPL
  segun el componente; revisa sus repositorios para el detalle).
- Ver `LICENSE` para el texto completo de la licencia MIT y el aviso de los cambios de este port.
