package org.jenkinsci.plugins.uniqueid;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.ExtensionList;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.PersistenceRoot;
import hudson.model.Project;
import hudson.model.Run;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.uniqueid.impl.JobIdStore;
import org.jenkinsci.plugins.uniqueid.impl.LegacyIdStore;
import org.jenkinsci.plugins.uniqueid.implv2.PersistenceRootIdStore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;
import org.junit.Before;

public class IdTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    PersistenceRootIdStore newStore ;
    JobIdStore oldStore;
    
    /**
     * This implementation also checks the consistency of stores in the plugin
     */
    @Test
    public void checkStoresUniqueStatus() {
        // Retrieve the new store class
        List<IdStore<PersistenceRoot>> extensions = IdStore.allForClass(PersistenceRoot.class);
        assertEquals(1, extensions.size());
        assertTrue(extensions.get(0) instanceof PersistenceRootIdStore);
        newStore = (PersistenceRootIdStore) extensions.get(0);
   
        // Retrieve the old store class
        List<LegacyIdStore<Job>> oldExtensions = LegacyIdStore.allForClass(Job.class);
        assertEquals(1, oldExtensions.size());
        assertTrue(oldExtensions.get(0) instanceof JobIdStore);
        oldStore = (JobIdStore) oldExtensions.get(0);
    }
   
    @Test
    public void projectWithOldRunIdStore() throws Exception {
        final Project p = jenkinsRule.createFreeStyleProject();
        assertNull(IdStore.getId(p));
     
        // Emulate old project property injection
        p.addProperty(new JobIdStore.JobIdProperty());
        String projectId = IdStore.getId(p);
        assertNotNull(LegacyIdStore.forClass(Project.class).get(p));
        assertNull(IdStore.forClass(Project.class).get(p));
        
        // We're able to retrieve build Id based on the legacy Id instance
        AbstractBuild build = jenkinsRule.buildAndAssertSuccess(p);
        final String buildId = IdStore.getId(build);
        assertNotNull(buildId);
        assertNull(LegacyIdStore.forClass(Run.class).get(build));
        assertNotNull(IdStore.forClass(Run.class).get(build));
             
        // Roundtrip, check the migration to the new IdStore completes correctly
        IdStore.makeId(build);
        jenkinsRule.jenkins.reload();
        AbstractProject resurrectedProject = jenkinsRule.jenkins.getItemByFullName(p.getFullName(), AbstractProject.class);
        assertEquals(projectId, IdStore.getId(resurrectedProject));
        assertEquals(buildId, IdStore.forClass(Run.class).get(build));
        
        assertEquals(buildId, IdStore.getId(resurrectedProject.getBuild(build.getId())));
    }
    
    @Test
    public void projectWithNewRunIdStore() throws Exception {
        Project p = jenkinsRule.createFreeStyleProject();
        assertNull(IdStore.getId(p));
        
        IdStore.makeId(p);
        String id = IdStore.getId(p);
        AbstractBuild build = jenkinsRule.buildAndAssertSuccess(p);

        final String buildId = IdStore.getId(build);
        assertNotNull(buildId); // Will be calculated from RPersistenceRootIdStore
        IdStore.makeId(build);
        
        // Roundtrip   
        jenkinsRule.jenkins.reload();
        AbstractProject resurrectedProject = jenkinsRule.jenkins.getItemByFullName(p.getFullName(), AbstractProject.class);
        assertEquals(id, IdStore.getId(resurrectedProject));
        assertEquals(buildId, IdStore.getId(resurrectedProject.getBuild(build.getId())));
    }

    @Test
    public void folder() throws Exception {
        Folder f = jenkinsRule.jenkins.createProject(Folder.class,"folder");
        assertNull(IdStore.getId(f));
        IdStore.makeId(f);
        String id = IdStore.getId(f);
        jenkinsRule.jenkins.reload();
        assertEquals(id, IdStore.getId(jenkinsRule.jenkins.getItemByFullName("folder", Folder.class)));

    }
}
