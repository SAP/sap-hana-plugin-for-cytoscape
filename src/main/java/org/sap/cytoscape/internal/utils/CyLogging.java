package org.sap.cytoscape.internal.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyLogging {

    // private static Logger logger = LoggerFactory.getLogger("org.cytoscape.application.userlog");
    private static Logger logger = LoggerFactory.getLogger(CyLogging.class);

    public static void debug(String msg){
        logger.debug("[SAP HANA plug-in] " + msg);
    }

    public static void info(String msg){
        logger.info( "[SAP HANA plug-in] " + msg);
    }

    public static void warn(String msg){
        logger.warn( "[SAP HANA plug-in] " + msg);
    }

    public static void err(String msg){
        logger.error("[SAP HANA plug-in] " + msg);
    }
}
