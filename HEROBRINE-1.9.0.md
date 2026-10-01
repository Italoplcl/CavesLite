# Herobrine 1.9.0 — Mannequin

- Herobrine ahora usa `EntityType.MANNEQUIN`, no Zombie.
- El Mannequin recibe la textura configurada en `herobrine.skin-value` como perfil completo.
- Mantiene LURKING, STALKING, CREEPING y WATCHER tipo Creaking sin depender de Creaking.
- WATCHER se congela mientras es observado y avanza cuando el jugador aparta la mirada.
- El combate ya no depende de IA de Zombie: persecución, huida y golpe son controlados por el plugin.
- Spawn seguro rechaza líquidos y superficies peligrosas.
- No genera estructuras ni modifica terreno. La única colocación de bloque permitida es una antorcha de redstone en aire sobre soporte válido.
- Comandos de prueba: `/dcaves summon herobrine <jugador> [lurking|stalking|creeping|watcher]` y `/dcaves herobrine debug [jugador]`.

Objetivo: Purpur 26.3 / Java 25.
