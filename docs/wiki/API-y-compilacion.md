# API y compilación

[Inicio](Home.md) · [Repositorio](https://github.com/mitsuba7891/LibreLogin-Fork)

## Requisitos

- JDK **25** para compilar contra Paper API 26.2.
- El wrapper `./gradlew` del repositorio.
- Acceso a los repositorios de las dependencias durante la primera compilación.

`gradle.properties` define la versión actual, `0.25.0`. El build puede incrementar automáticamente el patch; usa siempre `-PnoBump` si quieres verificar o empaquetar sin cambiar esa versión.

## Verificar el proyecto

Con `JAVA_HOME` apuntando a tu JDK 25:

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" \
  :Plugin:compileJava :Plugin:test :Plugin:licenseCheck
```

En el entorno de trabajo del fork, el JDK temporal está en `/home/ubuntu/.jdk25`:

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths=/home/ubuntu/.jdk25 \
  :Plugin:compileJava :Plugin:test :Plugin:licenseCheck
```

No es un directorio distribuido ni una ruta obligatoria para otros usuarios. Si Gradle informa `languageVersion=25`, comprueba la ruta de instalación del JDK.

Los resultados de tests se escriben en `Plugin/build/test-results/test/` y el informe HTML en `Plugin/build/reports/tests/test/index.html`.

## Empaquetar

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" \
  :Plugin:platformJars :Plugin:releaseArchive :Plugin:licenseCheck
```

Salidas con la versión actual:

```text
Plugin/build/libs/platform/LibreLogin-Paper-0.25.0.jar
Plugin/build/libs/platform/LibreLogin-Velocity-0.25.0.jar
Plugin/build/libs/platform/AuthLimbo-1.0.0.jar
Plugin/build/distributions/LibreLogin-0.25.0.zip
```

Compilar no publica commits, tags, releases ni artefactos. Los ZIP/JAR publicados en GitHub pertenecen a la revisión de su release; una compilación local de `beta` puede incluir correcciones posteriores.

## API para integraciones

Las interfaces están en el módulo [API](https://github.com/mitsuba7891/LibreLogin-Fork/tree/beta/API/src/main/java/xyz/kyngs/librelogin/api), incluyendo:

- `LibreLoginPlugin`: servicios proporcionados por el plugin.
- `authorization/AuthorizationProvider`: estado de autenticación y confirmación 2FA.
- `database/User`: perfil y metadatos de una cuenta.
- `event`: eventos de autenticación y cambios de cuenta.
- `totp/TOTPProvider`: alta y verificación del segundo factor.

Consulta las interfaces de la misma revisión que vas a instalar. Que un cliente esté conectado no significa que haya terminado el login; las integraciones que mueven jugadores deben respetar el estado de autorización y 2FA.

## Generar Javadoc local

No hay un sitio Javadoc desplegado para este fork. Puedes generarlo con:

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" :API:javadoc
```

La tarea utiliza el módulo API y deja la documentación en `API/build/docs/javadoc/`. El repositorio no publica automáticamente a un Maven de terceros: la publicación Maven es opt-in mediante `publishRepositoryUrl` y requiere las credenciales del repositorio elegido.

## Licencias

`licenseCheck` verifica las cabeceras del código. No sustituye la revisión de licencias de las dependencias; el estado de ese inventario está en [dependency-licenses.md](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/dependency-licenses.md).
