package com.ashessin.cs441.hw2.dblp.utils;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

public class ExtractGzip {
    private InputStream in;

    public ExtractGzip(File f) throws IOException {
        this.in = new FileInputStream(f);
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        long memstart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        final String CWD = System.getProperty("user.dir");
        String gzipFilePath = args[0];
        String outputFilePath = args[1];

        File gzipFile = new File(gzipFilePath);
        File outputFile = new File(outputFilePath.isEmpty() ? CWD + gzipFile.getName() : outputFilePath);

        ExtractGzip extractGzip = new ExtractGzip(gzipFile);
        extractGzip.unzip(outputFile);
        int res = extractGzip.close();

        long memend = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long end = System.currentTimeMillis();

        System.out.println("Extract Gzip file, Memory used (bytes): "
                + (memend - memstart));
        System.out.println("Extract Gzip file, Time taken (ms): "
                + (end - start));

        System.exit(res);
    }

    public void unzip(File fileTo) throws IOException, NoSuchAlgorithmException {
        final long MEGABYTE = 1024L * 1024L;
        long fileSize = in.available();
        System.out.println("Extracting " + fileSize / MEGABYTE + " MiB compressed file as " + fileTo.getAbsolutePath());
        try (OutputStream out = new FileOutputStream(fileTo)) {
            // http://java-performance.info/java-io-bufferedinputstream-and-java-util-zip-gzipinputstream/
            in = new GZIPInputStream(in, 8192);
            byte[] buffer = new byte[65536];
            long extractedSize = 0L;
            int noRead;
            while ((noRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, noRead);
                extractedSize += noRead;
                System.out.print("Processed " + (extractedSize / MEGABYTE) + " MiB ...\r");
            }
            out.flush();
            System.out.println("Wrote " + (extractedSize / MEGABYTE) + " MiB to " + fileTo.getAbsolutePath());
        }
    }

    public int close() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }
}