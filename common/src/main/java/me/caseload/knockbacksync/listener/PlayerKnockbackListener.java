package me.caseload.knockbacksync.listener;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import me.caseload.knockbacksync.Base;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import me.caseload.knockbacksync.player.PlatformPlayer;
import me.caseload.knockbacksync.player.PlayerData;

public abstract class PlayerKnockbackListener {

    public void onPlayerVelocity(PlatformPlayer victim, Vector3d velocity) {
        if (!Base.INSTANCE.getConfigManager().isToggled())
            return;

        User user = victim.getUser();
        if (user == null) return; // Prevent errors with players disconnecting while this is running (or with fake player?)

        PlayerData victimPlayerData = PlayerDataManager.getPlayerData(user);
        if (victimPlayerData == null)
            return;

        Vector3d hv = victimPlayerData.getHorizontalVelocity();
        Vector3d adjustedVelocity = hv != null ? new Vector3d(hv.getX(), velocity.getY(), hv.getZ()) : velocity;

        if (victimPlayerData.getNotNullPing() >= PlayerData.PING_OFFSET && victimPlayerData.isOffGroundSyncEnabled()) {
            adjustedVelocity = adjustedVelocity.withY(victimPlayerData.getCompensatedOffGroundVelocity());
        } else {
            adjustedVelocity = adjustedVelocity.withY(victimPlayerData.getVerticalVelocity());
        }

        victim.setVelocity(adjustedVelocity);
    }
}