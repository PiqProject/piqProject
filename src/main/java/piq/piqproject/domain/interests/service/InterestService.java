package piq.piqproject.domain.interests.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.interests.dto.request.InterestRequestDto;
import piq.piqproject.domain.interests.dto.response.InterestResponseDto;
import piq.piqproject.domain.interests.entity.InterestEntity;
import piq.piqproject.domain.interests.repository.InterestRepository;

import static piq.piqproject.common.error.exception.ErrorCode.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestService {

    private final InterestRepository interestRepository;

    @Transactional
    public InterestResponseDto createInterest(InterestRequestDto interestRequestDto) {
        if (interestRepository.existsByKeyword(interestRequestDto.getKeyword())) {
            throw new ConflictException(ALREADY_EXISTS_INTEREST);
        }

        InterestEntity interest = InterestEntity.of(interestRequestDto.getKeyword());        
        interestRepository.save(interest);

        return InterestResponseDto.of(interest);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<InterestResponseDto> getInterests() {
        List<InterestEntity> interests = interestRepository.findAll();

        List<InterestResponseDto> interestList = interests.stream()
                .map(InterestResponseDto::of)
                .toList();

        return ListResponseDto.from(interestList);
    }

}
