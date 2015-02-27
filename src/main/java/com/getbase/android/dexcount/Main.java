package com.getbase.android.dexcount;

import static com.getbase.android.dexcount.DexMethodCounts.buildMethodsTree;
import static com.getbase.android.dexcount.DexMethodCounts.printMethodUsage;

import com.android.dexdeps.DexData;
import com.android.dexdeps.DexDataException;
import com.google.common.base.Throwables;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class Main {
  public static void main(String[] args) {
    Main main = new Main();
    main.run(args);
  }

  void run(String[] args) {
    try {
      String fileName = checkArgs(args);
      System.out.println("Processing " + fileName);
      printMethodUsage(buildMethodsTree(readDexData(fileName)));
    } catch (UsageException ue) {
      System.err.println("DEX per-package method counts v1.0");
      System.err.println("Usage: dex-method-counts <file.apk>");
      System.exit(2);
    } catch (IOException ioe) {
      if (ioe.getMessage() != null) {
        System.err.println("Failed: " + ioe);
      }
      System.exit(1);
    } catch (DexDataException dde) {
      System.exit(1);
    }
  }

  /**
   * Opens an input file, which could be a .dex or a .jar/.apk with a
   * classes.dex inside.  If the latter, we extract the contents to a
   * temporary file.
   *
   * @param fileName the name of the file to open
   */
  List<DexData> readDexData(String fileName) throws IOException {
    try (ZipFile zipFile = new ZipFile(fileName)) {

      return Collections
          .list(zipFile.entries())
          .stream()
          .filter(zipEntry -> zipEntry.getName().matches("classes\\d*\\.dex"))
          .map(zipEntry -> {
            try (InputStream zis = zipFile.getInputStream(zipEntry)) {
              DexData dexData = new DexData(fromInputStream(zis));
              dexData.load();
              return dexData;
            } catch (IOException e) {
              Throwables.propagate(e);
            }

            throw new IllegalStateException();
          })
          .collect(Collectors.toList());
    }
  }

  private static RandomAccessFile fromInputStream(InputStream inputStream) throws IOException {
    File tempFile = File.createTempFile("dexdeps", ".dex");
    RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
    tempFile.delete();

    IOUtils.copy(inputStream, Channels.newOutputStream(raf.getChannel()));

    raf.seek(0);

    return raf;
  }

  String checkArgs(String[] args) {
    if (args.length != 1) {
      throw new UsageException();
    }

    return args[0];
  }

  private static class UsageException extends RuntimeException {
  }
}
