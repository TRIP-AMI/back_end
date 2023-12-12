package TripAmi.backend.app.reservation.domain;

import TripAmi.backend.app.member.domain.Member;
import TripAmi.backend.app.product.domain.Program;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservation")
@Getter
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    Program program;

    @Column(nullable = false)
    LocalDateTime fixedDate;

    @Column(nullable = false)
    Integer fixedPeople;

    @ElementCollection
    @CollectionTable(name = "reservation_people", joinColumns = @JoinColumn(name = "reservation_id"))
    @Column(name = "authmember_id")
    List<Long> joinedPersonIds = new ArrayList<>();

    public Reservation(Program program, LocalDateTime fixedDate) {
        this.program = program;
        this.fixedDate = fixedDate;
        this.fixedPeople = 0;
    }

    @SneakyThrows
    public void joinPerson(Long personId) {
        if (this.fixedPeople < program.getTotalPersonnel())
            fixedPeople++;
        else
            throw new RuntimeException("정원 초과되었습니다.");

        joinedPersonIds.add(personId);
    }
}
