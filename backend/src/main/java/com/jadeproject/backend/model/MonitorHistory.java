package com.jadeproject.backend.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "monitor_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MonitorHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    //Um monitor possui vários logs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false) //Mapeia a coluna FK 'monitor_id'
    @JsonIgnore
    @ToString.Exclude
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
    private OffsetDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        if (this.checkedAt == null) {
            //Garante UTC antes de salvar
            this.checkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
