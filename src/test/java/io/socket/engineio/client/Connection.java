package io.socket.engineio.client;

import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

public abstract class Connection {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    final static int TIMEOUT = 10000;
    final static int PORT = 3000;

    private Process serverProcess;
    private ExecutorService serverService;
    private Future serverOutput;
    private Future serverError;

    @Before
    public void startServer() throws IOException, InterruptedException {
        logger.fine("Starting server ...");

        final CountDownLatch latch = new CountDownLatch(1);
        serverProcess = Runtime.getRuntime().exec(
                "node src/test/resources/server.js", createEnv());
        serverService = Executors.newCachedThreadPool();
        serverOutput = serverService.submit(() -> {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getInputStream()));
            String line;
            try {
                line = reader.readLine();
                latch.countDown();
                do {
                    logger.fine("SERVER OUT: " + line);
                } while ((line = reader.readLine()) != null);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        });
        serverError = serverService.submit(() -> {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getErrorStream()));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    logger.fine("SERVER ERR: " + line);
                }
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        });
        latch.await(3000, TimeUnit.MILLISECONDS);
    }

    @After
    public void stopServer() throws InterruptedException {
        logger.fine("Stopping server ...");
        serverProcess.destroy();
        serverOutput.cancel(false);
        serverError.cancel(false);
        serverService.shutdown();
        serverService.awaitTermination(3000, TimeUnit.MILLISECONDS);
    }

    Socket.Options createOptions() {
        Socket.Options opts = new Socket.Options();
        opts.port = PORT;
        return opts;
    }

    String[] createEnv() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("DEBUG", "engine*");
        env.put("PORT", String.valueOf(PORT));
        String[] _env = new String[env.size()];
        int i = 0;
        for (String key : env.keySet()) {
            _env[i] = key + "=" + env.get(key);
            i++;
        }
        return _env;
    }
}
