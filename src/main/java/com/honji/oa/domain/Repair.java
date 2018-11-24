package com.honji.oa.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.honji.oa.enums.ProcessStatus;
import lombok.Data;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "OA_REPAIR")
public class Repair implements Serializable {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    /**
     * 故障设备名称
     */
    @NotEmpty
    @Column(name = "DEVICE_NAME", length = 30, nullable = false)
    private String deviceName;

    /**
     * 故障设备类型
     */
    @Column(name = "DEVICE_TYPE", columnDefinition = "tinyint", nullable = false)
    private int deviceType;

    /**
     * 故障描述
     */
    @Column(name = "DESCRIPTION", length = 200, nullable = false)
    private String description;

    /**
     * 申请人id
     */
    @NotEmpty
    @Column(name = "APPLICANT_ID", length = 10, nullable = false)
    private String applicantId;

    /**
     * 申请人名字
     */
    @NotEmpty
    @Column(name = "APPLICANT", length = 10, nullable = false)
    private String applicant;

    /**
     * 申请人手机号码
     */
    @NotEmpty
    @Column(name = "APPLICANT_MOBILE", length = 11, nullable = false)
    private String applicantMobile;

    /**
     * 申请人部门
     */
    @NotEmpty
    @Column(name = "APPLICANT_DEPART", length = 20, nullable = false)
    private String applicantDepart;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm")
    @Column(name = "CREATED_TIME", columnDefinition = "timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @UpdateTimestamp
    //@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATED_TIME", columnDefinition = "timestamp", nullable = false)
    private LocalDateTime updatedTime;

    /**
     * 服务评分
     */
    @Column(name = "SCORE", columnDefinition = "tinyint")
    private int score = 0;

    /**
     * 流程状态
     */
    @Enumerated
    @Column(name = "STATUS", columnDefinition = "tinyint", nullable = false)
    private ProcessStatus status = ProcessStatus.UNDER_AUDIT;

    // 流程任务
    @Transient
    private String taskId;

    //private Map<String, Object> variables;

    // 运行中的流程实例
    @Transient
    private String processInstanceId;

    // 历史的流程实例
    @Transient
    private HistoricProcessInstance historicProcessInstance;

    // 流程定义
    @Transient
    private ProcessDefinition processDefinition;

}
