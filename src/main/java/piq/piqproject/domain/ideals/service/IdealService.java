package piq.piqproject.domain.ideals.service;

import static piq.piqproject.common.error.exception.ErrorCode.ALREADY_EXISTS_IDEAL_CATEGORY;
import static piq.piqproject.common.error.exception.ErrorCode.ALREADY_EXISTS_IDEAL_OPTION;
import static piq.piqproject.common.error.exception.ErrorCode.DUPLICATE_IDEAL_OPTIONS;
import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_IDEAL_CATEGORY;
import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_IDEAL_OPTION;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.ideals.dto.request.CreateIdealOptionRequestDto;
import piq.piqproject.domain.ideals.dto.request.DeleteIdealOptionRequestDto;
import piq.piqproject.domain.ideals.dto.request.IdealRequestDto;
import piq.piqproject.domain.ideals.dto.response.IdealOptionResponseDto;
import piq.piqproject.domain.ideals.dto.response.IdealResponseDto;
import piq.piqproject.domain.ideals.entity.IdealCategoryEntity;
import piq.piqproject.domain.ideals.entity.IdealOptionEntity;
import piq.piqproject.domain.ideals.repository.IdealCategoryRepository;
import piq.piqproject.domain.ideals.repository.IdealOptionRepository;
import piq.piqproject.domain.users.repository.UserIdealRepository;

@Service
@RequiredArgsConstructor
public class IdealService {
    private final IdealCategoryRepository idealCategoryRepository;
    private final IdealOptionRepository idealOptionRepository;
    private final UserIdealRepository userIdealRepository;

    @Transactional
    public ListResponseDto<IdealResponseDto> createCategoriesWithOptions(List<IdealRequestDto> idealRequestDtos) {

        List<IdealResponseDto> idealResponseDtoList = new ArrayList<>();

        for (IdealRequestDto ideal : idealRequestDtos) {

            String category = ideal.getCategoryName();
            List<String> options = ideal.getOptions();

            // 1. 카테고리가 이미 존재하는지 확인
            if (idealCategoryRepository.existsByName(category)) {
                throw new ConflictException(ALREADY_EXISTS_IDEAL_CATEGORY);
            }

            // 2. 입력된 옵션들이 모두 다른지 확인
            validateOptionsAreUnique(options);

            // 3. 카테고리 엔티티 생성 및 저장
            IdealCategoryEntity categoryEntity = IdealCategoryEntity.of(category);
            idealCategoryRepository.save(categoryEntity);

            // 4. 옵션 엔티티들 생성 및 저장
            List<IdealOptionEntity> optionEntityList = options.stream()
                    .map(optionName -> IdealOptionEntity.of(categoryEntity, optionName))
                    .toList();
            idealOptionRepository.saveAll(optionEntityList);

            // 5. 생성된 엔티티를 응답 DTO로 변환하여 리스트에 추가
            List<IdealOptionResponseDto> idealOptions = optionEntityList.stream()
                    .map(IdealOptionResponseDto::of)
                    .toList();

            IdealResponseDto idealResponse = IdealResponseDto.of(categoryEntity, idealOptions);
            idealResponseDtoList.add(idealResponse);
        }

        return ListResponseDto.from(idealResponseDtoList);
    }

    @Transactional
    public IdealResponseDto addOptions(Long categoryId, CreateIdealOptionRequestDto createIdealOptionRequestDto) {
        List<String> options = createIdealOptionRequestDto.getOptions();

        // 1. 요청된 옵션들의 중복 여부 확인
        validateOptionsAreUnique(options);

        // 2. 카테고리 존재 여부 확인
        IdealCategoryEntity category = idealCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_IDEAL_CATEGORY));

        // 3. 옵션이 이미 존재하는 경우
        if (idealOptionRepository.existsByCategoryAndNameIn(category, options)) {
            throw new ConflictException(ALREADY_EXISTS_IDEAL_OPTION);
        }

        // 4. 옵션 엔티티 생성 및 저장
        List<IdealOptionEntity> idealoptionList = options.stream()
                .map(optionName -> IdealOptionEntity.of(category, optionName))
                .toList();
        idealOptionRepository.saveAll(idealoptionList);

        // 5. 응답 DTO로 변환
        List<IdealOptionResponseDto> idealOptionResponseDtos = idealoptionList.stream()
                .map(IdealOptionResponseDto::of)
                .toList();

        return IdealResponseDto.of(category, idealOptionResponseDtos);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<IdealResponseDto> getIdeals() {
        List<IdealCategoryEntity> idealCategoryList = idealCategoryRepository.findAll();

        List<IdealResponseDto> idealResponseDtos = idealCategoryList.stream()
                .map(category -> {
                    List<IdealOptionEntity> idealOptionList = idealOptionRepository
                            .findAllByCategoryId(category.getId());
                    List<IdealOptionResponseDto> idealOptionResponseDto = idealOptionList.stream()
                            .map(IdealOptionResponseDto::of).toList();
                    return IdealResponseDto.of(category, idealOptionResponseDto);
                })
                .toList();

        return ListResponseDto.from(idealResponseDtos);
    }

    @Transactional
    public void deleteOptions(DeleteIdealOptionRequestDto deleteIdealOptionResponseDto) {

        List<Long> optionIds = deleteIdealOptionResponseDto.getOptions();
        List<IdealOptionEntity> idealOptionList = idealOptionRepository.findAllById(optionIds);

        if (idealOptionList.size() != deleteIdealOptionResponseDto.getOptions().size()) {
            throw new NotFoundException(NOT_FOUND_IDEAL_OPTION);
        }

        idealOptionRepository.deleteAll(idealOptionList);
    }

    @Transactional
    public void deleteCategoey(Long categoryId) {
        IdealCategoryEntity idealCategory = idealCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_IDEAL_CATEGORY));

        idealCategoryRepository.delete(idealCategory);
    }

    /**
     * 하나의 카테고리 요청 내에서 옵션들의 중복 여부를 확인하는 메소드
     */
    private void validateOptionsAreUnique(List<String> options) {
        Set<String> uniqueOptions = new HashSet<>(options);

        if (uniqueOptions.size() != options.size()) {
            throw new ConflictException(DUPLICATE_IDEAL_OPTIONS);
        }
    }
}
