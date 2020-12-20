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
package org.l2j.gameserver.network.serverpackets;

import io.github.joealisson.mmocore.WritableBuffer;
import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;

/**
 * @author UnAfraid
 */
public class ExAlterSkillRequest extends ServerPacket {
    private final int _currentSkillId;
    private final int _nextSkillId;
    private final int _alterTime;

    public ExAlterSkillRequest(int currentSkill, int nextSkill, int alterTime) {
        _currentSkillId = currentSkill;
        _nextSkillId = nextSkill;
        _alterTime = alterTime;
    }

    @Override
    public void writeImpl(GameClient client, WritableBuffer buffer) {
        writeId(ServerExPacketId.EX_ALTER_SKILL_REQUEST, buffer );
        buffer.writeInt(_nextSkillId);
        buffer.writeInt(_currentSkillId);
        buffer.writeInt(_alterTime);
    }

}
