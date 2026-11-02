# Azani Internet Service Provider Information System

SCO 200 Object Oriented Programming II, Kenyatta University, September to November 2026.

A Java Swing desktop application on a MySQL database. It registers institutions, captures payments, records site visits and equipment orders, manages subscriptions and upgrades, runs the monthly billing cycle, and produces reports.

## Install and run

1. Run `Azani ISP Setup.exe` and follow the wizard. If Windows says "Windows protected your PC", the setup is simply not signed: click **More info**, then **Run anyway**.
2. Start **Azani ISP** from the Start menu or the desktop. The first window says "Starting the database" for a few seconds.

The setup needs no administrator rights, no Java, no MySQL and no internet. The app carries its own Java and its own MySQL 8.4 server, already loaded with the sample data. The server runs only while the app is open and accepts connections from this computer only.

The source code and the SQL scripts are installed with the app. Open them from the Start menu entry **Azani ISP source code**.

The data you enter is kept in `%LOCALAPPDATA%\Azani ISP`. To remove the app and that data, open Settings > Apps > Installed apps, find Azani ISP and choose Uninstall.

To look at the tables in MySQL Workbench while the app is open, connect to host `127.0.0.1`, port `3399`, user `root`, no password. If port 3399 was taken, the port in use is in `%LOCALAPPDATA%\Azani ISP\port.txt`.

## What you need to build it

- JDK 17 or later with `jpackage` (built and tested on JDK 25).
- MySQL Server 8.4, installed. The build copies its server program into the app.
- Inno Setup 6 for the installer (`winget install JRSoftware.InnoSetup`).
- The MySQL Connector/J jar, placed in the `lib` folder.

## Project layout

```
isp_azani/
  lib/                 Connector/J jar
  fonts/               fonts the Theme loads
  db/  model/  dao/  service/  ui/
  azani_db_ddl.sql     builds the tables
  latest_dml_azani.sql loads the sample data
  build.bat            builds the installer
  AzaniISP.iss         Inno Setup script that build.bat runs
  db.properties        your own MySQL, for running from source (not committed)
```

## Build the installer

Double-click `build.bat`. It compiles the code, makes `AzaniISP.jar`, copies the MySQL server from your MySQL installation, loads the two SQL scripts into a fresh data folder for it, bundles a Java runtime with `jpackage`, and wraps it all in `dist\Azani ISP Setup.exe`. Without Inno Setup it stops at `dist\Azani ISP.zip`, which runs after unzipping.

The `build` and `dist` folders are not committed. Hand in the setup file directly.

## Compile and run from source

Running from source uses your own MySQL server instead of the bundled one. Put `db.properties` in the project root with three keys, `db.url`, `db.user` and `db.password`, then open a terminal in the project root and run:

```
javac -encoding UTF-8 -d out -cp "lib/*" db\*.java model\*.java dao\*.java service\*.java ui\*.java
java -cp "out;lib/*" ui.MainFrame
```

On macOS or Linux, replace `;` with `:` and `\` with `/`.

## Database

The database `azani_db` holds twelve tables. `azani_db_ddl.sql` builds the tables and `latest_dml_azani.sql` loads the sample data; `build.bat` runs both to make the installed app's data. Do not rerun the scripts on a database you have used, since both wipe the data entered through the forms.

`db/LocalServer.java` starts and stops the installed app's own server; `db/DBConnection.java` hands every DAO its connection either way.

## Troubleshooting

- **Azani ISP is already open.** Only one copy runs at a time. Use the window that is already open.
- **The database server stopped with code ...** The installed app's server could not start. The reason is in `%LOCALAPPDATA%\Azani ISP\mysql-error.log`.
- **Database settings missing.** Running from source, the program did not find `db.properties`. Start it from the project root.
- **Could not find or load main class.** The compile step did not run, or `out` is missing from the classpath. Repeat the compile command.
- **Access denied for user.** Running from source, the password in `db.properties` is wrong. Check it for stray spaces or quotes.
- **Communications link failure.** Running from source, your MySQL server is not running. Start it and try again.
