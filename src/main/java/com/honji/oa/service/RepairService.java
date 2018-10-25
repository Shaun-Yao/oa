package com.honji.oa.service;

import com.honji.oa.domain.Repair;
import com.honji.oa.repository.RepairRepository;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepairService {

    private final String PROCESS_ID = "repair_process";

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepairRepository repairRepository;

    public void save(Repair repaire) {
        repairRepository.save(repaire);
    }

    public Repair findById(Long id) {
        return repairRepository.findById(id).get();
//        return repairRepository.findOne(id);
    }


    @Transactional
    public void start(Repair repaire) {
        repairRepository.save(repaire);
        final String id = repaire.getId().toString();

        //使用流程id+.+实体id作为流程的businessKey, 例如：repair_process.1
        final String businessKey = PROCESS_ID.concat("-").concat(id);
//        String operator = "user1";
//        String managers = "manager1,manager2";
        Map<String, Object> variables = new HashMap();
        variables.put("applicant", repaire.getApplicant());
        variables.put("manager", repaire.getApprover());

        runtimeService.startProcessInstanceByKey(PROCESS_ID, businessKey, variables);


        Task task = taskService.createTaskQuery().processInstanceBusinessKey(businessKey).singleResult();
//        taskService.claim(task.getId(), repaire.getApprover());
          taskService.complete(task.getId(), variables);
    }

    @Transactional
    public boolean approver(String id) {
        final String businessKey = PROCESS_ID.concat("-").concat(id);
        Map<String, Object> variables = new HashMap();
        variables.put("repairer", "repairer1");
        Task task = taskService.createTaskQuery().processInstanceBusinessKey(businessKey).singleResult();
        //taskService.claim(task.getId(), operator);
        taskService.complete(task.getId(), variables);
        //更新请假信息的审核人
//        VacationForm form = vacationFormService.findOne(Integer.parseInt(id));
//        if (form != null) {
//            form.setApprover(operator);
//            vacationFormService.save(form);
//        }

        return true;
    }

    public List<Repair> findTodoList(String assignee) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        List<Long> ids = new ArrayList<>();

        for(Task task : tasks) {
            ProcessInstance pi = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String businessKey = pi.getBusinessKey();
            String repairId = businessKey.split("-")[1];
            ids.add(Long.valueOf(repairId));
        }

        List<Repair> repairs = repairRepository.findAllById(ids);
//        List<Repair> repairs = repairRepository.findAll(ids);
        return repairs;
    }
}
