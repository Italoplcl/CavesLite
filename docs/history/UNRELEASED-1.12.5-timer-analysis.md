Historical working notes — this package was NOT approved as a release. The authorized correction is incorporated into the 2.0 preview.

# CavesLite 1.12.5 — temporizadores de Herobrine

Revisión del caso reportado: Herobrine golpeado permanece más de diez minutos junto al jugador, sin atacar ni desaparecer.

## Errores encontrados y correcciones

Los temporizadores consultaban `getFullTime()` del primer mundo cargado. Esa API usa el reloj del mundo, no un reloj independiente del encuentro. Un reloj pausado o ajustado puede detener/retrasar el timeout y mantener bloqueado el siguiente ataque. No se confirmó si el reloj del servidor del usuario estaba congelado: es un defecto comprobado del código que puede explicar el síntoma, no una atribución confirmada a CMI.

Todos los tiempos de Herobrine pasan a un reloj monotónico de tiempo transcurrido, independiente del día/noche, `/time`, reloj del sistema y del mundo elegido. El cooldown de ataque, tiempo de combate, observación, vida de encuentros y programación de encuentros usan la misma referencia. No se cambian sus valores configurados ni probabilidades.

La huida convertía `flee-duration-seconds: 4` en un mínimo de 20 segundos por un error de unidades. Ahora se respetan 4 segundos (mínimo válido: 1 segundo). La desaparición por timeout se comprueba antes del movimiento y de efectos visuales; una ruta bloqueada no prolonga la huida. La eliminación se realiza antes del sonido/partículas para que un fallo de esos efectos no deje el mannequin presente. La persecución de combate se actualiza en cada llamada programada, sin depender de una coincidencia de módulo con el reloj del mundo.

Valores incluidos: hasta 35 segundos de combate + 4 segundos de huida, aproximadamente 39 segundos desde el primer golpe si no hay una salida anterior. Puede desaparecer antes por salud baja, distancia o pérdida del objetivo. El scheduler comprueba cada 4 ticks; retrasos/pausas del servidor retrasan la ejecución hasta que vuelve a procesar tareas.

`/clite herobrine debug` muestra `combat`, `fleeing`, `combatSeconds` y `fleeSeconds`, además del daño solicitado/aplicado. Sin combate, siguen incluidos 8 segundos de observación continua y 45 segundos de vida máxima para los tipos stalking/lurking/creeping/watcher, con las condiciones de observación existentes.

## Validación

Compilación de todas las clases con Java 25 y API Purpur 26.3 correcta. Trece comprobaciones nuevas cubren el reloj independiente, 35 + 4 segundos, límite de huida, tiempos de mundo congelados/ajustados, configuración inválida/overflow y rollover de nanoTime. Se mantienen aprobadas 424 comprobaciones de idiomas/configuración. Son pruebas de temporización simulada y compilación, no una sesión Minecraft ni una reproducción confirmada del estado exacto del servidor del usuario.

Instalar con el servidor detenido sustituyendo el JAR por `build/libs/CavesLite-1.12.5.jar`. Conservar la configuración. Tras arrancar, invocar Herobrine, golpearlo y comprobar con `/clite herobrine debug` que avancen los segundos; debería huir al llegar a 35 y desaparecer unos 4 segundos después aunque esté bloqueado. Probar en supervivencia sin invulnerabilidad para revisar ataques. El caso de daño nulo debe interpretarse junto con `dealt`/`cancelled`; esta versión no fuerza daño contra protecciones ni cambia el nombre del mannequin.

English: Herobrine timers now use an elapsed monotonic clock independent of world/daylight/time adjustments. A units bug forced flee duration to at least 20 seconds despite the configured 4; the configured seconds are now honored. Expiration precedes movement and removal precedes cosmetic effects. Debug includes combat/flee state and elapsed seconds. Defaults remain 35 seconds combat plus 4 seconds escape. Java compilation, 13 timer regression checks and 424 existing configuration/language checks passed; in-game validation remains required.
