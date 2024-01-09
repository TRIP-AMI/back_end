package TripAmi.backend.app.review.domain;

import TripAmi.backend.app.order.domain.Order;
import TripAmi.backend.app.util.BaseEntity;
import TripAmi.backend.app.util.Star;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review")
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Review {
    @Id
    @Column(name = "review_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private Star star;

    private String content;

    @Embedded
    private BaseEntity baseEntity;

    @Builder
    public Review(Order order, Star star, String content) {
        this.order = order;
        this.star = star;
        this.content = content;
    }
}
