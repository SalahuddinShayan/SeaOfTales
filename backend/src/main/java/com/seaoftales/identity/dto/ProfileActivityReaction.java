package com.seaoftales.identity.dto;

import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.enums.ReactionType;

public record ProfileActivityReaction(Long id, ReactionType reactionType, ReactionTargetType targetType,
                                      Long targetId, Long novelId, String novelTitle,
                                      String chapterNumber, String chapterTitle) { }
