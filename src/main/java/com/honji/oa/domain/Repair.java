package com.honji.oa.domain;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
public class Repair implements Serializable {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    //申请者
    @Column(nullable = false)
    private String applicant;

    //审批者
    @Column(nullable = false)
    private String approver;

    //申请所处状态
    @Transient
    private String state;
}
