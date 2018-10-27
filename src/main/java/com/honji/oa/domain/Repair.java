package com.honji.oa.domain;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
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
    @Column(name = "DEVICE_NAME", length = 30, nullable = false)
    private String deviceName;

    /**
     * 故障描述
     */
    @Column(name = "DESCRIPTION", length = 200, nullable = false)
    private String description;

    /**
     * 申请人id
     */
    @Column(name = "APPLICANT_ID", length = 20, nullable = false)
    private String applicantId;

    /**
     * 申请人名字
     */
    @Column(name = "APPLICANT", length = 10, nullable = false)
    private String applicant;

    /**
     * 创建时间
     */
    @CreationTimestamp
    //@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_TIME", columnDefinition = "timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @UpdateTimestamp
    //@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATED_TIME", columnDefinition = "timestamp", nullable = false)
    private LocalDateTime updatedTime;

}
