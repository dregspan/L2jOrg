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
package org.l2j.scripts.handlers.communityboard;

import org.l2j.gameserver.Config;
import org.l2j.gameserver.cache.HtmCache;
import org.l2j.gameserver.handler.IParseBoardHandler;
import org.l2j.gameserver.model.actor.instance.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Thoss
 **/
public class AutoHpMpCp implements IParseBoardHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeBoard.class);
    private static final String[] CMD =  new String[] { "_bbsautohpmpcp" };

    private static Map<Integer, List<String>> _listenedPlayer = new ConcurrentHashMap<>();

    private AutoHpMpCp() {

    }

    @Override
    public boolean parseCommunityBoardCommand(String command, StringTokenizer tokens, Player player) {
        String subCommand = tokens.nextToken();

        if(subCommand.equalsIgnoreCase("autocp")) {
            processCommand(player, "autocp", tokens);
        }
        else if(subCommand.equalsIgnoreCase("autohp")) {
            processCommand(player, "autohp", tokens);
        }
        else if(subCommand.equalsIgnoreCase("automp")) {
            processCommand(player, "automp", tokens);
        }

        final String customPath = Config.CUSTOM_CB_ENABLED ? "Custom/" : "";
        HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + customPath + "home.html");
        return true;
    }

    private static int getPercentByCommand(Player activeChar, String command) {
        switch (command) {
            case "autocp" -> { return activeChar.getAutoCp(); }
            case "autohp" -> { return activeChar.getAutoHp(); }
            case "automp" -> { return activeChar.getAutoMp(); }
        }

        return 0;
    }

    private static void setPercentByCommand(Player activeChar, String command, int percent)  {
        switch (command) {
            case "autocp" -> activeChar.setAutoCp(percent);
            case "autohp" -> activeChar.setAutoHp(percent);
            case "automp" -> activeChar.setAutoMp(percent);
        }
    }

    private static String processCommand(Player activeChar, String command, StringTokenizer params) {
        if(getPercentByCommand(activeChar, command) > 0) {
            List<String> listenedKeys = _listenedPlayer.get(activeChar.getObjectId());
            if (listenedKeys == null) {
                LOGGER.error("Try to stop non existent command {} for {}", activeChar, command);
                return "";
            }
            _listenedPlayer.get(activeChar.getObjectId()).remove(command);

            setPercentByCommand(activeChar, command, 0);
            activeChar.sendMessage(command + " system disabled.");
        } else {
            int percent;
            try {
                percent = Math.min(99, Integer.parseInt(params.nextToken()));
            } catch(NumberFormatException e) {
                activeChar.sendMessage("Incorrect number");
                return "";
            }
            if(percent <= 0) {
                activeChar.sendMessage("You can not specify zero or negative value!");
                return "";
            }

            List<String> listenedKeys = _listenedPlayer.get(activeChar.getObjectId());
            if (listenedKeys == null)
                _listenedPlayer.put(activeChar.getObjectId(), new ArrayList<>());
            _listenedPlayer.get(activeChar.getObjectId()).add(command);

            setPercentByCommand(activeChar, command, percent);
            activeChar.sendMessage("You have enabled an " + command + " recovery. Your xP will automatically recover at a value of " + percent + "% or less.");
        }
        return "";
    }

    public void onPlayerCpChange(Player player) {
        if(player.isDead())
            return;

        List<String> listenedKeys = _listenedPlayer.get(player.getObjectId());
        if(listenedKeys == null || !listenedKeys.contains("autocp"))
            return;

        int percent = player.getAutoCp();
        int currentPercent = (int) (player.getCurrentCp() / (player.getMaxCp() / 100.));
        if(percent <= 0 || currentPercent <= 0 || currentPercent > percent)
            return;

        LOGGER.info("onPlayerCpChange for {}", player);
    }

    public void onPlayerHpChange(Player player) {
        if(player.isDead())
            return;

        List<String> listenedKeys = _listenedPlayer.get(player.getObjectId());
        if(listenedKeys == null || !listenedKeys.contains("autohp"))
            return;

        int percent = player.getAutoHp();
        int currentPercent = (int) (player.getCurrentHp() / (player.getMaxHp() / 100.));
        if(percent <= 0 || currentPercent <= 0 || currentPercent > percent)
            return;

        LOGGER.info("onPlayerHpChange for {}", player);
    }

    public void onPlayerMpChange(Player player) {
        if(player.isDead())
            return;

        List<String> listenedKeys = _listenedPlayer.get(player.getObjectId());
        if(listenedKeys == null || !listenedKeys.contains("automp"))
            return;

        int percent = player.getAutoMp();
        int currentPercent = (int) (player.getCurrentMp() / (player.getMaxMp() / 100.));
        if(percent <= 0 || currentPercent <= 0 || currentPercent > percent)
            return;

        LOGGER.info("onPlayerMpChange for {}", player);

        // TODO: Find item in player inventory that are instant heal (not over time) and trigger 1
        // TODO: Compare all player effects to item with effect in his inventory matching EffectType.MANAHEAL_BY_LEVEL || EffectType.MANAHEAL_PERCENT
        // TODO: If 1 effect is matching then do nothing (item in process)
        // TODO: if no effect currently, then trigger use an item with heal over time

        /*
        Item effectedItem = null;
        int effectedItemPower = 0;

        Item instantItem = null;
        int instantItemPower = 0;

        final EffectList playerEffects = player.getEffectList();
        loop: for(Item item : player.getInventory().getItems())
        {
            ItemSkillHolder skillEntry = item.getTemplate().getAllSkills().get(0);
            if(skillEntry == null)
                continue;

            Skill skill = skillEntry.getSkill();
            for(AbstractEffect et : skill.getEffects(EffectScope.GENERAL))
            {
                if(et.getEffectType() == EffectType.MANAHEAL_BY_LEVEL || et.getEffectType() == EffectType.MANAHEAL_PERCENT)
                {
                    for(BuffInfo effect : playerEffects.getEffects())
                    {
                        if(effect.getSkill() == skill)
                        {
                            // Dot not apply potion if another one already healing
                            effectedItem = null;
                            effectedItemPower = 0;
                            break loop;
                        }
                    }

                    if(!ItemFunctions.checkForceUseItem(player, item, false) || !ItemFunctions.checkUseItem(player, item, false))
                        continue loop;

                    int power = (int) et.getValue();
                    if(power > effectedItemPower)
                    {
                        if(skill.checkCondition(player, player, false, false, true, false, false))
                        {
                            effectedItem = item;
                            effectedItemPower = power;
                            continue loop;
                        }
                    }
                }
            }
        }

        loop: for(ItemInstance item : player.getInventory().getItems())
        {
            SkillEntry skillEntry = item.getTemplate().getFirstSkill();
            if(skillEntry == null)
                continue;

            if(!ItemFunctions.checkForceUseItem(player, item, false) || !ItemFunctions.checkUseItem(player, item, false))
                continue;

            Skill skill = skillEntry.getTemplate();
            for(EffectTemplate et : skill.getEffectTemplates(EffectUseType.NORMAL_INSTANT))
            {
                if(et.getEffectType() == EffectType.RestoreMP)
                {
                    int power = (int) et.getValue();
                    if(et.getParam().getBool("percent", false))
                        power = power * (int) (player.getMaxMp() / 100.);
                    if(power > instantItemPower)
                    {
                        if(skill.checkCondition(player, player, false, false, true, false, false))
                        {
                            instantItem = item;
                            instantItemPower = power;
                            continue loop;
                        }
                    }
                }
            }
        }

        if(instantItem != null)
            useItem(player, instantItem);

        if(effectedItem != null)
        {
            if(instantItemPower == 0 || percent >= (newMp + instantItemPower) / (player.getMaxMp() / 100.))
                useItem(player, effectedItem);
        }
        */
    }


    /*
    private static class ChangeCurrentCpListener extends OnPlayerCpChange {
        public ChangeCurrentCpListener(Player activeChar) {
            super(activeChar);
        }

        public void OnPlayerCpChange(Player player)
        {
            if(player.isDead())
                return;

            int percent = player.getVarInt("autocp", 0);
            int currentPercent = (int) (newCp / (player.getMaxCp() / 100.));
            if(percent <= 0 || currentPercent <= 0 || currentPercent > percent)
                return;

            ItemInstance effectedItem = null;
            int effectedItemPower = 0;

            ItemInstance instantItem = null;
            int instantItemPower = 0;

            final Collection<Abnormal> abnormals = player.getAbnormalList().values();
            loop: for(ItemInstance item : player.getInventory().getItems())
            {
                SkillEntry skillEntry = item.getTemplate().getFirstSkill();
                if(skillEntry == null)
                    continue;

                Skill skill = skillEntry.getTemplate();
                for(EffectTemplate et : skill.getEffectTemplates(EffectUseType.NORMAL))
                {
                    if(et.getEffectType() == EffectType.RestoreCP)
                    {
                        for(Abnormal abnormal : abnormals)
                        {
                            if(abnormal.getSkill() == skill)
                            {
                                for(Effect effect : abnormal.getEffects())
                                {
                                    if(effect.getEffectType() == EffectType.RestoreCP)
                                    {
                                        // Не хиляем, если уже наложена какая-либо хилка.
                                        effectedItem = null;
                                        effectedItemPower = 0;
                                        break loop;
                                    }
                                }
                            }
                        }

                        if(!ItemFunctions.checkForceUseItem(player, item, false) || !ItemFunctions.checkUseItem(player, item, false))
                            continue loop;

                        int power = (int) et.getValue();
                        if(power > effectedItemPower)
                        {
                            if(skill.checkCondition(player, player, false, false, true, false, false))
                            {
                                effectedItem = item;
                                effectedItemPower = power;
                                continue loop;
                            }
                        }
                    }
                }
            }

            loop: for(ItemInstance item : player.getInventory().getItems())
            {
                SkillEntry skillEntry = item.getTemplate().getFirstSkill();
                if(skillEntry == null)
                    continue;

                if(!ItemFunctions.checkForceUseItem(player, item, false) || !ItemFunctions.checkUseItem(player, item, false))
                    continue;

                Skill skill = skillEntry.getTemplate();
                for(EffectTemplate et : skill.getEffectTemplates(EffectUseType.NORMAL_INSTANT))
                {
                    if(et.getEffectType() == EffectType.RestoreCP)
                    {
                        int power = (int) et.getValue();
                        if(et.getParam().getBool("percent", false))
                            power = power * (int) (player.getMaxCp() / 100.);
                        if(power > instantItemPower)
                        {
                            if(skill.checkCondition(player, player, false, false, true, false, false))
                            {
                                instantItem = item;
                                instantItemPower = power;
                                continue loop;
                            }
                        }
                    }
                }
            }

            if(instantItem != null)
                useItem(player, instantItem);

            if(effectedItem != null)
            {
                if(instantItemPower == 0 || percent >= (newCp + instantItemPower) / (player.getMaxCp() / 100.))
                    useItem(player, effectedItem);
            }
        }
    }

    private static class ChangeCurrentHpListener implements OnChangeCurrentHpListener
    {
        @Override
        public void onChangeCurrentHp(Creature actor, double oldHp, double newHp)
        {
            if(!actor.isPlayer() || actor.isDead())
                return;

            Player player = actor.getPlayer();

            int percent = player.getVarInt("autohp", 0);
            int currentPercent = (int) (newHp / (player.getMaxHp() / 100.));
            if(percent <= 0 || currentPercent <= 0 || currentPercent > percent)
                return;

            ItemInstance effectedItem = null;
            int effectedItemPower = 0;

            ItemInstance instantItem = null;
            int instantItemPower = 0;

            final Collection<Abnormal> abnormals = player.getAbnormalList().values();
            loop: for(ItemInstance item : player.getInventory().getItems())
            {
                SkillEntry skillEntry = item.getTemplate().getFirstSkill();
                if(skillEntry == null)
                    continue;

                Skill skill = skillEntry.getTemplate();
                for(EffectTemplate et : skill.getEffectTemplates(EffectUseType.NORMAL))
                {
                    if(et.getEffectType() == EffectType.RestoreHP)
                    {
                        for(Abnormal abnormal : abnormals)
                        {
                            if(abnormal.getSkill() == skill)
                            {
                                for(Effect effect : abnormal.getEffects())
                                {
                                    if(effect.getEffectType() == EffectType.RestoreHP)
                                    {
                                        // Не хиляем, если уже наложена какая-либо хилка.
                                        effectedItem = null;
                                        effectedItemPower = 0;
                                        break loop;
                                    }
                                }
                            }
                        }

                        if(!ItemFunctions.checkForceUseItem(player, item, false) || !ItemFunctions.checkUseItem(player, item, false))
                            continue loop;

                        int power = (int) et.getValue();
                        if(power > effectedItemPower)
                        {
                            if(skill.checkCondition(player, player, false, false, true, false, false))
                            {
                                effectedItem = item;
                                effectedItemPower = power;
                                continue loop;
                            }
                        }
                    }
                }
            }

            loop: for(ItemInstance item : player.getInventory().getItems())
            {
                SkillEntry skillEntry = item.getTemplate().getFirstSkill();
                if(skillEntry == null)
                    continue;

                if(!ItemFunctions.checkForceUseItem(player, item, false) || !ItemFunctions.checkUseItem(player, item, false))
                    continue;

                Skill skill = skillEntry.getTemplate();
                for(EffectTemplate et : skill.getEffectTemplates(EffectUseType.NORMAL_INSTANT))
                {
                    if(et.getEffectType() == EffectType.RestoreHP)
                    {
                        int power = (int) et.getValue();
                        if(et.getParam().getBool("percent", false))
                            power = power * (int) (player.getMaxHp() / 100.);
                        if(power > instantItemPower)
                        {
                            if(skill.checkCondition(player, player, false, false, true, false, false))
                            {
                                instantItem = item;
                                instantItemPower = power;
                                continue loop;
                            }
                        }
                    }
                }
            }

            if(instantItem != null)
                useItem(player, instantItem);

            if(effectedItem != null)
            {
                if(instantItemPower == 0 || percent >= (newHp + instantItemPower) / (player.getMaxHp() / 100.))
                    useItem(player, effectedItem);
            }
        }
    }

    private static class PlayerEnterListener implements OnPlayerEnterListener
    {
        public void onPlayerEnter(Player player)
        {
            if(!Config.ALLOW_AUTOHEAL_COMMANDS)
                return;

            int percent = player.getVarInt("autocp", 0);
            if(percent > 0)
            {
                player.addListener(CHANGE_CURRENT_CP_LISTENER);
                player.sendMessage("You are using an automatic CP recovery. Your CP will automatically recover at a value of " + percent + "% or less.");
            }
            percent = player.getVarInt("autohp", 0);
            if(percent > 0)
            {
                player.addListener(CHANGE_CURRENT_HP_LISTENER);
                player.sendMessage("You are using an automatic HP recovery. Your HP will automatically recover at a value of " + percent + "% or less.");
            }
            percent = player.getVarInt("automp", 0);
            if(percent > 0)
            {
                player.addListener(CHANGE_CURRENT_MP_LISTENER);
                player.sendMessage("You are using an automatic MP recovery. Your MP will automatically recover at a value of " + percent + "% or less.");
            }
        }
    }

    private static void useItem(Player player, ItemInstance item)
    {
        // Запускаем в новом потоке, потому что итем может юзнуться несколько раз проигнорировав откат итема
        ThreadPoolManager.getInstance().execute(() -> player.useItem(item, false, false));
    }
    */

    public static IParseBoardHandler provider() {
        return new AutoHpMpCp();
    }

    @Override
    public String[] getCommunityBoardCommands() {
        return CMD;
    }
}