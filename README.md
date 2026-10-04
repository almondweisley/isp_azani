# Azani Internet Service Provider Information System

SCO 200 Object Oriented Programming II, Kenyatta University, September to November 2026.

A Java Swing desktop application on a MySQL database. It registers institutions, captures payments, records site visits and equipment orders, manages subscriptions and upgrades, runs the monthly billing cycle, and produces reports.

## What you need

- JDK 17 or later (built and tested on JDK 25).
- An internet connection, because the database is hosted on Aiven and not on your machine.
- The MySQL Connector/J jar, placed in the `lib` folder of the project.
- The `db.properties` file in the project root, already filled in for the Aiven database.

## Project layout

```
azani_isp/
  db.properties        connection settings
  lib/                 Connector/J jar
  isp_azani/           source root
    db/  model/  dao/  service/  ui/
```

## Compile

Open a terminal in the `azani_isp` folder, then run:

```
javac -d out -cp "lib/*" isp_azani\db\*.java isp_azani\model\*.java isp_azani\dao\*.java isp_azani\service\*.java isp_azani\ui\*.java
```

Packages that do not exist yet can be dropped from the command until they are built.

## Run

Run from the `azani_isp` folder, so that `db.properties` is found:

```
java -cp "out;lib/*" ui.MainFrame
```

Until `MainFrame` is written, the connection test runs with `java -cp "out;lib/*" dao.DAOTest`.

On macOS or Linux, replace `;` with `:` and `\` with `/`.

## Database

The database `azani_db` holds twelve tables and already contains the sample data. Do not rerun the SQL scripts, since both wipe the data entered through the forms.

`db.properties` holds three keys: `db.url`, `db.user` and `db.password`. The user is a marking account created for this submission.

## Troubleshooting

- **Database settings missing.** The program did not find `db.properties`. Start it from the `azani_isp` folder.
- **Could not find or load main class.** The compile step did not run, or `out` is missing from the classpath. Repeat the compile command.
- **Access denied for user.** The password in `db.properties` is wrong. Check it for stray spaces or quotes.
- **Communications link failure.** The computer has no internet access, or the Aiven service is powered off. Check the connection and try again.