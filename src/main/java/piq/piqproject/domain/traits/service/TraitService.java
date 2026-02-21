package piq.piqproject.domain.traits.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.traits.dto.request.CreateTraitOptionRequestDto;
import piq.piqproject.domain.traits.dto.request.DeleteTraitOptionRequestDto;
import piq.piqproject.domain.traits.dto.request.TraitRequestDto;
import piq.piqproject.domain.traits.dto.response.TraitOptionResponseDto;
import piq.piqproject.domain.traits.dto.response.TraitResponseDto;
import piq.piqproject.domain.traits.entity.TraitCategoryEntity;
import piq.piqproject.domain.traits.entity.TraitOptionEntity;
import piq.piqproject.domain.traits.repository.TraitCategoryRepository;
import piq.piqproject.domain.traits.repository.TraitOptionRepository;

@Service
@RequiredArgsConstructor
public class TraitService {
    private final TraitCategoryRepository traitCategoryRepository;
    private final TraitOptionRepository traitOptionRepository;

    @Transactional
    public TraitResponseDto createCategorieWithOptions(TraitRequestDto TraitRequestDtos) {

        String category = TraitRequestDtos.getCategoryName();
        List<String> options = TraitRequestDtos.getOptionNames();

        // 1. 카테고리가 이미 존재하는지 확인
        if (traitCategoryRepository.existsByName(category)) {
            throw new ConflictException(ErrorCode.ALREADY_EXISTS_TRAIT_CATEGORY);
        }

        // 2. 입력된 옵션들이 모두 다른지 확인
        validateOptionsAreUnique(options);

        // 3. 카테고리 엔티티 생성 및 저장
        TraitCategoryEntity categoryEntity = TraitCategoryEntity.of(category);
        traitCategoryRepository.save(categoryEntity);

        // 4. 옵션 엔티티들 생성 및 저장
        List<TraitOptionEntity> optionEntityList = options.stream()
                .map(optionName -> TraitOptionEntity.of(categoryEntity, optionName))
                .toList();
        traitOptionRepository.saveAll(optionEntityList);

        // 5. 생성된 엔티티를 응답 DTO로 변환하여 리스트에 추가
        List<TraitOptionResponseDto> TraitOptions = optionEntityList.stream()
                .map(TraitOptionResponseDto::of)
                .toList();

        TraitResponseDto TraitResponse = TraitResponseDto.of(categoryEntity, TraitOptions);

        return TraitResponse;
    }

    @Transactional
    public TraitResponseDto addOptions(Long categoryId, CreateTraitOptionRequestDto createTraitOptionRequestDto) {
        List<String> options = createTraitOptionRequestDto.getOptions();

        // 1. 요청된 옵션들의 중복 여부 확인
        validateOptionsAreUnique(options);

        // 2. 카테고리 존재 여부 확인
        TraitCategoryEntity category = traitCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_TRAIT_CATEGORY));

        // 3. 옵션이 이미 존재하는 경우
        if (traitOptionRepository.existsByCategoryAndNameIn(category, options)) {
            throw new ConflictException(ErrorCode.ALREADY_EXISTS_TRAIT_OPTION);
        }

        // 4. 옵션 엔티티 생성 및 저장
        List<TraitOptionEntity> traitOptionList = options.stream()
                .map(optionName -> TraitOptionEntity.of(category, optionName))
                .toList();
        traitOptionRepository.saveAll(traitOptionList);

        // 5. 응답 DTO로 변환
        List<TraitOptionResponseDto> traitOptionResponseDtos = traitOptionList.stream()
                .map(TraitOptionResponseDto::of)
                .toList();

        return TraitResponseDto.of(category, traitOptionResponseDtos);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<TraitResponseDto> getTraits() {
        List<TraitCategoryEntity> traitCategoryList = traitCategoryRepository.findAll();

        List<TraitResponseDto> traitResponseDtos = traitCategoryList.stream()
                .map(category -> {
                    List<TraitOptionEntity> traitOptionList = traitOptionRepository
                            .findAllByCategoryId(category.getId());
                    List<TraitOptionResponseDto> traitOptionResponseDto = traitOptionList.stream()
                            .map(TraitOptionResponseDto::of).toList();
                    return TraitResponseDto.of(category, traitOptionResponseDto);
                })
                .toList();

        return ListResponseDto.from(traitResponseDtos);
    }

    @Transactional
    public void deleteOption(DeleteTraitOptionRequestDto deleteTraitOptionResponseDto) {

        Long optionId = deleteTraitOptionResponseDto.getOption();
        TraitOptionEntity traitOption = traitOptionRepository.findById(optionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_TRAIT_OPTION, "there is no trait option"));

        traitOptionRepository.delete(traitOption);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        TraitCategoryEntity traitCategory = traitCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_TRAIT_CATEGORY));

        traitCategoryRepository.delete(traitCategory);
    }

    /**
     * 하나의 카테고리 요청 내에서 옵션들의 중복 여부를 확인하는 메소드
     */
    private void validateOptionsAreUnique(List<String> options) {
        Set<String> uniqueOptions = new HashSet<>(options);

        if (uniqueOptions.size() != options.size()) {
            throw new ConflictException(ErrorCode.DUPLICATE_TRAIT_OPTIONS);
        }
    }
}
