package com.axonivy.ivy.tool.cxf.codegen.mojo;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

public class CxfClientGeneratorFiles {

  private final Path outputDir;

  public CxfClientGeneratorFiles(Path outputDir) {
    this.outputDir = outputDir;
  }

  public void createDir() {
    if (outputDir == null) {
      return;
    }
    try {
      Files.createDirectories(outputDir);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to create client directory " + outputDir, ex);
    }
  }

  public void cleanup(Consumer<String> log) {
    if (outputDir == null) {
      return;
    }
    if (!Files.exists(outputDir)) {
      return;
    }

    log.accept("Cleaning existing files in " + outputDir);
    try (var stream = Files.walk(outputDir)) {
      stream.sorted(Comparator.reverseOrder())
          .filter(p -> !Objects.equals(p, outputDir))
          .map(Path::toFile)
          .forEach(File::delete);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
