package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.HelpArticleCategory;

@Getter
@Setter
@ToString
public class HelpArticleDto {
    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;
    private HelpArticleCategory category;
    private Integer sortOrder;
}
