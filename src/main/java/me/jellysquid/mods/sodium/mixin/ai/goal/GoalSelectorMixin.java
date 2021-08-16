package me.jellysquid.mods.sodium.mixin.ai.goal;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.profiler.IProfiler;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {
    private static final Goal.Flag[] CONTROLS = Goal.Flag.values();

    @Shadow
    @Final
    private Supplier<IProfiler> profiler;

    @Mutable
    @Shadow
    @Final
    private Set<PrioritizedGoal> availableGoals;

    @Shadow
    @Final
    private EnumSet<Goal.Flag> disabledFlags;

    @Shadow
    @Final
    private Map<Goal.Flag, PrioritizedGoal> lockedFlags;

    /**
     * Replace the goal set with an optimized collection type which performs better for iteration.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void reinit(Supplier<Profiler> supplier, CallbackInfo ci) {
        this.availableGoals = new ObjectLinkedOpenHashSet<>(this.availableGoals);
    }

    /**
     * Avoid the usage of streams entirely to squeeze out additional performance.
     *
     * @reason Remove lambdas and complex stream logic
     * @author JellySquid
     */
    @Overwrite
    public void tick() {
        this.updateGoalStates();
        this.tickGoals();
    }

    /**
     * Checks the state of all goals for the given entity, starting and stopping them as necessary (because a goal
     * has been disabled, the controls are no longer available or have been reassigned, etc.)
     */
    private void updateGoalStates() {
        this.profiler.get().push("goalUpdate");

        // Stop any goals which are disabled or shouldn't continue executing
        this.stopGoals();

        // Update the controls
        this.cleanupControls();

        // Try to start new goals where possible
        this.startGoals();

        this.profiler.get().pop();
    }

    /**
     * Attempts to stop all goals which are running and either shouldn't continue or no longer have available controls.
     */
    private void stopGoals() {
        for (PrioritizedGoal goal : this.availableGoals) {
            // Filter out goals which are not running
            if (!goal.isRunning()) {
                continue;
            }

            // If the goal shouldn't continue or any of its controls have been disabled, then stop the goal
            if (!goal.canContinueToUse() || this.areControlsDisabled(goal)) {
                goal.stop();
            }
        }
    }

    /**
     * Performs a scan over all currently held controls and releases them if their associated goal is stopped.
     */
    private void cleanupControls() {
        for (Goal.Flag control : CONTROLS) {
            PrioritizedGoal goal = this.lockedFlags.get(control);

            // If the control has been acquired by a goal, check if the goal should still be running
            // If the goal should not be running anymore, release the control held by it
            if (goal != null && !goal.isRunning()) {
                this.lockedFlags.remove(control);
            }
        }
    }

    /**
     * Attempts to start all goals which are not-already running, can be started, and have their controls available.
     */
    private void startGoals() {
        for (PrioritizedGoal goal : this.availableGoals) {
            // Filter out goals which are already running or can't be started
            if (goal.isRunning() || !goal.canUse()) {
                continue;
            }

            // Check if the goal's controls are available or can be replaced
            if (!this.areGoalControlsAvailable(goal)) {
                continue;
            }

            // Hand over controls to this goal and stop any goals which depended on those controls
            for (Goal.Flag control : goal.getFlags()) {
                PrioritizedGoal otherGoal = this.getGoalOccupyingControl(control);

                if (otherGoal != null) {
                    otherGoal.stop();
                }

                this.setGoalOccupyingControl(control, goal);
            }

            goal.start();
        }
    }

    /**
     * Ticks all running AI goals.
     */
    private void tickGoals() {
        this.profiler.get().push("goalTick");

        // Tick all currently running goals
        for (PrioritizedGoal goal : this.availableGoals) {
            if (goal.isRunning()) {
                goal.tick();
            }
        }

        this.profiler.get().pop();
    }

    /**
     * Returns true if any controls of the specified goal are disabled.
     */
    private boolean areControlsDisabled(PrioritizedGoal goal) {
        for (Goal.Flag control : goal.getFlags()) {
            if (this.isControlDisabled(control)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if all controls for the specified goal are either available (not acquired by another goal) or replaceable
     * (acquired by another goal, but eligible for replacement) and not disabled for the entity.
     */
    private boolean areGoalControlsAvailable(PrioritizedGoal goal) {
        for (Goal.Flag control : goal.getFlags()) {
            if (this.isControlDisabled(control)) {
                return false;
            }

            PrioritizedGoal occupied = this.getGoalOccupyingControl(control);

            if (occupied != null && !occupied.canBeReplacedBy(goal)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the specified control is disabled.
     */
    private boolean isControlDisabled(Goal.Flag control) {
        return this.disabledFlags.contains(control);
    }

    /**
     * Returns the goal which is currently holding the specified control, or null if no goal is.
     */
    private PrioritizedGoal getGoalOccupyingControl(Goal.Flag control) {
        return this.lockedFlags.get(control);
    }

    /**
     * Changes the goal which is currently holding onto a control.
     */
    private void setGoalOccupyingControl(Goal.Flag control, PrioritizedGoal goal) {
        this.lockedFlags.put(control, goal);
    }

}
