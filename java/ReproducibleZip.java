import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ReproducibleZip
{
  private static final Logger LOG =
    Logger.getLogger(ReproducibleZip.class.getCanonicalName());

  private ReproducibleZip()
  {

  }

  public static void main(final String[] args)
    throws IOException
  {
    if (args.length != 3) {
      System.err.println("usage: input time output.zip");
      System.exit(1);
    }

    final var pathIn =
      Paths.get(args[0]).toAbsolutePath();
    final var time =
      DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(args[1]);
    final var pathOut =
      Paths.get(args[2]).toAbsolutePath();

    final var fileTime =
      FileTime.from(Instant.from(time));

    try (var output =
           new ZipOutputStream(Files.newOutputStream(pathOut))) {

      output.setLevel(9);

      try (var pathStream = Files.walk(pathIn)) {
        final var files =
          pathStream.sorted()
            .map(Path::toAbsolutePath)
            .filter(Files::isRegularFile)
            .collect(Collectors.toList());

        for (final var file : files) {
          final var fileRelative = pathIn.relativize(file);
          final var fileName = fileRelative.toString();
          if (fileName.isEmpty() || fileName.isBlank()) {
            continue;
          }

          LOG.fine("zip: " + file);
          final var entry = new ZipEntry(fileName);
          entry.setSize(Files.size(file));
          entry.setCreationTime(fileTime);
          entry.setLastAccessTime(fileTime);
          entry.setLastModifiedTime(fileTime);
          entry.setCrc(crcOf(file));
          output.putNextEntry(entry);

          if (Files.isRegularFile(file)) {
            try (var fileStream = Files.newInputStream(file)) {
              fileStream.transferTo(output);
            }
          }
          output.closeEntry();
        }
      }
      output.finish();
    }
  }

  private static long crcOf(final Path file)
    throws IOException
  {
    final var crc32 = new CRC32();
    try (InputStream input = Files.newInputStream(file)) {
      final var buffer = new byte[1024];
      while (true) {
        final var r = input.read(buffer);
        if (r == -1) {
          break;
        }
        crc32.update(buffer, 0, r);
      }
    }
    return crc32.getValue();
  }
}