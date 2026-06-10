package shipeditor.parsing.loading;

public abstract class DataLoadingAction {

    /**
     * @return code that is expected to publish the results of loading process.
     */
    public abstract Runnable perform();

    /**
     * @return a human-readable name of the task for UI progress display.
     */
    public abstract String getTaskName();

}
