package piq.piqproject.domain.auth.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ApplePublicKeyResponse {
    private List<AppleKey> keys;

    @Getter
    @NoArgsConstructor
    public static class AppleKey {
        private String kty;
        private String kid;
        private String use;
        private String alg;
        private String n;
        private String e;
    }
}