package com.honji.oa;

import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class OaApplication implements CommandLineRunner {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private FormService formService;

//    @Autowired
//    private PlatformTransactionManager transactionManager;
//
//    @Autowired
//    HikariDataSource hikariDataSource;

    public static void main(String[] args) {
        SpringApplication.run(OaApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
//        final String processId = "testRepair";
//        System.out.println("Number of process definitions : "
//                + repositoryService.createProcessDefinitionQuery().count());
//        System.out.println("Number of tasks : " + taskService.createTaskQuery().count());
//        runtimeService.startProcessInstanceByKey(processId);
//
//        System.out.println("Number of tasks after process start: " + taskService.createTaskQuery().count());
    }

/*

        @Bean
        public SpringProcessEngineConfiguration getProcessEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager){
//        StandaloneProcessEngineConfiguration config = new StandaloneProcessEngineConfiguration();
//            config.setDataSource(dataSource);
//      config.setDatabaseType("mysql");
//      config.setDatabaseSchemaUpdate("true");
//        return config;
           SpringProcessEngineConfiguration config =
                             new SpringProcessEngineConfiguration();
      config.setDataSource(dataSource);
      config.setTransactionManager(transactionManager);
      config.setDatabaseType("mysql");
      config.setDatabaseSchemaUpdate("true");
      return config;

    }
*/



}

