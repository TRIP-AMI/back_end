package TripAmi.backend.auth.authmember.service;

import TripAmi.backend.app.member.service.MemberService;
import TripAmi.backend.auth.authmember.domain.*;
import TripAmi.backend.auth.authmember.service.dto.PasswordAuth;
import TripAmi.backend.auth.authmember.service.dto.PasswordPatternChecker;
import TripAmi.backend.auth.authmember.service.dto.RefreshTokenAuth;
import TripAmi.backend.auth.authmember.service.exception.AuthMemberNotFoundException;
import TripAmi.backend.auth.authmember.service.exception.ConfirmNotFoundException;
import TripAmi.backend.auth.jwt.service.JwtIssuer;
import TripAmi.backend.web.api.member.request.SignupMemberRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static TripAmi.backend.auth.authmember.service.dto.ConfirmTokenDto.*;


/**
 * todo 패키지간 의존성 리팩터링 인증 서비스와 애플리케이션을 분리할 필요가 있음.
 * <p>
 * 미봉책으로 AuthMember에 yourFoodId를 저장하여 해결했으나, 바람직하지 못해보임.
 */
@Service
@RequiredArgsConstructor
public class AuthMemberServiceImpl implements AuthMemberService {

    private final JwtIssuer jwtIssuer;

    private final AuthMemberRepository authMemberRepository;
    private final ConfirmRepository confirmRepository;
    private final PasswordHasher passwordHasher;
    private final TokenSender tokenSender;
    private final TransactionTemplate transactionTemplate;
    private final PasswordPatternChecker passwordPatternChecker;
    private final MemberService memberService;

    @Override
    public UUID join(SignupMemberRequest request) {
        String password = request.getPassword();

        // 비밀번호 유효성 검사
        passwordPatternChecker.checkPasswordPattern(password);

        String token = tokenSender.generateToken();
        Long memberId = transactionTemplate.execute(joinTransaction(request, token));

        tokenSender.sendEmail(request.getEmail(), token);

        return memberId;
    }

    private TransactionCallback<UUID> joinTransaction(SignupMemberRequest request, String token) {

        return transactionStatus -> {
            AuthMember authMember = AuthMember.builder()
                                        .username(request.getUsername())
                                        .raw(request.getPassword())
                                        .hasher(passwordHasher)
                                        .build();
            authMemberRepository.save(authMember);
            Confirm confirm = Confirm.builder()
                                  .authMember(authMember)
                                  .token(token)
                                  .build();
            confirmRepository.save(confirm);

            return memberService.createMember(request.getUsername());
        };
    }

    @Override
    @Transactional
    public void joinConfirm(ConfirmTokenRequest request) {
        AuthMember authMember = authMemberRepository.findByUsername(request.getUsername())
                                    .orElseThrow(AuthMemberNotFoundException::new);
        Confirm confirm = confirmRepository.findByAuthMember(authMember).orElseThrow(
            ConfirmNotFoundException::new);
        authMember.verifyPassword(request.getPassword(), passwordHasher);
        confirm.verifyToken(request.getToken());
        authMember.joinConfirm();
    }

    // todo your food member 의 ID와 auth member id 모두 기입
    @Override
    public AuthToken authenticate(PasswordAuth passwordAuth) {
        AuthMember authMember = authMemberRepository.findByUsername(passwordAuth.username())
                                    .orElseThrow(AuthMemberNotFoundException::new);

        authMember.verifyPassword(passwordAuth.password(), passwordHasher);
        authMember.verifyConfirmState();

        return jwtIssuer.issue(authMember.getYourFoodId(), authMember.getUsername());
    }

    @Override
    public AuthToken authenticate(RefreshTokenAuth refreshTokenAuth) {
        return jwtIssuer.renew(refreshTokenAuth.refreshToken(), refreshTokenAuth.username());
    }
}