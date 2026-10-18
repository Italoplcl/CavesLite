# DangerousCaves 1.10.1 - prueba dirigida

Esta build corrige regresiones de 1.10.0. No hace falta volver a probar mecanicas antiguas completas.

## 1. Estado OFF y rendimiento
1. Arrancar con todos los mobs desactivados.
2. No usar summon.
3. Jugar 5 minutos y ejecutar `/dcaves kill`.
4. Debe eliminar 0 entidades del plugin.
5. Ejecutar Spark 5-10 min. Revisar especialmente `MobManager.onNaturalSpawn`, `Mimic.cleanupExpiredChests` y `Mimic.onLoad`.

## 2. Dialog
- `/dcaves` debe abrir sin la demora apreciable de 1.10.0.
- Editar `plugins/DangerousCaves/dialogs.yml`, ejecutar `/dcaves reload` y confirmar cambios visuales.
- Cambiar nombre de un mob, activar/desactivar Mostrar nombre y guardar.
- Invocarlo manualmente y comprobar el nombre.
- Probar especialmente Giant Zombie y Chicken Jockey.

## 3. Mimic
- Activar solo Mimic.
- `max-active: 1` y `max-active-chests: 1` deben impedir acumulacion natural.
- No debe crear otro cofre si existe un cofre dentro del radio configurado.
- `/dcaves kill` debe retirar entidades Mimic y cofres Mimic, sin borrar cofres normales.
- Reiniciar y comprobar que no se acumulan cofres.

## 4. Kill
- Con todo OFF y sin summons: `/dcaves kill` debe dar 0.
- Invocar un mob simple: debe eliminar solo ese mob.
- Invocar Chicken Jockey/Spider Jockey/Skeleton Horseman: puede contar las dos entidades que forman el encuentro, pero no debe tocar entidades vanilla sin etiqueta valida del plugin.

## 1.10.3 - Options panel
1. Ejecuta `/dcaves options` como jugador OP.
2. Comprueba los 9 checks globales y cambia varios estados.
3. Pulsa Guardar y vuelve a abrir el panel: los estados deben persistir.
4. Verifica que cada sistema desactivado deje de actuar sin reiniciar.
5. Ejecuta `/dcaves reload` y confirma que los estados se conservan.
