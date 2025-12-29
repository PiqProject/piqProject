package piq.piqproject.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class CoordinateDto {
    private Double latitude; // 위도 (y)
    private Double longitude; // 경도 (x)
}