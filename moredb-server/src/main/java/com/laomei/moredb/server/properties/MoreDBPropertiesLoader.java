package com.laomei.moredb.server.properties;

import com.laomei.moredb.common.exception.MoreDBException;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;

/**
 * @author luobo.hwz on 2021/02/02 7:23 PM
 */
public class MoreDBPropertiesLoader {

    private static final String MORE_DB_PROPERTIES_FILE = "db.yaml";

    public static MoreDBProperties load() {
        final File file = getPropertiesFile();
        final Yaml yaml = new Yaml();
        try (final InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return yaml.loadAs(in, MoreDBProperties.class);
        } catch (IOException e) {
            throw new MoreDBException("load db.yaml failed");
        }
    }

    private static File getPropertiesFile() {
        final String rt = Paths.get("").toAbsolutePath().toString() + "/" + MORE_DB_PROPERTIES_FILE;
        final File rtFile = new File(rt);
        if (rtFile.exists()) {
            return rtFile;
        }
        final URL url = MoreDBProperties.class.getClassLoader()
                .getResource(MORE_DB_PROPERTIES_FILE);
        if (url == null) {
            throw new MoreDBException("can not found db.yaml");
        }
        return new File(url.getPath());
    }
}
