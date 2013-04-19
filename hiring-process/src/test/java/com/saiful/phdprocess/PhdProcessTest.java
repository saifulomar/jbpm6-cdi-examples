package com.saiful.phdprocess;
/*
 * Copyright 2013 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.runtime.manager.impl.cdi.InjectableRegisterableItemsFactory;
import org.jbpm.runtime.manager.util.TestUtil;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.manager.Context;
import org.kie.internal.runtime.manager.RuntimeEngine;
import org.kie.internal.runtime.manager.RuntimeManager;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.task.api.TaskService;
import org.kie.internal.task.api.model.Content;
import org.kie.internal.task.api.model.Task;
import org.kie.internal.task.api.model.TaskSummary;

import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 *
 * @author salaboy/saiful
 */
@RunWith(Arquillian.class)
public class PhdProcessTest {

    @Deployment()
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "hiring-example.jar")
                .addPackage("org.jboss.seam.persistence") //seam-persistence
                .addPackage("org.jboss.seam.transaction") //seam-persistence
                .addPackage("org.jbpm.services.task")
                .addPackage("org.jbpm.services.task.wih") // work items org.jbpm.services.task.wih
                .addPackage("org.jbpm.services.task.annotations")
                .addPackage("org.jbpm.services.task.api")
                .addPackage("org.jbpm.services.task.impl")
                .addPackage("org.jbpm.services.task.events")
                .addPackage("org.jbpm.services.task.exception")
                .addPackage("org.jbpm.services.task.identity")
                .addPackage("org.jbpm.services.task.factories")
                .addPackage("org.jbpm.services.task.internals")
                .addPackage("org.jbpm.services.task.internals.lifecycle")
                .addPackage("org.jbpm.services.task.lifecycle.listeners")
                .addPackage("org.jbpm.services.task.query")
                .addPackage("org.jbpm.services.task.util")
                .addPackage("org.jbpm.services.task.commands") // This should not be required here
                .addPackage("org.jbpm.services.task.deadlines") // deadlines
                .addPackage("org.jbpm.services.task.deadlines.notifications.impl")
                .addPackage("org.jbpm.services.task.subtask")
                .addPackage("org.kie.internal.runtime")
                .addPackage("org.kie.internal.runtime.manager")
                .addPackage("org.kie.internal.runtime.manager.cdi.qualifier")
                .addPackage("org.jbpm.runtime.manager")
                .addPackage("org.jbpm.runtime.manager.impl")
                .addPackage("org.jbpm.runtime.manager.impl.cdi")
                .addPackage("org.jbpm.runtime.manager.impl.cdi.qualifier")
                .addPackage("org.jbpm.runtime.manager.impl.context")
                .addPackage("org.jbpm.runtime.manager.impl.factory")
                .addPackage("org.jbpm.runtime.manager.impl.jpa")
                .addPackage("org.jbpm.runtime.manager.impl.manager")
                .addPackage("org.jbpm.runtime.manager.mapper")
                .addPackage("org.jbpm.runtime.manager.impl.task")
                .addPackage("org.jbpm.runtime.manager.impl.tx")
                .addPackage("org.jbpm.shared.services.api")
                .addPackage("org.jbpm.shared.services.impl")
                .addPackage("org.droolsjbpm.services.api")
                .addPackage("org.droolsjbpm.services.impl")
                .addPackage("org.droolsjbpm.services.api.bpmn2")
                .addPackage("org.droolsjbpm.services.impl.bpmn2")
                .addPackage("org.droolsjbpm.services.impl.event.listeners")
                .addPackage("org.droolsjbpm.services.impl.audit")
                .addPackage("org.droolsjbpm.services.impl.util")
                .addPackage("org.droolsjbpm.services.impl.vfs")
                .addPackage("org.droolsjbpm.services.impl.example")
                .addPackage("org.kie.commons.java.nio.fs.jgit")
                .addPackage("com.salaboy.hiring.process")
                .addAsResource("jndi.properties", "jndi.properties")
                .addAsManifestResource("META-INF/persistence.xml", ArchivePaths.create("persistence.xml"))
                //                .addAsManifestResource("META-INF/Taskorm.xml", ArchivePaths.create("Taskorm.xml"))
                .addAsManifestResource("META-INF/beans.xml", ArchivePaths.create("beans.xml"));

    }
    private static PoolingDataSource pds;

    @BeforeClass
    public static void setup() {
        TestUtil.cleanupSingletonSessionId();
        pds = TestUtil.setupPoolingDataSource();


    }

    @AfterClass
    public static void teardown() {
        pds.close();
    }

    @After
    public void tearDownTest() {
    }
    @Inject
    private EntityManagerFactory emf;
    @Inject
    private BeanManager beanManager;
    @Inject
    private RuntimeManagerFactory managerFactory;

    @Test
    public void simpleExecutionTest() {
        assertNotNull(managerFactory);
        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.getDefault()
                .entityManagerFactory(emf)
                .registerableItemsFactory(InjectableRegisterableItemsFactory.getFactory(beanManager, null));

        //builder.addAsset(ResourceFactory.newClassPathResource("repo/mapping.drl"), ResourceType.DRL);
        
        builder.addAsset(ResourceFactory.newClassPathResource("phdrepo/phdprocess.bpmn2"), ResourceType.BPMN2);


        RuntimeManager manager = managerFactory.newSingletonRuntimeManager(builder.get());
        testHiringProcess(manager, EmptyContext.get());

        manager.close();

    }

    private void testHiringProcess(RuntimeManager manager, Context context) {

        RuntimeEngine runtime = manager.getRuntimeEngine(context);
        KieSession ksession = runtime.getKieSession();
        TaskService taskService = runtime.getTaskService();


        assertNotNull(runtime);
        assertNotNull(ksession);
        
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("studentid","saiful");

        ProcessInstance processInstance = ksession.startProcess("phdprocess",params);

        List<TaskSummary> tasks = taskService.getTasksAssignedByGroup("staff", "en-UK");
        TaskSummary readinessReview = tasks.get(0);
        
        Task readinessReviewTask = taskService.getTaskById(readinessReview.getId());
        Content contentById = taskService.getContentById(readinessReviewTask.getTaskData().getDocumentContentId());
        assertNotNull(contentById);

        Map<String, Object> taskContent = (Map<String, Object>) ContentMarshallerHelper.unmarshall(contentById.getContent(), null);
        assertEquals("saiful", taskContent.get("in.studentid"));
        
        taskService.claim(readinessReview.getId(), "paul");
        taskService.start(readinessReview.getId(), "paul");

        Map<String, Object> hrOutput = new HashMap<String, Object>();
        hrOutput.put("out.readiness", "no");

        taskService.complete(readinessReview.getId(), "paul", hrOutput);

        tasks = taskService.getTasksAssignedByGroup("staff", "en-UK");
        TaskSummary progressReview = tasks.get(0);
        Task progressReviewTask = taskService.getTaskById(progressReview.getId());
        contentById = taskService.getContentById(progressReviewTask.getTaskData().getDocumentContentId());
        assertNotNull(contentById);
        taskContent = (Map<String, Object>) ContentMarshallerHelper.unmarshall(contentById.getContent(), null);
        
        assertEquals("saiful", taskContent.get("in.studentid"));

        taskService.claim(progressReview.getId(), "chris");
        taskService.start(progressReview.getId(), "chris");

        Map<String, Object> progressOutput = new HashMap<String, Object>();
        progressOutput.put("out.progress", "re-register");

        taskService.complete(progressReview.getId(), "chris", progressOutput);

        
        
        int removeAllTasks = taskService.removeAllTasks();
        System.out.println(">>> Removed Tasks > " + removeAllTasks);
    }
}
