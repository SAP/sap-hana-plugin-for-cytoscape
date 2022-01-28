package org.sap.cytoscape.internal.utils;

public class CyNetworkKey {
    Long suid;
    String name;

    public CyNetworkKey(Long suid, String name){
        this.suid = suid;
        this.name = name;
    }

    public Long getSUID(){
        return this.suid;
    }

    public String getName(){
        return this.name;
    }

    @Override
    public String toString(){
        return name;
    }
}
