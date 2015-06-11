package org.jenkinsci.plugins.uniqueid.impl;

import hudson.model.Action;
import hudson.model.Actionable;

import org.apache.commons.codec.binary.Base64;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nullable;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * An action which stores an id.
 * @deprecated users should not use this as it ties the ID explicitly to the item and as will not work when copying items (for example). 
 */
@Deprecated
@Restricted(NoExternalUse.class)
class Id implements Action {


    private final String id;

    protected Id() {
        this.id = Base64.encodeBase64String(UUID.randomUUID().toString().getBytes()).substring(0, 30);
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    public String getId() {
        return id;
    }

    
    /**
     * @deprecated Sub classes should not use this as it stores the ID in the actionable item.
     * @return
     */
    @Nullable
    @Deprecated
    protected static String getId(Actionable actionable) {
        Id id = actionable.getAction(Id.class);
        if (id != null) {
            return id.getId();
        } else {
            return null;
        }
    }


    private final static Logger LOGGER = Logger.getLogger(Id.class.getName());
}
