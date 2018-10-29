package com.honji.oa.controller;

import com.honji.oa.domain.Repair;
import com.honji.oa.service.RepairService;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpMessage;
import me.chanjar.weixin.cp.bean.WxCpUser;
import me.chanjar.weixin.cp.config.WxCpConfigStorage;
import me.chanjar.weixin.cp.message.WxCpMessageRouter;
import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
    private RepositoryService repositoryService;

    @Autowired
    private FormService formService;

    @Autowired
    private WxCpService wxService;

    @Autowired
    private WxCpMessageRouter router;

    @GetMapping("/auth")
    public String auth(@RequestParam String code, Model model) throws WxErrorException {
        //String code = request.getParameter("code");
        //WxCpConfigStorage configStorage = wxService.getWxCpConfigStorage();
        String[] res =  wxService.getOauth2Service().getUserInfo(code);
        String userId = res[0];
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).list();

        model.addAttribute("tasks", tasks);
        model.addAttribute("applicantId", res[0]);

        return "index";
    }

    @GetMapping("/index")
    public String index() {

        return "index";
    }

    @GetMapping("/toApply/{applicantId}")
    public String toApply(@PathVariable String applicantId, Model model) throws WxErrorException {

        WxCpUser wxCpUser = wxService.getUserService().getById(applicantId);
        //由于公司现有OA对接企业微信把微信的name用来存放id,把真正的名字存到了EnglishName字段，所以这里取EnglishName
        String name = wxCpUser.getEnglishName();
        //model.addAttribute("processDefinitionId", processDefinitionId);
        model.addAttribute("applicantId", applicantId);
        model.addAttribute("applicant", name);

        return "applyForm";
    }

    @PostMapping("/apply")
    public String apply(@ModelAttribute Repair repair) {
        repairService.apply(repair);
        return "index";
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

    @RequestMapping(value = "/todoList", method = {RequestMethod.GET, RequestMethod.POST})
    //@GetMapping("/todoList")
    public String todoList(Model model) {
        List<Repair> repairs = repairService.findTodoList("123");
        model.addAttribute("repairs", repairs);
        return "todoList";
    }

    @GetMapping("/toView/{id}/{taskId}")
    public String toView(@PathVariable Long id, @PathVariable String taskId, Model model) {
        Repair repair = repairService.findById(id);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
        model.addAttribute("repair", repair);
        model.addAttribute("taskId", taskId);
        model.addAttribute("comments", comments);
        return "viewForm";
    }

    @PostMapping("/complete/{taskId}")
    public String complete(@PathVariable String taskId, @RequestParam String comment) {
        Map<String, Object> variables = new HashMap();
        variables.put("repairer", "123");
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        taskService.addComment(taskId, processInstanceId, comment);
        taskService.complete(taskId, variables);

        return "redirect:/todoList";
    }

    @GetMapping("/toAudit/{taskId}")
    public String toEdit(@PathVariable("taskId") String taskId, Model model) {
        TaskFormData taskFormData = formService.getTaskFormData(taskId);
        model.addAttribute("formProperties", taskFormData.getFormProperties());
        model.addAttribute("taskId", taskId);
        return "auditForm";
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