package de.hglabor.plugins.kitapi.kit.kits;

import de.hglabor.plugins.kitapi.KitApi;
import de.hglabor.plugins.kitapi.kit.AbstractKit;
import de.hglabor.plugins.kitapi.kit.events.KitEvent;
import de.hglabor.plugins.kitapi.kit.settings.FloatArg;
import de.hglabor.plugins.kitapi.player.KitPlayer;
import de.hglabor.utils.noriskutils.RandomCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.function.Consumer;

public class GamblerKit extends AbstractKit implements Listener {
    public static final GamblerKit INSTANCE = new GamblerKit();
    @FloatArg(min = 0.0F)
    private final float cooldown;

    private final RandomCollection<RandomCollection<Consumer<Player>>> badLuckCollection;
    private final RandomCollection<RandomCollection<Consumer<Player>>> goodLuckCollection;
    private final String attributeKey;
    private final String gamblerAnimal;

    private GamblerKit() {
        super("Gambler", Material.OAK_BUTTON);
        setMainKitItem(getDisplayMaterial());
        cooldown = 30F;
        attributeKey = this.getName() + "Win";
        gamblerAnimal = this.getName() + "gamblerAnimal";
        badLuckCollection = new RandomCollection<>();
        goodLuckCollection = new RandomCollection<>();
        initRandomEffects();
    }

    @Override
    public void onDeactivation(KitPlayer kitPlayer) {
        GambleWin gambleWin = kitPlayer.getKitAttribute(attributeKey);
        if (gambleWin != null) {
            gambleWin.end();
        }
    }

    @Override
    public void onDisable(KitPlayer kitPlayer) {
        kitPlayer.getBukkitPlayer().ifPresent(player -> {
            for (Tameable tameable : player.getWorld().getEntitiesByClass(Tameable.class)) {
                if (tameable.getOwnerUniqueId() != null && tameable.getOwnerUniqueId().equals(player.getUniqueId())) {
                    if (tameable.hasMetadata(gamblerAnimal)) {
                        tameable.remove();
                    }
                }
            }
        });
    }

