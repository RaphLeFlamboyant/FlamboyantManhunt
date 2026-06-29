package me.flamboyant.manhunt.domain.role.ability;

public class CheckpointStorage {
    private Checkpoint checkpoint;

    public void saveCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public boolean hasCheckpoint() {
        return checkpoint != null;
    }
}
