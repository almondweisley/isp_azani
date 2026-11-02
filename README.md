# Azani Internet Service Provider Information System

SCO 200 Object Oriented Programming II, Kenyatta University, September to November 2026.

A Java Swing desktop application on a MySQL database. It registers institutions, captures payments, records site visits and equipment orders, manages subscriptions and upgrades, runs the monthly billing cycle, and produces reports.

## Install and run

1. Run `Azani ISP Setup.exe` and follow the wizard. It needs no administrator rights, no Java and no MySQL: the app carries its own Java, and the database is online.
2. Start **Azani ISP** from the Start menu or the desktop.
3. Keep the computer connected to the internet, because the database is hosted on Aiven and not on your machine.

The source code and the SQL scripts are installed with the app. Open them from the Start menu entry **Azani ISP source code**.

To remove the app, open Settings > Apps > Installed apps, find Azani ISP and choose Uninstall.

## What you need to build it

- JDK 17 or later with `jpackage` (built and tested on JDK 25).
- Inno Setup 6 for the installer (`winget install JRSoftware.InnoSetup`).
- The MySQL Connector/J jar, placed in the `lib` folder.
- The `db.properties` file in the project root, filled in for the Aiven database. It is not committed, because it holds the password.

## Project layout

```
isp_azani/
  db.properties        connection settings (not committed)
  lib/                 Connector/J jar
  fonts/               fonts the Theme loads
  db/  model/  dao/  service/  ui/
  build.bat            builds the installer
  AzaniISP.iss         Inno Setup script that build.bat runs
```

## Build the installer

Double-click `build.bat`. It compiles the code, puts `db.properties` and the fonts inside `AzaniISP.jar`, bundles a Java runtime with `jpackage`, and wraps it all in `dist\Azani ISP Setup.exe`. Without Inno Setup it stops at `dist\Azani ISP.zip`, which runs after unzipping.

The `build` and `dist` folders are not committed, because the jar inside them holds the database password. Hand in the setup file directly.

## Compile and run from source

Open a terminal in the project root, then run:

```
javac -encoding UTF-8 -d out -cp "lib/*" db\*.java model\*.java dao\*.java service\*.java ui\*.java
java -cp "out;lib/*" ui.MainFrame
```

Run from the project root, so that `db.properties` is found. On macOS or Linux, replace `;` with `:` and `\` with `/`.

## Database

The database `azani_db` holds twelve tables and already contains the sample data. `azani_db_ddl.sql` builds the tables and `latest_dml_azani.sql` loads the data. Do not rerun the SQL scripts, since both wipe the data entered through the forms.

`db.properties` holds three keys: `db.url`, `db.user` and `db.password`. The user is a marking account created for this submission. It can read and change rows, but it cannot create or drop tables.

## Troubleshooting

- **Database settings missing.** The program did not find `db.properties`. Start it from the project root, or rebuild with `build.bat`.
- **Could not find or load main class.** The compile step did not run, or `out` is missing from the classpath. Repeat the compile command.
- **Access denied for user.** The password in `db.properties` is wrong. Check it for stray spaces or quotes.
- **Communications link failure.** The computer has no internet access, or the Aiven service is powered off. Aiven powers off free services that sit idle; power it on again in the Aiven console, then retry.
