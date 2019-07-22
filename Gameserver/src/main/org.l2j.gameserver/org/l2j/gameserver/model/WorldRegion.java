package org.l2j.gameserver.model;

import io.github.joealisson.primitive.CHashIntMap;
import io.github.joealisson.primitive.IntMap;
import org.l2j.commons.threading.ThreadPoolManager;
import org.l2j.gameserver.Config;
import org.l2j.gameserver.ai.CtrlIntention;
import org.l2j.gameserver.model.actor.Attackable;
import org.l2j.gameserver.model.actor.Npc;
import org.l2j.gameserver.model.actor.Vehicle;
import org.l2j.gameserver.util.GameUtils;
import org.l2j.gameserver.util.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.l2j.gameserver.util.GameUtils.*;

public final class WorldRegion {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRegion.class);

    private final int regionX;
    private final int regionY;
    /**
     * Map containing visible objects in this world region.
     */
    private final IntMap<WorldObject> objects = new CHashIntMap<>();
    /**
     * Map containing nearby regions forming this world region's effective area.
     */
    private WorldRegion[] surroundingRegions;
    private boolean active;

    private ScheduledFuture<?> neighborsTask = null;
    private final Object taskLocker = new Object();

    WorldRegion(int regionX, int regionY) {
        this.regionX = regionX;
        this.regionY = regionY;

        // Default a newly initialized region to inactive, unless always on is specified.
        active = Config.GRIDS_ALWAYS_ON;
    }

    private boolean areNeighborsEmpty() {
        return checkEachSurroundingRegion(w -> !(w.isActive() && w.objects.values().stream().anyMatch(GameUtils::isPlayable)));
    }

    /**
     * Add the WorldObject in the L2ObjectHashSet(WorldObject) objects containing WorldObject visible in this WorldRegion <BR>
     * If WorldObject is a Player, Add the Player in the L2ObjectHashSet(Player) _allPlayable containing Player of all player in game in this WorldRegion <BR>
     *
     * @param object to be add on region
     */
    public void addVisibleObject(WorldObject object) {
        if (isNull(object)) {
            return;
        }

        objects.put(object.getObjectId(), object);

        if (isPlayable(object)) {
            // If this is the first player to enter the region, activate self and neighbors.
            if (!active && (!Config.GRIDS_ALWAYS_ON)) {
                startActivation();
            }
        }
    }

    /**
     * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
     */
    private void startActivation() {
        // First set self to active and do self-tasks...
        setActive(true);

        startNeighborsTask(true, Config.GRID_NEIGHBOR_TURNON_TIME);
    }

    /**
     * this function turns this region's AI and geodata on or off
     *
     * @param value
     */
    private void setActive(boolean value) {
        if (active == value) {
            return;
        }

        active = value;

        // Turn the AI on or off to match the region's activation.
        switchAI(value);

        LOGGER.debug("{} Grid {}", (value ? "Starting" : "Stopping"), this);
    }

    private void switchAI(boolean isOn) {
        if (objects.isEmpty()) {
            return;
        }

        int c = 0;
        if (!isOn) {
            for (WorldObject o : objects.values()) {
                if (isAttackable(o)) {
                    c++;
                    final Attackable mob = (Attackable) o;

                    // Set target to null and cancel attack or cast.
                    mob.setTarget(null);

                    // Stop movement.
                    mob.stopMove(null);

                    // Stop all active skills effects in progress on the Creature.
                    mob.stopAllEffects();

                    mob.clearAggroList();
                    mob.getAttackByList().clear();

                    // Stop the AI tasks.
                    if (mob.hasAI()) {
                        mob.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                        mob.getAI().stopAITask();
                    }
                } else if (o instanceof Vehicle) {
                    c++;
                }
            }
            LOGGER.debug("{} mobs were turned off", c);
        } else {
            for (WorldObject o : objects.values()) {
                if (isAttackable(o)) {
                    c++;
                    // Start HP/MP/CP regeneration task.
                    ((Attackable) o).getStatus().startHpMpRegeneration();
                } else if (isNpc(o)) {
                    ((Npc) o).startRandomAnimationTask();
                }
            }
            LOGGER.debug("{} mobs were turned on", c);
        }
    }

    private void startNeighborsTask(boolean activating, int taskStartTime) {
        synchronized (taskLocker) {
            if (nonNull(neighborsTask)) {
                neighborsTask.cancel(true);
                neighborsTask = null;
            }

            // Then, set a timer to activate the neighbors.
            neighborsTask = ThreadPoolManager.schedule(new NeighborsTask(activating), 1000 * taskStartTime);
        }
    }

    /**
     * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
     */
    private void startDeactivation() {
        startNeighborsTask(false, Config.GRID_NEIGHBOR_TURNOFF_TIME);
    }

    /**
     * Remove the WorldObject from the L2ObjectHashSet(WorldObject) objects in this WorldRegion. If WorldObject is a Player, remove it from the L2ObjectHashSet(Player) _allPlayable of this WorldRegion <BR>
     *
     * @param object
     */
    void removeVisibleObject(WorldObject object) {
        if (isNull(object) || objects.isEmpty()) {
            return;
        }

        objects.remove(object.getObjectId());

        if (isPlayable(object)) {
            if (areNeighborsEmpty() && !Config.GRIDS_ALWAYS_ON) {
                startDeactivation();
            }
        }
    }

    boolean checkEachSurroundingRegion(Predicate<WorldRegion> p) {
        for (WorldRegion worldRegion : surroundingRegions) {
            if (!p.test(worldRegion)) {
                return false;
            }
        }
        return true;
    }

    void forEachSurroundingRegion(Consumer<WorldRegion> action) {
        Arrays.stream(surroundingRegions).forEach(action);
    }

    <T extends WorldObject> void forEachObject(Class<T> clazz, Consumer<T> action, Predicate<T> filter) {
        regionToWorldObjectStream(this).filter(applyInstanceFilter(clazz, filter)).map(clazz::cast).forEach(action);
    }

    <T extends WorldObject> void forEachObjectInSurrounding(Class<T> clazz, Consumer<T> action, Predicate<T> filter) {
        filteredParallelSurroundingObjects(clazz, filter).forEach(action);
    }

    private <T extends WorldObject> Stream<T> filteredParallelSurroundingObjects(Class<T> clazz, Predicate<T> filter) {
        return Arrays.stream(surroundingRegions)
                .flatMap(WorldRegion::regionToWorldObjectStream)
                .filter(applyInstanceFilter(clazz, filter)).map(clazz::cast);
    }

    <T extends WorldObject> void forEachObjectInSurroundingLimiting(Class<T> clazz, int limit, Predicate<T> filter, Consumer<T> action) {
        filteredParallelSurroundingObjects(clazz, filter).limit(limit).forEach(action);
    }

    <T extends WorldObject> void forEachOrderedObjectInSurrounding(Class<T> clazz, int maxObjects, Comparator<T> comparator, Predicate<T> filter, Consumer<? super T> action) {
        filteredSurroundingObjects(clazz, filter).sorted(comparator).limit(maxObjects).forEach(action);
    }

    private <T extends WorldObject> Stream<T> filteredSurroundingObjects(Class<T> clazz, Predicate<T> filter) {
        return Arrays.stream(surroundingRegions).flatMap(r -> r.objects.values().stream()).filter(applyInstanceFilter(clazz, filter)).map(clazz::cast);
    }

    <T extends WorldObject> void forAnyObjectInSurrounding(Class<T> clazz, Consumer<T> action, Predicate<T> filter) {
        filteredParallelSurroundingObjects(clazz, filter).findAny().ifPresent(action);
    }

    WorldObject findObjectInSurrounding(WorldObject reference, int objectId, int range) {
        WorldObject object;
        for(var region : surroundingRegions) {
            if( nonNull(object = region.getObject(objectId)) ) {
                if(!MathUtil.isInsideRadius3D(reference, object, range)) {
                    return null;
                }
                return object;
            }
        }
        return null;
    }

    <T extends WorldObject> List<T> findAllObjectsInSurrounding(Class<T> clazz, Predicate<T> filter) {
        return filteredSurroundingObjects(clazz, filter).collect(Collectors.toList());
    }

    <T extends WorldObject> T findAnyObjectInSurrounding(Class<T> clazz, Predicate<T> filter) {
        return  filteredParallelSurroundingObjects(clazz, filter).findAny().orElse(null);
    }

    <T extends WorldObject> T findFirstObjectInSurrounding(Class<T> clazz, Predicate<T> filter, Comparator<T> comparator) {
        return filteredSurroundingObjects(clazz, filter).min(comparator).orElse(null);
    }

    <T extends WorldObject> boolean hasObjectInSurrounding(Class<T> clazz, Predicate<T> filter) {
        return Arrays.stream(surroundingRegions).flatMap(WorldRegion::regionToWorldObjectStream).anyMatch(applyInstanceFilter(clazz, filter));
    }

    <T extends WorldObject> long countObjectInSurrounding(Class<T> clazz, Predicate<T> filter) {
        return filteredParallelSurroundingObjects(clazz, filter).count();
    }

    WorldObject getObject(int objectId) {
        return objects.get(objectId);
    }

    void setSurroundingRegions(WorldRegion[] regions) {
        surroundingRegions = regions;
    }

    boolean isSurroundingRegion(WorldRegion region) {
        return nonNull(region) && regionX >= region.regionX - 1 && regionX <= (region.regionX + 1) && regionY >= (region.regionY - 1) && regionY <= (region.regionY + 1);
    }

    public boolean isActive() {
        return active;
    }


    private static <T extends WorldObject> Predicate<? super WorldObject> applyInstanceFilter(Class<T> clazz, Predicate<T> filter) {
        return object -> clazz.isInstance(object) && filter.test(clazz.cast(object));
    }

    private static Stream<? extends WorldObject> regionToWorldObjectStream(WorldRegion region) {
        return region.objects.values().parallelStream().unordered();
    }

    @Override
    public String toString() {
        return "(" + regionX + ", " + regionY + ")";
    }

    /**
     * Task of AI notification
     */
    private class NeighborsTask implements Runnable {
        private final boolean isActivating;

        NeighborsTask(boolean isActivating) {
            this.isActivating = isActivating;
        }

        @Override
        public void run() {
            forEachSurroundingRegion(w ->
            {
                if (isActivating || w.areNeighborsEmpty()) {
                    w.setActive(isActivating);
                }
            });
        }
    }
}
