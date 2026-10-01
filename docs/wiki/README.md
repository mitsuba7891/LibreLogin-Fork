# Fuentes de la wiki

El contenido para usuarios empieza en [Home.md](Home.md). Estas páginas se conservan en el repositorio principal para revisarlas junto con los cambios del plugin.

## Estado de GitHub Wiki

El 1 de octubre de 2026, el fork tiene `hasWikiEnabled: true`, pero `git ls-remote https://github.com/mitsuba7891/LibreLogin-Fork.wiki.git` devuelve «Repository not found». La wiki todavía no tiene un repositorio Git accesible; las páginas de esta carpeta aún no están publicadas allí.

Para inicializarla, el propietario debe crear la primera página **Home** desde [GitHub Wiki](https://github.com/mitsuba7891/LibreLogin-Fork/wiki), usando `Home.md` como contenido. Después se puede publicar el resto mediante el repositorio separado `LibreLogin-Fork.wiki.git`, con autorización explícita para ese commit/push.

Al publicar:

- Copiar las seis páginas de usuario y `_Sidebar.md`; no copiar este `README.md` de mantenimiento.
- En los enlaces internos de las páginas publicadas, usar nombres de página sin la extensión `.md` para la navegación de GitHub Wiki. Mantener `.md` en esta copia para que funcione en el repositorio principal.
- Conservar versiones y estado de publicación alineados con los JAR distribuidos.
- Cambiar el badge de documentación del README a la wiki pública sólo cuando las páginas estén accesibles. Por ahora enlaza esta copia local/versionada.

No es necesario crear un remoto `upstream` ni publicar en el repositorio del proyecto original.
