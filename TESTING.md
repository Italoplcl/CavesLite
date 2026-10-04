# CavesLite 1.11.0 — validación final

Esta es la única pauta de pruebas vigente del proyecto.

## 1. Inicio limpio

1. Retirar el JAR anterior.
2. Usar una carpeta `plugins/CavesLite/` nueva para validar defaults.
3. Iniciar Purpur/Paper 26.3 con Java 25.
4. Confirmar que no existan errores de carga y que se creen `config.yml`, `dialogs.yml` y `lang/`.
5. Confirmar que todos los encuentros especiales y BossBars individuales estén OFF por defecto.

## 2. Comandos y Dialogs

- `/clite` abre el Dialog principal sin demora apreciable.
- `/clite` funciona como alias.
- `/clite options` muestra los 9 checks globales y Guardar persiste los cambios.
- Editar `dialogs.yml`, ejecutar `/clite reload` y confirmar que cambia la presentación.
- Probar nombre editable y `show-name` en un mob simple y uno compuesto.

## 3. Seguridad de limpieza

1. Con todos los encuentros OFF, permanecer unos minutos en una zona con mobs vanilla.
2. Ejecutar `/clite kill`.
3. El resultado esperado es 0 entidades CavesLite eliminadas y los mobs vanilla deben permanecer.
4. Invocar manualmente un encuentro, repetir el comando y comprobar que sólo se elimina contenido propiedad de CavesLite.

## 4. Mimic

- Activar sólo Mimic.
- Confirmar límite activo y distancia mínima entre cofres.
- Confirmar que no se acumulan cofres al cargar/descargar chunks.
- Confirmar expiración de cofres no descubiertos.
- `/clite kill mimic` debe retirar Mimics/cofres etiquetados sin tocar cofres normales.
- Reinicio/apagado normal no debe dejar artefactos temporales sin control.

## 5. Encuentros

- Probar `/clite summon <id>` con spawn natural OFF.
- Giant Zombie, Chicken Jockey, Killer Bunny, Illusioner, Spider Jockey y Skeleton Horseman deben conservar su mecánica distintiva.
- En encuentros compuestos, el nombre visible debe aparecer sólo en la entidad principal cuando `show-name` esté activo.
- Probar Herobrine en lurking, stalking, creeping y watcher.

## 6. Opciones globales

Comprobar individualmente los checks de Ambient Sounds, Footsteps, Tension, BossBars, Spawn Titles, Warnings, Trails, Achievements y Leaderboards. Desactivar una opción debe impedir que su módulo produzca efectos visibles/audibles sin requerir reinicio.

## 7. Rendimiento

Con todos los encuentros OFF, ejecutar una sesión comparable con Spark. Revisar especialmente `MobManager.onNaturalSpawn()`, `MobManager.tick()`, `Mimic.cleanupExpiredChests()` y tareas periódicas. El objetivo es que CavesLite permanezca prácticamente inactivo cuando sus sistemas están desactivados; comparar contra el perfil de referencia anterior en vez de exigir un porcentaje absoluto.

Luego repetir con los encuentros que realmente se usarán en producción.


## Dialog reopening regression (1.11.1)
1. Ejecuta `/clite` o `/clite`.
2. Abre Cave Golem.
3. Cierra el Dialog sin guardar.
4. Ejecuta nuevamente `/clite` o `/clite`.
5. Cave Golem y cualquier otro encuentro deben abrir normalmente.
6. Repite el ciclo varias veces y prueba Guardar, Invocar y Eliminar.

## 1.12.0 - Dialogs/configuración
1. `/clite` abre el panel; `/clite` también; `/clite` debe ser comando desconocido.
2. Abrir/cerrar varios mobs repetidamente: ningún botón debe quedar muerto.
3. Activar un mob, Guardar y volver al panel: indicador verde. Desactivarlo: rojo.
4. Renombrar Cave Golem, Guardar e invocar: entidad, tooltip, Spawn Title y BossBar deben usar el nombre nuevo.
5. Crying Bat y Hexed Armor muestran sus opciones especiales y persisten tras Guardar.
6. Dead Miner y Watcher aceptan Base64 desde Dialog.
7. Editar drops y comando al morir; matar el mob con jugador y comprobar ambos. Con comando OFF no debe ejecutarse.
8. `/clite debug mobs` muestra cantidad, edad y ubicación sin cargar chunks adicionales.
9. Descargar/recargar el chunk de un mob con `max-active: 1`: no debe poder generarse un segundo mientras el primero siga persistente.
10. `/clite options`: Guardar debe aplicar inmediatamente sin `/clite reload`.
