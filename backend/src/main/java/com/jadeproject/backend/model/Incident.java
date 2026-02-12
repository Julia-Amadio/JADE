package com.jadeproject.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Um monitor possui vários incidentes
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    @JsonIgnore
    private Monitor monitor;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 20)
    private String severity;

    //columnDefinition = "TEXT" força o Hibernate a entender que é um texto longo, não um varchar(255) padrão
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
        //Regra de negócio: se não vier status, o padrão é OPEN
        if (this.status == null) {
            this.status = "OPEN";
        }
    }
}
