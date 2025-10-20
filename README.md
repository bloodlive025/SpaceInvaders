# SpaceInvaders

Juego tipo "Space Invaders" en Java — servidor/cliente con mensajes de red y renderizado simple.

Resumen
-------

SpaceInvaders es un proyecto educativo que implementa una versión simplificada del clásico juego Space Invaders en Java. El diseño separa claramente la lógica del servidor, el cliente y las estructuras de mensajes para practicar programación de redes, concurrencia y patrones básicos de diseño de juegos.

Características principales
-------------------------

- Arquitectura cliente/servidor.
- Comunicación por mensajes entre cliente y servidor.
- Renderizado y estado del juego en el cliente.
- Código pensado para ser entendible y extensible como proyecto de aprendizaje.

Estructura del repositorio
--------------------------

Root
- `pom.xml` — definición de Maven y dependencias.
- `src/main/java/` — código fuente:
  - `client/` — implementaciones del cliente (`GameClient.java`, `ClientHandler.java`, `ClientNetworkHandler.java`).
  - `server/` — implementación del servidor (`GameServer.java`).
  - `game/` — objetos y lógica del juego (`GameObject.java`, `GameState.java`, `GameRenderer.java`).
  - `messages/` — clases de mensajes (`Message.java`).

Requisitos
---------

- Java JDK 11 o superior (se recomienda 17 o 21).
- Maven 3.6+ para compilar y ejecutar.

Compilar
--------

Desde la raíz del proyecto (donde está `pom.xml`) ejecuta:

```powershell
mvn -DskipTests package
```

Ejecutar el servidor
--------------------

Desde la carpeta del proyecto:

```powershell
mvn -DskipTests exec:java -Dexec.mainClass="server.GameServer"
```

Ejecutar el cliente
-------------------

En otra terminal:

```powershell
mvn -DskipTests exec:java -Dexec.mainClass="client.GameClient"
```

Notas
-----

- Estas instrucciones asumen que el `groupId`/paquete raíz es el directo de `src/main/java`. Si los paquetes son diferentes, ajusta `-Dexec.mainClass` con el nombre de clase totalmente calificado (p. ej. `client.GameClient` si el paquete lo usa).
- El proyecto es principalmente didáctico; si deseas soporte para múltiples clientes o mejoras gráficas, puedo ayudarte a añadir funcionalidades.

