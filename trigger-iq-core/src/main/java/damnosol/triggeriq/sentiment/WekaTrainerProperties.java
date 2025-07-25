package damnosol.triggeriq.sentiment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trainer.weka")
public class WekaTrainerProperties {
    private int maxWords = 1000;
    private boolean tfTransform = true;
    private boolean idfTransform = true;

    // Getters and Setters
    public int getMaxWords() {
        return maxWords;
    }

    public void setMaxWords(int maxWords) {
        this.maxWords = maxWords;
    }

    public boolean isTfTransform() {
        return tfTransform;
    }

    public void setTfTransform(boolean tfTransform) {
        this.tfTransform = tfTransform;
    }

    public boolean isIdfTransform() {
        return idfTransform;
    }

    public void setIdfTransform(boolean idfTransform) {
        this.idfTransform = idfTransform;
    }
}