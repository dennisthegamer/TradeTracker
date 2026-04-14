package com.example.tradetracker.mixin;

import com.example.tradetracker.event.TradeEventHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public class MerchantMenuMixin {

    @Shadow @Final private Merchant merchant;
    @Shadow @Final private MerchantContainer slots;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void tradetracker$onTradeTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (!player.level().isClientSide()) return;

        MerchantOffer offer = slots.getActiveOffer();
        if (offer != null) {
            TradeEventHandler.onTradeCompleted(player, offer, merchant);
        }
    }
}
