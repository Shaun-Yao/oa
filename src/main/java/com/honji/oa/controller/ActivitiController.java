package com.honji.oa.controller;

import com.honji.oa.config.OaConstants;
import com.honji.oa.domain.Repair;
import com.honji.oa.service.RepairService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpMessage;
import me.chanjar.weixin.cp.bean.WxCpUser;
import me.chanjar.weixin.cp.config.WxCpConfigStorage;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class ActivitiController {

    @Autowired
    private RepairService repairService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private FormService formService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private WxCpService wxService;

    @Autowired
    private  HttpSession session;
/*

    @GetMapping("/auth")
    public String auth(@RequestParam String code,
                        HttpSession session, HttpServletRequest request) throws WxErrorException {
        String[] res =  wxService.getOauth2Service().getUserInfo(code);
        final String userId = res[0];
        WxCpUser wxCpUser = wxService.getUserService().getById(userId);
        //由于公司现有OA对接企业微信把微信的name用来存放id,把真正的名字存到了EnglishName字段，所以这里取EnglishName
        final String userName = wxCpUser.getEnglishName();
        session.setAttribute("userId", userId);
        session.setAttribute("userName", userName);

        return "forward:".concat(request.getRequestURI());
    }

    @GetMapping("/index")
    public String index(@RequestParam(required = false) String tab, Model model){
        model.addAttribute("tab", tab);
        return "index";
    }
*/



    @GetMapping("/index")
    public String index(@RequestParam(required = false) String code, @RequestParam(required = false) String tab,
                        HttpSession session, Model model) {
        this.initSession(code, session);
        model.addAttribute("tab", tab);
        return "index";
    }


    @GetMapping("/toApply")
    public String toApply(Model model) throws WxErrorException {
        String userId = (String) session.getAttribute(OaConstants.USER_ID);
        WxCpUser wxCpUser = wxService.getUserService().getById(userId);
        Integer[] departIds = wxCpUser.getDepartIds();
        List<WxCpDepart> departs = wxService.getDepartmentService().list(departIds[0]);
        model.addAttribute("applicantMobile", wxCpUser.getMobile());
        model.addAttribute("applicantDepart", departs.get(0).getName());
        return "applyForm";
    }

    @PostMapping("/apply")
    public String apply(@ModelAttribute Repair repair) {
        repairService.apply(repair);
        return "redirect:/index";
    }

    /**
     * 完成任务
     * @param taskId
     * @param comment
     * @return
     */
    //@ResponseBody
    @PostMapping("/complete/{taskId}")
    public String complete(@PathVariable String taskId, @RequestParam String comment,
                           @RequestParam String handler) {

        repairService.complete(taskId, comment, handler);
        return "redirect:/index";
    }

    /**
     * 结束流程
     * @param taskId
     * @param comment
     * @return
     */
    @ResponseBody
    @PostMapping("/finish/{taskId}")
    public void finish(@PathVariable("taskId") String taskId, @RequestParam String comment,
                       @ModelAttribute Repair repair, HttpSession session) {
        //添加备注前需要先设置当前用户名作为备注的userId
        identityService.setAuthenticatedUserId(String.valueOf(session.getAttribute("userName")));
        repairService.finish(taskId, comment, repair);
        //return "redirect:/index";
    }

    @ResponseBody
    @PostMapping("/transfer/{taskId}")
    public void transfer(@PathVariable("taskId") String taskId, @RequestParam String repairer,
                           @RequestParam String comment) {

       repairService.transfer(taskId, comment, repairer);
    }

    @GetMapping("/repairForm/{processDefinitionId}/{userId}")
    public String repairForm(@PathVariable("processDefinitionId") String pdid,
                             @PathVariable("userId") String userId, Model model) {

        //ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        //Object renderedStartForm = formService.getRenderedStartForm(processDefinitionId);
        StartFormData startFormData = formService.getStartFormData(pdid);

        model.addAttribute("formProperties", startFormData.getFormProperties());
        model.addAttribute("processDefinitionId", pdid);
        model.addAttribute("userId", userId);

        return "repairForm";
    }

    @PostMapping("/add/{processDefinitionId}")
    public String add(@PathVariable("processDefinitionId") String pdid, HttpServletRequest request) throws WxErrorException {

        // 先读取表单字段在根据表单字段的ID读取请求参数值
        StartFormData formData = formService.getStartFormData(pdid);
        Map<String, String> formValues = new HashMap();
        // 从请求中获取表单字段的值
        List<FormProperty> formProperties = formData.getFormProperties();
        for (FormProperty formProperty : formProperties) {
            String value = request.getParameter(formProperty.getId());
            formValues.put(formProperty.getId(), value);
        }
        // 提交表单字段并启动一个新的流程实例
        ProcessInstance processInstance = formService.submitStartFormData(pdid, formValues);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String manager = request.getParameter("manager");
        WxCpConfigStorage configStorage = wxService.getWxCpConfigStorage();
        WxCpMessage message = WxCpMessage.TEXTCARD().agentId(configStorage.getAgentId())
                .toUser(manager).title("有新的流程待办事项")
                .description("流程待办事项描述。。。")
                .url("http://f32597fb.ngrok.io/toAudit/".concat(task.getId()))
                .build();
        wxService.messageSend(message);
        return "repairForm";
    }

    @RequestMapping(value = "/todoList/{userId}", method = {RequestMethod.GET, RequestMethod.POST})
    //@GetMapping("/todoList")
    public String todoList(@PathVariable String userId, @RequestParam(defaultValue = "0") Integer offset,
                           @RequestParam(defaultValue = "5") Integer limit, Model model) {
        Page<Repair> repairPage = repairService.findTodoList(userId, offset, limit);
        model.addAttribute("repairPage", repairPage);
        return "todoList";
    }


    @GetMapping("/toView/{id}")
    public String toView(@PathVariable("id") Long id, Model model) {
        Repair repair = repairService.findById(id);
        String businessKey = OaConstants.REPAIR_PROCESS_ID.concat("-").concat(id.toString());
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
        String processInstanceId = null;
        if(processInstance == null) {
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
            processInstanceId = hpi.getId();
        } else {
            processInstanceId = processInstance.getId();
        }

        List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);

        model.addAttribute("repair", repair);
        model.addAttribute("comments", comments);
        return "viewForm";
    }


    @GetMapping("/toAudit/{id}/{taskId}")
    public String toAudit(@PathVariable Long id, @PathVariable String taskId, @RequestParam(required = false) String code,
                          HttpSession session, Model model) {

        this.initSession(code, session);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {// 任务已经结束返回查看界面
            return "redirect:/toView/".concat(id.toString());
        }

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();

        //String id = processInstance.getBusinessKey().split("-")[1];
        //Repair repair = repairService.findById(id);
        //String businessKey = OaConstants.REPAIR_PROCESS_ID.concat("-").concat(id.toString());
        //ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
        //Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        //final String taskId = task.getId();
        List<Comment> comments = taskService.getProcessInstanceComments(processInstance.getId());
        Object renderedStartForm = formService.getRenderedTaskForm(taskId);
        List<WxCpUser> users = new ArrayList<>();
        try {
            List<WxCpUser> repairers = wxService.getTagService().listUsersByTagId("1");
            //WxCpUser user = wxService.getUserService().getById(repairers.get(0).getUserId());
            //listUsersByTagId获取到的实体没englishName字段，所以要重新获取一遍
            for(WxCpUser repairer : repairers) {
                WxCpUser user = wxService.getUserService().getById(repairer.getUserId());
                //排除本人
                if(!repairer.getUserId().equals(session.getAttribute("userId"))) {
                    users.add(user);
                }
            }
        } catch (WxErrorException e) {
            log.error("获取电工标签成员出错", e);
            e.printStackTrace();
        }


        model.addAttribute("renderedStartForm", renderedStartForm);
        //model.addAttribute("repair", repair);
        model.addAttribute("repairers", users);
        model.addAttribute("taskId", taskId);
        model.addAttribute("comments", comments);
        return "auditForm";
    }

    private void initSession(String code, HttpSession session)  {
        if(!StringUtils.isBlank(code)) {//code非空则是微信网页登录跳转过来的
            try {
                String[] res = wxService.getOauth2Service().getUserInfo(code);
                final String userId = res[0];
                WxCpUser wxCpUser = wxService.getUserService().getById(userId);
                //由于公司现有OA对接企业微信把微信的name用来存放id,把真正的名字存到了EnglishName字段，所以这里取EnglishName
                final String userName = wxCpUser.getEnglishName();
                session.setAttribute("userId", userId);
                session.setAttribute("userName", userName);
            } catch (WxErrorException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/viewForm/{taskId}")
    public String viewForm(@PathVariable String taskId, Model model) {

        Object renderedStartForm = formService.getRenderedTaskForm(taskId);
        model.addAttribute("renderedStartForm", renderedStartForm);

        return "viewForm";
    }


    @PostMapping("/audit/{taskId}")
    public String audit(@PathVariable("taskId") String taskId, HttpServletRequest request) {
        TaskFormData taskFormData = formService.getTaskFormData(taskId);
        Map<String, String> formValues = new HashMap();
        // 从请求中获取表单字段的值
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        for (FormProperty formProperty : formProperties) {
            if (formProperty.isWritable()) {
                String value = request.getParameter(formProperty.getId());
                formValues.put(formProperty.getId(), value);
            }
        }

        formService.submitTaskFormData(taskId, formValues);
        return "todoList";
    }


}