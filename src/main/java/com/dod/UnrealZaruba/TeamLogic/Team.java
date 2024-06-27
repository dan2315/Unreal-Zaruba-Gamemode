package com.dod.UnrealZaruba.TeamLogic;


import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;


public class Team {
    BlockPos spawn;
    List<Player> members = new ArrayList<>();

    public Team(BlockPos spawn)
    {
        
    }

    public void Assign(Player player)
    {
        if (spawn == null) {
            player.sendMessage(new TextComponent("Скажи Доду, что он забыл спавн поставить))"), player.getUUID());
        }
        else {
            members.add(player);
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
        }
        
    }

    public void TryRemove(Player player)
    {
        if (members.contains(player)) {
            members.remove(player);
        }
    }

    public int MembersCount()
    {
        return members.size();
    }

    public void SetSpawn(BlockPos pos)
    {
        spawn = pos;
    }
}
