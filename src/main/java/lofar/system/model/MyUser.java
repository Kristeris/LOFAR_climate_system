package lofar.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Table(name = "MyUserTable")
@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class MyUser {

    @Column(name = "UId")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(value = AccessLevel.NONE)
    private long uId;

    @Size(min = 3, max = 20)
    @Pattern(regexp = "[a-z0-9]+")
    @Column(name = "Username", unique = true)
    @NotNull
    private String username;

    @NotNull
    @Column(name = "Password")
    private String password;

    @Email
    @Column(name = "Email", unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "AId")
    private MyAuthority authority;

    public MyUser(String username, String password, MyAuthority authority) {
        this.username = username;
        this.password = password;
        this.authority = authority;
    }

    public MyUser(String username, String password, String email, MyAuthority authority) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.authority = authority;
    }
}