package piq.piqproject.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFilterUtil {
    // 1. 휴대폰 번호 (010-1234-5678, 010 1234 5678, 공일공..., O1O...)
    // 설명: 01[016789] 뒤에 공백/./- 등이 섞이고 숫자가 나오는 패턴
    // (?i) 옵션은 필요 없으나 명시적으로 숫자와 유사한 문자(O, o) 등도 고려 가능
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?i)(01[016789]|공일[공영육칠팔구]|O1O|o1o|olo|OlO|0l0)[\\s\\-.]*[0-9영일이삼사오육칠팔구]{3,4}[\\s\\-.]*[0-9영일이삼사오육칠팔구]{4}");

    // 2. 이메일 (표준 패턴)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");

    // 3. SNS 아이디 (카톡, 인스타, 라인 등)
    // 설명: 키워드(카톡 등)가 나오고 -> (공백/특수문자/한글 조사)가 나오고 -> (영어/숫자/._ 아이디)가 나옴
    // (?i): 대소문자 무시
    private static final Pattern SNS_PATTERN = Pattern.compile(
            "(?i)(kakao|talk|insta|gram|line|telegram|카톡|카카오|인스타|아이디|ID|톡|라인|텔레)" // 키워드
                    + "[\\s\\-:=가-힣]*" // 구분자 (공백, 특수문자, '아이디는' 같은 한글 허용)
                    + "([a-zA-Z0-9._]{4,})" // 실제 아이디 (최소 4자 이상이어야 오탐 줄임)
    );

    /**
     * 메시지 검열 메서드
     * Receiver가 조회할 때 호출됨 (Sender는 원본을 봄)
     */
    public static String censorMessage(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String censored = message;

        // 휴대폰 번호 마스킹
        Matcher phoneMatcher = PHONE_PATTERN.matcher(censored);
        if (phoneMatcher.find()) {
            censored = phoneMatcher.replaceAll("[검열]");
        }

        // 이메일 마스킹
        Matcher emailMatcher = EMAIL_PATTERN.matcher(censored);
        if (emailMatcher.find()) {
            censored = emailMatcher.replaceAll("[검열]");
        }

        // SNS 아이디 마스킹
        Matcher snsMatcher = SNS_PATTERN.matcher(censored);
        if (snsMatcher.find()) {
            // 전체를 다 가려버림 (키워드 + 아이디)
            // 부분만 가리면 "카톡: [금지]" 이렇게 돼서 뭘 썼는지 티가 남
            censored = snsMatcher.replaceAll("[검열]");
        }

        return censored;
    }
}
