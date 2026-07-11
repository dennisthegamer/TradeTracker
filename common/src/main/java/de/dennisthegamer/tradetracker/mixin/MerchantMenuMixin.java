package de.dennisthegamer.tradetracker.mixin;

import de.dennisthegamer.tradetracker.event.TradeEventHandler;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@SuppressWarnings("unused")
@Mixin(MerchantResultSlot.class)
public class MerchantMenuMixin {

    @Shadow @Final private MerchantContainer slots;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void tradetracker$onTradeTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (!player.level().isClientSide()) return;

        MerchantOffer offer = slots.getActiveOffer();
        if (offer == null) return;

        UUID villagerUuid = null;
        Merchant merchant = ((MerchantContainerAccessor) slots).getMerchant();
        if (merchant instanceof AbstractVillager villager) {
            villagerUuid = villager.getUUID();
        }

        TradeEventHandler.onTradeCompleted(offer, villagerUuid);
    }
}
