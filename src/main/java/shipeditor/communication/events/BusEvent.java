package shipeditor.communication.events;

/**
 * Marker interface for all event messages published via {@link shipeditor.communication.EventBus}.
 * <p>
 * Implementations are decoupled event DTOs (Data Transfer Objects) passed between components, 
 * UI panels, and background tasks.
 */
@SuppressWarnings("MarkerInterface")
public interface BusEvent {
}
