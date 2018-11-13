package com.honji.oa.controller;

import com.honji.oa.config.OaConstants;
import com.honji.oa.domain.Repair;
import com.honji.oa.service.RepairService;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    @GetMapping("/index")
    public String index(@RequestParam String code, @RequestParam(required = false) String tab,
                        HttpSession session, Model model) throws WxErrorException {
        if(!StringUtils.isBlank(code)) {//code非空则是微信网页登录跳转过来的
            String[] res =  wxService.getOauth2Service().getUserInfo(code);
            final String userId = res[0];
            WxCpUser wxCpUser = wxService.getUserService().getById(userId);
            //由于公司现有OA对接企业微信把微信的name用来存放id,把真正的名字存到了EnglishName字段，所以这里取EnglishName
            final String userName = wxCpUser.getEnglishName();
//        Integer[] departIds = wxCpUser.getDepartIds();
//        List<WxCpDepart> departs = wxService.getDepartmentService().list(departIds[0]);
//        System.out.println(JsonUtils.toJson(departs));
            session.setAttribute("userId", userId);
            session.setAttribute("userName", userName);
        }

//        if(session.getAttribute("userId") != null) {
//            wxService.getOauth2Service().buildAuthorizationUrl();
//        }
        model.addAttribute("tab", tab);
        return "index";
    }
/*

    @GetMapping("/index")
    public String index(@RequestParam(required = false) String tab, Model model, HttpSession session) {
        model.addAttribute("tab", tab);
        session.setAttribute("userId", "518974");
        session.setAttribute("userName", "yao");
        return "index";
    }
*/

    @GetMapping("/toApply")
    public String toApply(Model model) throws WxErrorException {

        //WxCpUser wxCpUser = wxService.getUserService().getById(applicantId);
        //由于公司现有OA对接企业微信把微信的name用来存放id,把真正的名字存到了EnglishName字段，所以这里取EnglishName
        //String name = wxCpUser.getEnglishName();
        //model.addAttribute("processDefinitionId", processDefinitionId);
        //model.addAttribute("applicant", "yao");

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
                           @RequestParam String handler, HttpSession session) {
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

        //variables.put("handler", "518974");
        //Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        //添加备注前需要先设置当前用户名作为备注的userId
        identityService.setAuthenticatedUserId(String.valueOf(session.getAttribute("userName")));
        repairService.complete(taskId, comment, variables);
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
    public String transfer(@PathVariable("taskId") String taskId, @RequestParam String repairer,
                           @RequestParam String comment, HttpSession session) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        identityService.setAuthenticatedUserId(String.valueOf(session.getAttribute("userName")));
        taskService.setAssignee(taskId, repairer);
        taskService.addComment(taskId, processInstanceId, comment);

        return "success";
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


    @GetMapping("/toAudit/{id}")
    public String toAudit(@PathVariable Long id, Model model) {
        Repair repair = repairService.findById(id);
        String businessKey = OaConstants.REPAIR_PROCESS_ID.concat("-").concat(id.toString());
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        final String taskId = task.getId();
        List<Comment> comments = taskService.getProcessInstanceComments(processInstance.getId());

        Object renderedStartForm = formService.getRenderedTaskForm(taskId);
        model.addAttribute("renderedStartForm", renderedStartForm);
        model.addAttribute("repair", repair);
        model.addAttribute("taskId", taskId);
        model.addAttribute("comments", comments);
        return "auditForm";
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