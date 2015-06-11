package org.jenkinsci.plugins.uniqueid;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Project;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class IdTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void project() throws Exception {
        Project p = jenkinsRule.createFreeStyleProject();
        assertNull(IdStore.getId(p));
        IdStore.makeId(p);
        String id = IdStore.getId(p);
        AbstractBuild build = jenkinsRule.buildAndAssertSuccess(p);

        // a build will get an id computed from its parent.
        String buildId = IdStore.getId(build);
        assertEquals(buildId, id+"_1");

        // should be a no-op
        IdStore.makeId(build);
        assertEquals(IdStore.getId(build), buildId);

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
