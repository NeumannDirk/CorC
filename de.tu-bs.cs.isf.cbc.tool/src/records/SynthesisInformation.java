package records;

public class SynthesisInformation {
    private String[] mutableVariables;
    private Boolean isLoopVariantUpdate;

    public SynthesisInformation(String[] mutableVariables, Boolean isLoopVariantUpdate) {
        this.mutableVariables = mutableVariables;
        this.isLoopVariantUpdate = isLoopVariantUpdate;
    }

    public String[] getMutableVariables() {
        return mutableVariables;
    }

    public Boolean getIsLoopVariantUpdate() {
        return isLoopVariantUpdate;
    }
}