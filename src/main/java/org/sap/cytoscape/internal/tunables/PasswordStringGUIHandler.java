package org.sap.cytoscape.internal.tunables;

import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.AbstractGUITunableHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sap.cytoscape.internal.tunables.GUIDefaults.*;


/*
 * 
 * Cytoscape Work Swing Impl (work-swing-impl)
 * 
 *  
 * Copyright (C) 2006 - 2021 The Cytoscape Consortium
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * 
 */


/**
 * Slightly modified version of
 * https://github.com/cytoscape/cytoscape-impl/blob/develop/work-swing-impl/impl/src/main/java/org/cytoscape/work/internal/tunables/StringHandler.java
 */
public class PasswordStringGUIHandler extends AbstractGUITunableHandler implements DocumentListener {

    private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);

    private JPasswordField textField;
    private boolean readOnly;
    private boolean isUpdating;

    public PasswordStringGUIHandler(Field f, Object o, Tunable t) {
        super(f, o, t);
        init();
    }

    public PasswordStringGUIHandler(final Method getter, final Method setter, final Object instance, final Tunable tunable) {
        super(getter, setter, instance, tunable);
        init();
    }

    private void init() {
        readOnly = getParams().getProperty("readOnly", "false").equalsIgnoreCase("true");
        PasswordString s = null;

        try {
            s = (PasswordString)getValue();
        } catch (final Exception e) {
            logger.error("Could not initialize String Tunable.", e);
            s = new PasswordString("");
        }

        String textValue = (s==null?"":s.getPassword());

        textField = new JPasswordField(textValue);
        textField.setPreferredSize(new Dimension(2 * TEXT_BOX_WIDTH, textField.getPreferredSize().height));
        textField.setHorizontalAlignment(JTextField.LEFT);
        textField.getDocument().addDocumentListener(this);

        final JLabel label = new JLabel(getDescription());

        updateFieldPanel(panel, label, textField, horizontal);
        setTooltip(getTooltip(), label, textField);
        if (readOnly)
            textField.setEditable(false);
    }

    @Override
    public void update(){
        isUpdating = true;
        PasswordString s = null;
        try {
            s = (PasswordString)getValue();
            textField.setText(s.getPassword()); // TODO check here
        } catch (final Exception e) {
            logger.error("Could not set String Tunable.", e);
        }
        isUpdating = false;
    }

    /**
     * Catches the value inserted in the JTextField, and tries to set it to the initial object. If it can't, throws an
     * exception that displays the source error to the user
     */
    @Override
    public void handle() {
        if(isUpdating)
            return;

        final PasswordString string = new PasswordString(textField.getText());
        try {
            if (string != null)
                setValue(string);
        } catch (final Exception e) {
            logger.error("Could not set String Tunable.", e);
        }
    }

    /**
     * To get the item that is currently selected
     */
    @Override
    public String getState() {
        if ( textField == null )
            return "";

        final String text = new String(textField.getPassword());
        if ( text == null )
            return "";

        try {
            return text;
        } catch (Exception e) {
            logger.warn("Could not set String Tunable.", e);
            return "";
        }

    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        handle();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        handle();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        handle();
    }
}
