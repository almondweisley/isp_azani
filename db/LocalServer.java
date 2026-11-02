package db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * The installed app's own MySQL server, so the PC it runs on needs no MySQL of its own.
 *
 * The setup puts a MySQL 8.4 server in the app folder (app\mysql) with the sample data ready-made
 * in app\mysql\data. On the first start that data folder is copied to %LOCALAPPDATA%\Azani ISP,
 * where the user may write. The server listens on 127.0.0.1 only, and only while the app is open.
 *
 * build.bat runs main() once to make the ready-made data folder from the two SQL scripts.
 */
public final class LocalServer {

    private LocalServer() { }

    // The jpackage launcher sets this to the app's mysql folder (see build.bat).
    // It is missing when the app runs from source, which then uses db.properties.
    private static final String HOME_PROPERTY = "azani.mysql";
    private static final int PREFERRED_PORT = 3399;             // if taken, any free port will do
    private static final long START_TIMEOUT_MS = 90_000;        // generous: slow lab PCs, first start

    // Held open while the app runs; a field, so the garbage collector cannot close it and drop the lock.
    private static FileChannel instanceLock;

    public static boolean isBundled() {
        return System.getProperty(HOME_PROPERTY) != null;
    }

    /** Starts the server, or reuses one left running by an app that was killed, and points DBConnection at it. */
    public static void start() throws IOException, SQLException {
        Path home = Path.of(System.getProperty(HOME_PROPERTY));
        Path root = Path.of(System.getenv("LOCALAPPDATA"), "Azani ISP");
        Path data = root.resolve("data");
        Path portFile = root.resolve("port.txt");
        Path log = root.resolve("mysql-error.log");

        // One app at a time: a second copy would fight the first over the data folder.
        // Windows drops the lock when the app exits, even if it is killed.
        Files.createDirectories(root);
        instanceLock = FileChannel.open(root.resolve("app.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        if (instanceLock.tryLock() == null) {
            instanceLock.close();
            throw new IOException("Azani ISP is already open. Use the window that is already running.");
        }

        if (!Files.isDirectory(data)) {                          // first start on this PC
            Path partial = root.resolve("data.tmp");
            deleteTree(partial);                                 // left over if a first start was cut short
            copyTree(home.resolve("data"), partial);
            Files.move(partial, data);                           // only a complete copy gets the real name
        }

        // A server still running from a killed app keeps the data folder locked, so a second one could not start.
        Integer oldPort = readPort(portFile);
        if (oldPort != null && servesData(oldPort, data)) {
            useServer(oldPort, null);
            return;
        }

        int port = isFree(PREFERRED_PORT) ? PREFERRED_PORT : freePort();
        List<String> command = serverCommand(home, data, log);
        command.add("--port=" + port);
        Process server = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(root.resolve("mysql-console.log").toFile()))
                .start();
        Files.writeString(portFile, Integer.toString(port));
        waitUntilReady(server, port, log);
        useServer(port, server);
    }

    // The options both the build and the app start the server with. --no-defaults must come first.
    private static List<String> serverCommand(Path home, Path data, Path log) {
        return new ArrayList<>(List.of(
                home.resolve("bin").resolve("azani-mysqld.exe").toString(),
                "--no-defaults",                       // ignore any my.ini on the PC
                "--basedir=" + home,
                "--datadir=" + data,
                "--log-error=" + log,
                "--bind-address=127.0.0.1",            // this PC only: no firewall prompt, nothing on the network
                "--mysqlx=OFF",                        // no second port for the X protocol
                "--disable-log-bin",                   // no replication, so no binary log files
                "--performance-schema=OFF",            // saves a few hundred MB of memory
                "--innodb-buffer-pool-size=32M",       // the whole database is well under 1 MB
                "--innodb-redo-log-capacity=8M",
                "--lower-case-table-names=1"));        // the Windows default, fixed when the data is made
    }

