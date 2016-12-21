package com.sanid.lib.debugghost.commands;

/**
 * Created by norbertmoehring on 05/12/2016.
 */

public interface GhostCommand {

    String getLabel();

    String getKey();

    String getValue();

    void execute(String value);

}
