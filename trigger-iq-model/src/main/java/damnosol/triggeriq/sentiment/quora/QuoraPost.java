package damnosol.triggeriq.sentiment.quora;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class QuoraPost {
    private String title;
    private String link;

    public QuoraPost(String title, String link) {
        this.title = title;
        this.link = link;
    }

}