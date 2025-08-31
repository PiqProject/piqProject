// package piq.piqproject.domain.users.controller;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import
// org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;

// import piq.piqproject.domain.users.dto.SignUpRequestDto;
// import piq.piqproject.domain.users.entity.Gender;
// import piq.piqproject.domain.users.repository.UserRepository;

// import static org.assertj.core.api.Assertions.assertThat;
// import static
// org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static
// org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest // 실제 애플리케이션의 모든 Bean을 로드하는 통합 테스트
// @AutoConfigureMockMvc // 통합 테스트 환경에서 MockMvc를 사용하기 위한 설정
// @Transactional // 각 테스트가 끝난 후 DB를 롤백하여 테스트 간 독립성 보장
// public class UserControllerIntegrationTest {

// @Autowired
// private MockMvc mockMvc;

// @Autowired
// private ObjectMapper objectMapper;

// // 통합 테스트이므로 실제 UserRepository를 주입받습니다.
// @Autowired
// private UserRepository userRepository;

// }