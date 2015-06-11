package org.jenkinsci.plugins.uniqueid.impl;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.model.PersistenceRoot;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;

import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.uniqueid.implv2.PersistenceRootIdStore;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/** 
 * Converts legacy UniqueIDs that are stored inside a Folder/Job/Run configuration to UniqueIDs that are stored alongside the Folder/Job/Run.
 *
 */
@Restricted(NoExternalUse.class)
@Extension
public class IdStoreMigratorV1ToV2 {
    
    private static Logger LOGGER = Logger.getLogger(IdStoreMigratorV1ToV2.class.getName());

    /**
     * Migrates any IDs stored in Folder/Job/Run configuration 
     * @throws IOException
     */
   
    @Initializer(after=InitMilestone.JOB_LOADED, before=InitMilestone.COMPLETED, fatal=true)
    public static void migrateIdStore() throws IOException {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins is null, so it is impossible to migrate the IDs");
        }
        File marker = new File(jenkins.getRootDir(), "IdStoreMigration.txt");
        if (marker.exists()) {
            LOGGER.log(Level.INFO, "Migration of IDStore already perfomed, so skipping migration.");
            return;
        }
        LOGGER.log(Level.INFO, "Starting migration of IDs");

        performMigration(jenkins);

        LOGGER.log(Level.INFO, "Finished migration of IDs");
        if (!marker.createNewFile()) {
            throw new IOException("Failed to record the completion of the IDStore Migration.  " + 
                                  "This will cause performance issues on subsequent startup.  " + 
                                  "Please create an empty file at '" + marker.getCanonicalPath() + "'");
        }
    }

    @SuppressWarnings("unchecked")
    static void performMigration(@Nonnull Jenkins jenkins) {
        List<Item> allItems = jenkins.getAllItems();

        for (Item item : allItems) {
            // can only be Folder or Job here (not a run) - and these both implement PersistenceRoot
            if (item instanceof PersistenceRoot) {
                migrate((PersistenceRoot) item);
            }
            else {
                LOGGER.log(Level.WARNING, "Expected item of type Folder or Job which implement PersistenceRoot, but got a {0} so can not migrate the IdStore for this item",
                           item.getClass().getName());
            }

            if (item instanceof Job) {
                // need to migrate the RunIDs if they exist.
                Job<? extends Job, ? extends Run> job = (Job<? extends Job, ? extends Run>) item;
                RunList<? extends Run> builds = job.getBuilds();
                for (Run build : builds) {
                    migrate(build);
                }
            }
        }
    }

   private static void migrate(PersistenceRoot pr) {
       LOGGER.log(Level.FINE, "migrating {0}" , pr.toString());
       try {
            String id = LegacyIdStore.getId(pr);
            if (id != null) {
                PersistenceRootIdStore.create(pr, id);
                LegacyIdStore.removeId(pr);
            }
       } catch (IOException ex) {
           // need to rethrow (but add some context first) otherwise the migration will continue to run
           // and it will not have migrated everything :-(
           throw new IDStoreMigrationException("Failure whilst migrating " + pr.toString(), ex);
       }
    }

   /**
    * Exception to indicate a failure to migrate the IDStore.
    */
   private static class IDStoreMigrationException extends RuntimeException {
       
       public IDStoreMigrationException(String message, Throwable cause) {
           super(message,cause);
       }
   }
}
