package com.kshrd.kroya_api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kshrd.kroya_api.enums.PaymentType;
import com.kshrd.kroya_api.enums.PurchaseStatusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "purchase_tb")
public class PurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_date",columnDefinition = "timestamp default now()")
    private LocalDateTime createdDate;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_status_type")
    private PurchaseStatusType purchaseStatusType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_price")
    private Double totalPrice;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity buyer;

    @ManyToOne
    @JoinColumn(name = "food_sell_id")
    private FoodSellEntity foodSell;

}
