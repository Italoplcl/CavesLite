# CavesLite 1.12.3 — Herobrine

Cambios limitados al combate de Herobrine:

- Persecución por rutas sobre suelo: sube un bloque por transición, baja hasta tres y puede rodear obstáculos o usar escaleras para alcanzar otro piso. Búsqueda limitada a 512 nodos y chunks ya cargados. No vuela, atraviesa paredes ni escala paredes verticales sin ruta.
- Conserva el seguimiento de mirada durante combate.
- Ataque con fuente explícita MOB_ATTACK, atribuido a Herobrine, alcance comprobado después de moverse y línea de visión obligatoria. El cooldown usa la fecha del siguiente ataque en vez de depender de una coincidencia de módulos.
- El empuje adicional se aplica únicamente si el ataque redujo vida o absorción. Un golpe cancelado, bloqueado o contra un jugador invulnerable ya no produce el empuje falso del plugin.
- `/clite herobrine debug` añade `attack=[base=..., dealt=..., cancelled=...]`. `dealt` incluye absorción y se expresa en puntos de vida (2 puntos = 1 corazón). El diagnóstico se consulta mientras el encuentro está activo.

El daño configurado se conserva: `herobrine.damage: 2` es daño base, reducido normalmente por armadura, resistencia, escudo y protecciones. No se fuerza daño ignorando otros plugins ni se aumenta el daño sin cambiar esa opción. El código previo ya llamaba a damage(); por eso no puede atribuirse el daño nulo observado a una causa concreta sin la prueba del servidor. La fuente explícita elimina la dependencia del origen inferido y el diagnóstico permite comprobar la reducción/cancelación real.

Validación: compilación completa con JDK 25 y API Purpur 26.3; 424 comprobaciones de configuración/idiomas, 8 de espacio Nether y 8 escenarios de rutas aprobados. No se ejecutó un cliente Minecraft ni una prueba de daño real con jugadores.

Instalación: detener el servidor, sustituir el JAR de CavesLite por `build/libs/CavesLite-1.12.3.jar` y arrancar. Conservar config e idiomas. Probar primero en supervivencia sin armadura, escudo ni invulnerabilidad; golpear a Herobrine, subir/bajar por escalones y consultar el debug durante combate. Repetir equipado para observar la reducción normal de daño. Si `cancelled=true`, una protección está cancelando el golpe; si `base=0`, revisar la configuración. `dealt=0` también puede corresponder a escudo, invulnerabilidad o intervalos de inmunidad.

English: bounded ground routing now handles one-block climbs, drops up to three blocks and stair/detour paths. Combat uses an explicit attributed MOB_ATTACK source, line of sight and timestamp cooldown. Additional knockback only follows an actual health/absorption decrease. Debug reports requested versus applied damage and cancellation. Existing base damage and server protections are preserved. Compilation and 440 automated checks passed; in-game damage and navigation still require server validation.
