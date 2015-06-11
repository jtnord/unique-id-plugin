package org.jenkinsci.plugins.uniqueid.implv2;

import hudson.Extension;
import hudson.model.PersistenceRoot;
import hudson.model.Job;
import hudson.model.Run;

import org.jenkinsci.plugins.uniqueid.IdStore;

/**
 * Manages Unique IDs for Runs. 
 * Whilst we could use the {@link PersistenceRootIdStore} that will create extra files for
 * every single build. A build already has a unique identifier (build number / build id) and a parent job can have a
 * unique ID, so we build one from the parent and our build number to save creating a file.
 */
@Extension(ordinal=1) // needs to take priority over the PersistenceRootIdStore
public class RunIdStore extends IdStore<Run> {
    public RunIdStore() {
        super(Run.class);
    }

    @Override
    public void make(Run run) {
        // we calculate these on the fly, or serve up migrated IDs if they exist.
        // in order to calculate on the fly we require the parent to have an id.
        IdStore.makeId(run.getParent());
    }

    @Override
    public String get(Run run) {
        IdStore<PersistenceRoot> persistenceStore = IdStore.forClass(PersistenceRoot.class);
        
        String id = persistenceStore.get(run);
        if (id != null) {
            // migrated legacy id
            return id;
        }

        // calculate the ID.
        Job parent = run.getParent();
        String parentID = IdStore.getId(parent);
        if (parentID != null) {
            return parentID + '_' + run.getNumber();
        }
        return null;
    }

}
