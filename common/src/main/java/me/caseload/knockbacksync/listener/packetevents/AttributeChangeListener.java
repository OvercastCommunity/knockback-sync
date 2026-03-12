package me.caseload.knockbacksync.listener.packetevents;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import me.caseload.knockbacksync.Base;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import me.caseload.knockbacksync.player.PlayerData;
import me.caseload.knockbacksync.util.MathUtil;

import java.util.UUID;

public class AttributeChangeListener extends PacketListenerAbstract {

    public static final UUID SPRINTING_MODIFIER_UUID =
            UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");

    final double defaultGravity = 0.08;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!Base.INSTANCE.getConfigManager().isToggled()) return;

        if (event.getPacketType() == PacketType.Play.Server.UPDATE_ATTRIBUTES) {

            WrapperPlayServerUpdateAttributes packet = new WrapperPlayServerUpdateAttributes(event);

            User user = event.getUser();
            if (!PlayerDataManager.containsPlayerData(user)) return;

            // Get the attributes from the packet
            for (WrapperPlayServerUpdateAttributes.Property property : packet.getProperties()) {
                // You can now check for specific attributes
                if (property.getAttribute().equals(Attributes.GRAVITY)) {
                    onPlayerGravityChange(user, calculateValueWithModifiers(property));
                } else if (property.getAttribute().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                    onPlayerKnockBackChange(user, calculateValueWithModifiers(property));
                }
            }
        }
    }

    public double calculateValueWithModifiers(WrapperPlayServerUpdateAttributes.Property property) {
        double baseValue = property.getValue();
        double additionSum = 0;
        double multiplyBaseSum = 0;
        double multiplyTotalProduct = 1.0;

        for (WrapperPlayServerUpdateAttributes.PropertyModifier modifier : property.getModifiers()) {
            switch (modifier.getOperation()) {
                case ADDITION:
                    additionSum += modifier.getAmount();
                    break;
                case MULTIPLY_BASE:
                    multiplyBaseSum += modifier.getAmount();
                    break;
                case MULTIPLY_TOTAL:
                    multiplyTotalProduct *= (1.0 + modifier.getAmount());
                    break;
            }
        }

        double newValue = (baseValue + additionSum) * (1 + multiplyBaseSum) * multiplyTotalProduct;
        return MathUtil.clamp(newValue, property.getAttribute().getMinValue(), property.getAttribute().getMaxValue());
    }

    // Yes this is not properly latency compensated, that would require including a proper simulation engine
    // Laggy players will just have to deal with being on the wrong gravity for a few hundred ms, too bad!
    public void onPlayerGravityChange(User user, double newGravity) {
        PlayerData playerData = PlayerDataManager.getPlayerData(user);
        if (playerData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
            playerData.setGravityAttribute(newGravity);
        } else {
            playerData.setGravityAttribute(defaultGravity);
        }
    }

    private void onPlayerKnockBackChange(User user, double newKnockbackResistance) {
        PlayerData playerData = PlayerDataManager.getPlayerData(user);
        playerData.setKnockbackResistanceAttribute(newKnockbackResistance);
    }
}