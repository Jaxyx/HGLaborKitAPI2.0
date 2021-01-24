package de.hglabor.plugins.kitapi.kit.kits;

import de.hglabor.plugins.kitapi.kit.AbstractKit;
import de.hglabor.plugins.kitapi.kit.KitManager;
import de.hglabor.plugins.kitapi.player.KitPlayer;
import org.bukkit.Material;

import java.util.Random;

public class SurpriseKit extends AbstractKit {
    public final static SurpriseKit INSTANCE = new SurpriseKit();

    private SurpriseKit() {
        super("Surprise", Material.PUFFERFISH);
    }

    @Override
    public void enable(KitPlayer kitplayer) {
        AbstractKit randomKit = NoneKit.getInstance();
        int kitSlot = 0;
        for (AbstractKit kit : kitplayer.getKits()) {
            if (kit.equals(this)) {
                int randomNumber = new Random().nextInt(KitManager.getInstance().getEnabledKits().size());
                int i = 0;
                for (AbstractKit enabledKit : KitManager.getInstance().getEnabledKits()) {
                    if (i == randomNumber) {
                        randomKit = enabledKit;
                    }
                    i++;
                }
                kitplayer.getKits().set(kitSlot, randomKit);
                KitManager.getInstance().giveKitItemsIfSlotEmpty(kitplayer, randomKit);
                randomKit.enable(kitplayer);
            }
            kitSlot++;
        }
    }
}