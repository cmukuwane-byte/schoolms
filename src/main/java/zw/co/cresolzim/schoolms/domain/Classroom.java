package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

/** A physical room. Timetabling checks this for double bookings. */
@Entity @Table(name = "classroom")
@Getter @Setter @NoArgsConstructor
public class Classroom extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 60)
    private String block;

    @Column(nullable = false)
    private Integer capacity = 40;

    /** CLASSROOM, LABORATORY, WORKSHOP, COMPUTER_LAB, HALL, LIBRARY */
    @Column(name = "room_type", nullable = false, length = 20)
    private String roomType = "CLASSROOM";

    @Column(nullable = false)
    private boolean active = true;

    @Override public String toString() { return code + " - " + name; }
}
