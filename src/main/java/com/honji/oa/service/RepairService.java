package com.honji.oa.service;

import com.honji.oa.domain.Repair;
import com.honji.oa.repository.RepairRepository;
import org.activiti.engine.IdentityService;
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

    private final String PROCESS_ID = "testRepair";

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepairRepository repairRepository;

    @Autowired
    private IdentityService identityService;

    public void save(Repair repaire) {
        repairRepository.save(repaire);
    }

    public Repair findById(Long id) {
        return repairRepository.findById(id).get();
//        return repairRepository.findOne(id);
    }


    @Transactional
    public void apply(Repair repair) {
        repairRepository.save(repair);
        final String id = repair.getId().toString();

        //使用流程id加实体id作为流程的businessKey, 例如：repair_process-1
        final String businessKey = PROCESS_ID.concat("-").concat(id);
//        String operator = "user1";
//        String managers = "manager1,manager2";
        identityService.setAuthenticatedUserId(repair.getApplicantId());
        Map<String, Object> variables = new HashMap();
        //variables.put("applicant", repair.getApplicant());
        variables.put("manager", "518974");
        runtimeService.startProcessInstanceByKey(PROCESS_ID, businessKey, variables);


        //Task task = taskService.createTaskQuery().processInstanceBusinessKey(businessKey).singleResult();
//        taskService.claim(task.getId(), repaire.getApprover());


        //taskService.complete(task.getId());
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
        //TODO listPage
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        List<Repair> repairs = new ArrayList<>();
        //List<Long> ids = new ArrayList<>();

        for(Task task : tasks) {
            ProcessInstance pi = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String businessKey = pi.getBusinessKey();
            Long repairId = Long.valueOf(businessKey.split("-")[1]);
            Repair repair = repairRepository.findById(repairId).get();
            repair.setTask(task);
            repairs.add(repair);
            //ids.add(Long.valueOf(repairId));
        }

        return repairs;
    }
}
