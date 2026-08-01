# Direct dependency license report

This report covers the direct dependencies changed or reviewed by the modernization work. It is
not a complete transitive dependency license inventory.

Source: Maven Central POM metadata, fetched during this run from `https://repo1.maven.org/maven2/`.
The URLs below are reproducible and should be rechecked whenever a dependency version changes.

| Coordinate | POM source | Declared license | License URL |
| --- | --- | --- | --- |
| `org.mariadb.jdbc:mariadb-java-client:3.5.10` | [`mariadb-java-client-3.5.10.pom`](https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.pom) | LGPL-2.1-or-later | Not supplied in the POM |
| `com.mysql:mysql-connector-j:9.3.0` | [`mysql-connector-j-9.3.0.pom`](https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.3.0/mysql-connector-j-9.3.0.pom) | GPL-2.0 with Universal FOSS Exception | [Universal FOSS Exception](http://oss.oracle.com/licenses/universal-foss-exception) |
| `com.zaxxer:HikariCP:7.1.0` | [`HikariCP-7.1.0.pom`](https://repo1.maven.org/maven2/com/zaxxer/HikariCP/7.1.0/HikariCP-7.1.0.pom) | Apache License 2.0 | <https://www.apache.org/licenses/LICENSE-2.0.txt> |
| `org.xerial:sqlite-jdbc:3.53.2.1` | [`sqlite-jdbc-3.53.2.1.pom`](https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.53.2.1/sqlite-jdbc-3.53.2.1.pom) | Apache License 2.0 | <https://www.apache.org/licenses/LICENSE-2.0.txt> |
| `org.postgresql:postgresql:42.7.13` | [`postgresql-42.7.13.pom`](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.13/postgresql-42.7.13.pom) | BSD-2-Clause | <https://jdbc.postgresql.org/about/license.html> |
| `dev.samstevens.totp:totp:1.7.1` | [`totp-1.7.1.pom`](https://repo1.maven.org/maven2/dev/samstevens/totp/totp/1.7.1/totp-1.7.1.pom) | No license element declared in the POM | Verify from the upstream source before redistribution |

## Interpretation

- The MariaDB driver is LGPL-licensed and remains loaded through Libby. The distribution must
  preserve the driver's notices and comply with LGPL terms, including the user's ability to replace
  the library. Libby's relocation metadata is part of the runtime loading path and needs live-driver
  verification after upgrades.
- MySQL Connector/J is GPL-2.0 with the Universal FOSS Exception. Its relocation is isolated under
  `xyz.kyngs.librelogin.lib.mysql`; verify the final redistribution and notice requirements before
  publishing a release.
- HikariCP, SQLite JDBC and PostgreSQL JDBC declare permissive licenses in their POMs. SQLite JDBC
  bundles platform-specific native components; those native components and all transitive
  dependencies must still be included in a complete release audit.
- The absent TOTP POM license is a release blocker for a complete legal review. This report does
  not claim that the dependency is license-compatible solely because the artifact is available on
  Maven Central.
- This repository remains MPL-2.0. No source license header was removed or changed.
