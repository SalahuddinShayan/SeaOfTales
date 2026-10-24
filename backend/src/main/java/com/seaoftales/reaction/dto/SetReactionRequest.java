package com.seaoftales.reaction.dto;

import com.seaoftales.reaction.enums.ReactionType;
import jakarta.validation.constraints.NotNull;

public record SetReactionRequest(@NotNull ReactionType reactionType) {}
