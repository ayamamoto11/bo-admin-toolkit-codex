# SAP BusinessObjects Administration Utilities

JSP-based Java administration utility for SAP BusinessObjects 4.3 SP4.

The login screen collects CMS, user, and password values, logs in with Enterprise authentication, and then lets you choose an administration module.

## Project Layout

- `com.example.sapbo.core` - shared connection/config services
- `com.example.sapbo.modules.reports` - report administration utilities
- `com.example.sapbo.modules.universes` - universe inventory utilities
- `com.example.sapbo.modules.schedules` - placeholder package for schedule utilities
- `com.example.sapbo.modules.users` - placeholder package for user utilities
- `com.example.sapbo.ui` - JSP servlet controllers and display helpers
- `src/main/webapp` - JSP pages, CSS, and web deployment descriptor

## Authentication

Credentials are not hardcoded. The JSP form asks for:

- CMS, for example `server-name:6400`
- User
- Password

The application uses SAP Enterprise authentication (`secEnterprise`).

## SAP BusinessObjects SDK Dependencies

SAP BusinessObjects Enterprise Java SDK jars are provided by your BOE 4.3 installation or client tools and are typically not available from Maven Central.

This project is currently configured to read local BOE SDK jars from:

```text
C:\Software\Bojarslib
```

The expected jars include `cesession.jar`, `cecore.jar`, `ceplugins_core.jar`, `celib.jar`, `corbaidl.jar`, `logging.jar`, `bcm.jar`, and `ebus405.jar`.

For a team/shared build, install those jars into a private Maven repository instead of using local `systemPath` dependencies.

## Build and Deploy

After adding the SAP SDK dependencies:

```powershell
mvn clean package
```

Deploy the generated WAR from `target/bo-admin-utils-1.0.0-SNAPSHOT.war` to a servlet container such as Tomcat 9.

The project compiles for Java 8 bytecode so it can run on Tomcat instances that are still using Java 8.

## Result Limits

The main inventory modules currently request up to `100000` objects from the CMS Query Builder API.

If your BO environment grows beyond 100,000 reports or universes, update these constants/query strings:

- Report inventory: `src/main/java/com/example/sapbo/modules/reports/ReportInventoryModule.java`
  - `REPORT_QUERY`
  - Change `SELECT TOP 100000 ...` to a larger value.
- Universe inventory: `src/main/java/com/example/sapbo/modules/universes/UniverseInventoryModule.java`
  - `UNIVERSE_QUERY`
  - Change `SELECT TOP 100000 ...` to a larger value.

Smaller helper queries such as `SELECT TOP 100 ...` are used only for related objects like connections, universe lookups, or debug results. Those usually do not need to match the main inventory limit.

## Output

The report inventory includes:

- Report name
- Object ID
- CUID
- Owner ID
- Parent folder ID
- Folder path
- Universe name
- Universe type (`UNV` or `UNX`)
- Universe CUID

The universe inventory includes:

- Universe ID
- Universe name
- Universe CUID
- Universe type (`UNV` or `UNX`)
- Parent folder ID
- Folder path
- Connection ID
- Connection name
- Connection type, when BO exposes enough metadata to infer it
- Database/server/DSN, when BO exposes it in connection metadata

The report result set can be downloaded as `webi-report-inventory.xlsx`.
The universe result set can be downloaded as `universe-inventory.xlsx`.
