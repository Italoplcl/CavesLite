# Preview 2.0 — comprobaciones / QA

Estado: preview, pendiente de aprobación en Minecraft antes de publicar la versión final 2.0.

## Verificación automatizada

`gradle build --no-daemon` incluye seis verificadores en `tools/verification/`; sus clases no se empaquetan en el plugin. `gradle verifyPreview` permite ejecutarlos sin crear el JAR final.

La verificación local con Java 25 y la API Purpur 26.3.build.2650-stable aprobó:

| Verificador | Comprobaciones | Alcance |
|---|---:|---|
| VerifyRelease | 546 | YAML estricto, idiomas, referencias, fallback, placeholders, migración de textos y Nether apagado |
| VerifyEncounterClock | 15 | Tiempo monotónico, reloj del mundo detenido/ajustado, duración de combate/huida |
| VerifyHerobrineRoute | 8 | Rutas simuladas, desniveles, desvíos y pasos bloqueados |
| VerifyDialogPalette | 364 | Ambos idiomas, blanco/grises, colores antiguos/RGB y estados tachados |
| VerifyGiantPreview | 119 | Geometría completa, coordenadas locales, nodos anchos, ajustes/loot y preset aprobado |
| VerifyNetherSpace | 8 | Alturas, colisión, líquidos y chunks no cargados |

Total: 1.060 comprobaciones. Son validaciones de código/configuración y escenarios simulados; no equivalen a 1.057 partidas ni a una prueba del comportamiento completo de Minecraft. Se compilaron todas las clases del plugin. Gradle no está instalado en el entorno local; la ejecución de Gradle se comprueba aparte mediante GitHub Actions si está disponible.

## Prueba manual — español

1. **Instalación nueva:** confirmar que config básica y presets se exportan; 12 mobs activos, los seis acordados apagados, Herobrine activo, Nether OFF. Verificar mundos reales. Una actualización debe conservar el config antiguo y requerir copiar/adaptar el preset para adoptar los nuevos defaults.
2. **Dialogs:** revisar principal, opciones, 18 criaturas y Herobrine en es/en; activos blancos, inactivos grises tachados. Guardar cambios/reabrir. Verificar que un nombre sin editar conserva su traducción. Revisar los nuevos ajustes del gigante.
3. **Comandos:** probar tabla del README y TAB de `/clite debug` → mobs/herobrine. Probar desde consola con jugador/mundo explícito y sin ellos, IDs inexistentes y alias anteriores.
4. **Herobrine:** supervivencia, sin armadura, escudo, god mode ni otra invulnerabilidad. Golpear, observar varios ataques separados por aproximadamente 1,2 s y revisar `dealt/cancelled`. Después equiparse y confirmar reducciones normales. Mirada y persecución entre alturas ya fueron confirmadas por el usuario en 1.12.4; repetir para detectar regresiones.
5. **Herobrine y tiempo:** congelar solo el reloj diurno, no los ticks completos. Debe seguir atacando y comenzar a huir a los 35 s, desaparecer unos 4 s después incluso bloqueado. Ajustar hora adelante/atrás y repetir. Consultar `/clite debug herobrine` mientras está activo. No permitir más de uno por jugador, tampoco mediante invocación manual.
6. **Gigante con IA de Purpur apagada:** en un área amplia, invocar y entrar en supervivencia. Confirmar persecución, rodeo de obstáculos, subida/bajada de peldaños y espera cuando no hay ruta. Construir pasajes de ancho inferior a su cuerpo y techos inferiores a 12 bloques; no debe atravesarlos ni ser invocado dentro de ellos. Repetir en coordenadas alejadas del origen y con escala modificada si el servidor la usa.
7. **Ataques del gigante:** comprobar básico, aviso del pisotón y onda del terremoto. Intentar esquivar el aviso, saltar la onda y cubrirse con pared/piso. Una onda no debe golpear repetidamente al mismo jugador. Verificar daño reducido/protegido sin empuje falso. Confirmar que no crea explosiones reales ni cambia bloques.
8. **Refuerzos y loot:** esperar al menos 30 s, comprobar cantidad, cap 3, máximo dos oleadas y vida 45 s. Matar/retirar al gigante, descargar/cargar el área y reiniciar para comprobar limpieza. Los minions no deben romper puertas, recoger equipo ni dar loot/XP del plugin. Personalizar materiales/rangos/probabilidades/nombre/XP; probar loot desactivado y replace-vanilla. Verificar el comando de muerte común.
9. **Spawn natural:** probar sin comandos de invocación. Confirmar probabilidad/condiciones, cooldowns y caps globales de primarios cargados. Los mobs con enabled false no deben participar. Verificar que el gólem al romper mineral respete también ese estado y sus límites. Un porcentaje de reemplazo no es un objetivo de población total.
10. **Dimensiones:** Nether apagado por defecto. Al habilitarlo explícitamente, probar distintas alturas y espacio para el gigante, exclusión en fortalezas/bastiones y portales permitidos. Comparar multiplicador de portal 0/1/3. El End no produce encuentros naturales.
11. **Rendimiento:** comparar preset básico y performance con carga y ajustes de servidor similares. Revisar consola y perfiles Spark; el preset de rendimiento debe producir menos encuentros, sin cambios automáticos a la guía Paper/Purpur del administrador.

## English checklist

Run the equivalent checks above before approving 2.0: fresh-install versus migration defaults, ES/EN dialogs and grouped commands; repeated Herobrine damage and 35+4 second expiry under stopped/adjusted world time; Giant native ground routing and full-body fit with Purpur Giant AI disabled; warned non-destructive attacks with walls/floors/protections; bounded minions, unload/death cleanup and custom loot; real NATURAL spawning/cooldowns/caps and dimension exclusions; comparable profiling of both presets. Mocked checks and Java compilation do not replace these gameplay observations.
