package com.honji.oa.service.impl;

import com.honji.oa.config.OaConstants;
import com.honji.oa.domain.Repair;
import com.honji.oa.enums.ProcessStatus;
import com.honji.oa.repository.RepairRepository;
import com.honji.oa.service.RepairService;
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
public class RepairServiceImpl implements RepairService {


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

    @Autowired
    private FormService formService;



    @Override
    public Repair findById(Long id) {
        return repairRepository.findById(id).get();
//        return repairRepository.findOne(id);
    }


    //TODO 事务有问题 可能需要implement RepairService
    @Transactional(rollbackFor = Exception.class)
    @Override
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
            variables.put("repairer", "518974");
        } else if(deviceType == 1) {
            variables.put("hr_manager", "518974");
        }
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(OaConstants.REPAIR_PROCESS_ID, businessKey, variables);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String taskId = task.getId();
        Map<String, String> formData = new HashMap();
        formData.put("id", String.valueOf(repair.getId()));
        formData.put("taskId", taskId);
        formData.put("applicantId", repair.getApplicantId());
        formData.put("applicant", repair.getApplicant());
        formData.put("deviceName", repair.getDeviceName());
        formData.put("deviceType", String.valueOf(repair.getDeviceType()));
        formData.put("description", repair.getDescription());
        formData.put("createdTime", repair.getCreatedTime().toString());
        formService.saveFormData(taskId, formData);
    }


    @Override
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

    @Override
    public Page<Repair> findMyApplications(String userId, int page, int size) {
       /* final int offset = page * size;
        long total = historyService.createHistoricProcessInstanceQuery().startedBy(userId).count();
//        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
//                .startedBy(userId).orderByProcessInstanceId().desc().listPage(offset, size);
         List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                .startedBy(userId).orderByProcessInstanceId().desc().listPage(offset, size);
        List<Repair> repairs = new ArrayList<>();

        for(ProcessInstance pi : processInstances) {
            String businessKey = pi.getBusinessKey();
            Long repairId = Long.valueOf(businessKey.split("-")[1]);
            Repair repair = repairRepository.findById(repairId).get();
            //Task task = taskService.createTaskQuery().processInstanceBusinessKey(businessKey).singleResult();
            System.out.println(pi.getName());
            repairs.add(repair);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Repair> repairPage = new PageImpl<>(repairs, pageable, total);*/
        Pageable pageable = PageRequest.of(page, size);
        Page<Repair> repairPage = repairRepository.findByApplicantIdOrderByCreatedTimeDesc(userId, pageable);
        return repairPage;
    }

    @Transactional
    @Override
    public void complete(String taskId, String comment, String handler) {

        Map<String, Object> variables = new HashMap();
        final String hrManager = "hr_manager";
        final String repairer = "repairer";
        final String applicant = "applicant";
        switch (handler) {
            case hrManager:
                variables.put(hrManager, "518974");
                break;
            case repairer:
                variables.put(repairer, "518974");
                break;
            case applicant:
                variables.put(applicant, "518974");
                break;
            default:
                throw new IllegalArgumentException();
        }

        //添加备注前需要先设置当前用户名作为备注的userId
        //identityService.setAuthenticatedUserId(String.valueOf(session.getAttribute("userName")));

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        taskService.addComment(taskId, processInstanceId, comment);
        taskService.complete(taskId, variables);

        //完成任务后需要设置下一次任务的id到formData
        Task nextTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        String nextTaskId = nextTask.getId();
        Map<String, String> formData = new HashMap();
        formData.put("taskId", nextTaskId);
        formService.saveFormData(nextTaskId, formData);
    }

    @Transactional
    @Override
    public void finish(String taskId, String comment, Repair repair) {
        Repair dbRepair = repairRepository.getOne(repair.getId());
        dbRepair.setScore(repair.getScore());
        dbRepair.setStatus(ProcessStatus.FINISHED);//设置为结束状态
        repairRepository.save(dbRepair);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        taskService.addComment(taskId, processInstanceId, comment);
        taskService.complete(taskId);
    }
}