    @KitEvent
    @Override
    public void onPlayerRightClickKitItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        KitPlayer kitPlayer = KitApi.getInstance().getPlayer(player);
        int tick = 2;
        kitPlayer.activateKitCooldown(this);
        GambleWin gambleWin = new GambleWin(kitPlayer, player, 3, tick);
        kitPlayer.putKitAttribute(attributeKey, gambleWin);
        gambleWin.runTaskTimer(KitApi.getInstance().getPlugin(), 0, tick);
    }

    private void initRandomEffects() {
        int potionDauer = 10 * 20;

        RandomCollection<Consumer<Player>> badItems = new RandomCollection<>();
        badItems.add("§6Pumpkin Head", 0.75, p -> p.getInventory().setHelmet(new ItemStack(Material.PUMPKIN)));
        badItems.add("§6Pumpkin Head (trololol)", 0.075, p -> {
            ItemStack pumpkin = new ItemStack(Material.PUMPKIN);
            ItemMeta pumpkinMeta = pumpkin.getItemMeta();
            pumpkinMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            pumpkin.setItemMeta(pumpkinMeta);
            p.getInventory().setHelmet(new ItemStack(pumpkin));
        });
        badItems.add("§8Dirt", 1, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.DIRT, 16))));
        badItems.add("§8Dried Kelp", 1, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.DRIED_KELP_BLOCK))));

        RandomCollection<Consumer<Player>> cantBeClassifiedBad = new RandomCollection<>();
        //INSTANT DEATH
        cantBeClassifiedBad.add("§4§lInstant Death", 0.01, p -> p.teleport(new Location(p.getWorld(), p.getLocation().getX(), -1.0, p.getLocation().getZ())));

        //TODO MOBS could be classied as mobs lol
        cantBeClassifiedBad.add("§l§2Charged Creeper", 0.1, p -> {
            Creeper creeper = (Creeper) p.getWorld().spawnEntity(p.getLocation(), EntityType.CREEPER);
            creeper.setPowered(true);
        });
        cantBeClassifiedBad.add("§2Creeper", 0.25, p -> {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.CREEPER);
        });
        cantBeClassifiedBad.add("§0Wither", 0.001, p -> {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.WITHER);
        });

        //RANDOM TELEPORT
        cantBeClassifiedBad.add("Random Teleport", 1, p -> KitApi.getInstance().getRandomAlivePlayer().getBukkitPlayer().ifPresent(player -> p.teleport(player.getLocation())));

        //COORDS LEAK
        cantBeClassifiedBad.add("Coords Leak", 0.5, p -> {
            //TODO Localization
            Bukkit.broadcastMessage("§7" + p.getName() + " befindet sich bei §bX: " + p.getLocation().getX() + "Y: " + p.getLocation().getY() + "Z: " + p.getLocation().getZ());
        });

        //POTION EFFECTS
        RandomCollection<Consumer<Player>> badPotionEffects = new RandomCollection<>();
        badPotionEffects.add("§aPoison", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, potionDauer, 0)));
        badPotionEffects.add("§7Weakness", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, potionDauer, 0)));
        badPotionEffects.add("§dLevitation", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, potionDauer, 0)));
        badPotionEffects.add("§0Blindness", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, potionDauer, 0)));
        badPotionEffects.add("§8Slowness", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, potionDauer, 0)));
        badPotionEffects.add("§8Glowing", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, potionDauer, 0)));

        badLuckCollection.add(0.5, cantBeClassifiedBad);
        badLuckCollection.add(1, badPotionEffects);
        badLuckCollection.add(1, badItems);

        //GOOD EFFECTS
        RandomCollection<Consumer<Player>> goodPotionEffects = new RandomCollection<>();
        goodPotionEffects.add("§cStrength", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, potionDauer, 0)));
        goodPotionEffects.add("§eFire Resistance", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, potionDauer, 0)));
        goodPotionEffects.add("§3Damage Resistance", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, potionDauer, 0)));
        goodPotionEffects.add("§bSpeed", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, potionDauer, 0)));
        goodPotionEffects.add("Invisibility", 1, p -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, potionDauer, 0)));

        RandomCollection<Consumer<Player>> goodItems = new RandomCollection<>();

        goodItems.add("§6Wood", 0.7, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.OAK_PLANKS, 32))));

        goodItems.add("§7Elytra", 0.02, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.ELYTRA))));

        goodItems.add("§6Recraft", 0.6, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Arrays.asList(
                        new ItemStack(Material.RED_MUSHROOM, 16),
                        new ItemStack(Material.BROWN_MUSHROOM, 16),
                        new ItemStack(Material.BOWL, 16)
                )));
        goodItems.add("§9Water Bucket", 0.3, p -> KitApi.getInstance().giveKitItemsIfInvFull(KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.WATER_BUCKET))));

        goodItems.add("§2Spawn Egg", 0.1, p -> KitApi.getInstance().giveKitItemsIfInvFull(KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.CREEPER_SPAWN_EGG))));

        goodItems.add("$fIron Sword", 0.1, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.IRON_SWORD, 1))));

        goodItems.add("§6Golden Apple", 0.3, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.GOLDEN_APPLE, 1))));

        goodItems.add("§bDiamond Sword", 0.01, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Collections.singletonList(new ItemStack(Material.DIAMOND_SWORD, 1))));

        goodItems.add("§5End Crystal", 0.05, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Arrays.asList(
                        new ItemStack(Material.END_CRYSTAL, 1),
                        new ItemStack(Material.OBSIDIAN, 1)
                )));

        goodItems.add("§bDiamond Set", 0.001, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Arrays.asList(
                        new ItemStack(Material.DIAMOND_BOOTS, 1),
                        new ItemStack(Material.DIAMOND_LEGGINGS, 1),
                        new ItemStack(Material.DIAMOND_CHESTPLATE, 1),
                        new ItemStack(Material.DIAMOND_HELMET, 1),
                        new ItemStack(Material.DIAMOND_SWORD, 1)
                )));
        goodItems.add("§7Chain Set", 0.2, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Arrays.asList(
                        new ItemStack(Material.CHAINMAIL_BOOTS, 1),
                        new ItemStack(Material.CHAINMAIL_LEGGINGS, 1),
                        new ItemStack(Material.CHAINMAIL_CHESTPLATE, 1),
                        new ItemStack(Material.CHAINMAIL_HELMET, 1),
                        new ItemStack(Material.STONE_SWORD, 1)
                )));
        goodItems.add("§6Golden Set", 0.25, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Arrays.asList(
                        new ItemStack(Material.GOLDEN_BOOTS, 1),
                        new ItemStack(Material.GOLDEN_LEGGINGS, 1),
                        new ItemStack(Material.GOLDEN_CHESTPLATE, 1),
                        new ItemStack(Material.GOLDEN_HELMET, 1),
                        new ItemStack(Material.GOLDEN_SWORD, 1)
                )));

        goodItems.add("§3Coco Set", 0.2, p -> KitApi.getInstance().giveKitItemsIfInvFull(
                KitApi.getInstance().getPlayer(p), this,
                Arrays.asList(
                        new ItemStack(Material.COCOA_BEANS, 16),
                        new ItemStack(Material.JUNGLE_LOG, 4)
                )));

        RandomCollection<Consumer<Player>> cantBeClassified = new RandomCollection<>();
        cantBeClassified.add("§dDoggos", 1, p -> {
            Wolf wolf = (Wolf) p.getWorld().spawnEntity(p.getLocation(), EntityType.WOLF);
            wolf.setMetadata(gamblerAnimal, new FixedMetadataValue(KitApi.getInstance().getPlugin(), ""));
            wolf.setOwner(p);
        });
        cantBeClassified.add("§Mooshroom", 1, p -> p.getWorld().spawnEntity(p.getLocation(), EntityType.MUSHROOM_COW));
        cantBeClassified.add("§5Horse", 0.25, p -> {
            ZombieHorse horse = (ZombieHorse) p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE_HORSE);
            horse.setTamed(true);
            horse.setOwner(p);
            horse.setMetadata(gamblerAnimal, new FixedMetadataValue(KitApi.getInstance().getPlugin(), ""));
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        });


        goodLuckCollection.add(1, goodPotionEffects);
        goodLuckCollection.add(1, goodItems);
        goodLuckCollection.add(1, cantBeClassified);
    }

    @Override
    public float getCooldown() {
        return cooldown;
    }

    private class GambleWin extends BukkitRunnable {
        private final long END;
        private final Player player;
        private final KitPlayer kitPlayer;
        private final Random random;
        private final int tick;

        private boolean forceEnd;

        public GambleWin(KitPlayer kitPlayer, Player player, int gambleDuration, int tick) {
            this.player = player;
            this.kitPlayer = kitPlayer;
            this.random = new Random();
            this.tick = tick;
            this.END = System.currentTimeMillis() + gambleDuration * 1000L;
        }

        @Override
        public void run() {
            if (forceEnd) {
                return;
            }
            boolean goodOrBad = random.nextBoolean();
            RandomCollection<Consumer<Player>> randomCollection = goodOrBad ? GamblerKit.INSTANCE.goodLuckCollection.getRandom() : GamblerKit.INSTANCE.badLuckCollection.getRandom();
            Consumer<Player> randomEffect = randomCollection.getRandom();
            String name = randomCollection.getName(randomEffect);
            if (System.currentTimeMillis() >= END) {
                randomEffect.accept(player);
                player.sendTitle("", name, 0, 20, 0);
                player.playSound(player.getLocation(), goodOrBad ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_DONKEY_HURT, 0.8F, 1.0F);
                cancel();
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_BAMBOO_BREAK, 0.8F, 0.75F + random.nextFloat() / 2.0F);
                player.sendTitle("", name, 0, tick, 0);
            }
        }

        public void end() {
            kitPlayer.putKitAttribute(attributeKey, null);
            forceEnd = true;
            cancel();
        }
    }
}
