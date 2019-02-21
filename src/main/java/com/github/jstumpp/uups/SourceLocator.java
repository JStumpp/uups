package com.github.jstumpp.uups;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


interface SourceLocator {

    static SourceLocator local() {
        return local(new File(System.getProperty("user.dir")).toPath());
    }

    static SourceLocator local(final Path path) {
        Path bin = path.resolve("bin");
        Path target = path.resolve("target");
        return filename -> {
            try {
                List<String> files = Arrays.asList(filename,
                        filename.replace(".", File.separator) + ".java");
                List<Path> source = new ArrayList<>();
                source.add(Paths.get(filename));
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                            throws IOException {
                        if (dir.toFile().isHidden()) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        if (dir.equals(bin) || dir.equals(target)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                            throws IOException {
                        return files.stream()
                                .filter(f -> file.toString().endsWith(f))
                                .findFirst()
                                .map(f -> {
                                    source.add(0, file.toAbsolutePath());
                                    return FileVisitResult.TERMINATE;
                                })
                                .orElse(FileVisitResult.CONTINUE);
                    }
                });
                return new Source(source.get(0), Files.readAllLines(source.get(0), StandardCharsets.UTF_8));
            } catch (IOException e) {
                return new Source(Paths.get(filename), Collections.emptyList());
            }
        };
    }

    Source source(String filename);

    public class Source {
        private static final int[] RANGE = {0, 0};
        private final Path path;

        private final List<String> lines;

        public Source(final Path path, final List<String> lines) {
            this.path = path;
            this.lines = lines;
        }

        public Path getPath() {
            return path;
        }

        public List<String> getLines() {
            return lines;
        }

        public int[] range(final int line, final int size) {
            if (line < lines.size()) {
                int from = Math.max(line - size, 0);
                int toset = Math.max((line - from) - size, 0);
                int to = Math.min(from + toset + size * 2, lines.size());
                int fromset = Math.abs((to - line) - size);
                from = Math.max(from - fromset, 0);
                return new int[]{from, to};
            }
            return RANGE;
        }

        public String source(final int from, final int to) {
            if (from >= 0 && to <= lines.size()) {
                return lines.subList(from, to).stream()
                        .map(l -> l.length() == 0 ? " " : l)
                        .collect(Collectors.joining("\n"));
            }
            return "";
        }

        @Override
        public String toString() {
            return path.toString();
        }
    }
}
