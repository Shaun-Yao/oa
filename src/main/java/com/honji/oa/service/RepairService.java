package com.honji.oa.service;

import com.honji.oa.config.OaConstants;
import com.honji.oa.domain.Repair;
import com.honji.oa.enums.ProcessStatus;
import com.honji.oa.repository.RepairRepository;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpMessage;
import me.chanjar.weixin.cp.bean.WxCpUser;
import me.chanjar.weixin.cp.config.WxCpConfigStorage;
import org.activiti.engine.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Task;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RepairService {

/*

    Repair findById(Long id);

    void apply(Repair repair);

    Page<Repair> findTodoList(String assignee, int page, int size);

    Page<Repair> findMyApplications(String userId, int page, int size);

    void complete(String taskId, String comment, String handler);

    void finish(String taskId, String comment, Repair repair);
*/

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

    @Autowired
    private WxCpService wxService;


    @Autowired
    private HttpServletRequest request;
    
    public Repair findById(Long id) {
        return repairRepository.findById(id).get();
    }


    //TODO 事务有问题
    @Transactional(rollbackFor = Exception.class)
    public void apply(Repair repair, MultipartFile file) {
        repairRepository.save(repair);
        final String id = repair.getId().toString();

        //使用流程id加实体id作为流程的businessKey, 例如：repair_process-1
        final String businessKey = OaConstants.REPAIR_PROCESS_ID.concat("-").concat(id);
        identityService.setAuthenticatedUserId(repair.getApplicantId());
        Map<String, Object> variables = new HashMap();
        final int deviceType = repair.getDeviceType();
        variables.put("deviceType", deviceType);
        String handler = null;
        try {
            if(deviceType == 0) {
                List<WxCpUser> repairers = wxService.getTagService().listUsersByTagId("1");
                handler = repairers.get(2).getUserId();//微信按userid排序，无法手动排序，所以只能取第一个
                variables.put("repairer", handler);
            } else if(deviceType == 1) {
                List<WxCpUser> managers = wxService.getTagService().listUsersByTagId("2");
                handler = managers.get(0).getUserId();//微信按userid排序，无法手动排序，所以只能取第一个
                variables.put("hr_manager", handler);
            }
        } catch (WxErrorException e) {
            e.printStackTrace();
        }
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(OaConstants.REPAIR_PROCESS_ID, businessKey, variables);
        final String processInstanceId = processInstance.getId();
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        final String taskId = task.getId();
        if (!file.isEmpty()) {
            final String fileName = file.getOriginalFilename();
            String attachmentType = FilenameUtils.getExtension(fileName);
            try {
                Attachment attachment = taskService.createAttachment(attachmentType, taskId, processInstanceId,
                        fileName, "repair attachment", file.getInputStream());
                log.info("物品维修附件{}上传成功", fileName);
            } catch (IOException e) {
                log.error("物品维修附件上传众所失败", e);
            }
        }
        Map<String, String> formData = new HashMap();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        formData.put("id", String.valueOf(repair.getId()));
        formData.put("taskId", taskId);
        formData.put("applicantId", repair.getApplicantId());
        formData.put("applicant", repair.getApplicant());
        formData.put("applicantMobile", repair.getApplicantMobile());
        formData.put("applicantDepart", repair.getApplicantDepart());
        formData.put("deviceName", repair.getDeviceName());
        formData.put("deviceType", String.valueOf(repair.getDeviceType()));
        formData.put("description", repair.getDescription());
        formData.put("createdTime", repair.getCreatedTime().format(formatter));
        formService.saveFormData(taskId, formData);

        this.sendMsg(handler, repair, taskId);
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
            repair.setTaskId(task.getId());
            repairs.add(repair);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Repair> repairPage = new PageImpl<>(repairs, pageable, total);
        return repairPage;
    }

    
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

    @Transactional(rollbackFor = Exception.class)
    public void complete(String taskId, String comment, String handlerRole) {

        Map<String, Object> variables = new HashMap();
        final String hrManager = "hr_manager";
        final String repairer = "repairer";
        final String applicant = "applicant";
        String handler = null;
        //添加备注前需要先设置当前用户名作为备注的userId
        identityService.setAuthenticatedUserId(String.valueOf(request.getSession().getAttribute("userName")));
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();


        try {
            switch (handlerRole) {
                case hrManager:
                    List<WxCpUser> repairers = wxService.getTagService().listUsersByTagId("1");
                    handler = repairers.get(2).getUserId();//微信按userid排序，无法手动排序，所以只能取第一个
                    variables.put(hrManager, handler);
                    break;
                case repairer:
                    List<WxCpUser> managers = wxService.getTagService().listUsersByTagId("2");
                    handler = managers.get(0).getUserId();//微信按userid排序，无法手动排序，所以只能取第一个
                    variables.put(repairer, handler);
                    break;
                case applicant:
                    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                            .processInstanceId(processInstanceId).singleResult();
                    String id = processInstance.getBusinessKey().split("-")[1];
                    Repair repair = repairRepository.getOne(Long.valueOf(id));
                    variables.put(applicant, repair.getApplicantId());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        } catch (WxErrorException e) {
            e.printStackTrace();
        }


        taskService.addComment(taskId, processInstanceId, comment);
        taskService.complete(taskId, variables);

        //完成任务后需要设置下一次任务的id到formData
        Task nextTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        String nextTaskId = nextTask.getId();
        Map<String, String> formData = new HashMap();
        formData.put("taskId", nextTaskId);
        formService.saveFormData(nextTaskId, formData);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(nextTask.getProcessInstanceId()).singleResult();
        String id = processInstance.getBusinessKey().split("-")[1];
        Repair repair = repairRepository.getOne(Long.valueOf(id));

        this.sendMsg(handler, repair, nextTaskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transfer(String taskId, String comment, String repairer) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        identityService.setAuthenticatedUserId(String.valueOf(
                request.getSession().getAttribute("userName")));
        taskService.setAssignee(taskId, repairer);
        taskService.addComment(taskId, processInstanceId, comment);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).singleResult();
        String id = processInstance.getBusinessKey().split("-")[1];
        Repair repair = repairRepository.getOne(Long.valueOf(id));
        this.sendMsg(repairer, repair, task.getId());
    }


    @Transactional(rollbackFor = Exception.class)
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

    private void sendMsg(String handler, Repair repair, String taskId) {
        String rootPath = request.getRequestURL().substring(0,
                request.getRequestURL().length() - request.getRequestURI().length());
        String auditPath = rootPath.concat("/toAudit/").concat(repair.getId().toString())
                .concat("/").concat(taskId);
        WxCpConfigStorage configStorage = wxService.getWxCpConfigStorage();
        WxCpMessage message = WxCpMessage.TEXTCARD().agentId(configStorage.getAgentId())
                .toUser(handler).title("有新的流程待办事项")
                .description(repair.getDeviceName().concat(": ").concat(repair.getDescription()))
                .url(auditPath)
                .build();
        try {
            wxService.messageSend(message);
        } catch (WxErrorException e) {
            log.error("发送消息通知失败", e);
            e.printStackTrace();
        }
    }


}
