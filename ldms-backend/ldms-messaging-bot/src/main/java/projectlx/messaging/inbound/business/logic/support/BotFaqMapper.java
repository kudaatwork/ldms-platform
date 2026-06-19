package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.model.BotFaq;
import projectlx.messaging.inbound.utils.dtos.BotFaqDto;
import projectlx.messaging.inbound.utils.enums.BotFaqCategory;

public final class BotFaqMapper {

    private BotFaqMapper() {
    }

    public static BotFaqDto toDto(BotFaq entity) {
        if (entity == null) {
            return null;
        }
        BotFaqDto dto = new BotFaqDto();
        dto.setId(entity.getId());
        dto.setQuestion(entity.getQuestion());
        dto.setAnswer(entity.getAnswer());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().name() : BotFaqCategory.GENERAL.name());
        dto.setKeywords(entity.getKeywords());
        dto.setPublished(entity.isPublished());
        dto.setUseCount(entity.getUseCount());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedAt(entity.getModifiedAt());
        return dto;
    }

    public static BotFaqCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return BotFaqCategory.GENERAL;
        }
        try {
            return BotFaqCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return BotFaqCategory.GENERAL;
        }
    }
}
