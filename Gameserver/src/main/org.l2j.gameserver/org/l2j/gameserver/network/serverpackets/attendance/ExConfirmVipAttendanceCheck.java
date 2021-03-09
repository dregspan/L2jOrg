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
package org.l2j.gameserver.network.serverpackets.attendance;

import io.github.joealisson.mmocore.WritableBuffer;
import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;
import org.l2j.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExConfirmVipAttendanceCheck extends ServerPacket {
    private final boolean available;
    private final int index;
    private final boolean hasCafePoints;
    private final boolean isVip;

    public ExConfirmVipAttendanceCheck(boolean available, int index, boolean hasCafePoints, boolean isVip) {
        this.available = available;
        this.index = index;
        this.hasCafePoints = hasCafePoints;
        this.isVip = isVip;
    }

    @Override
    public void writeImpl(GameClient client, WritableBuffer buffer) {
        writeId(ServerExPacketId.EX_CONFIRM_VIP_ATTENDANCE_CHECK, buffer );
        buffer.writeByte(available);
        buffer.writeByte(index);
        buffer.writeInt(hasCafePoints);
        buffer.writeInt(isVip);
    }
}