    private static void useServer(int port, Process server) {
        Properties settings = new Properties();
        settings.setProperty("db.url", url(port, "azani_db"));
        settings.setProperty("db.user", "root");
        settings.setProperty("db.password", "");       // root has no password: the server only listens on 127.0.0.1
        DBConnection.useLocalServer(settings);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(port, server)));   // runs when the app exits
    }

    private static String url(int port, String database) {
        return "jdbc:mysql://127.0.0.1:" + port + "/" + database
                + "?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000";
    }

    private static void waitUntilReady(Process server, int port, Path log) throws IOException {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!server.isAlive()) {
                throw new IOException("The database server stopped with code " + server.exitValue()
                        + ".\nDetails are in " + log);
            }
            try (Connection con = DriverManager.getConnection(url(port, ""), "root", "")) {
                return;                                          // it answers: ready
            } catch (SQLException notYet) {
                pause(300);
            }
        }
        kill(server);
        throw new IOException("The database server did not start within "
                + START_TIMEOUT_MS / 1000 + " seconds.\nDetails are in " + log);
    }

    // SHUTDOWN lets MySQL write everything to disk before it stops.
    private static void stop(int port, Process server) {
        try (Connection con = DriverManager.getConnection(url(port, ""), "root", "");
             Statement st = con.createStatement()) {
            st.execute("SHUTDOWN");
        } catch (SQLException e) {
            // already stopped
        }
        if (server == null) return;                              // reused server: it stops on its own
        try {
            if (!server.waitFor(30, TimeUnit.SECONDS)) {
                kill(server);
            }
        } catch (InterruptedException e) {
            kill(server);
        }
    }

    // On Windows mysqld is a small monitor process that runs the real server as its child: stop both.
    private static void kill(Process server) {
        server.descendants().forEach(ProcessHandle::destroyForcibly);
        server.destroyForcibly();
    }

    /** True when the server on this port is ours, not some other MySQL on the PC. */
    private static boolean servesData(int port, Path data) {
        try (Connection con = DriverManager.getConnection(url(port, ""), "root", "");
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT @@datadir")) {
            return rs.next() && Path.of(rs.getString(1)).equals(data);
        } catch (SQLException e) {
            return false;
        }
    }

    private static Integer readPort(Path portFile) {
        try {
            return Integer.valueOf(Files.readString(portFile).trim());
        } catch (IOException | NumberFormatException e) {
            return null;                                         // no file yet, or an unreadable one
        }
    }

    private static boolean isFree(int port) {
        try (ServerSocket s = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();                             // port 0 = "any free port"
        }
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {            // parents come before their children
            for (Path f : (Iterable<Path>) files::iterator) {
                Path target = to.resolve(from.relativize(f).toString());
                if (Files.isDirectory(f)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(f, target);
                }
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path f : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(f);                                 // children before their folder
            }
        }
    }

    // ------------------------------------------------------------------ build step

    /**
     * build.bat: makes the ready-made data folder.
     * Arguments: the mysql folder, a log file outside it, then the SQL scripts in the order to run them.
     */
    public static void main(String[] args) throws Exception {
        Path home = Path.of(args[0]).toAbsolutePath();
        Path log = Path.of(args[1]).toAbsolutePath();
        Path data = home.resolve("data");
        deleteTree(data);

        List<String> init = serverCommand(home, data, log);
        init.add(2, "--initialize-insecure");                    // creates root with no password
        Process initialize = new ProcessBuilder(init).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log.resolveSibling("mysql-console.log").toFile()))
                .start();
        if (initialize.waitFor() != 0) {
            throw new IOException("MySQL could not create the data folder. Details are in " + log);
        }

        int port = freePort();
        List<String> command = serverCommand(home, data, log);
        command.add("--port=" + port);
        Process server = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log.resolveSibling("mysql-console.log").toFile()))
                .start();
        waitUntilReady(server, port, log);

        // allowMultiQueries: each script goes to the server whole, the way Workbench would run it
        try (Connection con = DriverManager.getConnection(url(port, "") + "&allowMultiQueries=true", "root", "");
             Statement st = con.createStatement()) {
            for (int i = 2; i < args.length; i++) {
                boolean result = st.execute(Files.readString(Path.of(args[i])));
                while (result || st.getUpdateCount() != -1) {    // step through every statement, so errors surface
                    result = st.getMoreResults();
                }
                System.out.println("      loaded " + args[i]);
            }
            try (ResultSet rs = st.executeQuery("SELECT (SELECT COUNT(*) FROM azani_db.institutions), "
                    + "(SELECT COUNT(*) FROM azani_db.payments)")) {
                rs.next();
                System.out.println("      azani_db ready: " + rs.getInt(1) + " institutions, "
                        + rs.getInt(2) + " payments");
            }
        }

        stop(port, server);
        if (server.isAlive() || server.exitValue() != 0) {
            throw new IOException("MySQL did not shut down cleanly. Details are in " + log);
        }
    }
}
