# Pruebas 1.9.2

Esta build no rediseña las mecánicas de los 12 mobs clásicos.

Comprobar:
1. Con config nueva, ningún mob custom aparece naturalmente y Herobrine no genera encuentros.
2. No aparece ninguna BossBar por defecto.
3. `/dcaves summon <mob>` permite probar cada mob con prioridad 0.
4. Mimic: no crea un cofre si hay otro cofre dentro de 16 bloques.
5. Mimic: no mantiene más de 1 cofre Mimic activo por mundo con defaults.
6. Mimic: un cofre abandonado expira a los 15 minutos.
7. `/dcaves kill` elimina entidades custom y cofres Mimic cargados.
8. Un apagado normal del servidor no deja cofres Mimic cargados abandonados.
