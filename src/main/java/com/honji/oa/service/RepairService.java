package com.honji.oa.service;

import com.honji.oa.config.OaConstants;
import com.honji.oa.domain.Repair;
import com.honji.oa.repository.RepairRepository;
import org.activiti.engine.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepairService {


    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;


    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepairRepository repairRepository;

    @Autowired
    private IdentityService identityService;



    public Repair findById(Long id) {
        return repairRepository.findById(id).get();
//        return repairRepository.findOne(id);
    }


    //TODO 事务有问题 可能需要implement RepairService
    @Transactional
    public void apply(Repair repair) {
        repairRepository.save(repair);
        final String id = repair.getId().toString();

        //使用流程id加实体id作为流程的businessKey, 例如：repair_process-1
        final String businessKey = OaConstants.REPAIR_PROCESS_ID.concat("-").concat(id);
        identityService.setAuthenticatedUserId(repair.getApplicantId());
        Map<String, Object> variables = new HashMap();
        final int deviceType = repair.getDeviceType();
        variables.put("deviceType", deviceType);
        if(deviceType == 0) {
            variables.put("handler", "123");
        } else if(deviceType == 1) {
            variables.put("handler", "234");
        }

        runtimeService.startProcessInstanceByKey(OaConstants.REPAIR_PROCESS_ID, businessKey, variables);

    }

    @Transactional
    public boolean approver(String id) {
        final String businessKey = OaConstants.REPAIR_PROCESS_ID.concat("-").concat(id);
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

    public Page<Repair> findTodoList(String assignee, int page, int size) {

        final int offset = page * size;
        long total = taskService.createTaskQuery().taskAssignee(assignee).count();
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee)
                .orderByTaskCreateTime().desc().listPage(offset, size);
        List<Repair> repairs = new ArrayList<>();

        for(Task task : tasks) {
            ProcessInstance pi = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String businessKey = pi.getBusinessKey();
            Long repairId = Long.valueOf(businessKey.split("-")[1]);
            Repair repair = repairRepository.findById(repairId).get();
            repair.setProcessInstanceId(pi.getId());
            repairs.add(repair);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Repair> repairPage = new PageImpl<>(repairs, pageable, total);
        return repairPage;
    }

    public Page<Repair> findMyApplications(String userId, int page, int size) {
        /*final int offset = page * size;
        long total = runtimeService.createProcessInstanceQuery().startedBy(userId).count();
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                .startedBy(userId).orderByProcessInstanceId().desc().listPage(offset, size);
        List<Repair> repairs = new ArrayList<>();

        for(ProcessInstance pi : processInstances) {
            String businessKey = pi.getBusinessKey();
            Long repairId = Long.valueOf(businessKey.split("-")[1]);
            Repair repair = repairRepository.findById(repairId).get();
            repair.setProcessInstanceId(pi.getId());
            repairs.add(repair);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Repair> repairPage = new PageImpl<>(repairs, pageable, total);*/
        Pageable pageable = PageRequest.of(page, size);
        Page<Repair> repairPage = repairRepository.findByApplicantIdOrderByCreatedTimeDesc(userId, pageable);
        return repairPage;
    }

    @Transactional
    public void complete(String taskId, String comment, Map<String, Object> variables) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        taskService.addComment(taskId, processInstanceId, comment);
        taskService.complete(taskId, variables);
    }

    @Transactional
    public void finish(String taskId, String comment, Repair repair) {
        Repair dbRepair = repairRepository.getOne(repair.getId());
        dbRepair.setScore(repair.getScore());
        repairRepository.save(dbRepair);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        taskService.addComment(taskId, processInstanceId, comment);
        taskService.complete(taskId);
    }
}
