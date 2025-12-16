package piq.piqproject.domain.users.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserProfileUpdatedEvent {
    private final Long userId;
}