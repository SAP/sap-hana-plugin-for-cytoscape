package org.sap.cytoscape.internal.utils;

/**
 * Simple utils that solve HANA specific issues
 */
public class HanaUtils {
    /**
     *  Helper function to handle the String representation of DB null values
     *
     * @param obj   Value of a database record
     * @return      String representation of the value; "DB_NULL" if value has been null
     */
    public static String toStrNull(Object obj){
        return obj == null ? "DB_NULL" : obj.toString();
    }

    /**
     * Surrounds a string with double quotes
     *
     * @param id    String
     * @return      Quoted String
     */
    public static String quoteIdentifier(String id){
        return '"' + id + '"';
    }

    /**
     * Method to parse a HANA build string
     */
    public static boolean isCloudEdition(String buildStr){
        if (buildStr==null){
            return false;
        }
        return buildStr.contains("/CE");
    }
}
