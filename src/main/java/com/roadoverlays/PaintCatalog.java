package com.roadoverlays;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The packaged catalog is also used by Gradle to generate models, names, and recipes. */
public final class PaintCatalog {
    public record Design(String id, String texture, String name) {
    }

    public static final List<Design> DESIGNS = load();

    private PaintCatalog() {
    }

    private static List<Design> load() {
        List<Design> designs = new ArrayList<>();
        try (var stream = PaintCatalog.class.getResourceAsStream("/paint_catalog.tsv")) {
            if (stream == null) throw new IllegalStateException("Missing paint_catalog.tsv");
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#") || line.isBlank()) continue;
                    String[] columns = line.split("\t", -1);
                    if (columns.length < 4) throw new IllegalStateException("Invalid paint catalog row: " + line);
                    designs.add(new Design(columns[0], columns[1], columns[2]));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load paint catalog", e);
        }
        return List.copyOf(designs);
    }
}
