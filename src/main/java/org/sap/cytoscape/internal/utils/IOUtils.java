package org.sap.cytoscape.internal.utils;

import org.sap.cytoscape.internal.hdb.HanaConnectionCredentials;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

public class IOUtils {

    /**
     * The filename where credentials will be stored between sessions
     */
    public static String getCacheFile(){
        return Paths.get(System.getProperty("user.home"), "saphana_cytoscape_cache.properties").toString();
    }

    public static void cacheProperties(Properties properties) throws IOException {
        cacheProperties(getCacheFile(), properties);
    }

    public static void cacheProperties(String file, Properties properties) throws IOException {
        try (OutputStream output = new FileOutputStream(file)){
            properties.store(output, null);
        } catch(IOException e){
            System.err.println("Cannot store properties");
            System.err.println(e);
            throw e;
        }
    }

    public static Properties loadProperties() throws IOException {
        return loadProperties(getCacheFile());
    }

    public static Properties loadProperties(String file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            // load cached credentials
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (IOException e) {
            // this will happen at least on the first start and is likely
            // not an issue
            System.err.println("Cannot load cached connection credentials");
            System.err.println(e);
            throw e;
        }
    }

    /**
     *
     * @param file
     * @throws IOException
     */
    public static void clearCachedCredentials(String file) throws IOException {
        File fileObject = new File(file);
        fileObject.delete();
    }

    /**
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Properties loadResourceProperties(String fileName) throws IOException {
        Properties resultProps = new Properties();

        InputStream inputStream = IOUtils.class.getClassLoader().getResourceAsStream(fileName);
        resultProps.load(inputStream);

        return resultProps;
    }
}
