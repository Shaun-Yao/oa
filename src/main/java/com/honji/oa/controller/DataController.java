package com.honji.oa.controller;

import com.honji.oa.domain.Repair;
import com.honji.oa.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/datas")
public class DataController {

    @Autowired
    private RepairService repairService;

    @GetMapping(value = "/myApplications/{userId}")
    public Page<Repair> myApplications(@PathVariable String userId, @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "5") int size) {
        Page<Repair> repairPage = repairService.findMyApplications(userId, page, size);
        return repairPage;

    }


}
