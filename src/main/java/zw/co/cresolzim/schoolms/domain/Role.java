package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "app_role")
@Getter @Setter @NoArgsConstructor
public class Role extends BaseEntity {

    public static final String ADMIN     = "ROLE_ADMIN";
    public static final String HEAD      = "ROLE_HEAD";
    public static final String TEACHER   = "ROLE_TEACHER";
    public static final String GUARDIAN  = "ROLE_GUARDIAN";
    public static final String LIBRARIAN = "ROLE_LIBRARIAN";
    public static final String MATRON    = "ROLE_MATRON";

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(length = 120)
    private String description;

    public Role(String name, String description) {
        this.name = name; this.description = description;
    }
}
