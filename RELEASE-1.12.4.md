# CavesLite 1.12.4 — Dialogs

Paleta de texto de los paneles administrativos unificada con la identidad de CavesLite: títulos y acciones en blanco, descripciones y texto secundario en gris/gris oscuro. Los nombres de encuentros activos se muestran blancos; los inactivos, grises tachados. Se aplica también a Herobrine. Los estados usan este formato fijo; las antiguas claves main.active-prefix/main.inactive-prefix quedan obsoletas y se conservan solo para compatibilidad de archivos.

La presentación se aplica al renderizar todos los textos de AdminDialogs, incluidos colores de nombres configurados, personalizaciones antiguas y formatos RGB. No sobrescribe los archivos del usuario. También se actualizaron los textos incluidos en español e inglés. Los colores de texto negro se representan como gris oscuro para seguir siendo legibles sobre el fondo nativo del Dialog.

Se mantienen los materiales e iconos de Minecraft y el fondo nativo del cliente. Esta versión modifica solo la presentación de los Dialogs: no cambia spawns, daño, probabilidades, recompensas, mensajes externos a los paneles ni las mecánicas de Herobrine. Incluye los cambios anteriores de 1.12.3.

Validación: código completo compilado con JDK 25 y API Purpur 26.3; 338 comprobaciones de paleta (ambos idiomas, códigos antiguos y RGB, tachado de inactivos y ausencia de tachado en activos) y 424 comprobaciones de idiomas/configuración aprobadas. No se abrió un cliente Minecraft para inspección visual.

Instalación: detener el servidor, sustituir el JAR por build/libs/CavesLite-1.12.4.jar y arrancar. Conservar config.yml, dialogs.yml y lang/. Revisar /clite, /clite options, un panel de mob y el de Herobrine, en español e inglés. Confirmar que al guardar y reabrir el estado activo/inactivo coincide con el nombre blanco/tachado.

English: administration Dialog text now uses white and grays. Active encounters have white names; inactive encounters have gray struck-through names, including Herobrine. The presentation layer also handles existing customized/legacy/RGB text without rewriting user files. Native Minecraft item icons/background remain. Gameplay and text outside Dialogs are unchanged. Compilation, 338 palette checks and 424 language/configuration checks passed; in-game visual inspection remains pending.
