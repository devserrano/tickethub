# TicketHub

Mesa de ayuda web: los usuarios reportan problemas, el equipo de soporte los atiende y todo queda registrado con folio, responsable, tiempos (SLA) y evidencia.

Proyecto final de **Sistemas de la Información** — FES Aragón, UNAM.

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Maven |
| Seguridad | Spring Security + JWT |
| Datos | MySQL 8.4, Spring Data JPA, Flyway |
| Archivos | S3 (MinIO en local) |
| Correo | Spring Mail (Mailpit en local) |
| Reportes | OpenPDF (PDF), Apache POI (Excel) |
| Pruebas | JUnit 5, Postman/Newman, k6 |
| Infra | Docker, docker-compose, GitHub Actions |

## Cómo levantarlo en local

**Requisitos:** JDK 21, Docker Desktop, IntelliJ IDEA (o Maven 3.9+).

1. Clona el repo:
   ```bash
   git clone https://github.com/<usuario>/tickethub.git
   cd tickethub
   ```
2. Crea tu `.env` (no se sube a Git) y cambia los valores `CAMBIAR` por contraseñas tuyas:
   ```bash
   cp .env.example .env
   ```
3. Levanta MySQL, MinIO y Mailpit y espera a que MySQL diga `healthy`:
   ```bash
   docker compose up -d
   docker compose ps
   ```
4. Corre la API desde IntelliJ (`TicketHubApplication`, con el directorio de trabajo en la raíz del proyecto
   para que lea el `.env`) o con Maven:
   ```bash
   mvn spring-boot:run
   ```
   Al arrancar, Flyway crea las tablas y la app crea los usuarios semilla.
5. Listo:
   | Qué | URL |
   | --- | --- |
   | API | http://localhost:8080 |
   | Swagger UI | http://localhost:8080/swagger-ui.html |
   | Health check | http://localhost:8080/actuator/health |
   | Bandeja de correos (Mailpit) | http://localhost:8025 |
   | Consola MinIO | http://localhost:9001 (minioadmin / minioadmin) |

Todo dockerizado, incluida la API:

```bash
docker compose --profile app up -d --build
```

Las variables de entorno están en `.env.example`.

### Base de datos (MySQL en Docker)

Cada quien levanta su propio MySQL idéntico desde el repo; no hay base compartida.

| Dato | Valor |
| --- | --- |
| Host / puerto | `localhost` / `3307` (el 3307 evita chocar con un MySQL instalado en tu máquina) |
| Base de datos | `tickethub` |
| Usuario / contraseña | `DB_USER` / `DB_PASSWORD` de tu `.env` |

Para conectarte con DBeaver o Workbench usa esos datos. Comandos útiles:

```bash
docker compose stop mysql          # pausar sin perder datos
docker compose start mysql         # reanudar
docker compose logs -f mysql       # ver logs
docker compose down -v             # BORRA todo (incluida la base) y empiezas de cero
```

**Migraciones:** el esquema lo manda Flyway (`src/main/resources/db/migration`). Nunca edites una migración
que ya se ejecutó; crea una nueva (`V3__...sql`) y avisa al equipo para no repetir número.

**Usuarios semilla** (los crea la app al arrancar; contraseñas en tu `.env`):

| Correo | Rol | Contraseña |
| --- | --- | --- |
| admin@tickethub.local | ADMIN | `ADMIN_PASSWORD` |
| liberador@tickethub.local | LIBERADOR | `DEMO_PASSWORD` |
| agente@tickethub.local | AGENT | `DEMO_PASSWORD` |
| usuario@tickethub.local | USER | `DEMO_PASSWORD` |

## Estructura del proyecto

Arquitectura **por capas**: cada capa solo habla con la de abajo
(`controller` → `service` → `repository` → base de datos). La clase principal va en el paquete raíz
para que Spring escanee todo lo que está debajo.

