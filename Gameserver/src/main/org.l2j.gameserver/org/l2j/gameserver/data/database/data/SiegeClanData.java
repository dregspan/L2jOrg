/*
 * Copyright © 2019-2020 L2JOrg
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

import org.l2j.commons.database.annotation.Column;
import org.l2j.commons.database.annotation.NonUpdatable;
import org.l2j.commons.database.annotation.Table;
import org.l2j.gameserver.engine.siege.SiegeClanStatus;
import org.l2j.gameserver.model.actor.Npc;
import org.l2j.gameserver.model.actor.instance.SiegeFlag;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

/**
 * @author JoeAlisson
 */
@Table("siege_clans")
public class SiegeClanData {

    @NonUpdatable
    private final Set<Npc> flags = ConcurrentHashMap.newKeySet();

    @Column("castle_id")
    private int castleId;

    @Column("clan_id")
    private int clanId;

    @Column("recruit_mercenary")
    private boolean recruitingMercenary;

    @Column("mercenary_reward")
    private long mercenaryReward;

    private SiegeClanStatus status;

    public SiegeClanData() {
    }

    public SiegeClanData(int id, SiegeClanStatus status, int castleId) {
        this.clanId = id;
        this.status = status;
        this.castleId = castleId;
    }

    public Set<Npc> getFlags() {
        return flags;
    }

    public boolean removeFlag(Npc flag) {
        if (isNull(flag)) {
            return false;
        }

        flag.deleteMe();

        return flags.remove(flag);
    }

    public void removeFlags() {
        flags.forEach(this::removeFlag);
    }

    public void addFlag(SiegeFlag siegeFlag) {
        flags.add(siegeFlag);
    }

    public int getNumFlags() {
        return flags.size();
    }

    public int getCastleId() {
        return castleId;
    }

    public int getClanId() {
        return clanId;
    }

    public SiegeClanStatus getStatus() {
        return status;
    }

    public void setStatus(SiegeClanStatus status) {
        this.status = status;
    }

    public void setMercenaryReward(long reward) {
        this.mercenaryReward = reward;
    }

    public long getMercenaryReward() {
        return mercenaryReward;
    }

    public void setRecruitingMercenary(boolean recruiting) {
        this.recruitingMercenary = recruiting;
    }

    public boolean isRecruitingMercenary() {
        return recruitingMercenary;
    }
}
