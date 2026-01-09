package org.aicode.aicode.controller;

import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class OpenRouterController {

    @PostMapping("/execute")
    public Map<String,String> execute(@RequestBody Map<String,String> req) {

        Map<String,String> res = new HashMap<>();

        try {
            Path dir = Files.createTempDirectory("exec");
            String lang = req.get("language");
            String code = req.get("code");

            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");

            ProcessBuilder pb;

            switch (lang) {

                case "java" -> {
                    Files.writeString(dir.resolve("Main.java"), code);
                    runCmd("javac Main.java", dir);
                    pb = new ProcessBuilder(
                            "java", "-cp", dir.toString(), "Main"
                    );
                }

                case "cpp" -> {
                    Files.writeString(dir.resolve("main.cpp"), code);

                    if (isWindows) {
                        runCmd("g++ main.cpp -o main.exe", dir);
                        pb = new ProcessBuilder("main.exe");
                    } else {
                        runCmd("g++ main.cpp -o main", dir);
                        pb = new ProcessBuilder("./main");
                    }
                }

                case "c" -> {
                    Files.writeString(dir.resolve("main.c"), code);

                    if (isWindows) {
                        runCmd("gcc main.c -o main.exe", dir);
                        pb = new ProcessBuilder("main.exe");
                    } else {
                        runCmd("gcc main.c -o main", dir);
                        pb = new ProcessBuilder("./main");
                    }
                }

                case "python" -> {
                    Files.writeString(dir.resolve("main.py"), code);
                    pb = new ProcessBuilder(
                            isWindows ? "python" : "python3",
                            "main.py"
                    );
                }

                default -> throw new RuntimeException("Unsupported language");
            }

            pb.directory(dir.toFile());
            pb.redirectErrorStream(true);

            Process p = pb.start();

            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                res.put("output", "❌ Time limit exceeded");
                return res;
            }

            res.put("output",
                    new String(p.getInputStream().readAllBytes()));

        } catch (Exception e) {
            res.put("output", e.toString());
        }

        return res;
    }

    private void runCmd(String cmd, Path dir) throws Exception {
        Process p = new ProcessBuilder(cmd.split(" "))
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();

        if (p.waitFor() != 0) {
            throw new RuntimeException(
                    new String(p.getInputStream().readAllBytes())
            );
        }
    }
}
