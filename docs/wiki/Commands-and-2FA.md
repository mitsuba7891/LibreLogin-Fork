# Commands and 2FA

[Home](Home.md) · [Configuration](Configuration.md) · [Troubleshooting](Troubleshooting.md)

**Jump to:** [Account commands](#account-commands) · [Premium](#premium-autologin) · [2FA setup](#enable-2fa-without-a-map) · [Recovery](#two-factor-recovery) · [Email](#email-recovery) · [Staff commands](#staff-commands-and-permissions)

## Account commands

| Command | Purpose |
|---|---|
| `/register <password> <confirm_password>` | Register an offline account. Alias: `/reg`. |
| `/login <password>` | Log in to an account without 2FA. Aliases: `/l` and `/log` when enabled. |
| `/login <password> <2fa_code>` | Log in with a second factor. |
| `/changepassword <old_password> <new_password>` | Change the password. Aliases: `/changepass`, `/passwd`, `/passch`. |
| `/premium <password>` | Request premium autologin. |
| `/premiumconfirm` | Confirm the premium request when prompted. |
| `/cracked` | Disable premium autologin and return to manual login. |
| `/2fa` | Start 2FA setup from an authenticated account. |
| `/2faconfirm <code>` | Confirm the authenticator secret and save the factor. |

> **Login with 2FA:** The one-time code does not replace the password. Supply both with `/login <password> <2fa_code>`.

## Premium autologin

Use `/premium <password>` and follow the `/premiumconfirm` prompt to request premium mode. Subsequent connections require a valid Mojang session for that account; an offline client with the same nickname does not prove ownership.

Use `/cracked` to return to manual login. Premium/autologin accounts must follow that flow before configuring TOTP 2FA.

## Enable 2FA without a map

### Step 1 — Keep verification enabled

The administrator sets `totp.enabled: true`. For text-only setup, set `totp.qr-enabled: false` and restart.

### Step 2 — Start setup

Log in and run `/2fa`. LibreLogin moves you to the configured limbo during setup. If premium autologin is active, follow the `/cracked` flow first.

### Step 3 — Add the account to your authenticator

Enter the displayed **manual secret**, or import the `otpauth://` provisioning URI using a compatible app.

### Step 4 — Confirm and use the factor

Run `/2faconfirm <code>` with the current authenticator code. On later connections, use `/login <password> <2fa_code>`.

This works on both Paper and Velocity without maps. Older custom messages may still say "scan the map"; `totp-manual-info` contains the usable alternative. Edit `messages.yml` to adjust that wording.

> **Keep the setup secret private:** The secret and URI can generate future codes. Do not share them in screenshots or reports. Disconnecting cancels pending setup; the secret is saved only after confirmation.

## Two-factor recovery

Disabling `totp.enabled` globally does not remove a saved factor. If the provider is disabled, enable it again and restart.

If the owner loses their authenticator, an administrator can remove **that account's factor** after verifying ownership:

```text
/librelogin user 2faoff <name>
```

Required permission: **`librepremium.user.2faoff`**. The administrator can use the console. The owner then logs in with their password and sets up a new secret. LibreLogin does not generate single-use recovery codes.

## Email recovery

Available when the administrator configures SMTP and enables `mail.enabled: true`:

| Command | Purpose |
|---|---|
| `/setemail <email> <password>` | Request a recovery email link. |
| `/verifyemail <token>` | Verify the address with the emailed token. |
| `/resetpassword` | Request a password-reset email for the account. |
| `/confirmpasswordreset <token> <new_password> <confirm_password>` | Confirm the password change. |

The command system resolves the UUIDs present in internal method signatures; players do not type them. SMTP port 465 uses implicit TLS; other ports depend on the SMTP server configuration.

## Staff commands and permissions

Many permission names retain the **`librepremium.*`** prefix for compatibility even though the command is `/librelogin`.

| Command | Permission |
|---|---|
| `/librelogin reload messages` | `librepremium.reload.messages` |
| `/librelogin reload configuration` | `librepremium.reload.configuration` |
| `/librelogin user info <name>` | `librepremium.user.info` |
| `/librelogin user premium <name>` | `librepremium.user.premium` |
| `/librelogin user cracked <name>` | `librepremium.user.cracked` |
| `/librelogin user 2faoff <name>` | `librepremium.user.2faoff` |
| `/librelogin user unregister <name>` | `librepremium.user.unregister` |
| `/librelogin user delete <name>` | `librepremium.user.delete` |
| `/librelogin dump` | `librelogin.dump` |
| `/librelogin email test <email>` | `librelogin.email.test` |

`unregister` clears credentials and authentication metadata; `delete` removes the account row. They are not general workarounds for an invalid session. Structural changes to databases, aliases and integrations require a restart even when the reload command is available.
