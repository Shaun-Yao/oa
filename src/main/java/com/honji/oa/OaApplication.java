package com.honji.oa;

import lombok.extern.slf4j.Slf4j;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class OaApplication implements CommandLineRunner {


//    @Autowired
//    private DataSourceTransactionManager transactionManager;

//    @Autowired
//    private PlatformTransactionManager transactionManager;
//
//
//    @Autowired
//    private DataSource dataSource;

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
    public SpringProcessEngineConfiguration processEngineConfiguration() {
        log.info("datasource={}", dataSource);
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(dataSource);
        config.setTransactionManager(transactionManager);
        config.setDatabaseSchemaUpdate("true");
        config.setAsyncExecutorActivate(false);
        return config;
    }


    @Bean
    public ProcessEngineFactoryBean processEngine() {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
        return factoryBean;
    }

*/



}

