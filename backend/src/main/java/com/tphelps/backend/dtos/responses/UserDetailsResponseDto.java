package com.tphelps.backend.dtos.responses;

import com.tphelps.backend.dtos.notes.UserNoteDto;

import java.util.List;

public record UserDetailsResponseDto(String email, String username, List<UserNoteDto> userNotesDto) {
}
