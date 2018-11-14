package com.honji.oa.service;

import com.honji.oa.domain.Repair;
import org.springframework.data.domain.Page;

public interface RepairService {


    Repair findById(Long id);



    void apply(Repair repair);


    Page<Repair> findTodoList(String assignee, int page, int size);


    Page<Repair> findMyApplications(String userId, int page, int size);

    void complete(String taskId, String comment, String handler);

    void finish(String taskId, String comment, Repair repair);
}