```
src/main/java/com/tickethub
├── TicketHubApplication.java   # clase principal (@SpringBootApplication)
├── config/          # configuración general: Swagger, CORS, propiedades app.*
├── constants/       # constantes globales (rutas de la API)
├── controller/      # endpoints REST: reciben la petición y regresan DTOs
├── dto/
│   ├── request/     # lo que llega en el body (con validaciones)
│   └── response/    # lo que regresa la API
├── entity/          # "beans"/modelos: clases mapeadas a tablas con JPA
├── enums/           # Role, TicketStatus, Priority
├── exception/       # excepciones propias + manejador global de errores
├── mapper/          # convierte entity <-> dto
├── repository/      # acceso a datos (Spring Data JPA)
├── scheduler/       # tareas programadas (job del SLA)
├── security/        # SecurityConfig, errores 401/403, JWT (filtro y servicio)
├── service/         # interfaces con la lógica de negocio
│   └── impl/        # implementación de cada servicio
└── util/            # utilidades (generador de folio, etc.)

src/main/resources
├── application.yml
├── db/migration/    # scripts Flyway (V1__..., V2__...)
└── templates/email/ # plantillas HTML de correo (fase 4)

src/test/java/com/tickethub   # pruebas, mismo árbol de paquetes que main
postman/   k6/   docs/
```

**Flujo de una petición:**
`controller` → `service` (interfaz) / `service/impl` → `repository` → `entity` → `mapper` → `dto/response`.

**Reglas del equipo:**
- El controller no tiene lógica, solo llama al service.
- Nunca se regresa una entidad en la API, siempre un DTO.
- Los repositorios solo se usan desde los services.
- `util/` es solo para utilidades transversales; si algo pertenece a un tema (correo, archivos, JWT), va en su capa.
- Configuración propia en `application.yml` bajo `app:`, leída con una clase `@ConfigurationProperties` en `config/`.

## Diagramas

### Flujo de estados del ticket

```mermaid
stateDiagram-v2
    [*] --> PENDING_REVIEW: crea ticket
    PENDING_REVIEW --> REJECTED: liberador rechaza (motivo)
    PENDING_REVIEW --> APPROVED: liberador aprueba
    PENDING_REVIEW --> CANCELLED: spam o duplicado
    REJECTED --> PENDING_REVIEW: solicitante corrige
    REJECTED --> CANCELLED: solicitante o admin
    APPROVED --> APPROVED: urgente (sube prioridad, recalcula SLA)
    APPROVED --> IN_PROGRESS: se asigna agente
    IN_PROGRESS --> ON_HOLD: falta info
    ON_HOLD --> IN_PROGRESS: solicitante responde
    IN_PROGRESS --> RESOLVED: solución escrita
    RESOLVED --> IN_PROGRESS: reabre (máx. 3 días)
    RESOLVED --> CLOSED: confirma o 3 días sin respuesta
    CLOSED --> [*]
    CANCELLED --> [*]
```

### Modelo entidad-relación

```mermaid
erDiagram
    USERS ||--o{ TICKETS : crea
    USERS ||--o{ TICKETS : "atiende (asignado)"
    CATEGORIES ||--o{ TICKETS : clasifica
    TICKETS ||--o{ COMMENTS : tiene
    TICKETS ||--o{ ATTACHMENTS : tiene
    COMMENTS ||--o{ ATTACHMENTS : "puede tener"
    TICKETS ||--o{ TICKET_HISTORY : registra
    USERS ||--o{ TICKETS : "libera (revisa)"
    USERS ||--o{ PASSWORD_RESET_TOKENS : solicita
    USERS ||--o{ NOTIFICATIONS : recibe
    TICKETS ||--o{ NOTIFICATIONS : genera
```

### Arquitectura

_Pendiente: diagrama de arquitectura (frontend → API en capas → MySQL / S3 / SMTP)._

## Cómo trabajamos (equipo)

- `master`: lo que está desplegado. Solo entra por Pull Request.
- `integracion`: donde el equipo junta su trabajo antes de pasar a `master`.
- Ramas de trabajo desde `integracion`: `feature/auth-login`, `feature/tickets-crud`, `fix/...`
- Commits cortos y en presente: `feat: crear endpoint de login`, `fix: validar tamaño de adjunto`.
- Antes del PR: `mvn verify` en verde (lo mismo corre GitHub Actions).
- Nunca subir `.env` ni contraseñas reales.

## Pruebas

```bash
mvn test                                  # unitarias
newman run postman/TicketHub.postman_collection.json -e postman/local.postman_environment.json
k6 run k6/load-test.js                    # carga
```
