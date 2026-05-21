# SAP BusinessObjects Administration Utilities

JSP-based Java administration utility for SAP BusinessObjects 4.3 SP4.

The first assignment screen collects CMS, user, and password values, logs in with Enterprise authentication, lists Web Intelligence reports on screen, and exports the result to Excel.

## Project Layout

- `com.example.sapbo.core` - shared connection/config services
- `com.example.sapbo.modules.reports` - report administration utilities
- `com.example.sapbo.modules.universes` - placeholder package for universe utilities
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

The same result set can be downloaded as `webi-report-inventory.xlsx`.
