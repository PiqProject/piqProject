package piq.piqproject.common.list;

import java.util.List;

import lombok.Getter;

@Getter
public class ListResponseDto<T extends Listable> {
    private final int totalCount;
    private final List<T> list;

    private ListResponseDto(List<T> list) {
        this.list = list;
        this.totalCount = (list == null) ? 0 : list.size();
    }

    /**
     * List<T>를 받아서 ListResponseDto<T>로 변환합니다.
     * 단 T는 Listable 인터페이스를 구현해야 합니다.
     * 
     * @param list 변환할 리스트 (Listable을 구현한 객체들의 리스트)
     * @return 변환된 ListResponseDto 객체
     */
    public static <T extends Listable> ListResponseDto<T> from(List<T> list) {
        return new ListResponseDto<T>(list);
    }
}
