package de.dennisthegamer.tradetracker.render;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Direction indicators for villagers marked for tracking: rendered as a fixed block
 * of lines at a configurable screen anchor (default top center). Only the arrow
 * glyph and the distance change with the camera; the label itself does not move.
 */
public class TrackingArrowHud {

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final int PADDING = 2;
    private static final int SCREEN_MARGIN = 5;
    private static final int LINE_SPACING = 3;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;

        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled || !config.showDirectionArrow) return;

        List<TradeMemoryStore.VillagerRecord> marked = TradeMemoryStore.getInstance().getMarkedVillagers();
        if (marked.isEmpty()) return;

        String currentDimension = mc.level.dimension().identifier().toString();
        Font font = mc.font;

        List<String> lines = new ArrayList<>();
        for (TradeMemoryStore.VillagerRecord rec : marked) {
            if (!currentDimension.equals(rec.dimension)) continue;

            double dx = rec.x + 0.5 - mc.player.getX();
            double dz = rec.z + 0.5 - mc.player.getZ();
            int distance = (int) Math.sqrt(dx * dx + dz * dz);

            // Yaw the player would need to face the target (MC: yaw 0 = +Z, 90 = -X)
            double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double relative = Mth.wrapDegrees(targetYaw - mc.player.getYRot());

            int sector = Math.floorMod((int) Math.round(relative / 45.0), 8);
            lines.add(ARROWS[sector] + " " + name(rec) + " · " + distance + "m");
        }
        if (lines.isEmpty()) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int lineStep = font.lineHeight + PADDING * 2 + LINE_SPACING;
        int blockHeight = lines.size() * lineStep - LINE_SPACING;

        TradeTrackerConfig.ArrowPosition pos = config.getArrowPosition();
        int y = switch (verticalAnchor(pos)) {
            case 0 -> SCREEN_MARGIN + PADDING;
            case 1 -> (screenH - blockHeight) / 2 + PADDING;
            default -> screenH - SCREEN_MARGIN - blockHeight + PADDING;
        };

        int textColor = 0xFF000000 | (config.glowColor & 0xFFFFFF);
        for (String label : lines) {
            int textWidth = font.width(label);
            int x = switch (horizontalAnchor(pos)) {
                case 0 -> SCREEN_MARGIN + PADDING;
                case 1 -> (screenW - textWidth) / 2;
                default -> screenW - SCREEN_MARGIN - textWidth - PADDING;
            };

            graphics.fill(x - PADDING, y - PADDING,
                    x + textWidth + PADDING, y + font.lineHeight + PADDING, 0x66000000);
            graphics.text(font, label, x, y, textColor, true);

            y += lineStep;
        }
    }

    /** 0 = top, 1 = middle, 2 = bottom */
    private static int verticalAnchor(TradeTrackerConfig.ArrowPosition pos) {
        return switch (pos) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> 1;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> 2;
        };
    }

    /** 0 = left, 1 = center, 2 = right */
    private static int horizontalAnchor(TradeTrackerConfig.ArrowPosition pos) {
        return switch (pos) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> 1;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> 2;
        };
    }

    private static String name(TradeMemoryStore.VillagerRecord rec) {
        return rec.customTag != null ? rec.customTag : rec.profession;
    }
}
