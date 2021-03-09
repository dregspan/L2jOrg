/*
 * Copyright © 2019-2021 L2JOrg
 *
 * This file is part of the L2JOrg project.
 *
 * L2JOrg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2JOrg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2j.gameserver.data.database.data;

import org.l2j.commons.database.annotation.Table;
import org.l2j.gameserver.model.Location;

/**
 * @author JoeAlisson
 */
@Table(value = "castle_siege_guards", autoGeneratedProperty = "id")
public class CastleSiegeGuardData {

    private int id;
    private int castleId;
    private int npcId;
    private int x;
    private int y;
    private int z;
    private int heading;
    private int respawnDelay;
    private int isHired;

    public int getNpcId() {
        return npcId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getHeading() {
        return heading;
    }

    public int getRespawnDelay() {
        return respawnDelay;
    }

    public static CastleSiegeGuardData of(int castleId, int npcId, Location location, int hired) {
        var data = new CastleSiegeGuardData();
        data.castleId = castleId;
        data.npcId = npcId;
        data.x = location.getX();
        data.y = location.getY();
        data.z = location.getZ();
        data.heading = location.getHeading();
        data.isHired = hired;
        return data;
    }
}
