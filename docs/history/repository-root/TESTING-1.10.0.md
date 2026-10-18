# DangerousCaves 1.10.0 — pruebas

## Panel
- `/dcaves` abre el Dialog nativo.
- Deben aparecer 18 mobs + Herobrine.
- Abrir varios mobs y comprobar icono, Spawn natural, Prioridad, BossBar, Y min/max, cooldown y máximo activo.
- Guardar, cerrar y volver a abrir: los valores deben persistir.
- `Invocar` debe funcionar aunque Spawn natural esté OFF.
- `Eliminar` debe retirar el tipo seleccionado.

## Nuevos encuentros
Todos deben venir OFF por defecto.
- Giant Zombie: invocación manual; natural sólo de noche, Overworld y en altura configurada.
- Chicken Jockey: pollo + zombie bebé; movilidad muy rápida.
- Killer Bunny: conejo asesino rojo/agresivo.
- Illusioner: entidad vanilla Illusioner funcional.
- Spider Jockey: araña + esqueleto montado.
- Skeleton Horseman: caballo esqueleto + jinete esqueleto.

## Regresión
- Los 12 mobs anteriores y Herobrine siguen OFF por defecto.
- BossBars siguen OFF por defecto.
- Mimic conserva limpieza de 1.9.2.
- `/dcaves summon`, `/dcaves kill` y `/dcaves reload` siguen funcionando.
