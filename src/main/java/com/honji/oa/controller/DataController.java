package com.honji.oa.controller;

import com.honji.oa.domain.Repair;
import com.honji.oa.service.RepairService;
import org.activiti.engine.TaskService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping(value = "/datas")
public class DataController {

    @Autowired
    private RepairService repairService;

    @Autowired
    private TaskService taskService;

    @GetMapping(value = "/myApplications/{userId}")
    public Page<Repair> myApplications(@PathVariable String userId, @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "5") int size) {
        System.out.println(userId);
        Page<Repair> repairPage = repairService.findMyApplications(userId, page, size);
        return repairPage;

    }

    @GetMapping(value = "/todoList/{userId}")
    public Page<Repair> todoList(@PathVariable String userId, @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size) {
        Page<Repair> repairPage = repairService.findTodoList(userId, page, size);
        return repairPage;
    }

    @GetMapping(value = "/attachment/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getAttachmentContent(@PathVariable String id) throws IOException {
        InputStream in = taskService.getAttachmentContent(id);
        return IOUtils.toByteArray(in);
    }

}
