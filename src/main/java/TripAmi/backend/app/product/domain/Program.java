package TripAmi.backend.app.product.domain;

import TripAmi.backend.app.member.domain.Ami;
import TripAmi.backend.app.product.ProgramTheme;
import TripAmi.backend.app.util.BaseEntity;
import TripAmi.backend.app.util.infra.StringListConverter;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("program")
@PrimaryKeyJoinColumn(name = "program_id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "program")
@Getter
public class Program extends Product {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ami_id")
    Ami ami;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    List<Spot> spots = new ArrayList<>();

    @Column(name = "total_people")
    Integer totalPeople;

    @Enumerated(value = EnumType.STRING)
    ProgramTheme theme;

    @Convert(converter = StringListConverter.class)
    List<String> keywords;

    //todo address class 작성
    String location;

    String subTitle;

    LocalDateTime startTime;

    @Embedded
    private BaseEntity baseEntity;

    @Builder
    public Program(String title, List<String> images, String content, Integer price, List<Spot> spots, Ami ami, Integer totalPeople, ProgramTheme theme, List<String> keywords, String location, String subTitle, LocalDateTime startTime) {
        super(title, images, content, price);
        this.spots.addAll(spots);
        this.totalPeople = totalPeople;
        this.theme = theme;
        this.keywords = keywords;
        this.location = location;
        this.baseEntity = new BaseEntity();
        this.subTitle = subTitle;
        this.startTime = startTime;
        this.setAmi(ami);
    }

    public void setAmi(Ami ami) {
        this.ami = ami;
        ami.addProgram(this);
    }
}
