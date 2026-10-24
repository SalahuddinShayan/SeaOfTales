package com.seaoftales.reaction.dto;

import com.seaoftales.reaction.enums.ReactionType;

import java.util.Map;

public record ReactionSummary(Map<ReactionType, Long> counts, long total, ReactionType currentUserReaction) {}
