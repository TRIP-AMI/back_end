package TripAmi.backend.web.api.member.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Getter;

@Getter
/**
 * 멤버 생성 DTO
 */
public class SignupMemberRequest {
    @NotEmpty
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;
    @NotEmpty
    @Size(min = 10, message = "비밀번호는 최소 10자 이상이어야 합니다.")
    private String password;

    public SignupMemberRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public SignupMemberRequest() {
    }
}
