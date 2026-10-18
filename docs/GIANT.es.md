# Zombie Gigante — 2.0 preview

El gigante de CavesLite usa objetivos y navegación propios sobre la API de Paper. Solo afecta a entidades identificadas como `giant-zombie`; no activa ni cambia `have-ai`/`have-hostile-ai` globales de Purpur y no necesita otro plugin de IA.

## Movimiento y espacio

Persigue al jugador elegible más cercano dentro de 32 bloques (supervivencia/aventura). Usa navegación terrestre nativa y su hitbox real, más una comprobación de volumen completo en los puntos de ruta. Puede rodear obstáculos y superar desniveles transitables; no se teletransporta para atravesarlos. Si no existe ruta donde quepa, detiene esa ruta y vuelve a calcular periódicamente. No escala paredes verticales ni entra deliberadamente en túneles pequeños.

El cuerpo nominal requiere 3,6 bloques de ancho y 12 de alto. Al invocar se comprueba espacio; después se valida también el tamaño real de la entidad. Una escala de servidor distinta puede necesitar más espacio. Se rechazan paredes, techos, líquidos dentro del cuerpo y chunks no cargados en las comprobaciones. Los puntos de navegación se convierten desde el origen del nodo al centro correspondiente al ancho del gigante.

`movement.enabled`, `speed` (atributo de movimiento, 0.23 inicial), `follow-range` y `repath-ticks` controlan la persecución. El paso máximo inicial es de un bloque. Desactivar persecución no desactiva por sí solo los ataques cercanos. La navegación real alrededor de terrenos complejos debe validarse dentro del servidor; una ruta imposible debe resultar en espera, no en movimiento forzado.

## Ataques incluidos

| Ataque | Daño base | Radio | Cooldown | Advertencia |
|---|---:|---:|---:|---:|
| Básico | 6 puntos | 3 bloques | 30 ticks | 0 ticks |
| Pisotón | 8 puntos | 4,5 bloques | 200 ticks | 24 ticks |
| Terremoto | 6 puntos | 8 bloques | 360 ticks | 32 ticks |

Dos puntos de daño equivalen a un corazón antes de reducciones; 20 ticks equivalen aproximadamente a un segundo con el servidor a 20 TPS. Los cooldowns de habilidades usan el reloj monotónico del plugin. Armadura, escudo, absorción y protecciones conservan su funcionamiento.

El básico golpea a un objetivo cercano y empuja moderadamente (0.45). El pisotón detiene la navegación, avisa con sonido/partículas y afecta a jugadores cercanos tras la preparación (empuje 0.7). El terremoto avisa y emite una onda expansiva: radio creciente, un impacto como máximo por jugador y por onda, empuje 0.6. Saltar durante el paso de la onda puede evitarla porque requiere que el jugador esté en el suelo. No es una animación de cámara ni un recurso de cliente.

Los ataques exigen proximidad vertical, línea de visión y un trazado entre los pies del gigante y el jugador para no golpear a través de paredes o pisos. El daño efectivo se comprueba antes de aplicar el empuje adicional. Durante preparación/onda no sigue caminando ni intercala otros ataques. No se crean explosiones reales ni se modifican bloques.

Cada bloque `attacks.basic`, `attacks.stomp`, `attacks.quake` tiene `enabled`, `damage`, `radius`, `knockback`, `cooldown-ticks` y `warning-ticks`. `attacks.quake.wave-step` controla cuánto avanza la onda por actualización del plugin (cada 4 ticks). Sonidos de aviso/impacto, volumen y tono están en `sounds`; `effects.particles` controla las partículas. Sonidos vacíos y volumen 0 silencian los avisos correspondientes.

## Refuerzos

`reinforcements` permite zombies de apoyo. Defaults: dos por invocación, máximo tres cargados por gigante, máximo dos oleadas por entidad y cooldown de 30 segundos. Empiezan a intentarse después de ese cooldown. Cada zombie tiene vida 16 y caduca a los 45 segundos. No sustituyen apariciones NATURAL ni participan del pool de pesos del plugin.

Se colocan en posiciones cargadas con espacio y suelo sólido, reciben objetivo y no pueden romper puertas, recoger objetos ni generar refuerzos de zombie por su atributo. Los minions no dan drops/experiencia propios de CavesLite. Se retiran al morir/desaparecer su dueño, al caducar o con la limpieza del plugin; al volver a cargar se comprueba dueño/expiración. Las oleadas consumidas se guardan en la entidad para no recuperarlas al recargar settings.

Opciones: `enabled`, `amount`, `max-active`, `max-waves`, `cooldown-seconds`, `lifetime-seconds`, `health`. El código acota máximos de trabajo para evitar valores no finitos o cantidades extremas: hasta 8 por intento, 12 simultáneos y 10 oleadas; los valores aprobados son mucho menores.

## Botín personalizable

`loot` es una tabla de drops al morir, no botín privado separado para cada jugador. Se combina con los drops vanilla y `extra-drops`, salvo que actives `replace-vanilla`.

```yaml
loot:
  enabled: true
  replace-vanilla: false
  experience: 30
  items:
    - material: ROTTEN_FLESH
      min: 4
      max: 8
      chance: 100
    - material: IRON_INGOT
      min: 1
      max: 2
      chance: 25
```

Cada entrada tiene tirada independiente de 0–100%, rango de cantidad y material válido. `name` permite un nombre personalizado opcional. Materiales inválidos se omiten; cantidades se acotan al tamaño máximo de stack. `loot.enabled: false` desactiva esta tabla/experiencia personalizada, no elimina automáticamente los drops vanilla ni los `extra-drops` comunes. El comando de muerte común también sigue disponible.

## Aparición y administración

Preset básico: activo, peso 1, cooldown natural 600 segundos, cap 1 y Nether OFF. En Overworld aparece de noche, con cielo y Y 80–320; en Nether autorizado no se aplican esas alturas/contexto, pero debe caber y respetar exclusiones estructurales.

`/clite` → Zombie gigante ofrece vida, estado, peso y controles principales de persecución/ataques/refuerzos/loot. YAML contiene todos los ajustes avanzados. Prueba `/clite summon giant-zombie` en un espacio amplio, `/clite debug mobs giant-zombie` y `/clite kill giant-zombie` para limpiar también los refuerzos.

Las habilidades del gigante no destruyen terreno. Otras criaturas y otros plugins conservan sus propias reglas; esta garantía no convierte todo el servidor en libre de daño a bloques.
