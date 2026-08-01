# Configuration migration assessment

## Decision

LibreLogin now uses YAML (`config.yml` and `messages.yml`) as its active configuration format.
Existing HOCON files (`config.conf` and `messages.conf`) are read once with the compatibility
loader and converted automatically on startup.

The migration writes and validates the YAML file before creating a backup named
`<legacy-file>.pre-yaml.bak`. The legacy file is never deleted or overwritten. If conversion
fails, the new YAML file is removed and the original HOCON file remains available.

The current loader still builds a format-independent Configurate tree, merges defaults, runs
ordered revision migrators, writes the revision, and saves the result. The migrators therefore
continue to operate without a second migration history.

## Legacy HOCON features handled during conversion

The source currently relies on:

- comments and generated header text;
- nested objects addressed with dotted paths;
- lists for limbo servers/worlds and allowed commands;
- maps whose values are lists for lobby mappings;
- scalar strings, booleans, integers and longs;
- revisioned node migrations and default merging;
- legacy `messages.conf` keys containing legacy colour codes and MiniMessage markup.

The implementation does not currently expose a project-specific `${...}` substitution or
`include` directive. HOCON-specific substitutions would be resolved by the legacy parser before
being serialized to YAML. YAML has different quoting, scalar and comment behaviour, so generated
strings are kept as scalar node values and administrators should quote values containing YAML
punctuation.

## Migration safeguards

The implementation:

1. loads legacy HOCON with `HoconConfigurationLoader`;
2. serializes the complete node tree through `YamlConfigurationLoader`;
3. validates that the generated YAML can be loaded again;
4. creates a `.pre-yaml.bak` copy of the original file;
5. runs the existing revision migrators against the YAML tree;
6. preserves lists, maps, scalar values and message placeholders in the Configurate tree.

The migration is intentionally one-way at runtime: once the YAML file exists it is preferred over
the legacy file. The backup remains available for manual rollback.

## Current administrator impact

After upgrading, use `config.yml` and `messages.yml`. Existing `.conf` files may remain beside
their YAML backups, but changes made to `.conf` after migration are not read while the YAML file
exists. Edit the YAML files instead. The numeric `revision` field remains managed by LibreLogin;
do not edit it manually.
