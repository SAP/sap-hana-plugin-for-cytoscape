package org.sap.cytoscape.internal.tunables;

import org.cytoscape.command.AbstractStringTunableHandler;
import org.cytoscape.work.Tunable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Slightly modified version of
 * https://github.com/cytoscape/cytoscape-impl/blob/develop/command-executor-impl/src/main/java/org/cytoscape/command/internal/tunables/FileTunableHandler.java
 */
public class PasswordTunableHandler extends AbstractStringTunableHandler {

    public PasswordTunableHandler(Field f, Object o, Tunable t) {
        super(f,o,t);
    }

    public PasswordTunableHandler(Method get, Method set, Object o, Tunable t) {
        super(get,set,o,t);
    }

    public Object processArg(String arg) throws Exception {
        return new PasswordString(arg);
    }
}
