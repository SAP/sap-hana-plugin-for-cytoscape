package org.sap.cytoscape.internal.tunables;

/**
 * A password String. When use in combination with Tunable annotation a
 * masked text field should be shown
 */
public class PasswordString {
    private String password;

    public PasswordString(String password) {
        this.password = password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword(){
        return this.password;
    }
}
