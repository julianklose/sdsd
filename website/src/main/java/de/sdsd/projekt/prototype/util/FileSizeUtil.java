package de.sdsd.projekt.prototype.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONObject;

/**
 * The Class FileSizeUtil.
 *
 * @author Markus Schr&ouml;der
 */
public class FileSizeUtil {

    /**
     * Human readable byte count.
     *
     * @param bytes the bytes
     * @param si the si
     * @return the string
     */
    //from https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Human readable byte count.
     *
     * @param bytes the bytes
     * @return the string
     */
    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount(bytes, true);
    }

    /**
     * Memory statistics.
     *
     * @return the string
     */
    public static String memoryStatistics() {
        Runtime instance = Runtime.getRuntime();

        StringBuilder sb = new StringBuilder();

        long t = instance.totalMemory();
        long f = instance.freeMemory();
        long u = t - f;
        long m = instance.maxMemory();

        sb
                .append(humanReadableByteCount(u)).append(" used / ")
                .append(humanReadableByteCount(t)).append(" total / ")
                .append(humanReadableByteCount(m)).append(" max");

        return sb.toString();
    }

    /**
     * Count lines.
     *
     * @param file the file
     * @return the int
     */
    // https://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
    public static int countLines(File file) {
        if(file.isDirectory())
            return 0;
        
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            byte[] c = new byte[1024];

            int readChars = is.read(c);
            if (readChars == -1) {
                // bail out if nothing to read
                return 0;
            }

            // make it easy for the optimizer to tune this loop
            int count = 0;
            while (readChars == 1024) {
                for (int i = 0; i < 1024;) {
                    if (c[i++] == '\n') {
                        ++count;
                    }
                }
                readChars = is.read(c);
            }

            // count remaining characters
            while (readChars != -1) {
                //System.out.println(readChars);
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
                readChars = is.read(c);
            }

            return count == 0 ? 1 : count;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }
    
    /**
     * Memory statistics json.
     *
     * @return the JSON object
     */
    public static JSONObject memoryStatisticsJson() {
        Runtime instance = Runtime.getRuntime();

        long t = instance.totalMemory();
        long f = instance.freeMemory();
        long u = t - f;
        long m = instance.maxMemory();

        JSONObject resp = new JSONObject();

        resp.put("used_bytes", u);
        resp.put("total_bytes", t);
        resp.put("max_bytes", m);

        resp.put("used_ratio", u / (float) t);
        resp.put("used_percent", (u / (float) t) * 100 + "%");

        resp.put("used", humanReadableByteCount(u));
        resp.put("total", humanReadableByteCount(t));
        resp.put("max", humanReadableByteCount(m));

        StringBuilder sb = new StringBuilder();
        sb
                .append(humanReadableByteCount(u)).append(" used / ")
                .append(humanReadableByteCount(t)).append(" total / ")
                .append(humanReadableByteCount(m)).append(" max");
        resp.put("text", sb.toString());

        return resp;
    }

    

}
