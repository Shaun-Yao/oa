package com.honji.oa.controller;

import com.honji.oa.service.RepairService;
import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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

    @GetMapping("/repairForm/{processDefinitionId}")
    public String repairForm(@PathVariable("processDefinitionId") String pdid, Model model) {

        //ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        //Object renderedStartForm = formService.getRenderedStartForm(processDefinitionId);
        StartFormData startFormData = formService.getStartFormData(pdid);

        model.addAttribute("formProperties", startFormData.getFormProperties());
        model.addAttribute("processDefinitionId", pdid);

        return "repairForm";
    }

    @PostMapping("/add/{processDefinitionId}")
    public String add(@PathVariable("processDefinitionId") String pdid, HttpServletRequest request) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(pdid).singleResult();
        // 先读取表单字段在根据表单字段的ID读取请求参数值
        StartFormData formData = formService.getStartFormData(pdid);
        Map<String, String> formValues = new HashMap<String, String>();
        // 从请求中获取表单字段的值
        List<FormProperty> formProperties = formData.getFormProperties();
        for (FormProperty formProperty : formProperties) {
            String value = request.getParameter(formProperty.getId());
            formValues.put(formProperty.getId(), value);
        }
        // 提交表单字段并启动一个新的流程实例
        ProcessInstance processInstance = formService.submitStartFormData(pdid, formValues);
        return "repairForm";
    }

    @GetMapping("/todoList")
    public String todoList(Model model) {
        List<Task> tasks = taskService.createTaskQuery().list();
        model.addAttribute("tasks", tasks);
        return "todoList";
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
        Map<String, String> formValues = new HashMap<String, String>();
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