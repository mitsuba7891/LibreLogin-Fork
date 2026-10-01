# Comandos y 2FA

[Inicio](Home.md) · [Configuración](Configuracion.md) · [Solución de problemas](Solucion-de-problemas.md)

## Cuentas

| Comando | Uso |
|---|---|
| `/register <contraseña> <repetición>` | Registrar una cuenta offline. Alias `/reg`. |
| `/login <contraseña>` | Entrar a una cuenta sin 2FA. Alias `/l` y, si está habilitado, `/log`. |
| `/login <contraseña> <código_2fa>` | Entrar a una cuenta con segundo factor. |
| `/changepassword <anterior> <nueva>` | Cambiar la contraseña. Alias `/changepass`, `/passwd`, `/passch`. |
| `/premium <contraseña>` | Solicitar autologin premium. |
| `/premiumconfirm` | Confirmar la solicitud premium cuando el plugin lo indique. |
| `/cracked` | Desactivar autologin premium y volver al login manual. |
| `/2fa` | Iniciar el alta de 2FA desde una cuenta ya autenticada. |
| `/2faconfirm <código>` | Confirmar la clave del autenticador y guardar el segundo factor. |

El código 2FA no sustituye la contraseña: en el siguiente login se proporcionan juntos.

## Activar 2FA sin mapa

1. El administrador mantiene `totp.enabled: true`. Para un flujo sólo de texto, configura `totp.qr-enabled: false` y reinicia.
2. Inicia sesión. Si tu cuenta tiene autologin premium, sigue el flujo de `/cracked` antes de configurar 2FA.
3. Ejecuta `/2fa`. El plugin te mueve al limbo configurado durante la activación.
4. En tu aplicación de autenticación, añade una cuenta TOTP usando la **clave manual** mostrada, o importa la URI `otpauth://` en una aplicación compatible.
5. Ejecuta `/2faconfirm <código>` con el código de la aplicación.
6. En las siguientes conexiones usa `/login <contraseña> <código_2fa>`.

Esta ruta funciona en Paper y Velocity sin mapas. Los textos personalizados antiguos pueden seguir diciendo «escanea el mapa»; la línea `totp-manual-info` contiene la alternativa válida. Para ajustar ese texto, edita `messages.yml`.

La clave y la URI permiten generar tus códigos futuros: guárdalas de forma privada y no las compartas en capturas o reportes. Desconectarte durante el alta cancela la activación pendiente. La clave sólo queda guardada al confirmar.

## Recuperación de 2FA

Desactivar globalmente `totp.enabled` no retira un segundo factor guardado: esa cuenta debe seguir verificándolo. Si el proveedor está desactivado, vuelve a habilitarlo y reinicia.

Si el propietario perdió el autenticador, un administrador puede retirar el factor de **esa cuenta** después de verificar su identidad:

```text
/librelogin user 2faoff <nombre>
```

Permiso requerido: `librepremium.user.2faoff`. El administrador puede usar la consola. Después, el usuario vuelve a entrar con su contraseña y configura otra clave. El plugin no genera códigos de recuperación de un solo uso.

## Correo

Disponible cuando el administrador configure SMTP y `mail.enabled: true`:

| Comando | Uso |
|---|---|
| `/setemail <correo> <contraseña>` | Solicitar vincular un email de recuperación. |
| `/verifyemail <token>` | Confirmar el email con el token recibido. |
| `/resetpassword` | Solicitar un email de restablecimiento para la cuenta. |
| `/confirmpasswordreset <token> <nueva> <repetición>` | Confirmar el cambio de contraseña. |

Los UUID presentes en las firmas internas de estos comandos los resuelve el sistema de comandos; el jugador no necesita escribirlos. El puerto SMTP 465 usa TLS implícito; otros puertos dependen de la configuración del servidor SMTP.

## Administración y permisos

El nombre de muchos permisos sigue siendo `librepremium.*` por compatibilidad, aunque el comando sea `/librelogin`.

| Comando | Permiso |
|---|---|
| `/librelogin reload messages` | `librepremium.reload.messages` |
| `/librelogin reload configuration` | `librepremium.reload.configuration` |
| `/librelogin user info <nombre>` | `librepremium.user.info` |
| `/librelogin user premium <nombre>` | `librepremium.user.premium` |
| `/librelogin user cracked <nombre>` | `librepremium.user.cracked` |
| `/librelogin user 2faoff <nombre>` | `librepremium.user.2faoff` |
| `/librelogin user unregister <nombre>` | `librepremium.user.unregister` |
| `/librelogin user delete <nombre>` | `librepremium.user.delete` |
| `/librelogin dump` | `librelogin.dump` |
| `/librelogin email test <correo>` | `librelogin.email.test` |

`unregister` elimina las credenciales y datos de autenticación de la cuenta, mientras que `delete` borra su fila. No los uses como solución genérica para un error de sesión. Los cambios estructurales de BD, alias e integraciones requieren reiniciar, aunque exista el comando de recarga.
