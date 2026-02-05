package com.jadeproject.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "monitor_history")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class MonitorHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Um monitor possui vários logs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false) //Mapeia a coluna FK 'monitor_id'
    @JsonIgnore
    private Monitor monitor;

    //Não temos 'nullable = false' pois um TimeOut, por exemplo, não retorna status code
    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "latency_ms")
    private Integer latency;

    //Sem '= true' ou '=false'. O serviço de ping deve ser OBRIGADO a dizer se foi sucesso ou falha.
    //Esquecendo de setar, ele salva NULL (o que indica erro de código, porém é melhor que falso positivo).
    @Column(name = "is_successful")
    private Boolean isSuccessful;

    @Column(name = "checked_at", updatable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        if (this.checkedAt == null) {
            this.checkedAt = LocalDateTime.now();
        }
    }
}